package fi.helsinki.cs.tmc.intellij.services.errors;

import fi.helsinki.cs.tmc.core.domain.Course;
import fi.helsinki.cs.tmc.core.exceptions.TmcCoreException;
import fi.helsinki.cs.tmc.intellij.services.ObjectFinder;

import com.intellij.notification.Notification;
import com.intellij.notification.NotificationGroup;
import com.intellij.notification.NotificationGroupManager;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.CommonBundle;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.Icon;

/**
 * Pops up user friendly warnings and specified error messages. For example pops up an error message
 * on startup if there is no internet connection or TMC user settings (username, password or server
 * address) are incorrect.
 */
public class ErrorMessageService {

    /**
     * Notification group used by IntelliJ to group TMC notifications.
     */
    public static NotificationGroup notifications() {
        try {
            return NotificationGroupManager.getInstance().getNotificationGroup("TMC Notifications");
        } catch (Throwable t) {
            return null;
        }
    }

    private static final Logger logger = LoggerFactory.getLogger(ErrorMessageService.class);

    public void showInfoBalloon(String message) {
        showBalloonNotification(message, NotificationType.INFORMATION);
    }

    public void showExercisesAreUpToDate(Course course) {
        ApplicationManager.getApplication().invokeLater(() -> Messages.showMessageDialog(
                "All exercises for " + course.toString() + " are up to date.",
                "All Exercises Are up to Date.",
                Messages.getInformationIcon()
        ));
    }

    public void showPopupWithDetails(String message,
                                     String title,
                                     String details,
                                     NotificationType notificationType) {
        Project currentProject = new ObjectFinder().findCurrentProject();
        Icon icon = iconForNotificationType(notificationType);

        ApplicationManager.getApplication().invokeLater(() -> Messages.showDialog(
                currentProject,
                message,
                title,
                details,
                new String[] { CommonBundle.getOkButtonText() },
                0,
                0,
                icon
        ));
    }

    /**
     * Shows a human readable error message to the user as a popup or a notification balloon.
     *
     * @param coreException The cause of the error.
     * @param showAsPopup if the error message will be a pop up or not.
     */
    public void showHumanReadableErrorMessage(TmcCoreException coreException, boolean showAsPopup) {
        logger.info("Showing human readable TmcCoreException. {} @ErrorMessageService", coreException);
        PresentableErrorMessage content = PresentableErrorMessage.forTmcException(coreException);
        showNotification(content.getMessage(), content.getMessageType(), showAsPopup);
    }

    /**
     * Shows an error message to the user as a popup or a notification balloon.
     *
     * @param exception The cause of the error, prepended to the description.
     * @param errorDescription Additional description of the error.
     * @param showAsPopup if the error message will be a pop up or not.
     */
    public void showErrorMessageWithExceptionDetails(Exception exception,
                                                     String errorDescription,
                                                     boolean showAsPopup) {
        logger.info("Showing Exception. {} @ErrorMessageService", exception);
        String message = exception + ". \n" + errorDescription;
        showNotification(message, NotificationType.ERROR, showAsPopup);
    }

    public void showErrorMessagePopup(String errorMessage) {
        logger.info("Showing error message: {}. @ErrorMessageService", errorMessage);
        showNotification(errorMessage, NotificationType.ERROR, true);
    }

    private void showNotification(String message, NotificationType type, boolean showAsPopup) {
        if (showAsPopup) {
            showPopup(message, type);
        } else {
            showBalloonNotification(message, type);
        }
    }

    private void showBalloonNotification(String message, NotificationType type) {
        try {
            NotificationGroup group = notifications();
            if (group != null) {
                Notification notification = group.createNotification(message, type);
                Project currentProject = new ObjectFinder().findCurrentProject();

                ApplicationManager.getApplication().invokeLater(() -> {
                    try {
                        notification.notify(currentProject);
                    } catch (Throwable t) {
                        logger.warn("Failed to notify balloon: {}", t.getMessage());
                    }
                });
            }
        } catch (Throwable t) {
            logger.warn("Failed to show balloon notification: {}", t.getMessage());
        }
    }

    private void showPopup(String message, NotificationType notificationType) {
        Project currentProject = new ObjectFinder().findCurrentProject();
        Icon icon = iconForNotificationType(notificationType);

        ApplicationManager.getApplication().invokeLater(() -> Messages.showMessageDialog(currentProject, message, "", icon));
    }

    private Icon iconForNotificationType(NotificationType type) {
        if (type == NotificationType.WARNING) {
            return Messages.getWarningIcon();
        } else if (type == NotificationType.ERROR) {
            return Messages.getErrorIcon();
        } else if (type == NotificationType.INFORMATION) {
            return Messages.getInformationIcon();
        } else {
            return Messages.getErrorIcon();
        }
    }
}
