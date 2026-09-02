package fi.helsinki.cs.tmc.intellij.services;

import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.progress.util.ProgressWindow;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ThreadingService {

    private static final Logger logger = LoggerFactory.getLogger(ThreadingService.class);

    public void runWithNotification(
            final Runnable run, Project project, ProgressWindow progressWindow) {
        logger.info("Processing runWithNotification. @ThreadingService");

        String title = (progressWindow != null && progressWindow.getTitle() != null && !progressWindow.getTitle().isBlank())
                ? progressWindow.getTitle()
                : "TMC Task";

        boolean cancelable = true;

        ProgressManager.getInstance().run(new Task.Backgroundable(project, title, cancelable) {
            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                try {
                    indicator.setText(title);
                    run.run();
                } catch (Throwable t) {
                    logger.warn("Exception during TMC background task", t);
                }
            }
        });
    }
}
