package fi.helsinki.cs.tmc.intellij.actions.buttonactions;

import fi.helsinki.cs.tmc.core.domain.Course;
import fi.helsinki.cs.tmc.core.domain.Exercise;
import fi.helsinki.cs.tmc.intellij.services.ObjectFinder;
import fi.helsinki.cs.tmc.intellij.services.PathResolver;
import fi.helsinki.cs.tmc.intellij.services.TestRunningService;
import fi.helsinki.cs.tmc.intellij.services.ThreadingService;
import fi.helsinki.cs.tmc.intellij.services.exercises.CourseAndExerciseManager;
import fi.helsinki.cs.tmc.intellij.snapshots.ButtonInputListener;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.fileEditor.FileDocumentManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RunTestsAction extends AnAction {

    private static final Logger logger = LoggerFactory.getLogger(RunTestsAction.class);

    @Override
    public void actionPerformed(AnActionEvent anActionEvent) {
        logger.info("Run tests action performed. @RunTestsAction");
        com.intellij.openapi.project.Project project = anActionEvent.getProject();
        if (project == null) {
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
