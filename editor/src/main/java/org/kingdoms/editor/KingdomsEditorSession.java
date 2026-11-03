package org.kingdoms.editor;

import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import okhttp3.*;
import okio.ByteString;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Type;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.SecureRandom;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@SuppressWarnings("NullableProblems")
public final class KingdomsEditorSession {
    private static final String BASE_URL = "https://editor.kingdomsx.com";
    public static final String MANIFEST_PATH = "kingdomsx-editor.json";
    public static final int SUPPORTED_PROTOCOL_VERSION = 1;

    private static final Pattern SCHEMATIC_FILE = Pattern.compile(
            "^(?:(.+)/)?schematics/(structures|turrets)/([^/]+)/([1-9]\\d*)\\.(?:schematic|schem)$",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern BUILDING_CONFIG = Pattern.compile(
            "^(?:(.+)/)?(Structures|Turrets)/([^/]+)\\.ya?ml$", Pattern.CASE_INSENSITIVE);
    private static final Pattern OUTPOST_PAGE = Pattern.compile(
            "^(?:(.+)/)?guis/[^/]+/structures/outpost/[1-9]\\d*\\.ya?ml$", Pattern.CASE_INSENSITIVE);

    private static final long MAX_YAML_BYTES = 10L * 1024 * 1024;
    private static final long MAX_SCHEMATIC_BYTES = 25L * 1024 * 1024;
    private static final long MAX_ARCHIVE_BYTES = 25L * 1024 * 1024;
    private static final long MAX_EXPANDED_BYTES = 100L * 1024 * 1024;
    private static final int MAX_ARCHIVE_FILES = 1_000;
    private static final int MAX_YAML_FILES = 500;
    private static final String EDITOR_SOCKET_PROTOCOL = "kingdomsx-editor-v1";

    private static final Gson GSON = new GsonBuilder().disableHtmlEscaping().create();
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    public static final String REMOTE_PAYLOAD_KIND_ORIGINAL = "original";
    public static final String REMOTE_PAYLOAD_KIND_RESULT = "result";

    private static boolean isYaml(String fileName) {
        fileName = fileName.toLowerCase(Locale.ENGLISH);
        return fileName.endsWith(".yml") || fileName.endsWith(".yaml");
    }

    private static boolean isZip(String fileName) {
        fileName = fileName.toLowerCase(Locale.ENGLISH);
        return fileName.endsWith(".zip") || fileName.endsWith(".7z");
    }

    public static WorkspaceSource loadWorkspaceSourceFromFolder(Path entryPath, Predicate<Path> visitPath) throws Exception {
        String entryName = entryPath.getFileName().toString();
        return buildWorkspaceSource(entryName + ".zip", readWorkspaceDirectory(entryPath, visitPath));
    }

    public static WorkspaceSource loadWorkspaceSource(Path entryPath) throws Exception {
        BasicFileAttributes attrs = Files.readAttributes(entryPath, BasicFileAttributes.class);
        String entryName = entryPath.getFileName().toString();

        if (attrs.isSymbolicLink()) {
            throw new IllegalArgumentException("The local session source cannot be a symbolic link.");
        }
        if (attrs.isDirectory()) {
            return buildWorkspaceSource(entryName + ".zip",
                    readWorkspaceDirectory(entryPath, null));
        }
        if (!attrs.isRegularFile()) {
            throw new IllegalArgumentException(
                    "The local session source must be a YAML file, ZIP archive, or directory.");
        }

        byte[] bytes = Files.readAllBytes(entryPath);

        if (isYaml(entryName)) {
            if (bytes.length == 0) {
                throw new IllegalArgumentException("The YAML file is empty.");
            }
            Map<String, byte[]> map = new LinkedHashMap<>();
            map.put(entryName, bytes);
            return buildWorkspaceSource(entryName + ".zip", map);
        }

        if (isZip(entryName)) {
            if (bytes.length > MAX_ARCHIVE_BYTES) {
                throw new IllegalArgumentException("The ZIP archive exceeds the 25 MiB editor limit.");
            }
            List<FileEntry> extracted = WorkspaceSession.extractWorkspaceArchive(bytes);
            Map<String, byte[]> map = new LinkedHashMap<>();
            for (FileEntry e : extracted) {
                map.put(e.path, e.bytes);
            }
            return buildWorkspaceSource(entryName, map);
        }

        throw new IllegalArgumentException(
                "The local session simulator accepts a Kingdoms data directory, ZIP archive, or one YAML file.");
    }

    public interface FileVisitorEntry {
        void visit(String relativePath, Path file) throws IOException;
    }

    private static Map<String, byte[]> readWorkspaceDirectory(Path root, Predicate<Path> fileTest) throws IOException {
        final Map<String, byte[]> entries = new LinkedHashMap<>();

        walkWorkspaceDirectory(root, fileTest, (relativePath, file) ->
                entries.put(relativePath, Files.readAllBytes(file)));

        return entries;
    }

    static void walkWorkspaceDirectory(Path root, Predicate<Path> fileTest, FileVisitorEntry visitor) throws IOException {
        Files.walkFileTree(root, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                if (attrs.isSymbolicLink()) {
                    throw new IOException("The session source contains a symbolic link: " + file);
                }
                if (!fileTest.test(file.toAbsolutePath().normalize())) return FileVisitResult.SKIP_SUBTREE;
                if (!attrs.isRegularFile()) {
                    return FileVisitResult.CONTINUE;
                }

                String relative = root.relativize(file).toString().replace('\\', '/');
                if (isKingdomsManagedFile(relative)) return FileVisitResult.CONTINUE;
                if (!isYaml(relative)
                        && !relative.toLowerCase(Locale.ENGLISH).matches(".*\\.(schematic|schem)$")) {
                    return FileVisitResult.CONTINUE;
                }
                visitor.visit(relative, file);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                if (attrs.isSymbolicLink()) {
                    throw new IOException("The session source contains a symbolic link: " + dir);
                }
                if (!fileTest.test(dir.toAbsolutePath().normalize())) return FileVisitResult.SKIP_SUBTREE;
                return FileVisitResult.CONTINUE;
            }
        });
    }

    public static final class WorkspaceSource {
        public final String name;
        final Map<String, byte[]> temporaryFileBytes;
        public final Map<String, String> files = new HashMap<>();
        public final int yamlCount;
        public final int schematicCount;
        public final EditorCapabilities capabilities;

        WorkspaceSource(String name, Map<String, byte[]> temporaryFileBytes, int yamlCount,
                        int schematicCount, EditorCapabilities capabilities) {
            this.name = name;
            this.temporaryFileBytes = temporaryFileBytes;
            this.yamlCount = yamlCount;
            this.schematicCount = schematicCount;
            this.capabilities = capabilities;
        }

        public void saveLatestHashes(Path output) throws IOException {
            List<Map<String, String>> fileContent = new ArrayList<>(files.size());

            for (Map.Entry<String, String> file : files.entrySet()) {
                String relativePath = file.getKey();

                Map<String, String> obj = new LinkedHashMap<>(2);

                obj.put("path", relativePath);
                obj.put("sha256", file.getValue());

                fileContent.add(obj);
            }

            String content = GSON.toJson(fileContent);
            Files.write(output, content.getBytes(StandardCharsets.UTF_8));
        }

        public static Map<String, String> readHashedFiles(Path hashesFile) throws IOException {
            byte[] bytes = Files.readAllBytes(hashesFile);

            String json = StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(bytes))
                    .toString();

            Type type = new TypeToken<List<Map<String, String>>>() {}.getType();
            List<Map<String, String>> hashes = GSON.fromJson(json, type);
            Map<String, String> map = new  LinkedHashMap<>(hashes.size());;

            for (Map<String, String> entry : hashes) {
                String path = entry.get("path");
                String sha256 = entry.get("sha256");

                map.put(path, sha256);
            }

            return map;
        }

        public static WorkspaceSource loadWorkspaceSource(Map<String, String> hashedFiles, ServerContract contract) throws IOException {
            WorkspaceSource source = new WorkspaceSource(
                    "Remote Contract",
                    null,
                    contract.yamlFiles.size(),
                    contract.schematicFiles.size(),
                    contract.capabilities
            );

            source.files.putAll(hashedFiles);

            return source;
        }
    }

    private static WorkspaceSource buildWorkspaceSource(String name, Map<String, byte[]> availableEntries) {
        if (availableEntries.containsKey(MANIFEST_PATH)) {
            throw new IllegalArgumentException(
                    "The " + name + " source already contains the reserved " + MANIFEST_PATH + " entry.");
        }

        List<Map.Entry<String, byte[]>> yamlEntries = availableEntries.entrySet().stream()
                .filter(e -> isYaml(e.getKey())
                        && !isKingdomsManagedFile(e.getKey()))
                .collect(Collectors.toList());

        if (yamlEntries.isEmpty()) {
            throw new IllegalArgumentException(
                    "The " + name + " source does not contain editable YAML files. " + availableEntries.keySet());
        }
        if (yamlEntries.size() > MAX_YAML_FILES) {
            throw new IllegalArgumentException(
                    "The " + name + " source contains more than " + MAX_YAML_FILES + " YAML files. " + availableEntries.size());
        }

        Map<String, String> yamlByIdentity = new LinkedHashMap<>();
        for (Map.Entry<String, byte[]> e : yamlEntries) {
            yamlByIdentity.put(identity(e.getKey()), e.getKey());
        }

        Map<String, SchematicLinkage> schematicDirectories = buildingCapabilities(yamlEntries);

        List<Map.Entry<String, byte[]>> schematicEntries = availableEntries.entrySet().stream()
                .filter(e -> {
                    Matcher m = SCHEMATIC_FILE.matcher(e.getKey());
                    if (!m.matches()) return false;
                    int lastSlash = e.getKey().lastIndexOf('/');
                    String dir = lastSlash >= 0 ? e.getKey().substring(0, lastSlash) : "";
                    return schematicDirectories.containsKey(identity(dir));
                })
                .collect(Collectors.toList());

        Map<String, String> outpostPageDirectories = new HashMap<>();
        for (Map.Entry<String, byte[]> e : yamlEntries) {
            Matcher m = OUTPOST_PAGE.matcher(e.getKey());
            if (m.matches()) {
                int lastSlash = e.getKey().lastIndexOf('/');
                String outpostDir = lastSlash >= 0 ? e.getKey().substring(0, lastSlash) : "";
                outpostPageDirectories.put(identity(outpostDir), outpostDir);
            }
        }

        Map<String, byte[]> entries = new HashMap<>(yamlEntries.size() + schematicEntries.size());
        for (Map.Entry<String, byte[]> e : yamlEntries) {
            entries.put(e.getKey(), e.getValue());
        }
        for (Map.Entry<String, byte[]> e : schematicEntries) {
            entries.put(e.getKey(), e.getValue());
        }

        validateSelectedEntries(entries, yamlByIdentity);

        return new WorkspaceSource(name, entries, yamlEntries.size(), schematicEntries.size(),
                new EditorCapabilities(schematicDirectories, outpostPageDirectories));
    }

    private static Map<String, SchematicLinkage> buildingCapabilities(List<Map.Entry<String, byte[]>> yamlEntries) {
        Map<String, SchematicLinkage> result = new HashMap<>();
        for (Map.Entry<String, byte[]> e : yamlEntries) {
            Matcher m = BUILDING_CONFIG.matcher(e.getKey());
            if (!m.matches()) continue;
            String prefix = m.group(1) != null ? m.group(1) : "";
            String family = m.group(2);
            String building = m.group(3);
            String directory = Stream.of(prefix, "schematics",
                            family.toLowerCase(Locale.ENGLISH), building)
                    .filter(s -> s != null && !s.isEmpty())
                    .collect(Collectors.joining("/"));
            result.put(identity(directory), new SchematicLinkage(directory, e.getKey()));
        }
        return result;
    }

    private static void validateSelectedEntries(Map<String, byte[]> entries,
                                                Map<String, String> yamlByIdentity) {
        if (entries.size() > MAX_ARCHIVE_FILES - 1) {
            throw new IllegalArgumentException(
                    "The selected server config ZIP contains more than "
                            + String.format("%,d", MAX_ARCHIVE_FILES - 1) + " files.");
        }

        long expandedBytes = 0;
        Set<String> identities = new HashSet<>();

        for (Map.Entry<String, byte[]> e : entries.entrySet()) {
            String entryPath = e.getKey();
            byte[] bytes = e.getValue();
            String entryIdentity = identity(entryPath);

            if (!identities.add(entryIdentity)) {
                throw new IllegalArgumentException(
                        "The session source contains " + entryPath + " more than once.");
            }
            if (isYaml(entryPath) && bytes.length > MAX_YAML_BYTES) {
                throw new IllegalArgumentException(entryPath + " exceeds the 10 MiB YAML limit.");
            }
            if (!isYaml(entryPath) && bytes.length > MAX_SCHEMATIC_BYTES) {
                throw new IllegalArgumentException(entryPath + " exceeds the 25 MiB schematic limit.");
            }
            expandedBytes += bytes.length;
        }

        if (expandedBytes > MAX_EXPANDED_BYTES) {
            throw new IllegalArgumentException(
                    "The selected server config ZIP exceeds the 100 MiB expanded limit.");
        }

        long yamlCountInEntries = entries.keySet().stream()
                .filter(KingdomsEditorSession::isYaml)
                .count();
        if (yamlByIdentity.size() != yamlCountInEntries) {
            throw new IllegalArgumentException(
                    "The session source contains configuration paths that differ only by letter case.");
        }
    }

    private static String identity(String value) {
        return value.toLowerCase(Locale.ENGLISH);
    }

    public static final class Session {
        public final String id;
        public final String key;
        public final String serverToken;
        public final ServerContract remoteContract; // type from your RemoteWorkspaceManifest module
        public final String linkSeed;
        public final String link;

        public Session(String id, String key, String serverToken, ServerContract remoteContract, String linkSeed) {
            this.id = id;
            this.key = key;
            this.serverToken = serverToken;
            this.remoteContract = remoteContract;
            this.link = BASE_URL + "/#s/v1/" + linkSeed;
            this.linkSeed = linkSeed;
        }

        public int getProtocol() {
            return remoteContract.protocol;
        }

        public void close() throws Exception {
            apiBytes(id, "", HTTPMethod.DELETE, authorization(serverToken), null);
        }

        @SuppressWarnings("deprecation")
        public void saveManifest(Path folder, boolean pretty) throws IOException {
            Path manifestPath = folder.resolve((pretty ? "readable-" : "") + MANIFEST_PATH);
            if (Files.exists(manifestPath)) return;

            if (!pretty) {
                Files.write(manifestPath, remoteContract.manifestBytes);
                return;
            }

            Gson GSON = new GsonBuilder()
                    .disableHtmlEscaping()
                    .setPrettyPrinting()
                    .create();

            String json = new String(remoteContract.manifestBytes, StandardCharsets.UTF_8);
            JsonElement element = new JsonParser().parse(json);
            Files.write(manifestPath, GSON.toJson(element).getBytes(StandardCharsets.UTF_8));
        }

        @Override
        public String toString() {
            return "Session{" +
                    "id='" + id + '\'' +
                    ", key='" + key + '\'' +
                    ", serverToken='" + serverToken + '\'' +
                    ", linkSeed='" + linkSeed + '\'' +
                    ", link='" + link + '\'' +
                    '}';
        }
    }

    public static byte[] loadManifest(Path folder) throws IOException {
        Path manifestPath = folder.resolve(MANIFEST_PATH);
        if (!Files.exists(manifestPath)) return null;
        return Files.readAllBytes(manifestPath);
    }

    public static ServerContract deserializeContract(
            Map<String, String> hashedFiles,
            String pluginVersion,
            byte[] manifestBytes
    ) throws CharacterCodingException {
        return RemoteWorkspaceManifest.finalizeContract(hashedFiles, pluginVersion, manifestBytes);
    }

    public static Session createSession(WorkspaceSource source, String pluginVersion) throws Exception {
        String linkSeed = secret(32);
        KingdomsEditorSecurity.RemoteEditorSecrets secrets =
                KingdomsEditorSecurity.deriveRemoteEditorSecrets(linkSeed);
        String serverToken = secret(32);

        List<Map<String, String>> files = new ArrayList<>();
        for (Map.Entry<String, byte[]> e : source.temporaryFileBytes.entrySet()) {
            Map<String, String> obj = new LinkedHashMap<>(2);
            String hash = KingdomsEditorSecurity.sha256Hex(e.getValue());

            obj.put("path", e.getKey());
            obj.put("sha256", hash);

            files.add(obj);
            source.files.put(e.getKey(), hash);
        }

        JsonObject manifestJson = new JsonObject();
        manifestJson.addProperty("protocol", SUPPORTED_PROTOCOL_VERSION);
        manifestJson.addProperty("pluginVersion", pluginVersion);
        manifestJson.add("files", GSON.toJsonTree(files));
        {
            JsonObject capabilities = new JsonObject();
            capabilities.add("schematicDirectories", GSON.toJsonTree(source.capabilities.schematicDirectories.values()));
            capabilities.add("outpostPageDirectories", GSON.toJsonTree(source.capabilities.outpostPageDirectories.values()));
            manifestJson.add("capabilities", capabilities);
        }
        byte[] manifest = GSON.toJson(manifestJson).getBytes(StandardCharsets.UTF_8);

        Map<String, byte[]> archiveEntries = new HashMap<>(source.temporaryFileBytes);
        archiveEntries.put(MANIFEST_PATH, manifest);

        byte[] archive = zip(archiveEntries);
        if (archive.length > MAX_ARCHIVE_BYTES) {
            throw new IllegalArgumentException("The selected server config ZIP exceeds the 25 MiB limit.");
        }

        ServerContract remoteContract =
                RemoteWorkspaceManifest.finalizeContract(pluginVersion, source, manifest);

        source.temporaryFileBytes.clear();
        byte[] encryptedOriginal = KingdomsEditorSecurity.encryptRemotePayload(archive,
                secrets.id, secrets.key, KingdomsEditorSecurity.RemotePayloadKind.ORIGINAL.getCode(), 0);

        // PUT session metadata
        JsonObject body = new JsonObject();
        body.addProperty("protocol", SUPPORTED_PROTOCOL_VERSION);
        body.addProperty("name", source.name);
        body.addProperty("pluginVersion", pluginVersion);
        body.addProperty("serverTokenHash", tokenHash(serverToken));
        body.addProperty("browserTokenHash", tokenHash(secrets.browserToken));

        JsonObject original = new JsonObject();
        original.addProperty("bytes", encryptedOriginal.length);
        original.addProperty("sha256", KingdomsEditorSecurity.sha256Hex(encryptedOriginal));
        body.add("original", original);

        apiJson(secrets.id, "", HTTPMethod.PUT,
                Collections.singletonMap("Content-Type", "application/json"),
                GSON.toJson(body).getBytes(StandardCharsets.UTF_8));

        // PUT original payload
        Map<String, String> payloadHeaders = payloadHeaders(serverToken, encryptedOriginal);
        apiJson(secrets.id, "payloads/original", HTTPMethod.PUT, payloadHeaders, encryptedOriginal);

        return new Session(secrets.id, secrets.key, serverToken, remoteContract, linkSeed);
    }

    public enum SessionState {
        AWAITING_ORIGINAL, READY,
        RESULT_READY, STARTED, APPLYING,
        APPLIED, FAILED, CANCELLED, EXPIRED, CONFLICTED,
        VALIDATION_REJECTED;

        public String apiCode() {
            return name().toLowerCase(Locale.ENGLISH);
        }

        public static SessionState of(String state) {
            try {
                return valueOf(state.toUpperCase(Locale.ENGLISH));
            } catch (IllegalArgumentException ex) {
                return null;
            }
        }
    }

    private enum HTTPMethod {
        GET, PUT, POST, DELETE
    }

    public static SessionState monitorSession(Session session, Path outputDirectory,
                                              KingdomsEditorListener editorListener,
                                              AtomicBoolean stopRequested) throws Exception {

        String wsOrigin = BASE_URL.replaceFirst("^http", "ws");
        String url = wsOrigin + "/socket/editor/v1/sessions/" + session.id;

        OkHttpClient client = new OkHttpClient.Builder()
                .readTimeout(0, java.util.concurrent.TimeUnit.MILLISECONDS)
                .connectTimeout(0, java.util.concurrent.TimeUnit.MILLISECONDS)
                .writeTimeout(0, java.util.concurrent.TimeUnit.MILLISECONDS)
                .pingInterval(30, java.util.concurrent.TimeUnit.SECONDS) // keep-alive, otherwise throws a EOFException
                .build();

        Request request = new Request.Builder()
                .url(url)
                .header("Sec-WebSocket-Protocol", EDITOR_SOCKET_PROTOCOL + ", kingdomsx-editor-cap.server." + session.serverToken)
                .build();

        final Object lock = new Object();
        final AtomicReference<Throwable> failure = new AtomicReference<>();
        final AtomicBoolean terminalReached = new AtomicBoolean(false);
        final AtomicReference<WebSocket> socketRef = new AtomicReference<>();
        final AtomicReference<SessionState> editorState = new AtomicReference<>();

        // Serialize message handling so applyRevision never runs concurrently
        final java.util.concurrent.ExecutorService processing =
                java.util.concurrent.Executors.newSingleThreadExecutor(r -> {
                    Thread t = new Thread(r, "editor-session-processor");
                    t.setDaemon(true);
                    return t;
                });

        WebSocketListener listener = new WebSocketListener() {
            @Override
            public void onOpen(WebSocket webSocket, Response response) {
                socketRef.set(webSocket);
                editorListener.debug("WebSocket opened: " + response);
            }

            @Override
            public void onMessage(WebSocket webSocket, String text) {
                processing.submit(() -> handleMessage(text, session, outputDirectory,
                        webSocket, terminalReached, failure, editorState, lock, editorListener));
            }

            @Override
            public void onMessage(WebSocket webSocket, ByteString bytes) {
                processing.submit(() -> handleMessage(bytes.utf8(), session, outputDirectory,
                        webSocket, terminalReached, failure, editorState, lock, editorListener));
            }

            @Override
            public void onClosing(WebSocket webSocket, int code, String reason) {
                editorListener.debug("onClosing " + code + " - " + reason);
                webSocket.close(1000, null);
            }

            @Override
            public void onClosed(WebSocket webSocket, int code, String reason) {
                synchronized (lock) {
                    if (!stopRequested.get() && !terminalReached.get() && failure.get() == null) {
                        failure.set(new IOException(
                                "The live editor session connection closed unexpectedly."));
                    }
                    editorListener.close(code, reason);
                    lock.notifyAll();
                }
            }

            @Override
            public void onFailure(WebSocket webSocket, Throwable cause, Response response) {
                synchronized (lock) {
                    if (failure.get() == null) {
                        failure.set(new EditorSocketException(
                                "Error during live editor session connection "
                                        + url + ": " + response,
                                cause,
                                (response == null ? 0 : response.code()),
                                url)
                        );
                    }
                    lock.notifyAll();
                }
            }
        };

        WebSocket socket = client.newWebSocket(request, listener);
        socketRef.set(socket);

        // Wait until stop, terminal state, or failure
        try {
            synchronized (lock) {
                while (!stopRequested.get() && !terminalReached.get() && failure.get() == null) {
                    lock.wait(500);
                }
            }

            if (stopRequested.get()) {
                WebSocket ws = socketRef.get();
                if (ws != null) {
                    ws.close(1000, "Local monitoring stopped");
                }
            }

            Throwable err = failure.get();
            if (err != null) {
                if (err instanceof Exception) {
                    throw (Exception) err;
                }
                throw new Exception(err);
            }
        } finally {
            editorListener.debug("Shutting down the monitor...");
            processing.shutdownNow();
            // noinspection resource
            client.dispatcher().executorService().shutdown();
            client.connectionPool().evictAll();
        }

        return editorState.get();
    }

    @SuppressWarnings("SynchronizationOnLocalVariableOrMethodParameter")
    private static void handleMessage(String text,
                                      Session session,
                                      Path outputDirectory,
                                      WebSocket webSocket,
                                      AtomicBoolean terminalReached,
                                      AtomicReference<Throwable> failure,
                                      AtomicReference<SessionState> editorState,
                                      Object lock,
                                      KingdomsEditorListener editorListener) {
        try {
            editorListener.debug("Received message: " + text);
            JsonObject snapshot = parseJson(text).getAsJsonObject();

            if (!snapshot.has("type") || !"session".equals(snapshot.get("type").getAsString())
                    || !snapshot.has("protocol") || snapshot.get("protocol").getAsInt() != SUPPORTED_PROTOCOL_VERSION) {
                throw new UnsupportedOperationException("Received an unknown response from the server " + snapshot);
            }

            String state = snapshot.has("state") ? snapshot.get("state").getAsString() : "";
            SessionState msgState = SessionState.of(state);
            if (msgState == null) throw new IllegalArgumentException("Unknown message state: '" + state + "'");

            switch (msgState) {
                case READY:
                    editorListener.onReady();
                    break;

                case RESULT_READY: {
                    int revision = snapshot.get("resultRevision").getAsInt();
                    applyRevision(session, revision, outputDirectory, editorListener);
                    break;
                }

                case APPLYING:
                case APPLIED:
                case VALIDATION_REJECTED:
                case CONFLICTED:
                    // No need to acknowledge.
                    editorState.set(msgState);
                    break;

                case CANCELLED:
                case EXPIRED: {
                    editorState.set(msgState);
                    terminalReached.set(true);
                    webSocket.close(1000, "Editor session ended");
                    synchronized (lock) {
                        lock.notifyAll();
                    }
                    break;
                }

                case AWAITING_ORIGINAL: // We don't even receive this because we don't run the socket before sending the original.
                default:
                    throw new UnsupportedOperationException("Unexpected response from server: " + state + " -> " + snapshot);
            }
        } catch (Throwable t) {
            webSocket.close(1011, "Local apply failed");
            failure.set(t);
            synchronized (lock) {
                lock.notifyAll();
            }
        }
    }

    private static void applyRevision(Session session, int revision,
                                      Path outputDirectory,
                                      KingdomsEditorListener editorListener) throws Exception {
        JsonObject snapshot = apiJson(session.id, "", HTTPMethod.GET,
                authorization(session.serverToken), null);

        if (!"result_ready".equals(snapshot.get("state").getAsString())
                || snapshot.get("resultRevision").getAsInt() != revision) {
            return;
        }

        apiState(session, revision, SessionState.STARTED, null);

        try {
            byte[] encrypted = apiBytes(session.id, "payloads/result/" + revision, HTTPMethod.GET,
                    authorization(session.serverToken), null);

            byte[] archive = KingdomsEditorSecurity.decryptRemotePayload(encrypted,
                    session.id, session.key,
                    KingdomsEditorSecurity.RemotePayloadKind.RESULT.getCode(), revision);

            List<FileEntry> entries = WorkspaceSession.extractWorkspaceArchive(archive);
            validateResult(entries, session);

            Path revisionDirectory = outputDirectory.resolve("revisions").resolve(String.valueOf(revision));
            Files.createDirectories(revisionDirectory);

            for (FileEntry entry : entries) {
                if (MANIFEST_PATH.equals(entry.path)) continue;
                Path destination = revisionDirectory;
                for (String part : entry.path.split("/")) {
                    destination = destination.resolve(part);
                }
                Files.createDirectories(destination.getParent());

                if (Files.exists(destination)) {
                    throw new FileAlreadyExistsException(destination.toString());
                }

                Files.write(destination, entry.bytes);
            }

            boolean successful = editorListener.applyRevision(revisionDirectory, entries);
            apiState(session, revision, successful ? SessionState.APPLIED : SessionState.CONFLICTED, null);
        } catch (Exception error) {
            try {
                apiState(session, revision, SessionState.FAILED, error.getMessage());
            } catch (Exception ignored) {
            }
            throw error;
        }
    }

    private static void validateResult(List<FileEntry> entries, Session session) {
        Map<String, byte[]> map = new HashMap<>();
        for (FileEntry e : entries) {
            map.put(e.path, e.bytes);
        }
        RemoteWorkspaceManifest.validateRemoteWorkspaceOutput(session.remoteContract, map);
    }

    // -------------------------------------------------------------------------
    // HTTP helpers
    // -------------------------------------------------------------------------

    private static void apiState(Session session, int revision,
                                 SessionState outcome, String failureCode) throws Exception {
        JsonObject body = new JsonObject();
        body.addProperty("revision", revision);
        body.addProperty("outcome", outcome.apiCode());
        if (failureCode != null) {
            body.addProperty("failureCode", failureCode);
        }
        Map<String, String> headers = new LinkedHashMap<>(authorization(session.serverToken));
        headers.put("Content-Type", "application/json");
        apiJson(session.id, "apply", HTTPMethod.POST, headers,
                GSON.toJson(body).getBytes(StandardCharsets.UTF_8));
    }

    private static Map<String, String> authorization(String token) {
        Map<String, String> h = new LinkedHashMap<>();
        h.put("Authorization", "KingdomsX-Editor " + token);
        return h;
    }

    private static Map<String, String> payloadHeaders(String token, byte[] bytes) throws Exception {
        Map<String, String> h = new LinkedHashMap<>(authorization(token));
        h.put("Content-Type", "application/octet-stream");
        h.put("X-KingdomsX-Payload-Bytes", String.valueOf(bytes.length));
        h.put("X-KingdomsX-Payload-Sha256", KingdomsEditorSecurity.sha256Hex(bytes));
        return h;
    }

    private static JsonObject apiJson(String id, String route, HTTPMethod method,
                                      Map<String, String> headers, byte[] body) throws Exception {
        byte[] responseBody = apiBytes(id, route, method, headers, body);
        if (responseBody.length == 0) return new JsonObject();
        return parseJson(responseBody).getAsJsonObject();
    }

    private static byte[] apiBytes(String id, String route, HTTPMethod method,
                                   Map<String, String> headers, byte[] body) throws Exception {
        String url = KingdomsEditorSession.BASE_URL + "/api/editor/v1/sessions/" + id
                + (route == null || route.isEmpty() ? "" : "/" + route);

        HttpURLConnection conn = null;
        try {
            conn = (HttpURLConnection) new URL(url).openConnection();
            conn.setRequestMethod(method.name());
            conn.setConnectTimeout(15_000);
            conn.setReadTimeout(60_000);
            conn.setRequestProperty("Accept", "application/json");
            if (headers != null) {
                for (Map.Entry<String, String> h : headers.entrySet()) {
                    conn.setRequestProperty(h.getKey(), h.getValue());
                }
            }
            if (body != null) {
                conn.setDoOutput(true);
                try (OutputStream os = conn.getOutputStream()) {
                    os.write(body);
                }
            }

            int status = conn.getResponseCode();
            InputStream is = status >= 400 ? conn.getErrorStream() : conn.getInputStream();
            byte[] response = is == null ? new byte[0] : readAll(is);

            if (status >= 200 && status < 300) {
                return response;
            }

            String message = "The local editor Worker returned HTTP " + status + ".";
            try {
                JsonObject err = parseJson(response)
                        .getAsJsonObject();
                if (err.has("message")) {
                    message = err.get("message").getAsString();
                }
            } catch (Exception ignored) {
            }
            throw new IOException(message);
        } catch (IOException e) {
            if (e.getMessage() != null && e.getMessage().startsWith("The local editor Worker")) {
                throw e;
            }
            throw new IOException(
                    "Could not reach the editor Worker at " + KingdomsEditorSession.BASE_URL, e);
        } finally {
            if (conn != null) conn.disconnect();
        }
    }

    private static JsonElement parseJson(byte[] response) {
        return parseJson(new String(response, StandardCharsets.UTF_8));
    }

    @SuppressWarnings("deprecation")
    private static JsonElement parseJson(String text) {
        return new JsonParser().parse(text);
    }

    private static byte[] readAll(InputStream is) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        byte[] buf = new byte[8192];
        int n;
        while ((n = is.read(buf)) != -1) {
            bos.write(buf, 0, n);
        }
        return bos.toByteArray();
    }

    // -------------------------------------------------------------------------
    // Utilities
    // -------------------------------------------------------------------------

    public static Path createOutputDirectory(Path parent, String linkSeed) throws IOException {
        return Files.createDirectories(parent.resolve(linkSeed));
    }

    private static String tokenHash(String token) {
        return KingdomsEditorSecurity.sha256Hex(token.getBytes(StandardCharsets.UTF_8));
    }

    @SuppressWarnings("SameParameterValue")
    private static String secret(int bytes) {
        byte[] buf = new byte[bytes];
        SECURE_RANDOM.nextBytes(buf);
        return base64Url(buf);
    }

    private static String base64Url(byte[] data) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(data);
    }

    private static boolean isKingdomsManagedFile(String fileName) {
        return "globals.yml".equals(fileName.replace('\\', '/').replaceAll(".*/", "").toLowerCase(Locale.ENGLISH));
    }

    public static byte[] zip(Map<String, byte[]> entries) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();

        try (ZipOutputStream zip = new ZipOutputStream(output)) {
            zip.setLevel(6);

            for (Map.Entry<String, byte[]> entry : entries.entrySet()) {
                zip.putNextEntry(new ZipEntry(entry.getKey()));
                zip.write(entry.getValue());
                zip.closeEntry();
            }
        }

        return output.toByteArray();
    }
}