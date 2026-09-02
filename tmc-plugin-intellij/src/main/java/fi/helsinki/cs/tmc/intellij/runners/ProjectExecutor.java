package fi.helsinki.cs.tmc.intellij.runners;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.Executor;
import com.intellij.execution.RunManager;
import com.intellij.execution.RunnerAndConfigurationSettings;
import com.intellij.execution.configurations.ModuleBasedConfiguration;
import com.intellij.execution.executors.DefaultRunExecutor;
import com.intellij.execution.runners.ExecutionEnvironmentBuilder;
import com.intellij.execution.util.ExecutionErrorDialog;
import com.intellij.openapi.project.Project;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ProjectExecutor {

    private static final Logger logger = LoggerFactory.getLogger(ProjectExecutor.class);

    public ProjectExecutor() {
        logger.info("ProjectExecutor initialized.");
    }

    public void executeConfiguration(Project project, ModuleBasedConfiguration appCon) {
        if (project == null || project.isDisposed()) {
            logger.warn("No usable project found, can't execute the configuration.");
            return;
        }
        logger.info("Starting to build execution environment.");
        RunManager runManager = RunManager.getInstance(project);
        Executor executor = DefaultRunExecutor.getRunExecutorInstance();
        RunnerAndConfigurationSettings selectedConfiguration =
                runManager.createConfiguration(appCon, appCon.getFactory());
        try {
            logger.info("Executing project.");
            ExecutionEnvironmentBuilder.create(executor, selectedConfiguration)
                    .buildAndExecute();
        } catch (ExecutionException e1) {
            ExecutionErrorDialog.show(e1, "Error", project);
        }
    }
}
