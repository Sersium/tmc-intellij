package fi.helsinki.cs.tmc.intellij.actions;

import fi.helsinki.cs.tmc.intellij.holders.ProjectListManagerHolder;
import fi.helsinki.cs.tmc.intellij.ui.projectlist.ProjectListWindow;

import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import icons.TmcIcons;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OpenToolWindowAction extends AnAction implements ToolWindowFactory {

    private static final Logger logger = LoggerFactory.getLogger(OpenToolWindowAction.class);

    public OpenToolWindowAction() {
        super("TMC Exercises", "Open TMC Exercise List", TmcIcons.SIDE_PANEL);
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.BGT;
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
        e.getPresentation().setEnabledAndVisible(true);
        e.getPresentation().setIcon(TmcIcons.SIDE_PANEL);
        e.getPresentation().setText("TMC Exercises");
        e.getPresentation().setDescription("Open TMC Exercise List");
    }

    public void actionPerformed(AnActionEvent anActionEvent) {
        logger.info("Performing OpenToolWindowAction. @OpenToolWindowAction");
        openToolWindow(anActionEvent.getProject());
    }

    public void openToolWindow(Project project) {
        logger.info("Opening tool window. @OpenToolWindowAction");
        if (project == null) {
            logger.warn("project was null ending openToolWindow @OpenToolWindowAction");
            return;
        }
        ToolWindow projectList = ToolWindowManager.getInstance(project).getToolWindow("TMC Project List");

        if (projectList == null) {
            logger.warn("ToolWindow was null ending openToolWindow @OpenToolwindowAction");
            return;
        }

        if (projectList.isVisible()) {
            projectList.hide(null);
        } else {
            projectList.show(null);
        }
    }

    @Override
    public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {
        logger.info("Creating tool window content. @OpenToolWindowAction");
        ProjectListWindow window = new ProjectListWindow();
        ContentFactory cf = ContentFactory.getInstance();
        Content content = cf.createContent(window.getBasePanel(), "", true);
        toolWindow.getContentManager().addContent(content);
        ProjectListManagerHolder.get().addWindow(window);
    }
}
