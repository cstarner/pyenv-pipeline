package com.github.pyenvpipeline.jenkins.steps;

import com.google.common.collect.ImmutableSet;
import hudson.EnvVars;
import hudson.Extension;
import hudson.model.TaskListener;
import hudson.tasks.Shell;
import org.jenkinsci.plugins.durabletask.BourneShellScript;
import org.jenkinsci.plugins.durabletask.DurableTask;
import org.jenkinsci.plugins.workflow.steps.StepContext;
import org.jenkinsci.plugins.workflow.steps.StepExecution;
import org.jenkinsci.plugins.workflow.steps.durable_task.BatchScriptStep;
import org.jenkinsci.plugins.workflow.steps.durable_task.DurableTaskStep;
import org.jenkinsci.plugins.workflow.steps.durable_task.ShellStep;
import org.kohsuke.stapler.DataBoundConstructor;

import java.io.Serializable;
import java.util.Set;

/**
 * Created by Colin Home on 5/22/2017.
 */
public class PyEnvShellStep extends DurableTaskStep implements Serializable {

    private String script;

    @DataBoundConstructor public PyEnvShellStep(String script) {
        if (script==null)
            throw new IllegalArgumentException();
        this.script = script;
    }

    public String getScript() {
        return script;
    }

    @Override protected DurableTask task() {
        return new BourneShellScript(script);
    }

    @Override public StepExecution start(StepContext context) throws Exception {
        String path = context.get(EnvVars.class).get("PATH");
        if (path != null && path.contains("$PATH")) {
            context.get(TaskListener.class).getLogger().println("Warning: JENKINS-41339 probably bogus PATH=" + path + "; perhaps you meant to use ‘PATH+EXTRA=/something/bin’?");
        }

        String commandPrefix = context.get(EnvVars.class).get(PyEnvConstants.ENV_VAR_KEY);
        if (commandPrefix != null){
            StringBuilder stringBuilder = new StringBuilder(this.script);
            stringBuilder.insert(0, commandPrefix);
            this.script = stringBuilder.toString();
        }

        return super.start(context);
    }

    @Extension
    public static final class DescriptorImpl extends DurableTaskStepDescriptor {

        @Override public String getDisplayName() {
            return "PyEnvVar Shell Script";
        }

        @Override public String getFunctionName() {
            return "pysh";
        }

        @Override
        public Set<? extends Class<?>> getProvidedContext() {
            return ImmutableSet.of(EnvVars.class);
        }
    }
}
