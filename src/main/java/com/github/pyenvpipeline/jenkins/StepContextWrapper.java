package com.github.pyenvpipeline.jenkins;

import com.github.pyenvpipeline.jenkins.steps.WithPythonEnvStep;
import hudson.model.TaskListener;
import hudson.util.LogTaskListener;
import org.jenkinsci.plugins.workflow.steps.StepContext;

import java.io.PrintStream;
import java.util.logging.Level;
import java.util.logging.Logger;

public class StepContextWrapper {
    private static final Logger LOGGER = Logger.getLogger(WithPythonEnvStep.class.getCanonicalName());
    private StepContext stepContext;
    private boolean isUnix;
    private String workspaceDirectory;

    public StepContextWrapper(StepContext stepContext, boolean isUnix, String workspaceDirectory) {
        this.stepContext = stepContext;
        this.isUnix = isUnix;
        this.workspaceDirectory = workspaceDirectory;
    }

    public String getWorkspaceDirectory() {
        return workspaceDirectory;
    }

    public StepContext getStepContext() {
        return stepContext;
    }

    public boolean isUnix() {
        return isUnix;
    }

    public PrintStream logger() {
        TaskListener l;
        try {
            l = stepContext.get(TaskListener.class);
            if (l != null) {
                LOGGER.log(Level.FINEST, "JENKINS-34021: DurableTaskStep.Execution.listener present in {0}", stepContext);
            } else {
                LOGGER.log(Level.WARNING, "JENKINS-34021: TaskListener not available upon request in {0}", stepContext);
                l = new LogTaskListener(LOGGER, Level.FINE);
            }
        } catch (Exception x) {
            LOGGER.log(Level.FINE, "JENKINS-34021: could not get TaskListener in " + stepContext, x);
            l = new LogTaskListener(LOGGER, Level.FINE);
        }
        return l.getLogger();
    }
}
