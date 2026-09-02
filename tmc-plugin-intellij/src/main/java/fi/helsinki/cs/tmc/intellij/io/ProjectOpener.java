package fi.helsinki.cs.tmc.intellij.io;

import com.google.common.base.Optional;
import fi.helsinki.cs.tmc.core.domain.Course;
import fi.helsinki.cs.tmc.intellij.holders.ProjectListManagerHolder;
import fi.helsinki.cs.tmc.intellij.holders.TmcSettingsManager;
import fi.helsinki.cs.tmc.intellij.importexercise.ExerciseImport;
import fi.helsinki.cs.tmc.intellij.services.ObjectFinder;
import fi.helsinki.cs.tmc.intellij.services.PathResolver;
import fi.helsinki.cs.tmc.intellij.services.errors.ErrorMessageService;
import fi.helsinki.cs.tmc.intellij.snapshots.ActivateSnapshotsListeners;

import com.intellij.ide.impl.ProjectUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.ui.Messages;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/** Opens the project using intellij ProjectManager, when given the path. */
public class ProjectOpener {

    private static final Logger logger = LoggerFactory.getLogger(ProjectOpener.class);

    public void openProject(String path, String courseName) {
        Course course = new ObjectFinder().findCourse(courseName, "name");
        TmcSettingsManager.get().setCourse(Optional.fromNullable(course));
        openProject(path);
    }

    public void openProject(String path) {
        openProject(new ObjectFinder().findCurrentProject(), path);
    }

    public void openProject(Project project, String path) {
        logger.info("Opening project from {}. @ProjectOpener", path);
        Path projectPath = Paths.get(path);
        if (Files.isDirectory(projectPath)) {
            if (project == null || project.getBasePath() == null || !projectPath.equals(Paths.get(project.getBasePath()))) {
                try {
                    if (project != null && !project.isDisposed()) {
                        try {
                            new ActivateSnapshotsListeners(project).removeListeners();
                        } catch (Throwable t) {
                            logger.warn("Could not remove snapshot listeners: {}", t.getMessage());
                        }
                    }
                    ExerciseImport.importExercise(path);
                    ProjectUtil.openOrImport(projectPath);

                    try {
                        String[] split = PathResolver.getCourseAndExerciseName(path);
                        if (split != null && split.length >= 2) {
                            Course course = new ObjectFinder().findCourse(split[split.length - 2], "name");
                            if (course != null) {
                                TmcSettingsManager.get().setCourse(Optional.of(course));
                            }
                        }
                    } catch (Throwable t) {
                        logger.warn("Could not update active course in settings: {}", t.getMessage());
                    }

                } catch (Exception exception) {
                    logger.warn(
                            "Could not open project from path. @ProjectOpener",
                            exception);
                    new ErrorMessageService()
                            .showErrorMessageWithExceptionDetails(
                                    exception, "Could not open project from path. " + path, true);
                }
            }
        } else {
            logger.warn("Directory no longer exists. @ProjectOpener");
            Messages.showErrorDialog(
                    new ObjectFinder().findCurrentProject(),
                    "Directory no longer exists",
                    "File not found");
            ProjectListManagerHolder.get().refreshAllCourses();
        }
    }

    public void openProject(Path path) {
        logger.info(
                "Redirecting openProject with path -> "
                        + "openProject with string. @ProjectOpener");
        openProject(path.toString());
    }
}
