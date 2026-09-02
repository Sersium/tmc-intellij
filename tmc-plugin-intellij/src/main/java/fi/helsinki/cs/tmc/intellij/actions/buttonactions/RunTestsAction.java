package fi.helsinki.cs.tmc.intellij.actions.buttonactions;

import fi.helsinki.cs.tmc.core.domain.Course;
import fi.helsinki.cs.tmc.core.domain.Exercise;
import fi.helsinki.cs.tmc.intellij.services.ObjectFinder;
import fi.helsinki.cs.tmc.intellij.services.PathResolver;
import fi.helsinki.cs.tmc.intellij.services.TestRunningService;
import fi.helsinki.cs.tmc.intellij.services.ThreadingService;
import fi.helsinki.cs.tmc.intellij.services.exercises.CourseAndExerciseManager;
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
        e.getPresentation().setEnabledAndVisible(true);
        e.getPresentation().setIcon(TmcIcons.TEST_BUTTON);
        e.getPresentation().setText("TMC Test");
        e.getPresentation().setDescription("Run TMC tests for current project");
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

        String[] courseExercise = PathResolver.getCourseAndExerciseName(project);
        if (courseExercise == null || courseExercise.length < 2) {
            return;
        }

        String courseName = getCourseName(courseExercise);
        String exerciseName = getExerciseName(courseExercise);

        Course course = new ObjectFinder().findCourse(courseName, "name");
        if (course == null) {
            course = new ObjectFinder().findCourse(courseName, "title");
        }

        FileDocumentManager.getInstance().saveAllDocuments();

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
        }
    }

    private String getCourseName(String[] courseExercise) {
        return courseExercise[courseExercise.length - 2];
    }

    private String getExerciseName(String[] courseExercise) {
        return courseExercise[courseExercise.length - 1];
    }
}
