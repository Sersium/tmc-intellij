package fi.helsinki.cs.tmc.intellij.runners;

import com.intellij.execution.RunManager;
import com.intellij.ide.util.TreeClassChooser;
import com.intellij.openapi.module.Module;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RunProject {

    private static final Logger logger = LoggerFactory.getLogger(RunProject.class);

    private final RunConfigurationFactory factory;

    public RunProject(RunManager runManager, Module module, String configurationType) {
        logger.info("Creating RunConfigurationFactory.");
        factory = new RunConfigurationFactory(runManager, module, configurationType);
        if (makeSureConfigurationIsCorrectType(runManager)) {
            return;
        }
        factory.createRunner();
    }

    private boolean makeSureConfigurationIsCorrectType(RunManager runManager) {
        if (runManager.getSelectedConfiguration() == null || factory.checkConfigurationType()) {
            logger.info("Prompting user to choose main class with Chooser.");
            final TreeClassChooser[] chooserHolder = new TreeClassChooser[1];
            if (com.intellij.openapi.application.ApplicationManager.getApplication().isDispatchThread()) {
                chooserHolder[0] = factory.chooseMainClassForProject();
            } else {
                com.intellij.openapi.application.ApplicationManager.getApplication().invokeAndWait(() -> {
                    chooserHolder[0] = factory.chooseMainClassForProject();
                });
            }
            TreeClassChooser chooser = chooserHolder[0];
            if (chooser == null || chooser.getSelected() == null) {
                logger.warn("Choosing main class returned null and running is cancelled.");
                return true;
            }
            logger.info("Creating configurations.");
            Runnable createAndConfig = () -> {
                factory.createConfiguration();
                factory.configApplicationConfiguration(chooser);
            };
            if (com.intellij.openapi.application.ApplicationManager.getApplication().isDispatchThread()) {
                com.intellij.openapi.application.WriteIntentReadAction.run(createAndConfig);
            } else {
                com.intellij.openapi.application.ApplicationManager.getApplication().invokeAndWait(() ->
                        com.intellij.openapi.application.WriteIntentReadAction.run(createAndConfig));
            }
        }
        return false;
    }
}
