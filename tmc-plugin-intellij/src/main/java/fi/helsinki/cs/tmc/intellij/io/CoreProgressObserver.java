package fi.helsinki.cs.tmc.intellij.io;

import fi.helsinki.cs.tmc.core.domain.ProgressObserver;

import com.intellij.openapi.progress.util.ProgressWindow;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;

public class CoreProgressObserver extends ProgressObserver {

    private final ProgressWindow progressWindow;
    private static final Logger logger = LoggerFactory.getLogger(CoreProgressObserver.class);

    public CoreProgressObserver(ProgressWindow progressWindow) {
        this.progressWindow = progressWindow;
    }

    @Override
    public void progress(long mysteryLong, String status) {
        logger.info("Setting progress status. @CoreProgressObserver");
        try {
            ProgressIndicator indicator = ProgressManager.getInstance().getProgressIndicator();
            if (indicator != null) {
                if (status != null && !status.isEmpty()) {
                    indicator.setText(status);
                }
                indicator.checkCanceled();
            }
            if (progressWindow != null) {
                progressWindow.setText(status);
                progressWindow.setText2(status);
                progressWindow.checkCanceled();
            }
        } catch (Throwable ignored) {
        }
    }

    @Override
    public void progress(long mysteryLong, Double progress, String status) {
        logger.info("Setting progress status. @CoreProgressObserver");
        try {
            ProgressIndicator indicator = ProgressManager.getInstance().getProgressIndicator();
            if (indicator != null) {
                if (status != null && !status.isEmpty()) {
                    indicator.setText(status);
                }
                if (progress != null) {
                    if (indicator.isIndeterminate()) {
                        indicator.setIndeterminate(false);
                    }
                    indicator.setFraction(progress);
                }
                indicator.checkCanceled();
            }
            if (progressWindow != null) {
                progressWindow.setText(status);
                progressWindow.setText2(status);
                if (progress != null) {
                    if (progressWindow.isIndeterminate()) {
                        progressWindow.setIndeterminate(false);
                    }
                    progressWindow.setFraction(progress);
                }
                progressWindow.checkCanceled();
            }
        } catch (Throwable ignored) {
        }
    }

    @Override
    public void start(long mysteryLong) {
        logger.info("Opening progress window. @CoreProgressObserver");
    }

    @Override
    public void end(long mysteryLong) {
        logger.info("Closing progress window. @CoreProgressObserver");
    }
}
