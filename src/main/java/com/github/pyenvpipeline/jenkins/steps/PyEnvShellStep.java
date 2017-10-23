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

import hudson.Extension;
import org.apache.commons.lang.StringUtils;
import org.jenkinsci.plugins.durabletask.BourneShellScript;
import org.jenkinsci.plugins.durabletask.DurableTask;
import org.jenkinsci.plugins.workflow.steps.durable_task.DurableTaskStep;
import org.kohsuke.stapler.DataBoundConstructor;

import java.util.ArrayList;


public class PyEnvShellStep extends PyEnvDurableTaskBase {

    private final String script;

    @Override
    protected DurableTask getDurableTask(String fullScript) {
        return new BourneShellScript(fullScript);
    }

    @Override
    public String getFullScript(String directoryName) {

        if (!directoryName.endsWith("/")) {
            directoryName += "/";
        }

        String commandLocation = directoryName + "bin/activate";

        if (commandLocation.contains(" ")) {
            commandLocation = "\"" + commandLocation + "\"";
        }

        commandLocation += ";";

        ArrayList<String> portions = new ArrayList<>();
        portions.add(".");
        portions.add(commandLocation);
        portions.addAll(splitWhileRespectingQuotes(script, false));

        return StringUtils.join(portions, " ");
    }

    @DataBoundConstructor public PyEnvShellStep(String script) {
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
