package org.kingdoms.editor;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static org.kingdoms.editor.KingdomsEditorSession.*;

public class TestKingdomsEditor {
    private static final String PLUGIN_VERSION = "local-preview";
    private static final Gson GSON = new GsonBuilder()
            .disableHtmlEscaping()
            .setPrettyPrinting()
            .create();

    @Test
    public void uploadEntireFolder() throws Exception {
        Path inputPath = Paths.get(System.getProperty("user.home") + "\\Desktop\\Test Server\\26\\plugins\\Kingdoms").toAbsolutePath().normalize();
        Path outputPathRoot = inputPath.resolve("temp").resolve("editor");
        Path editorStatePath = inputPath.resolve("editor-state.json");

        Predicate<Path> fileTest = path ->
                !path.equals(inputPath.resolve("temp"))
                        && !path.equals(inputPath.resolve("backups"))
                        && !path.equals(inputPath.resolve("logs"))
                        && !path.equals(inputPath.resolve("data"))
                        && !path.equals(inputPath.resolve("repository"));

        WorkspaceSource source;
        Session session;

        SavedEditorState previousSession;
        boolean usePreviousSession;

        System.out.println("Creating workspace source...");
        Path outputDirectory = null;
        Path hashesFile;

        if (Files.exists(editorStatePath)) {
            String readJson = new String(Files.readAllBytes(editorStatePath), StandardCharsets.UTF_8);
            previousSession = GSON.fromJson(readJson, SavedEditorState.class);
            outputDirectory = outputPathRoot.resolve(previousSession.linkSeed);

            boolean usePrev = true;
            if (!Files.exists(outputDirectory)) {
                System.out.println("The metadata of previous session is missing.");
                usePrev = false;
            }

            if (usePrev) {
                LocalDateTime time = LocalDateTime.parse(previousSession.savedDate);
                boolean hasBeenAnHour = Duration.between(time, LocalDateTime.now()).toMinutes() >= 55;

                if (hasBeenAnHour) {
                    System.out.println("Previous sessions expired: " + Duration.between(time, LocalDateTime.now()).toMinutes() + " minutes since.");
                    usePrev = false;
                }
            }

            if (usePrev) {
                boolean protocolMismatch = previousSession.protocol != SUPPORTED_PROTOCOL_VERSION;

                if (protocolMismatch) {
                    System.out.println("Previous sessions protocol doesn't match the supported protocol: " + previousSession.protocol + " != " + SUPPORTED_PROTOCOL_VERSION);
                    usePrev = false;
                }
            }

            usePreviousSession = usePrev;
        } else {
            usePreviousSession = false;
            previousSession = null;
        }

        if (usePreviousSession) {
            System.out.println("Using previous session... " + previousSession.linkSeed);
            hashesFile = outputDirectory.resolve("hashes.json");

            byte[] manifestBytes = KingdomsEditorSession.loadManifest(outputDirectory);
            Map<String, String> hashedFiles = WorkspaceSource.readHashedFiles(hashesFile);
            ServerContract remoteContract = KingdomsEditorSession.deserializeContract(hashedFiles, PLUGIN_VERSION, manifestBytes);

            source = WorkspaceSource.loadWorkspaceSource(hashedFiles, remoteContract);
            session = new Session(
                    previousSession.id, previousSession.key, previousSession.serverToken,
                    remoteContract,
                    previousSession.linkSeed
            );
        } else {
            System.out.println("Creating session...");
            source = loadWorkspaceSourceFromFolder(inputPath, fileTest);
            session = createSession(source, PLUGIN_VERSION);
            outputDirectory = createOutputDirectory(outputPathRoot, session.linkSeed);
            hashesFile = outputDirectory.resolve("hashes.json");

            String json = GSON.toJson(SavedEditorState.from(session));
            Files.write(editorStatePath, json.getBytes(StandardCharsets.UTF_8));

            source.saveLatestHashes(hashesFile);
            session.saveManifest(outputDirectory, false);
            session.saveManifest(outputDirectory, true);
        }

        System.out.println("Session: " + session);

        KingdomsEditorListener listener = new KingdomsEditorListener() {
            @Override
            public void close(int code, String reason) {
                System.out.println("Closing socket [" + code + "] " + reason);
            }

            @Override
            public boolean applyRevision(Path revisionOutputDirectory, List<FileEntry> entries) {
                try {
                    List<LinkedFileEntry> cmp = LinkedFileEntry.compareFiles(entries, inputPath, source, fileTest);
                    List<LinkedFileEntry> added = cmp.stream().filter(x -> x.getFileRevision() == LinkedFileEntry.FileRevisionResult.ADDED).collect(Collectors.toList());
                    List<LinkedFileEntry> removed = cmp.stream().filter(x -> x.getFileRevision() == LinkedFileEntry.FileRevisionResult.REMOVED).collect(Collectors.toList());
                    List<LinkedFileEntry> changed = cmp.stream().filter(x -> x.getFileRevision() == LinkedFileEntry.FileRevisionResult.CHANGED).collect(Collectors.toList());
                    List<LinkedFileEntry> conflicted = cmp.stream().filter(x -> x.getFileRevision() == LinkedFileEntry.FileRevisionResult.CONFLICT).collect(Collectors.toList());

                    System.out.println("Applying revision... " + revisionOutputDirectory.toAbsolutePath()
                            + "\n  - Added (" + added.size() + ") " + added.stream().map(x -> x.relativePath).collect(Collectors.joining(", "))
                            + "\n  - Removed (" + removed.size() + ") " + removed.stream().map(x -> x.relativePath).collect(Collectors.joining(", "))
                            + "\n  - Changed (" + changed.size() + ") " + changed.stream().map(x -> x.relativePath).collect(Collectors.joining(", "))
                            + "\n  - Conflicts (" + conflicted.size() + ") " + conflicted.stream().map(x -> x.relativePath).collect(Collectors.joining(", ")));

                    if (!conflicted.isEmpty()) return false;

                    cmp.forEach(x -> {
                        try {
                            x.applyChanges(source);
                        } catch (IOException e) {
                            throw new RuntimeException("Failed to apply changes for " + x.getRelativePath(), e);
                        }
                    });

                    source.saveLatestHashes(hashesFile);
                    return true;
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }

            @Override
            public void debug(String message) {
                System.out.println("[TestKingdomsEditor] " + message);
            }
        };


        System.out.printf("Selected %,d configuration files and %,d schematics.%n",
                source.yamlCount, source.schematicCount);
        System.out.println("Session Link: " + session.link);
        System.out.println("Input: " + inputPath);
        System.out.println("Output: " + outputDirectory);

        List<Path> deleted = RevisionHistoryCleanup.deleteOldFolders(
                outputPathRoot,
                Instant.now().minus(1, ChronoUnit.DAYS)
        );
        System.out.println("Deleted a total of " + deleted.size() + " old revision folders: " + deleted);

        final AtomicBoolean stopRequested = new AtomicBoolean(false);
        SessionState lastState = null;
        boolean retry = true;
        while (retry) {
            try {
                lastState = monitorSession(session, outputDirectory, listener, stopRequested);
                retry = false;
            } catch (EditorSocketException ex) {
                switch (ex.getErrorCode()) {
                    case 410: // Gone
                        System.err.println("Session closed (410), opening a new session (Run the tests again)");
                        if (Files.exists(editorStatePath)) Files.delete(editorStatePath);
                        retry = false;
                        break;

                    case 0:
                        System.out.println("Retrying...");
                        ex.printStackTrace();
                        break;

                    default:
                        System.err.println("Not sure how to handle this HTTP Code: " + ex.getErrorCode());
                        ex.printStackTrace();
                        retry = false;
                }
            } catch (Exception ex) {
                ex.printStackTrace();
                System.out.println("Retrying...");
            }
        }

        if (lastState != null) {
            switch (lastState) {
                case CONFLICTED:
                case CANCELLED:
                    System.out.println("Deleting editor session with state: " + lastState);
                    if (Files.exists(editorStatePath)) Files.delete(editorStatePath);
                    break;

                default:
                    System.out.println("Session editor: " + lastState);
            }
        }
    }
}
