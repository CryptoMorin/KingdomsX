package org.kingdoms.editor;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class RemoteWorkspaceManifest {
    private RemoteWorkspaceManifest() {}

    private static final String MANIFEST = "kingdomsx-editor.json";

    private static final Pattern YAML_FILE =
            Pattern.compile("\\.ya?ml$", Pattern.CASE_INSENSITIVE);

    private static final Pattern SCHEMATIC_FILE =
            Pattern.compile("\\.(?:schematic|schem)$", Pattern.CASE_INSENSITIVE);

    private static final Pattern SCHEMATIC_DIRECTORY =
            Pattern.compile(
                    "^(?:(.+)/)?schematics/(structures|turrets)/([^/]+)$",
                    Pattern.CASE_INSENSITIVE
            );

    private static final Pattern SCHEMATIC_LEVEL_FILE =
            Pattern.compile(
                    "^([1-9]\\d*)\\.(?:schematic|schem)$",
                    Pattern.CASE_INSENSITIVE
            );

    private static final Pattern OUTPOST_PAGE_DIRECTORY =
            Pattern.compile(
                    "^(?:(.+)/)?guis/[^/]+/structures/outpost$",
                    Pattern.CASE_INSENSITIVE
            );

    private static final Pattern OUTPOST_PAGE_FILE =
            Pattern.compile("^[1-9]\\d*\\.ya?ml$", Pattern.CASE_INSENSITIVE);

    public static ServerContract finalizeContract(
            String pluginVersion,
            KingdomsEditorSession.WorkspaceSource source,
            byte[] manifest
    ) {

        Map<String, String> files = declaredFiles(source.files);
        Map<String, String> yamlFiles = filterFiles(files, YAML_FILE);
        Map<String, String> schematicFiles = filterFiles(files, SCHEMATIC_FILE);

        if (yamlFiles.isEmpty()) {
            throw new IllegalArgumentException(
                    "The server did not include any editable YAML files."
            );
        }

        EditorCapabilities capabilities = source.capabilities;

        ServerContract contract = new ServerContract(
                KingdomsEditorSession.SUPPORTED_PROTOCOL_VERSION,
                pluginVersion,
                files,
                yamlFiles,
                schematicFiles,
                capabilities,
                manifest
        );

        validateRemoteEntries(contract, source.files.keySet());

        return contract;
    }

    public static ServerContract finalizeContract(
            Map<String, String> pathHashFiles,
            String pluginVersion,
            byte[] manifestBytes
    ) throws CharacterCodingException {
        String source = StandardCharsets.UTF_8.newDecoder()
                .onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT)
                .decode(ByteBuffer.wrap(manifestBytes))
                .toString();

        JsonElement parsed = new JsonParser().parse(source);
        if (!parsed.isJsonObject()) {
            throw new IllegalArgumentException("Expected JSON object.");
        }

        JsonObject manifest = parsed.getAsJsonObject();
        JsonElement protocol = manifest.get("protocol");
        if (protocol == null || !protocol.isJsonPrimitive()
                || !protocol.getAsJsonPrimitive().isNumber()
                || !isExpectedProtocol(protocol, KingdomsEditorSession.SUPPORTED_PROTOCOL_VERSION)) {
            throw new IllegalArgumentException("The server sent editor data in a format that is not supported.");
        }

        JsonElement versionElement = manifest.get("pluginVersion");
        if (!isString(versionElement) || versionElement.getAsString().trim().isEmpty()) {
            throw new IllegalArgumentException("The server did not include its KingdomsX version.");
        }

        Map<String, String> files = declaredFiles(pathHashFiles);
        Map<String, String> yamlFiles = filterFiles(files, YAML_FILE);
        Map<String, String> schematicFiles = filterFiles(files, SCHEMATIC_FILE);

        if (yamlFiles.isEmpty()) {
            throw new IllegalArgumentException(
                    "The server did not include any editable YAML files."
            );
        }
        EditorCapabilities capabilities = declaredCapabilities(manifest.get("capabilities"), yamlFiles);
        for (String path : schematicFiles.keySet()) {
            if (!remoteSchematicPathEditable(capabilities, path)) {
                throw new IllegalArgumentException("The server did not mark the schematic folder for " + path + " as editable.");
            }
        }

        ServerContract contract = new ServerContract(
                KingdomsEditorSession.SUPPORTED_PROTOCOL_VERSION,
                pluginVersion,
                files,
                yamlFiles,
                schematicFiles,
                capabilities,
                manifestBytes
        );

        validateRemoteEntries(contract, pathHashFiles.keySet());

        return contract;
    }

    // ---------------------------------------------------------------------
    // Validate files before writing workspace output
    // ---------------------------------------------------------------------

    public static void validateRemoteWorkspaceOutput(
            ServerContract contract,
            Map<String, byte[]> entries
    ) {
        if (contract == null) return;
        validateRemoteEntries(contract, entries.keySet());

        if (!equalBytes(
                entries.get(MANIFEST),
                contract.manifestBytes
        )) {
            throw new IllegalArgumentException(
                    "The manifest file was changed"
            );
        }
    }

    // ---------------------------------------------------------------------
    // Check whether schematic and outpost paths are editable
    // ---------------------------------------------------------------------

    public static boolean remoteSchematicPathEditable(
            EditorCapabilities contract,
            String path
    ) {
        if (contract == null) {
            return true;
        }

        String value = path == null ? "" : path;
        String normalized = value.replaceFirst(
                "/[1-9]\\d*\\.(?:schematic|schem)$",
                ""
        );

        normalized = manifestPath(normalized);

        return contract.schematicDirectories
                .containsKey(identity(normalized));
    }

    public static boolean remoteOutpostPagePathEditable(
            ServerContract contract,
            String path
    ) {
        if (contract == null) {
            return true;
        }

        String normalized = manifestPath(path);
        int separator = normalized.lastIndexOf('/');

        if (separator < 0) {
            return false;
        }

        String filename = normalized.substring(separator + 1);

        if (!OUTPOST_PAGE_FILE.matcher(filename).matches()) {
            return false;
        }

        String directory = normalized.substring(0, separator);

        return contract.capabilities.outpostPageDirectories.containsKey(identity(directory));
    }

    // ---------------------------------------------------------------------
    // File declarations
    // ---------------------------------------------------------------------

    private static Map<String, String> declaredFiles(Map<String, String> nameHashEntries) {
        Map<String, String> files = new LinkedHashMap<>();
        Set<String> identities = new HashSet<>();

        for (Map.Entry<String, String> entry : nameHashEntries.entrySet()) {
            String path = manifestPath(entry.getKey());

            if ((!YAML_FILE.matcher(path).find()
                    && !SCHEMATIC_FILE.matcher(path).find())
                    || path.equals(MANIFEST)) {
                throw invalidEditorFile();
            }

            String pathIdentity = identity(path);

            if (!identities.add(pathIdentity)) {
                throw new IllegalArgumentException(
                        "The server listed " + path + " more than once."
                );
            }

            files.put(path, entry.getValue());
        }

        return files;
    }

    private static Map<String, String> filterFiles(
            Map<String, String> files,
            Pattern pattern
    ) {
        Map<String, String> filtered = new LinkedHashMap<>();

        for (Map.Entry<String, String> entry : files.entrySet()) {
            if (pattern.matcher(entry.getKey()).find()) {
                filtered.put(entry.getKey(), entry.getValue());
            }
        }

        return filtered;
    }

    // ---------------------------------------------------------------------
    // Capability declarations
    // ---------------------------------------------------------------------

    private static EditorCapabilities declaredCapabilities(
            JsonElement value,
            Map<String, String> yamlFiles
    ) {
        if (!isPlainObject(value)) {
            throw new IllegalArgumentException(
                    "The server sent invalid editor permissions."
            );
        }

        JsonObject object = value.getAsJsonObject();

        Map<String, SchematicLinkage> schematicDirectories =
                declaredSchematicDirectories(
                        object.get("schematicDirectories"),
                        yamlFiles
                );

        Map<String, String> outpostPageDirectories =
                declaredOutpostPageDirectories(
                        object.get("outpostPageDirectories"),
                        yamlFiles
                );

        return new EditorCapabilities(
                schematicDirectories,
                outpostPageDirectories
        );
    }

    private static Map<String, SchematicLinkage>
    declaredSchematicDirectories(
            JsonElement serverManifest,
            Map<String, String> yamlFiles
    ) {
        if (serverManifest == null || !serverManifest.isJsonArray()) {
            throw new IllegalArgumentException(
                    "The server sent an invalid schematic folder list: " + serverManifest
            );
        }

        Map<String, SchematicLinkage> directories = new LinkedHashMap<>();

        for (JsonElement element : serverManifest.getAsJsonArray()) {
            if (!isPlainObject(element)) {
                throw invalidSchematicDirectory();
            }

            JsonObject entry = element.getAsJsonObject();
            JsonElement pathElement = entry.get("path");
            JsonElement configElement = entry.get("config");

            if (!isString(pathElement) || !isString(configElement)) {
                throw invalidSchematicDirectory();
            }

            String path = manifestPath(pathElement.getAsString());
            String config = manifestPath(configElement.getAsString());

            Matcher details = SCHEMATIC_DIRECTORY.matcher(path);

            if (!details.matches()
                    || !YAML_FILE.matcher(config).find()
                    || !yamlFiles.containsKey(config)
                    || !schematicConfigMatches(details, config)) {
                throw invalidSchematicDirectory();
            }

            String pathIdentity = identity(path);

            if (directories.containsKey(pathIdentity)) {
                throw new IllegalArgumentException(
                        "The server listed " + path + " more than once."
                );
            }

            directories.put(
                    pathIdentity,
                    new SchematicLinkage(path, config)
            );
        }

        return directories;
    }

    private static Map<String, String> declaredOutpostPageDirectories(
            JsonElement serverManifest,
            Map<String, String> yamlFiles
    ) {
        if (serverManifest == null || !serverManifest.isJsonArray()) {
            throw new IllegalArgumentException(
                    "The server sent an invalid outpost page folder list: " + serverManifest
            );
        }

        Map<String, String> directories = new LinkedHashMap<>();

        for (JsonElement element : serverManifest.getAsJsonArray()) {
            if (!isString(element)) {
                throw new IllegalArgumentException(
                        "The server sent invalid information for an outpost page folder."
                );
            }

            String path = manifestPath(element.getAsString());
            String pathIdentity = identity(path);

            boolean includesPage = yamlFiles.keySet().stream()
                    .anyMatch(file -> directChild(
                            file,
                            path,
                            OUTPOST_PAGE_FILE
                    ));

            if (!OUTPOST_PAGE_DIRECTORY.matcher(path).matches() || !includesPage) {
                throw new IllegalArgumentException(
                        "The server sent invalid information for an outpost page folder."
                );
            }

            if (directories.containsKey(pathIdentity)) {
                throw new IllegalArgumentException(
                        "The server listed " + path + " more than once."
                );
            }

            directories.put(pathIdentity, path);
        }

        return directories;
    }

    // ---------------------------------------------------------------------
    // Validate workspace entries
    // ---------------------------------------------------------------------

    private static void validateRemoteEntries(
            ServerContract contract,
            Set<String> filePaths
    ) {
        for (String path : filePaths) {
            if (path.equals(MANIFEST)) {
                continue;
            }

            if (YAML_FILE.matcher(path).find()) {
                if (contract.yamlFiles.containsKey(path)
                        || remoteOutpostPagePathEditable(contract, path)) {
                    continue;
                }
            } else if (SCHEMATIC_FILE.matcher(path).find()
                    && remoteSchematicPathEditable(contract.capabilities, path)) {
                continue;
            }

            throw new IllegalArgumentException(
                    path + " cannot be added to this server editor session."
            );
        }

        for (String path : contract.yamlFiles.keySet()) {
            if (remoteOutpostPagePathEditable(contract, path)) {
                continue;
            }

            if (!filePaths.contains(path)) {
                throw new IllegalArgumentException(
                        path + " cannot be removed from this server editor session."
                );
            }
        }

        for (String directory : contract.capabilities.outpostPageDirectories.values()) {
            boolean hasPages = filePaths.stream()
                    .anyMatch(path -> directChild(
                            path,
                            directory,
                            OUTPOST_PAGE_FILE
                    ));

            if (!hasPages) {
                throw new IllegalArgumentException(
                        directory + " must keep at least one outpost page."
                );
            }
        }

        for (SchematicLinkage schematic
                : contract.capabilities.schematicDirectories.values()) {

            String directory = schematic.path;
            Set<Long> levels = new HashSet<>();

            for (String path : filePaths) {
                if (!directChild(path, directory, SCHEMATIC_LEVEL_FILE)) {
                    continue;
                }

                String filename = path.substring(directory.length() + 1);
                Matcher matcher = SCHEMATIC_LEVEL_FILE.matcher(filename);

                if (!matcher.matches()) {
                    continue;
                }

                long level;

                try {
                    level = Long.parseLong(matcher.group(1));
                } catch (NumberFormatException exception) {
                    throw new IllegalArgumentException(
                            directory + " contains an invalid schematic level."
                    );
                }

                if (!levels.add(level)) {
                    throw new IllegalArgumentException(
                            directory + " contains more than one schematic for level " + level + "."
                    );
                }
            }

            if (!levels.isEmpty() && !levels.contains(1L)) {
                throw new IllegalArgumentException(
                        directory + " must include a level 1 schematic before later levels."
                );
            }
        }
    }

    // ---------------------------------------------------------------------
    // Schematic configuration validation
    // ---------------------------------------------------------------------

    private static boolean schematicConfigMatches(
            Matcher directoryDetails,
            String config
    ) {
        String prefix = directoryDetails.group(1);
        String family = directoryDetails.group(2);
        String building = directoryDetails.group(3);

        String configFamily = family.equalsIgnoreCase("turrets")
                ? "Turrets"
                : "Structures";

        List<String> segments = new ArrayList<>();

        if (prefix != null && !prefix.isEmpty()) {
            segments.add(prefix);
        }

        segments.add(configFamily);
        segments.add(building + ".yml");

        String expected = String.join("/", segments);
        String expectedLong = expected.replaceFirst(
                "(?i)\\.yml$",
                ".yaml"
        );

        return identity(expected).equals(identity(config))
                || identity(expectedLong).equals(identity(config));
    }

    // ---------------------------------------------------------------------
    // Path helpers
    // ---------------------------------------------------------------------

    private static boolean directChild(
            String path,
            String directory,
            Pattern pattern
    ) {
        String prefix = directory + "/";

        if (!identity(path).startsWith(identity(prefix))) {
            return false;
        }

        String relative = path.substring(prefix.length());

        return !relative.contains("/")
                && pattern.matcher(relative).matches();
    }

    private static String manifestPath(String value) {
        String path = value == null ? "" : value;

        if (path.isEmpty()
                || path.indexOf('\0') >= 0
                || path.contains("\\")
                || path.startsWith("/")
                || path.matches("^[A-Za-z]:.*")) {
            throw invalidPath();
        }

        if (path.endsWith("/")) {
            throw invalidPath();
        }

        String[] segments = path.split("/", -1);

        for (String segment : segments) {
            if (segment.isEmpty()
                    || segment.equals(".")
                    || segment.equals("..")) {
                throw invalidPath();
            }
        }

        return path;
    }

    private static String identity(String value) {
        return value.toLowerCase(Locale.US);
    }

    // ---------------------------------------------------------------------
    // Gson and type-validation helpers
    // ---------------------------------------------------------------------

    private static boolean isPlainObject(JsonElement value) {
        return value != null && value.isJsonObject();
    }

    private static boolean isString(JsonElement value) {
        return value != null
                && value.isJsonPrimitive()
                && value.getAsJsonPrimitive().isString();
    }

    private static boolean isExpectedProtocol(
            JsonElement value,
            int expectedProtocol
    ) {
        try {
            return value.getAsBigDecimal().compareTo(
                    java.math.BigDecimal.valueOf(expectedProtocol)
            ) == 0;
        } catch (NumberFormatException exception) {
            return false;
        }
    }

    private static IllegalArgumentException invalidPath() {
        return new IllegalArgumentException(
                "The server sent an unsafe or invalid editor file path."
        );
    }

    private static IllegalArgumentException invalidEditorFile() {
        return new IllegalArgumentException(
                "The server sent invalid information for one of its editor files."
        );
    }

    private static IllegalArgumentException invalidSchematicDirectory() {
        return new IllegalArgumentException(
                "The server sent invalid information for a schematic folder."
        );
    }

    private static boolean equalBytes(byte[] left, byte[] right) {
        return left != null
                && right != null
                && Arrays.equals(left, right);
    }
}