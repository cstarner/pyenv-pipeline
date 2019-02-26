package com.github.pyenvpipeline.jenkins.steps;

import hudson.Extension;
import org.jenkinsci.plugins.durabletask.DurableTask;
import org.jenkinsci.plugins.durabletask.WindowsBatchScript;
import org.jenkinsci.plugins.workflow.steps.durable_task.DurableTaskStep;
import org.kohsuke.stapler.DataBoundConstructor;

@Deprecated
public class PyEnvBatStep extends PyEnvDurableTaskBase {
    private final String script;

    @Override
    protected DurableTask getDurableTask() {
        return new WindowsBatchScript(script);
    }

    @DataBoundConstructor
    public PyEnvBatStep(String script) {
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
            return "PyEnvVar Batch Script";
        }


        @Override public String getFunctionName() {
            return "pybat";
        }
    }
}
