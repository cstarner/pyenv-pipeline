package com.github.pyenvpipeline.jenkins.steps;

import hudson.Extension;
import org.jenkinsci.plugins.durabletask.BourneShellScript;
import org.jenkinsci.plugins.durabletask.DurableTask;
import org.jenkinsci.plugins.workflow.steps.durable_task.DurableTaskStep;
import org.kohsuke.stapler.DataBoundConstructor;

@Deprecated
public class PyEnvShellStep extends PyEnvDurableTaskBase {

    private final String script;

    @Override
    protected DurableTask getDurableTask() {
        return new BourneShellScript(script);
    }

    @DataBoundConstructor
    public PyEnvShellStep(String script) {
        if (script==null)
            throw new IllegalArgumentException();
        this.script = script;
    }

    public String getScript() {
        return script;
    }

    @Extension
    public static final class DescriptorImpl extends DurableTaskStep.DurableTaskStepDescriptor {

        @Override public String getDisplayName() {
            return "PyEnvVar Shell Script";
        }

        @Override public String getFunctionName() {
            return "pysh";
        }
    }
}
