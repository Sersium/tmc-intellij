package fi.helsinki.cs.tmc.intellij.actions.buttonactions;

import fi.helsinki.cs.tmc.intellij.holders.TmcCoreHolder;
import fi.helsinki.cs.tmc.intellij.holders.TmcSettingsManager;
import fi.helsinki.cs.tmc.intellij.io.CoreProgressObserver;
import fi.helsinki.cs.tmc.intellij.services.ObjectFinder;
import fi.helsinki.cs.tmc.intellij.services.ProgressWindowMaker;
import fi.helsinki.cs.tmc.intellij.services.TestRunningService;
import fi.helsinki.cs.tmc.intellij.services.ThreadingService;
import fi.helsinki.cs.tmc.intellij.services.exercises.CheckForExistingExercises;
import fi.helsinki.cs.tmc.intellij.services.exercises.CourseAndExerciseManager;
import fi.helsinki.cs.tmc.intellij.services.exercises.ExerciseUploadingService;
import fi.helsinki.cs.tmc.intellij.snapshots.ButtonInputListener;
import fi.helsinki.cs.tmc.intellij.ui.submissionresult.SubmissionResultHandler;

import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.progress.util.ProgressWindow;
import com.intellij.openapi.project.Project;
import icons.TmcIcons;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UploadExerciseAction extends AnAction {

    private static final Logger logger = LoggerFactory.getLogger(UploadExerciseAction.class);

    public UploadExerciseAction() {
        super("TMC Submit", "Submit current exercise to TMC server", TmcIcons.SUBMIT_BUTTON);
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.BGT;
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
        e.getPresentation().setEnabledAndVisible(true);
        e.getPresentation().setIcon(TmcIcons.SUBMIT_BUTTON);
        e.getPresentation().setText("TMC Submit");
        e.getPresentation().setDescription("Submit current exercise to TMC server");
    }

    @Override
    public void actionPerformed(AnActionEvent anActionEvent) {
        logger.info("Performing UploadExerciseAction. @UploadExerciseAction");
        uploadExercise(anActionEvent.getProject());
    }

    public void uploadExercise(Project project) {
        if (project == null || project.isDisposed()) {
            return;
        }

        new ButtonInputListener().receiveSubmit();
        try {
            com.intellij.openapi.application.WriteIntentReadAction.run(() ->
                    FileDocumentManager.getInstance().saveAllDocuments());
        } catch (Throwable t) {
            try {
                FileDocumentManager.getInstance().saveAllDocuments();
            } catch (Throwable ignored) {
            }
        }

        ProgressWindow window =
                ProgressWindowMaker.make(
                        "Submitting exercise to TMC...",
                        project,
                        true,
                        true,
                        true);
        CoreProgressObserver observer = new CoreProgressObserver(window);

        callExerciseUploadService(project, observer, window);
    }

    private void callExerciseUploadService(
            Project project, CoreProgressObserver observer, ProgressWindow window) {

        new ExerciseUploadingService()
                .startUploadExercise(
                        project,
                        TmcCoreHolder.get(),
                        new ObjectFinder(),
                        new CheckForExistingExercises(),
                        new SubmissionResultHandler(),
                        TmcSettingsManager.get(),
                        new CourseAndExerciseManager(),
                        new ThreadingService(),
                        new TestRunningService(),
                        observer,
                        window);
    }
}
