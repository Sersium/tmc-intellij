package fi.helsinki.cs.tmc.intellij.actions.buttonactions;

import fi.helsinki.cs.tmc.core.domain.Course;
import fi.helsinki.cs.tmc.core.domain.Exercise;
import fi.helsinki.cs.tmc.intellij.services.ObjectFinder;
import fi.helsinki.cs.tmc.intellij.services.PathResolver;
import fi.helsinki.cs.tmc.intellij.services.TestRunningService;
import fi.helsinki.cs.tmc.intellij.services.ThreadingService;
import fi.helsinki.cs.tmc.intellij.services.exercises.CourseAndExerciseManager;
import fi.helsinki.cs.tmc.intellij.services.errors.ErrorMessageService;
import fi.helsinki.cs.tmc.intellij.snapshots.ButtonInputListener;

import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.project.Project;
import icons.TmcIcons;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RunTestsAction extends AnAction {

    private static final Logger logger = LoggerFactory.getLogger(RunTestsAction.class);

    public RunTestsAction() {
        super("TMC Test", "Run TMC tests for current project", TmcIcons.TEST_BUTTON);
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.BGT;
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
        boolean isBusy = fi.helsinki.cs.tmc.intellij.services.TmcOperationState.isOperationRunning();
        e.getPresentation().setEnabledAndVisible(true);
        e.getPresentation().setEnabled(!isBusy);
        e.getPresentation().setIcon(TmcIcons.TEST_BUTTON);
        e.getPresentation().setText("TMC Test");
        if (isBusy) {
            e.getPresentation().setDescription("Cannot test while "
                    + fi.helsinki.cs.tmc.intellij.services.TmcOperationState.getCurrentOperation().getDescription());
        } else {
            e.getPresentation().setDescription("Run TMC Tests (Shift+Alt+T)");
        }
    }

    @Override
    public void actionPerformed(AnActionEvent anActionEvent) {
        runTestsForProject(anActionEvent.getProject());
    }

    public void runTestsForProject(Project project) {
        logger.info("Run tests action performed. @RunTestsAction");
        if (project == null || project.isDisposed()) {
            return;
        }

        if (!fi.helsinki.cs.tmc.intellij.services.TmcOperationState.tryStartOperation(
                fi.helsinki.cs.tmc.intellij.services.TmcOperationState.Operation.TESTING)) {
            new ErrorMessageService().showInfoBalloon(
                    "Another TMC operation is already in progress ("
                            + fi.helsinki.cs.tmc.intellij.services.TmcOperationState.getCurrentOperation().getDescription() + ").");
            return;
        }

        try {
            String[] courseExercise = PathResolver.getCourseAndExerciseName(project);
            if (courseExercise == null || courseExercise.length < 2) {
                new ErrorMessageService().showInfoBalloon("Active project is not recognized as a TMC exercise.");
                fi.helsinki.cs.tmc.intellij.services.TmcOperationState.finishOperation();
                return;
            }

            String courseName = getCourseName(courseExercise);
            String exerciseName = getExerciseName(courseExercise);

            Course course = new ObjectFinder().findCourse(courseName, "name");
            if (course == null) {
                course = new ObjectFinder().findCourse(courseName, "title");
            }

            try {
                com.intellij.openapi.application.WriteIntentReadAction.run(() ->
                        FileDocumentManager.getInstance().saveAllDocuments());
            } catch (Throwable t) {
                try {
                    FileDocumentManager.getInstance().saveAllDocuments();
                } catch (Throwable ignored) {
                }
            }

            new ButtonInputListener().receiveTestRun();

            Exercise exercise = new CourseAndExerciseManager().getExercise(
                    course != null ? course.getTitle() : courseName, exerciseName);
            if (exercise == null) {
                exercise = new CourseAndExerciseManager().getExercise(courseName, exerciseName);
            }

            if (exercise != null) {
                new TestRunningService()
                        .runTests(
                                exercise,
                                project,
                                new ThreadingService(),
                                new ObjectFinder());
            } else {
                new ErrorMessageService().showInfoBalloon("Could not find exercise '" + exerciseName + "' in TMC database.");
                fi.helsinki.cs.tmc.intellij.services.TmcOperationState.finishOperation();
            }
        } catch (Throwable t) {
            fi.helsinki.cs.tmc.intellij.services.TmcOperationState.finishOperation();
            throw t;
        }
    }

    private String getCourseName(String[] courseExercise) {
        return courseExercise[courseExercise.length - 2];
    }

    private String getExerciseName(String[] courseExercise) {
        return courseExercise[courseExercise.length - 1];
    }
}
