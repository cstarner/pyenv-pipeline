package com.github.pyenvpipeline.jenkins.steps;

import org.jenkinsci.plugins.durabletask.DurableTask;
import org.jenkinsci.plugins.workflow.steps.StepContext;
import org.jenkinsci.plugins.workflow.steps.StepExecution;
import org.jenkinsci.plugins.workflow.steps.durable_task.DurableTaskStep;

import java.io.Serializable;

// Deprecated as of v2.0.0.
@Deprecated
public abstract class PyEnvDurableTaskBase extends DurableTaskStep implements Serializable {
    private DurableTask task;

    @Override
    protected DurableTask task() {
        return task;
    }

    void setTask(DurableTask task) {
        this.task = task;
    }

    abstract DurableTask getDurableTask();

    @Override public StepExecution start(StepContext context) throws Exception {
        setTask(getDurableTask());
        return super.start(context);
    }
}