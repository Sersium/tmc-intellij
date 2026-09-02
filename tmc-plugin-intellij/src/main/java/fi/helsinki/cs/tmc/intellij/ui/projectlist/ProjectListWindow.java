package fi.helsinki.cs.tmc.intellij.ui.projectlist;

import fi.helsinki.cs.tmc.intellij.holders.ProjectListManagerHolder;
import fi.helsinki.cs.tmc.intellij.holders.TmcSettingsManager;
import fi.helsinki.cs.tmc.intellij.io.ProjectOpener;
import fi.helsinki.cs.tmc.intellij.services.ObjectFinder;
import fi.helsinki.cs.tmc.intellij.services.ProgressWindowMaker;
import fi.helsinki.cs.tmc.intellij.services.ThreadingService;
import fi.helsinki.cs.tmc.intellij.services.exercises.CourseAndExerciseManager;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.progress.util.ProgressWindow;
import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.intellij.uiDesigner.core.Spacer;

import icons.TmcIcons;

import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Insets;
import java.util.List;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.JToolBar;

/**
 * Swing component that is displayed in the project list side panel.
 */
public class ProjectListWindow {

    private static final Logger logger = LoggerFactory.getLogger(ProjectListWindow.class);

    private JTabbedPane tabbedPaneBase;

    public JPanel getBasePanel() {
        return basePanel;
    }

    private JPanel basePanel;
    private JToolBar toolbar;

    public ProjectListWindow() {
        logger.info("Adding course tabs and exercises"
                + " to ProjectListWindow. @ProjectListWindow");
        if (TmcSettingsManager.get().getOrganization().isPresent()) {
            addCourseTabsAndExercises();
        }
    }

    public void addCourseTabsAndExercises() {
        logger.info("Creating course tabs and exercises. @ProjectListWindow");
        tabbedPaneBase.removeAll();
        toolbar.removeAll();
        ObjectFinder finder = new ObjectFinder();
        List<String> courses = finder.listAllDownloadedCourses();

        final ProjectOpener opener = new ProjectOpener();
        CourseTabFactory factory = new CourseTabFactory();

        if (courses != null && !courses.isEmpty()) {
            createCourseSpecificTabs(finder, opener, tabbedPaneBase,
                courses, factory, new CourseAndExerciseManager());

            JButton runButton = new JButton("Run", TmcIcons.RUN_BUTTON);
            runButton.setToolTipText("Run current project (TMC Run)");
            runButton.addActionListener(e -> {
                com.intellij.openapi.project.Project project = new ObjectFinder().findCurrentProject();
                if (project != null) {
                    new fi.helsinki.cs.tmc.intellij.actions.buttonactions.RunProjectAction().runProject(project);
                }
            });

            JButton testButton = new JButton("Test", TmcIcons.TEST_BUTTON);
            testButton.setToolTipText("Run TMC Tests (Shift+Alt+T)");
            testButton.addActionListener(e -> {
                com.intellij.openapi.project.Project project = new ObjectFinder().findCurrentProject();
                if (project != null) {
                    new fi.helsinki.cs.tmc.intellij.actions.buttonactions.RunTestsAction().runTestsForProject(project);
                }
            });

            JButton submitButton = new JButton("Submit", TmcIcons.SUBMIT_BUTTON);
            submitButton.setToolTipText("Submit to TMC Server (Shift+Alt+U)");
            submitButton.addActionListener(e -> {
                com.intellij.openapi.project.Project project = new ObjectFinder().findCurrentProject();
                if (project != null) {
                    new fi.helsinki.cs.tmc.intellij.actions.buttonactions.UploadExerciseAction().uploadExercise(project);
                }
            });

            JButton refreshButton = addFunctionalityToRefreshButton();
            refreshButton.setToolTipText("Refresh exercise list");

            toolbar.add(runButton);
            toolbar.add(testButton);
            toolbar.add(submitButton);
            toolbar.add(refreshButton);

            fi.helsinki.cs.tmc.intellij.services.TmcOperationState.addStateListener(isBusy -> {
                runButton.setEnabled(!isBusy);
                testButton.setEnabled(!isBusy);
                submitButton.setEnabled(!isBusy);
                refreshButton.setEnabled(!isBusy);
            });

            setActiveTabToSelectedCourse();
        } else {
            JPanel emptyPanel = new JPanel();
            emptyPanel.setLayout(new javax.swing.BoxLayout(emptyPanel, javax.swing.BoxLayout.Y_AXIS));
            emptyPanel.setBorder(javax.swing.BorderFactory.createEmptyBorder(20, 20, 20, 20));

            javax.swing.JLabel messageLabel = new javax.swing.JLabel("<html><b>No exercises downloaded yet.</b><br><br>"
                    + "To download exercises:<br>"
                    + "1. Select <b>TMC &rarr; Settings</b> to choose your course.<br>"
                    + "2. Click <b>Download course exercises</b> or use <b>TMC &rarr; Download current course's exercises</b>.</html>");
            messageLabel.setAlignmentX(java.awt.Component.CENTER_ALIGNMENT);

            JButton downloadBtn = new JButton("Download course exercises");
            downloadBtn.setAlignmentX(java.awt.Component.CENTER_ALIGNMENT);
            downloadBtn.addActionListener(e -> {
                com.intellij.openapi.project.Project project = new ObjectFinder().findCurrentProject();
                new fi.helsinki.cs.tmc.intellij.actions.buttonactions.DownloadExerciseAction().downloadExercises(project, false);
            });

            emptyPanel.add(messageLabel);
            emptyPanel.add(javax.swing.Box.createRigidArea(new Dimension(0, 15)));
            emptyPanel.add(downloadBtn);

            tabbedPaneBase.addTab("Exercises", emptyPanel);
        }
    }

    private void setActiveTabToSelectedCourse() {
        logger.info("Setting active tab to selected course. @ProjectListWindow");

        if (TmcSettingsManager.get().getCurrentCourse().isPresent()) {
            String course = TmcSettingsManager.get().getCurrentCourse().get().getTitle();
            for (int i = 0; i < tabbedPaneBase.getTabCount(); i++) {
                if (tabbedPaneBase.getTitleAt(i).equals(course)) {
                    tabbedPaneBase.setSelectedIndex(i);
                    return;
                }
            }
        }
    }

    private void createCourseSpecificTabs(ObjectFinder finder,
                                          ProjectOpener opener,
                                          JTabbedPane tabbedPanelBase,
                                          List<String> courses,
                                          CourseTabFactory factory,
                                          CourseAndExerciseManager courseAndExerciseManager) {

        logger.info("Starting to create all course specific tabs. @ProjectListWindow");

        for (String course : courses) {
            logger.info("Creating course specific tab for "
                    + course + ". @ProjectListWindow");
            factory.createCourseSpecificTab(finder, opener, course, tabbedPanelBase,
                    courseAndExerciseManager);
        }
    }

    @NotNull
    private JButton addFunctionalityToRefreshButton() {
        logger.info("Adding functionality to refresh projects button. "
                + "@ProjectListWindow");
        JButton refreshButton = new JButton(TmcIcons.REFRESH);
        refreshButton.setBorderPainted(true);
        refreshButton.setEnabled(true);

        ProgressWindow window = ProgressWindowMaker.make("Refreshing project list",
                new ObjectFinder().findCurrentProject(), false, true, true);

        refreshButton.addActionListener(actionEvent -> {
            ThreadingService threadingService = new ThreadingService();
            threadingService.runWithNotification(
                    new Thread(() -> refreshProjectList()),
                    new ObjectFinder().findCurrentProject(),
                    window);
        });

        return refreshButton;
    }

    public void refreshProjectList() {
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            new CourseAndExerciseManager().initiateDatabase();
            ApplicationManager.getApplication().invokeLater(() -> {
                logger.info("Refreshing project list. @ProjectListWindow");
                ProjectListManagerHolder.get().refreshAllCourses();
            });
        });
    }


    {
// GUI initializer generated by IntelliJ IDEA GUI Designer
// >>> IMPORTANT!! <<<
// DO NOT EDIT OR ADD ANY CODE HERE!
        setupUi();
    }

    /**
     * Method generated by IntelliJ IDEA GUI Designer.
     * >>> IMPORTANT!! <<<
     * DO NOT edit this method OR call it in your code!
     *
     * @noinspection ALL
     */
    private void setupUi() {
        basePanel = new JPanel();
        basePanel.setLayout(new GridLayoutManager(3, 3, new Insets(0, 0, 0, 0), -1, -1));

        tabbedPaneBase = new JTabbedPane();

        basePanel.add(tabbedPaneBase, new GridConstraints(1, 0, 1, 3,
                GridConstraints.ANCHOR_CENTER,
                GridConstraints.FILL_BOTH,
                GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
                GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
                null, new Dimension(200, 200), null, 0, false));

        toolbar = new JToolBar();
        toolbar.setBorderPainted(false);
        toolbar.setFloatable(false);
        toolbar.setForeground(new Color(-16777216));

        basePanel.add(toolbar, new GridConstraints(0, 0, 1, 3, GridConstraints.ANCHOR_CENTER,
                GridConstraints.FILL_HORIZONTAL,
                GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null,
                new Dimension(-1, 20), null, 0, false));

        final Spacer spacer1 = new Spacer();
        basePanel.add(spacer1, new GridConstraints(2, 1, 1, 1, GridConstraints.ANCHOR_CENTER,
                GridConstraints.FILL_HORIZONTAL,
                GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
    }

    /**
     * @noinspection ALL.
     */
    public JComponent getRootComponent() {
        return basePanel;
    }

    {
// GUI initializer generated by IntelliJ IDEA GUI Designer
// >>> IMPORTANT!! <<<
// DO NOT EDIT OR ADD ANY CODE HERE!
        $$$setupUI$$$();
    }

    /**
     * Method generated by IntelliJ IDEA GUI Designer
     * >>> IMPORTANT!! <<<
     * DO NOT edit this method OR call it in your code!
     *
     * @noinspection ALL
     */
    private void $$$setupUI$$$() {
        basePanel = new JPanel();
        basePanel.setLayout(new GridLayoutManager(3, 3, new Insets(0, 0, 0, 0), -1, -1));
        tabbedPaneBase = new JTabbedPane();
        basePanel.add(tabbedPaneBase, new GridConstraints(1, 0, 1, 3, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, new Dimension(200, 200), null, 0, false));
        toolbar = new JToolBar();
        toolbar.setBorderPainted(false);
        toolbar.setFloatable(false);
        toolbar.setForeground(new Color(-16777216));
        basePanel.add(toolbar, new GridConstraints(0, 0, 1, 3, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(-1, 20), null, 0, false));
        final Spacer spacer1 = new Spacer();
        basePanel.add(spacer1, new GridConstraints(2, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
    }

    /**
     * @noinspection ALL
     */
    public JComponent $$$getRootComponent$$$() {
        return basePanel;
    }
}
