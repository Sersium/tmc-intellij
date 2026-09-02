package fi.helsinki.cs.tmc.intellij.services.exercises;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import fi.helsinki.cs.tmc.core.domain.Course;
import fi.helsinki.cs.tmc.core.domain.Exercise;
import fi.helsinki.cs.tmc.core.domain.ProgressObserver;
import fi.helsinki.cs.tmc.core.exceptions.ShowToUserException;
import fi.helsinki.cs.tmc.core.exceptions.TmcCoreException;
import fi.helsinki.cs.tmc.intellij.holders.ProjectListManagerHolder;
import fi.helsinki.cs.tmc.intellij.holders.TmcCoreHolder;
import fi.helsinki.cs.tmc.intellij.holders.TmcSettingsManager;
import fi.helsinki.cs.tmc.intellij.io.SettingsTmc;
import fi.helsinki.cs.tmc.intellij.services.ObjectFinder;
import fi.helsinki.cs.tmc.intellij.services.errors.ErrorMessageService;
import fi.helsinki.cs.tmc.intellij.services.persistence.ExerciseDatabase;
import fi.helsinki.cs.tmc.intellij.services.persistence.PersistentExerciseDatabase;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Holds a database of courses in memory, allowing quick fetching of course when necessary without
 * calling the TmcCore.
 *
 * <p>Also gives methods to update project list when necessary
 */
public class CourseAndExerciseManager {

    private static final Logger logger = LoggerFactory.getLogger(CourseAndExerciseManager.class);

    public Exercise getExercise(String course, String exercise) {
        logger.info("Get exercise from CourseAndExerciseManager. @CourseAndExerciseManager");

        try {
            Map<String, List<Exercise>> courses =
                    PersistentExerciseDatabase.getInstance()
                            .getExerciseDatabase()
                            .getCourses();

            List<Exercise> exercises = courses != null ? courses.get(course) : null;

            if (exercises != null) {
                for (Exercise exc : exercises) {
                    if (exerciseIsTheCorrectOne(exc, exercise)) {
                        logger.info("Found " + exc + " @CourseAndExerciseManager");
                        return exc;
                    }
                }
            } else if (courses != null) {
                for (List<Exercise> list : courses.values()) {
                    if (list != null) {
                        for (Exercise exc : list) {
                            if (exerciseIsTheCorrectOne(exc, exercise)) {
                                logger.info("Found " + exc + " in fallback @CourseAndExerciseManager");
                                return exc;
                            }
                        }
                    }
                }
            }
        } catch (Exception exception) {
            logger.warn(
                    "Exercise was not found. @CourseAndExerciseManager",
                    exception);
        }
        return null;
    }

    private boolean exerciseIsTheCorrectOne(Exercise exc, String exerciseName) {
        logger.info(
                "Checking if {} equals {}. @CourseAndExerciseManager", exc.getName(), exerciseName);

        return exc.getName().equals(exerciseName);
    }

    public List<Exercise> getExercises(String course) {
        logger.info("Getting list of exercises. @CourseAndExerciseManager");

        try {
            return getDatabase().getCourses().get(course);
        } catch (Exception exception) {
            logger.warn(
                    "Course was not found. @CourseAndExerciseManager",
                    exception);
            ErrorMessageService error = new ErrorMessageService();
            error.showErrorMessageWithExceptionDetails(
                    exception, "Could not find the course.", false);
        }
        return null;
    }

    public ExerciseDatabase getDatabase() {
        logger.info("Get database from CourseAndExerciseManager. @CourseAndExerciseManager");
        return PersistentExerciseDatabase.getInstance().getExerciseDatabase();
    }

    public void initiateDatabase() {
        logger.info(
                "Initiating database. Fetching courses from TmcCore."
                        + " @CourseAndExerciseManager");

        try {
            Map<String, List<Exercise>> database = new HashMap<>();
            List<Course> courses =
                    TmcCoreHolder.get().listCourses(ProgressObserver.NULL_OBSERVER).call();

            for (Course course : courses) {
                fetchCourseFromTmcCore(database, course);
            }

            getDatabase().setCourses(database);
        } catch (TmcCoreException exception) {
            logger.warn(
                    "Failed to fetch courses from TmcCore. @CourseAndExerciseManager",
                    exception);
            if (TmcSettingsManager.get().getOrganization().isPresent()) {
                showMessageDialog();
                refreshCoursesOffline();
            }
        } catch (Exception exception) {
            logger.warn("Unexpected failure while initiating database.", exception);
        }
    }

    private void showMessageDialog() {
        Project currentProject = new ObjectFinder().findCurrentProject();
        Icon icon = Messages.getErrorIcon();
        String message =
                "Failed to fetch courses from the server.\nPlease check your internet connection.";

        ApplicationManager.getApplication()
                .invokeLater(
                        () -> {
                            int answer =
                                    Messages.showOkCancelDialog(
                                            currentProject,
                                            message,
                                            "",
                                            "Try again",
                                            "Cancel",
                                            icon);
                            if (answer == 0) {
                                this.initiateDatabase();
                            }
                        });
    }

    private void fetchCourseFromTmcCore(Map<String, List<Exercise>> database, Course course) {
        try {
            logger.info("Fetching {} from TmcCore. @CourseAndExerciseManager", course);
            course =
                    TmcCoreHolder.get()
                            .getCourseDetails(ProgressObserver.NULL_OBSERVER, course)
                            .call();
            List<Exercise> exercises =
                    new CheckForExistingExercises()
                            .getListOfDownloadedExercises(
                                    course.getExercises(), TmcSettingsManager.get());
            database.put(course.getTitle(), exercises);
            database.put(course.getName(), exercises);
        } catch (Exception exception) {
            logger.warn(
                    "Failed to initiate database. @CourseAndExerciseManager",
                    exception);
            new ErrorMessageService()
                    .showErrorMessageWithExceptionDetails(
                            exception, "Failed to initiate database", true);
        }
    }

    private void refreshCoursesOffline() {
        logger.info(
                "Refreshing side panel course view for user while offline. "
                        + " @CourseAndExerciseManager");
        Map<String, List<Exercise>> courses = getExerciseDatabase().getCourses();

        for (String courseName : courses.keySet()) {
            List<Exercise> exercises = courses.get(courseName);

            removeExercisesNotFoundFromLocalDirectories(exercises, courseName);
        }
        getDatabase().setCourses(courses);
    }

    private void removeExercisesNotFoundFromLocalDirectories(
            List<Exercise> exercises, String courseName) {
        logger.info(
                "Removing all exercises from the side panel, that are not "
                        + "found from local directories . @CourseAndExerciseManager");
        List<String> exerciseNamesThroughDirectories =
                getExerciseNamesThroughDirectories(courseName);

        Iterator<Exercise> iterator = exercises.iterator();

        while (iterator.hasNext()) {
            Exercise exercise = iterator.next();

            if (!exerciseNamesThroughDirectories.contains(exercise.getName())) {
                logger.info("Removed {}. @CourseAndExerciseManager", exercise.getName());
                iterator.remove();
            }
        }
    }

    private List<String> getExerciseNamesThroughDirectories(String courseName) {
        logger.info(
                "Fetching {} course exercise names from the local "
                        + "directories. @CourseAndExerciseManager",
                courseName);

        return new ObjectFinder().listAllDownloadedExercises(courseName);
    }

    private static ExerciseDatabase getExerciseDatabase() {
        logger.info(
                "Get ExerciseDatabase from PersistentExerciseDatabase. "
                        + "@CourseAndExerciseManager");
        return PersistentExerciseDatabase.getInstance().getExerciseDatabase();
    }

    public void updateSingleCourse(
            String courseName,
            CheckForExistingExercises checker,
            ObjectFinder finder,
            SettingsTmc settings) {

        logger.info("Updating single course. @CourseAndExerciseManager");
        boolean isNewCourse = getDatabase().getCourses().get(courseName) == null;

        Course course = finder.findCourse(courseName, "title");

        if (course == null) {
            return;
        }

        List<Exercise> existing =
                checker.getListOfDownloadedExercises(course.getExercises(), settings);

        getDatabase().getCourses().put(courseName, existing);

        if (isNewCourse) {
            ProjectListManagerHolder.get().refreshAllCourses();
        } else {
            ProjectListManagerHolder.get().refreshCourse(courseName);
        }
    }

    public boolean isCourseInDatabase(String string) {
        logger.info(
                "Checking if course {} exists in the database." + " @CourseAndExerciseManager",
                string);
        if (string == null) {
            return false;
        }
        Map<String, List<Exercise>> courses = PersistentExerciseDatabase.getInstance()
                .getExerciseDatabase()
                .getCourses();
        if (courses.containsKey(string)) {
            return true;
        }
        com.google.common.base.Optional<Course> selectedCourse =
                TmcSettingsManager.get().getCurrentCourse();
        return selectedCourse.isPresent()
                && string.equals(selectedCourse.get().getName())
                && courses.containsKey(selectedCourse.get().getTitle());
    }
}
