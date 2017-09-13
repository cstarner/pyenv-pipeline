package com.github.pyenvpipeline.jenkins.steps;

import hudson.Extension;
import hudson.util.ArgumentListBuilder;
import org.jenkinsci.plugins.durabletask.DurableTask;
import org.jenkinsci.plugins.durabletask.WindowsBatchScript;
import org.jenkinsci.plugins.workflow.steps.durable_task.DurableTaskStep;
import org.kohsuke.stapler.DataBoundConstructor;


public class PyEnvBatStep extends PyEnvDurableTaskBase {

    private final String script;

    @Override
    public ArgumentListBuilder getArgumentList(String directoryName) {

        String commandLocation = directoryName + "\\Scripts\\activate";

        ArgumentListBuilder argumentListBuilder = new ArgumentListBuilder();
        argumentListBuilder.add(commandLocation);
        argumentListBuilder.add("&&");
        argumentListBuilder.add(script);

        return argumentListBuilder;
    }

    @DataBoundConstructor public PyEnvBatStep(String script) {
        if (script==null)
            throw new IllegalArgumentException();
        this.script = script;
    }

    public String getScript() {
        return script;
    }

    @Override protected DurableTask task() {
        return new WindowsBatchScript(getScript());
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
