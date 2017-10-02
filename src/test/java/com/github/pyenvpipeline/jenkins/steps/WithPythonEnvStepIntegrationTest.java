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

import hudson.Functions;
import hudson.tools.ToolDescriptor;
import hudson.tools.ToolInstallation;
import jenkins.plugins.shiningpanda.tools.PythonInstallation;
import jenkins.plugins.shiningpanda.tools.PythonInstallationFinder;
import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.LoggerRule;
import java.util.List;
import java.util.logging.Level;
import java.util.regex.Pattern;
import static org.junit.Assert.assertTrue;

public class WithPythonEnvStepIntegrationTest {

    private static final String OS_REPLACE_TARGET ="{{OS_SHELL_COMMAND}}";

    @Rule
    public JenkinsRule j = new JenkinsRule();

    @Rule
    public LoggerRule loggingRule = new LoggerRule();

    private static final String shiningPandaTargetInstallation = "jenkins.plugins.shiningpanda.tools.PythonInstallation";

    private<T extends ToolInstallation> T[] unboxToolInstallations(ToolDescriptor<T> descriptor) {
        return descriptor.getInstallations();
    }

    private String formatOSSpecificNodeScipts(String osAgnosticScript) {
        if (Functions.isWindows()) {
            return osAgnosticScript.replaceAll(Pattern.quote(OS_REPLACE_TARGET), "pybat");
        } else {
            return osAgnosticScript.replaceAll(Pattern.quote(OS_REPLACE_TARGET), "pysh");
        }
    }

    @Test
    public void shouldSetEnvVar() throws Exception {
        // We only test the relative dir name here, since we can't easily predict the full directory name
        WorkflowJob job = j.jenkins.createProject(WorkflowJob.class, "p");
        job.setDefinition(new CpsFlowDefinition("node { withPythonEnv('python') { echo \"current virtualenv " +
                "relative dir: ${" + PyEnvConstants.VIRTUALENV_RELATIVE_DIRECTORY_NAME_ENV_VAR_KEY + "}\" } }",
                true));
        WorkflowRun run = j.assertBuildStatusSuccess(job.scheduleBuild2(0));
        j.assertLogContains("current virtualenv relative dir: .pyenv-python", run);
    }

    private PythonInstallation findSinglePythonInstallation(ToolDescriptor descriptor) throws Exception {

        PythonInstallation installation = null;

        if (descriptor.getId().equals(shiningPandaTargetInstallation)) {
            ToolInstallation[] installations = unboxToolInstallations(descriptor);

            if (installations.length > 0) {
                installation = (PythonInstallation) installations[0];
            } else {
                List<PythonInstallation> foundInstallations = PythonInstallationFinder.getInstallations();

                if (foundInstallations.size() > 0) {
                    installation = foundInstallations.get(0);
                }
            }
        }

        return installation;
    }

    private PythonInstallation findFirstPythonInstallation() throws Exception{
        PythonInstallation installation = null;

        for (ToolDescriptor<? extends ToolInstallation> desc: ToolInstallation.all()) {
            installation = findSinglePythonInstallation(desc);
            if (installation != null) {
                PythonInstallation.DescriptorImpl cast = (PythonInstallation.DescriptorImpl) desc;
                cast.setInstallations(installation);
                break;
            }
        }

        return installation;
    }

    @Test
    public void ensureDurableTaskCompatibility() throws Exception {
        // We ensure that returnStdout works by examining the lines of output from a Pipeline script that activates
        // a withPythonEnv block, and outputs the version of the Python command that prints the version of the
        // interpreter. If the prefix "python version: " is found in a line of the logs, and that line contains any
        // other text after the prefix, we will know that the returnStdout optional argument works for these commands
        // in a manner compatible with the "sh" and "bat" steps.

        PythonInstallation installation = findFirstPythonInstallation();

        // Python versions prior to 3.4 outputed the results of "python --version" to stderr; 3.4 and after use stdout.
        // Additionally, we can't use print in this instance, due to statement incompatibility between Python 2 and
        // Python 3. This is a semantically equivalent workaround that should work regardless of Python version
        String platformInlinePythonCommand = "python -c \\\"import platform; import sys; sys.stdout.write(platform.python_version()+\\'\\\\n\\')\\\"";

        WorkflowJob job = j.jenkins.createProject(WorkflowJob.class, "p");
        String script = formatOSSpecificNodeScipts("node { withPythonEnv('" + installation.getName() + "') { def version = " +
                OS_REPLACE_TARGET +"(script: '" + platformInlinePythonCommand + "', returnStdout: true).trim()\n" +
                "echo \"python version: ${version}\"" +
                "}  }");
        job.setDefinition(new CpsFlowDefinition(script,true));

        WorkflowRun run = j.assertBuildStatusSuccess(job.scheduleBuild2(0));

        String logs = JenkinsRule.getLog(run);

        boolean foundOutput = false;
        String prefix = "python version: ";
        for (String line: logs.split("\n")) {
            if (line.startsWith(prefix)) {
                foundOutput = !line.equals(prefix);
                break;
            }
        }

        Assert.assertTrue(foundOutput);
    }

    @Test
    public void shouldUseShiningPanda() throws Exception {
        // Here, we dictate a single PythonInstallation to be used for the ShiningPanda test, so
        // that we can pick the appropriate name, and verify that it is used later down the line
        // Note that this will not work if there are no findable Python Installations on the testing
        // system (i.e. findable via the PythonFinder class provided by the ShiningPanda plugin).
        PythonInstallation installation = findFirstPythonInstallation();

        String workflowScript = "node { withPythonEnv('python') { echo \"current virtualenv relative dir: ${" +
                PyEnvConstants.VIRTUALENV_RELATIVE_DIRECTORY_NAME_ENV_VAR_KEY + "}\" }";

        if (installation!=null) {
            workflowScript += "\nwithPythonEnv('" + installation.getName() + "') { echo \"current virtualenv absolute " +
                    "dir: ${" + PyEnvConstants.VIRTUALENV_LOCATION_ENV_VAR_KEY + "}\" }";
        }

        workflowScript += "}";

        loggingRule = loggingRule.capture(30);
        loggingRule.record(WithPythonEnvStep.class, Level.FINE);
        WorkflowJob job = j.jenkins.createProject(WorkflowJob.class, "p");
        job.setDefinition(new CpsFlowDefinition(workflowScript,
                true));
        j.assertBuildStatusSuccess(job.scheduleBuild2(0));
        // Verifies that the presence of ShiningPanda installation is respected
        List<String> messages = loggingRule.getMessages();

        String expectedFoundFlaggedShiningPandaString = "Found Python ToolDescriptor: "
                + shiningPandaTargetInstallation;
        boolean foundExpectedFlaggedShiningPanda = false;
        String expectedMatchedShiningPandaString = "Matched ShiningPanda tool name: " + installation.getName();
        boolean foundExpectedMatchedShiningPanda = false;

        for (String mess : messages) {
            if (!foundExpectedFlaggedShiningPanda) {
                foundExpectedFlaggedShiningPanda = mess.equals(expectedFoundFlaggedShiningPandaString);
            }

            if (!foundExpectedMatchedShiningPanda) {
                foundExpectedMatchedShiningPanda = mess.equals(expectedMatchedShiningPandaString);
            }

            if (foundExpectedFlaggedShiningPanda && foundExpectedMatchedShiningPanda) {
                break;
            }
        }

        assertTrue(foundExpectedFlaggedShiningPanda);
        assertTrue(foundExpectedMatchedShiningPanda);
    }
}
