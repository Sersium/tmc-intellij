package fi.helsinki.cs.tmc.intellij.services.exercises;

import com.google.common.base.Optional;
import fi.helsinki.cs.tmc.core.TmcCore;
import fi.helsinki.cs.tmc.core.commands.GetUpdatableExercises.UpdateResult;
import fi.helsinki.cs.tmc.core.domain.Course;
import fi.helsinki.cs.tmc.core.domain.Exercise;
import fi.helsinki.cs.tmc.core.domain.ProgressObserver;
import fi.helsinki.cs.tmc.intellij.actions.buttonactions.DownloadExerciseAction;
import fi.helsinki.cs.tmc.intellij.holders.TmcCoreHolder;
import fi.helsinki.cs.tmc.intellij.holders.TmcSettingsManager;
import fi.helsinki.cs.tmc.intellij.io.SettingsTmc;
import fi.helsinki.cs.tmc.intellij.services.ObjectFinder;
import fi.helsinki.cs.tmc.intellij.services.errors.ErrorMessageService;

import com.intellij.notification.NotificationType;
import com.intellij.notification.NotificationAction;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/** Checks if there are undone and downloadable exercises. */
public class CheckForNewExercises {

    private static final Logger logger = LoggerFactory.getLogger(CheckForNewExercises.class);

    public void doCheck(Project project) {
        ApplicationManager.getApplication()
                .executeOnPooledThread(
                        () -> {
                            logger.info("Checking for new exercises.");
                            TmcCore core = TmcCoreHolder.get();
                            SettingsTmc settings = TmcSettingsManager.get();
                            Course course =
                                    new ObjectFinder()
                                            .findCourseNoDetails(
                                                    settings.getCourseName(), core);

                            if (course == null) {
                                return;
                            }
                            settings.setCourse(Optional.of(course));
                            CourseAndExerciseManager manager = new CourseAndExerciseManager();
                            try {
                                getExerciseUpdateData(project, core, settings, manager);
                            } catch (Exception e) {
                                logger.warn("Checking for new exercises failed.", e);
                            }
                        });
    }

    private boolean getExerciseUpdateData(
            Project project, TmcCore core, SettingsTmc settings, CourseAndExerciseManager manager)
            throws Exception {
        logger.info("Trying to get exercise update data.");
        UpdateResult result =
                core.getExerciseUpdates(ProgressObserver.NULL_OBSERVER, settings.getCurrentCourse().get())
                        .call();
        if (hasNewIncompleteExercises(
                result.getNewExercises(), manager.getExercises(
                        settings.getCurrentCourse().get().getTitle()))) {
            ApplicationManager.getApplication().invokeLater(
                    () -> createNotificationForNewExercises(project, settings));
            return true;
        }
        return false;
    }

    private void createNotificationForNewExercises(Project project, SettingsTmc settings) {
        ErrorMessageService.notifications()
                .createNotification(
                        "New exercises!",
                        "New exercises found for "
                                + settings.getCurrentCourse().get().getTitle() + ".",
                        NotificationType.INFORMATION)
                .addAction(NotificationAction.createSimpleExpiring(
                        "Download exercises",
                        () -> new DownloadExerciseAction().downloadExercises(project, false)))
                .notify(project);
    }

    private boolean hasNewIncompleteExercises(
            List<Exercise> newExercises, List<Exercise> exercises) {
        if (newExercises == null) {
            return false;
        }
        List<Exercise> downloaded = exercises == null ? java.util.Collections.emptyList() : exercises;
        return newExercises.stream()
                .anyMatch(ex -> !exerciseIsOnList(ex, downloaded) && !ex.isCompleted());
    }

    private boolean exerciseIsOnList(Exercise ex, List<Exercise> exercises) {
        return exercises.stream().anyMatch(e -> e.getName().equals(ex.getName()));
    }
}
