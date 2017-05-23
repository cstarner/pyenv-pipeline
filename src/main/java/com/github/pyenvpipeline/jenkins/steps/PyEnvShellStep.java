package com.github.pyenvpipeline.jenkins.steps;

import hudson.tasks.Shell;
import org.jenkinsci.plugins.durabletask.DurableTask;
import org.jenkinsci.plugins.workflow.steps.durable_task.BatchScriptStep;
import org.jenkinsci.plugins.workflow.steps.durable_task.DurableTaskStep;
import org.jenkinsci.plugins.workflow.steps.durable_task.ShellStep;
import org.kohsuke.stapler.DataBoundConstructor;

import java.io.Serializable;

/**
 * Created by Colin Home on 5/22/2017.
 */
public class PyEnvShellStep extends DurableTaskStep implements Serializable {

    private String script;

    @DataBoundConstructor
    public PyEnvShellStep(String script){
        super();
        this.script = script;
    }

    @Override
    protected DurableTask task() {
        return null;
    }


}
