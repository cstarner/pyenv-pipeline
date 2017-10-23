/*
 * The MIT License
 *
 * Copyright 2017 Colin Starner.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 *
 */

package com.github.pyenvpipeline.jenkins.steps;

import hudson.EnvVars;
import hudson.model.TaskListener;
import org.jenkinsci.plugins.durabletask.DurableTask;
import org.jenkinsci.plugins.workflow.steps.StepContext;
import org.jenkinsci.plugins.workflow.steps.StepExecution;
import org.jenkinsci.plugins.workflow.steps.durable_task.DurableTaskStep;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public abstract class PyEnvDurableTaskBase extends DurableTaskStep implements Serializable {
    private static final Logger LOGGER = Logger.getLogger(PyEnvBatStep.class.getName());
    private DurableTask task;

    @Override
    protected DurableTask task() {
        return task;
    }

    protected void setTask(DurableTask task) {
        this.task = task;
    }

    protected abstract DurableTask getDurableTask(String fullScript);

    protected List<String> splitWhileRespectingQuotes(String script, boolean stripQuotes) {
        // Helper function intended to split the input command into separate portions
        // Splits along spaces, except in the presence of quotation marks
        List<String> result = new ArrayList<>();

        Matcher m = Pattern.compile("([^\"]\\S*|\".+?\")\\s*").matcher(script);
        while(m.find()) {

            String s = m.group(1);

            if (stripQuotes) {
                s = s.replace("\"", "");
            }

            result.add(s);
        }

        return result;
    }

    public abstract String getFullScript(String directoryName);

    @Override public StepExecution start(StepContext context) throws Exception {
        String path = context.get(EnvVars.class).get("PATH");
        if (path != null && path.contains("$PATH")) {
            context.get(TaskListener.class).getLogger().println("Warning: JENKINS-41339 probably bogus PATH=" + path + "; perhaps you meant to use ‘PATH+EXTRA=/something/bin’?");
        }

        String absoluteDirectoryName = context.get(EnvVars.class).get(PyEnvConstants.VIRTUALENV_LOCATION_ENV_VAR_KEY);

        if (absoluteDirectoryName != null) {
            String script = getFullScript(absoluteDirectoryName);
            LOGGER.fine("Full command: " + script);
            DurableTask task = getDurableTask(script);
            setTask(task);
        }

        return super.start(context);
    }


}
