package fi.helsinki.cs.tmc.intellij.snapshots;

import fi.helsinki.cs.tmc.intellij.services.PathResolver;
import fi.helsinki.cs.tmc.intellij.services.exercises.CourseAndExerciseManager;
import fi.helsinki.cs.tmc.spyware.*;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class ActivateSnapshotsListeners {

    private static final Logger logger = LoggerFactory.getLogger(ActivateSnapshotsListeners.class);
    private static final Key<SnapshotsFileListener> FILE_LISTENER =
            Key.create("tmc.snapshot.file.listener");
    private static final Key<Boolean> LISTENERS_ACTIVE =
            Key.create("tmc.snapshot.listeners.active");

    private final Project project;

    public ActivateSnapshotsListeners(Project project) {
        logger.info("Activating snapshots listeners.");
        this.project = project;
    }

    public void activateListeners() {
        if (!isCourseInDatabase(project)) {
            removeListeners();
            return;
        }
        if (project.getUserData(LISTENERS_ACTIVE) == null) {
            new HostInformationGenerator().updateHostInformation(SnapshotsEventManager.get());
            new SnapshotsRunListener(project);
            SnapshotsFileListener fileListener = new SnapshotsFileListener(project);
            fileListener.createAndAddListener();
            project.putUserData(FILE_LISTENER, fileListener);
            new SnapshotsTabListener(project);
            project.putUserData(LISTENERS_ACTIVE, Boolean.TRUE);
        }
    }

    public void removeListeners() {
        logger.info("Trying to remove file listeners and close it.");
        SnapshotsFileListener listener = project.getUserData(FILE_LISTENER);
        if (listener == null) {
            return;
        }

        try {
            listener.close();
        } catch (IOException e) {
            logger.warn("Failed to close listener.", e);
        } finally {
            project.putUserData(FILE_LISTENER, null);
            project.putUserData(LISTENERS_ACTIVE, null);
        }
    }

    private boolean isCourseInDatabase(Project project) {
        String basePath = project.getBasePath();
        return basePath != null
                && new CourseAndExerciseManager()
                        .isCourseInDatabase(PathResolver.getCourseName(basePath));
    }
}
