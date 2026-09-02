package fi.helsinki.cs.tmc.intellij.snapshots;

import fi.helsinki.cs.tmc.core.domain.Exercise;
import fi.helsinki.cs.tmc.core.utilities.JsonMaker;
import fi.helsinki.cs.tmc.intellij.services.PathResolver;
import fi.helsinki.cs.tmc.intellij.services.exercises.CourseAndExerciseManager;
import fi.helsinki.cs.tmc.intellij.snapshots.snapshotsutils.ActiveThreadSet;
import fi.helsinki.cs.tmc.intellij.snapshots.snapshotsutils.RecursiveZipper;
import fi.helsinki.cs.tmc.spyware.*;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.openapi.vfs.newvfs.BulkFileListener;
import com.intellij.openapi.vfs.newvfs.events.VFileContentChangeEvent;
import com.intellij.openapi.vfs.newvfs.events.VFileCopyEvent;
import com.intellij.openapi.vfs.newvfs.events.VFileCreateEvent;
import com.intellij.openapi.vfs.newvfs.events.VFileDeleteEvent;
import com.intellij.openapi.vfs.newvfs.events.VFileEvent;
import com.intellij.openapi.vfs.newvfs.events.VFileMoveEvent;
import com.intellij.openapi.vfs.newvfs.events.VFilePropertyChangeEvent;

import org.jetbrains.annotations.NotNull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

public class SnapshotsFileListener implements Closeable {

    private static final Logger logger = LoggerFactory.getLogger(SnapshotsFileListener.class);
    private String projectPath;
    private final ActiveThreadSet snapshotterThreads;
    private final Project project;
    private Disposable listenerDisposable;

    public SnapshotsFileListener(Project project) {
        this.project = project;
        this.projectPath = project.getBasePath();
        this.snapshotterThreads = new ActiveThreadSet();
    }

    public void removeListener() {
        if (listenerDisposable != null) {
            Disposer.dispose(listenerDisposable);
        }
        listenerDisposable = null;
    }

    public void createAndAddListener() {
        removeListener();
        listenerDisposable = Disposer.newDisposable("TMC snapshot file listener");
        Disposer.register(project, listenerDisposable);
        project.getMessageBus().connect(listenerDisposable).subscribe(
                VirtualFileManager.VFS_CHANGES, getVirtualFileListener());
        this.projectPath = project.getBasePath();
    }

    @NotNull
    private BulkFileListener getVirtualFileListener() {
        return new BulkFileListener() {
            @Override
            public void before(@NotNull List<? extends VFileEvent> events) {
                for (VFileEvent event : events) {
                    if (event instanceof VFileDeleteEvent && isProperfile(event.getFile())) {
                        sendMetadata(JsonMaker.create()
                                .add("cause", "file_delete")
                                .add("file", new PathResolver()
                                        .getPathRelativeToProject(event.getPath())));
                    }
                }
            }

            @Override
            public void after(@NotNull List<? extends VFileEvent> events) {
                for (VFileEvent event : events) {
                    processAfterEvent(event);
                }
            }
        };
    }

    private void processAfterEvent(VFileEvent event) {
        VirtualFile file = event.getFile();
        if (!isProperfile(file)) {
            return;
        }
        String path = event.getPath();
        if (event instanceof VFilePropertyChangeEvent propertyChange) {
            sendMetadata(JsonMaker.create()
                    .add("cause", setFolderOrFile(path, "rename"))
                    .add("file", new PathResolver().getPathRelativeToProject(path))
                    .add("previous_name", String.valueOf(propertyChange.getOldValue())));
        } else if (event instanceof VFileContentChangeEvent) {
            sendMetadata(JsonMaker.create()
                    .add("cause", "file_change")
                    .add("file", new PathResolver().getPathRelativeToProject(path)));
        } else if (event instanceof VFileCreateEvent) {
            prepareMetaData("create", path);
        } else if (event instanceof VFileMoveEvent) {
            prepareMetaData("move", path);
        } else if (event instanceof VFileCopyEvent copyEvent) {
            sendMetadata(JsonMaker.create()
                    .add("cause", setFolderOrFile(path, "copy"))
                    .add("file", new PathResolver().getPathRelativeToProject(path))
                    .add("from", copyEvent.getFile().getPath()));
        }
    }

    private void prepareMetaData(String action, String path) {
        sendMetadata(JsonMaker.create()
                .add("cause", setFolderOrFile(path, action))
                .add("file", new PathResolver().getPathRelativeToProject(path)));
    }

    private String setFolderOrFile(String path, String action) {
        return (Files.isDirectory(Paths.get(path)) ? "folder_" : "file_") + action;
    }

    private void sendMetadata(JsonMaker metadata) {
        if (!new CourseAndExerciseManager()
                .isCourseInDatabase(PathResolver.getCourseName(projectPath))) {
            return;
        }

        Exercise exercise = getExercise();
        if (exercise == null) {
            return;
        }

        logger.info("Starting zipping thread for exercise: {}", exercise);
        SnapshotThread thread = new SnapshotThread(exercise, projectPath, metadata);
        snapshotterThreads.addThread(thread);
        thread.setDaemon(true);
        thread.start();
    }

    @Override
    public void close() throws IOException {
        removeListener();
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            try {
                snapshotterThreads.joinAll();
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            }
        });
    }

    public boolean isProperfile(VirtualFile virtualFile) {
        if (virtualFile == null || projectPath == null
                || !Path.of(virtualFile.getPath()).startsWith(Path.of(projectPath))) {
            return false;
        }
        String path = virtualFile.getPath();
        return !(path.contains("/.idea/")
                || path.endsWith(".iml")
                || path.endsWith(".xml")
                || path.contains("/out/")
                || path.endsWith(".txt")
                || path.contains("/build/")
                || path.endsWith(".zip")
                || path.endsWith(".jar"));
    }

    private static class SnapshotThread extends Thread {
        private final Exercise exercise;
        private final String projectPathInfo;
        private final JsonMaker metadata;

        private SnapshotThread(Exercise exercise, String projectPathInfo, JsonMaker metadata) {
            super("Source snapshot");
            this.exercise = exercise;
            this.projectPathInfo = projectPathInfo;
            this.metadata = metadata;
        }

        @Override
        public void run() {
            // Note that, being in a thread, this is inherently prone to races that modify the
            // projectPath.
            // For now we just accept that. Not sure if the FileObject API would allow some sort of
            // global locking of the  projectPath.
            File projectPathDir = new File(projectPathInfo);
            RecursiveZipper.ZippingDecider zippingDecider =
                    new ZippingDeciderWrapper(projectPathInfo);
            RecursiveZipper zipper = new RecursiveZipper(projectPathDir, zippingDecider);
            try {
                byte[] data = zipper.zipProjectSources();
                LoggableEvent event = new LoggableEvent(exercise, "code_snapshot", data, metadata);
                SnapshotsEventManager.add(event);
            } catch (IOException ex) {
                // Warning might be also appro1priate, but this often races with  projectPath
                // closing
                // during integration tests, and there warning would cause a dialog to appear,
                // failing the test.
                logger.warn("Error zipping  projectPath sources in: " + projectPathDir, ex);
            }
        }
    }

    private static class ZippingDeciderWrapper implements RecursiveZipper.ZippingDecider {
        private static final long MAX_FILE_SIZE = 100 * 1024; // 100KB

        protected static final String[] BLACKLISTED_FILE_EXTENSIONS = {
            ".min.js",
            ".pack.js",
            ".jar",
            ".war",
            ".mp3",
            ".ogg",
            ".wav",
            ".png",
            ".jpg",
            ".jpeg",
            ".ttf",
            ".eot",
            ".woff"
        };

        private final String projectPathInfo;

        public ZippingDeciderWrapper(String projectPathInfo) {
            this.projectPathInfo = projectPathInfo;
        }

        protected boolean isProbablyBundledBinary(String zipPath) {
            for (String ext : BLACKLISTED_FILE_EXTENSIONS) {
                if (zipPath.endsWith(ext)) {
                    return true;
                }
            }
            return false;
        }

        protected boolean isTooBig(File file) {
            return file.length() > MAX_FILE_SIZE;
        }

        protected boolean hasNoSnapshotFile(File dir) {
            return new File(dir, ".tmcnosnapshot").exists();
        }

        @Override
        public boolean shouldZip(String zipPath) {
            File file = new File(projectPathInfo, zipPath);
            if (file.isDirectory()) {
                if (hasNoSnapshotFile(file)) {
                    return false;
                }
            } else {
                if (isProbablyBundledBinary(zipPath)) {
                    return false;
                }
                if (isTooBig(file)) {
                    return false;
                }
            }

            return true;
        }
    }

    public Exercise getExercise() {
        return PathResolver.getExercise(projectPath);
    }
}
