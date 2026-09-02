package fi.helsinki.cs.tmc.intellij.services;

import fi.helsinki.cs.tmc.core.TmcCore;
import fi.helsinki.cs.tmc.core.domain.Course;
import fi.helsinki.cs.tmc.core.domain.ProgressObserver;
import fi.helsinki.cs.tmc.core.exceptions.ShowToUserException;
import fi.helsinki.cs.tmc.core.exceptions.TmcCoreException;
import fi.helsinki.cs.tmc.intellij.holders.TmcCoreHolder;
import fi.helsinki.cs.tmc.intellij.holders.TmcSettingsManager;
import fi.helsinki.cs.tmc.intellij.services.errors.ErrorMessageService;

import com.intellij.ide.DataManager;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;

import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.TestOnly;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.awt.Component;
import java.awt.KeyboardFocusManager;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Finds various exercises and courses from the disk or by asking the TMCServer. */
public class ObjectFinder {

    private static final Logger logger = LoggerFactory.getLogger(ObjectFinder.class);

    @Nullable
    public Course findCourse(String searchTerm, String titleOrName) {
        TmcCore core = TmcCoreHolder.get();
        List<Course> courses = getCourses(core);

        if (courses != null) {

            for (Course c : courses) {
                if ((titleOrName.equals("name") && c.getName().equals(searchTerm))
                        || (titleOrName.equals("title") && c.getTitle().equals(searchTerm))) {
                    try {
                        logger.info("Trying to get course details from TmcCore. @ObjectFinder", c);

                        return core.getCourseDetails(ProgressObserver.NULL_OBSERVER, c).call();
                    } catch (TmcCoreException exception) {
                        logger.warn(
                                "Could not find course. @ObjectFinder",
                                exception);
                        new ErrorMessageService().showHumanReadableErrorMessage(exception, false);
                    } catch (Exception e) {
                        logger.warn("Could not find course. @ObjectFinder", e);
                        new ErrorMessageService()
                                .showErrorMessageWithExceptionDetails(
                                        e, "Could not find course.", true);
                    }
                }
            }
        }

        return null;
    }

    @TestOnly
    public Course findCourseForTesting(String searchTerm, String titleOrName, TmcCore core) {
        List<Course> courses = getCourses(core);

        for (Course c : courses) {
            if ((titleOrName.equals("name") && c.getName().equals(searchTerm))
                    || (titleOrName.equals("title") && c.getTitle().equals(searchTerm))) {
                try {
                    return core.getCourseDetails(ProgressObserver.NULL_OBSERVER, c).call();
                } catch (TmcCoreException exception) {
                    new ErrorMessageService().showHumanReadableErrorMessage(exception, false);
                } catch (Exception e) {
                    new ErrorMessageService()
                            .showErrorMessageWithExceptionDetails(
                                    e, "Could not find course.", true);
                }
            }
        }

        return null;
    }

    private List<Course> getCourses(TmcCore core) {
        logger.info("Processing getCourses @ObjectFinder");
        List<Course> courses = null;

        try {
            courses = core.listCourses(ProgressObserver.NULL_OBSERVER).call();
        } catch (ShowToUserException exception) {
            logger.warn(
                "Failed to fetch courses from TmcCore. @ObjectFinder",
                exception);
            ErrorMessageService error = new ErrorMessageService();
            error.showErrorMessagePopup(
                "Failed to fetch courses from the server.\nPlease check your internet connection.");
        } catch (TmcCoreException e) {
            logger.warn("Getting courses failed @ObjectFinder", e);
            new ErrorMessageService().showHumanReadableErrorMessage(e, false);
        } catch (Exception e) {
            logger.warn("Getting courses failed @ObjectFinder", e);
            new ErrorMessageService()
                    .showErrorMessageWithExceptionDetails(
                            e, "Something went wrong while trying to get the course list", true);
        }

        return courses;
    }

    public List<String> listAllDownloadedCourses() {
        logger.info("Processing listAllDownloadedCourses. @ObjectFinder");
        List<String> courseTitles = new ArrayList<>();

        List<Course> courses = getCourses(TmcCoreHolder.get());
        if (courses == null) {
            return null;
        }

        Map<String, Course> coursesByName = courses.stream()
                .collect(Collectors.toMap(Course::getName, Function.identity(), (first, second) -> first));

        for (String name :
                getListOfDirectoriesInPath(TmcSettingsManager.get().getProjectBasePath())) {
            Course course = coursesByName.get(name);

            if (course == null) {
                continue;
            }

            courseTitles.add(course.getTitle());
        }

        return courseTitles;
    }

    public List<String> listAllDownloadedExercises(String courseTitle) {
        logger.info(
                "Processing listAllDownloadedExercises from course {}. @ObjectFinder", courseTitle);
        Course course = findCourse(courseTitle, "title");
        if (course == null) {
            return new ArrayList<>();
        }
        return getListOfDirectoriesInPath(
                TmcSettingsManager.get().getProjectBasePath()
                        + File.separator
                        + course.getName());
    }

    private List<String> getListOfDirectoriesInPath(String folderPath) {
        logger.info("Processing getListOfDirectoriesInPath. @ObjectFinder");
        List<String> fileNames = new ArrayList<>();

        try (DirectoryStream<Path> directoryStream =
                Files.newDirectoryStream(Paths.get(folderPath))) {
            logger.info("Getting list of directories in path. @ObjectFinder");
            addExercisesToList(fileNames, directoryStream);
        } catch (Exception ex) {
            logger.warn(
                    "Could not get list of directories in path. @ObjectFinder",
                    ex);
        }
        Collections.sort(fileNames);
        return fileNames;
    }

    private void addExercisesToList(List<String> fileNames, DirectoryStream<Path> directoryStream) {
        for (Path path : directoryStream) {
            if (!Files.isDirectory(path)) {
                continue;
            }

            // courseAndExerciseName is an array where the second to last element is course name and
            // last element is exercise name
            String[] courseAndExerciseNameArray = PathResolver.getCourseAndExerciseName(path);
            String exerciseName = courseAndExerciseNameArray == null
                    ? null : getExerciseName(courseAndExerciseNameArray);
            if (exerciseName == null || exerciseName.isEmpty() || exerciseName.charAt(0) == '.') {
                logger.info("exerciseCourse variable = null. @ObjectFinder");
                continue;
            }
            logger.info(
                    "Adding exercise to list. @ObjectFinder",
                    getExerciseName(courseAndExerciseNameArray));
            fileNames.add(exerciseName);
        }
    }

    private String getExerciseName(String[] courseAndExerciseName) {
        logger.info("Trying to getExerciseName. @ObjectFinder");
        return courseAndExerciseName[courseAndExerciseName.length - 1];
    }

    public Project findCurrentProject() {
        logger.info("Trying to findCurrentProject. @ObjectFinder");
        Component focusOwner = KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner();
        Project project = focusOwner == null ? null : CommonDataKeys.PROJECT.getData(
                DataManager.getInstance().getDataContext(focusOwner));
        if (project != null) {
            return project;
        }
        Project[] projects = ProjectManager.getInstance().getOpenProjects();
        return projects.length == 0 ? null : projects[projects.length - 1];
    }

    public Course findCourseNoDetails(String courseName, TmcCore core) {
        if (courseName == null) {
            return null;
        }
        try {
            List<Course> list = core.listCourses(ProgressObserver.NULL_OBSERVER).call();
            for (Course cor : list) {
                if (cor.getName().equals(courseName)) {
                    return cor;
                }
            }
        } catch (Exception e) {
            logger.warn("Could not find course {}", courseName, e);
        }
        return null;
    }
}
