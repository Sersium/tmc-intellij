package fi.helsinki.cs.tmc.intellij.actions.buttonactions;

import fi.helsinki.cs.tmc.intellij.runners.RunProject;

import com.intellij.execution.RunManager;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.vfs.VirtualFile;
import icons.TmcIcons;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RunProjectAction extends AnAction {

    private static final Logger logger = LoggerFactory.getLogger(RunProjectAction.class);

    public RunProjectAction() {
        super("TMC Run", "Run current project (TMC Run)", TmcIcons.RUN_BUTTON);
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.BGT;
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
        e.getPresentation().setEnabledAndVisible(true);
        e.getPresentation().setIcon(TmcIcons.RUN_BUTTON);
        e.getPresentation().setText("TMC Run");
        e.getPresentation().setDescription("Run current project (TMC Run)");
    }

    @Override
    public void actionPerformed(AnActionEvent anActionEvent) {
        runProject(anActionEvent.getProject());
    }

    /**
     * Main run method, readies project module for running.
     */
    public void runProject(Project project) {
        logger.info("Run project action called.");
        if (project == null || project.isDisposed()) {
            return;
        }
        logger.info("Getting RunManager.");
        RunManager runManager = RunManager.getInstance(project);
        Module module = getModule(project);
        if (module == null) {
            logger.warn("No module found for running project.");
            return;
        }
        String configurationType = getConfigurationType();
        logger.info("Creating RunProject object.");
        new RunProject(runManager, module, configurationType);
    }

    @NotNull
    private String getConfigurationType() {
        logger.info("Getting configurationtype.");
        return "Application";
    }

    private Module getModule(Project project) {
        logger.info("Trying to find module.");
        try {
            VirtualFile[] roots =
                    ProjectRootManager.getInstance(project).getContentRootsFromAllModules();
            if (roots != null && roots.length > 0) {
                Module mod = ProjectRootManager.getInstance(project).getFileIndex().getModuleForFile(roots[0]);
                if (mod != null) {
                    return mod;
                }
            }
            Module[] modules = ModuleManager.getInstance(project).getModules();
            if (modules != null && modules.length > 0) {
                return modules[0];
            }
        } catch (Exception e) {
            logger.warn("Failed to find module for project", e);
        }
        return null;
    }
}
