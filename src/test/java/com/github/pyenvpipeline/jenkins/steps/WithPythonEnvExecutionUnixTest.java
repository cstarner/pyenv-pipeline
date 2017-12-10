package com.github.pyenvpipeline.jenkins.steps;

import hudson.DescriptorExtensionList;
import hudson.tools.ToolDescriptor;
import hudson.tools.ToolInstallation;
import hudson.util.ArgumentListBuilder;
import jenkins.plugins.shiningpanda.tools.PythonInstallation;
import org.junit.Assert;
import org.junit.Test;


public class WithPythonEnvExecutionUnixTest extends WithPythonEnvExecutionTestBase {

    @Override
    String getWorkspace() {
        return "/home/user/.jenkins";
    }

    @Override
    PythonInstallation getPythonInstallation() {
        return new PythonInstallation("CPython-2.7-Unix", "/usr/bin/python27", null);
    }

    @Test
    public void testUnixGetCreateVirtualEnvCommand() throws Exception {
        // Ensure name with python is respected
        WithPythonEnvStep unixLiteralLocation = new WithPythonEnvStep("/usr/bin/python");
        WithPythonEnvStep.Execution execution = new WithPythonEnvStep.Execution(unixLiteralLocation, mockStepContext);
        ArgumentListBuilder createVirtualenvCommand = execution.getCreateVirtualEnvCommand(mockStepContext, true, ".pyenv-python");
        Assert.assertEquals("/usr/bin/python -m virtualenv --python=/usr/bin/python /home/user/.jenkins/.pyenv-python", createVirtualenvCommand.toString());

        // Ensure name without python has "python" appended to it
        WithPythonEnvStep unixDirectoryLocation = new WithPythonEnvStep("/usr/bin");
        execution = new WithPythonEnvStep.Execution(unixDirectoryLocation, mockStepContext);
        createVirtualenvCommand = execution.getCreateVirtualEnvCommand(mockStepContext, true, ".pyenv-python");
        Assert.assertEquals("/usr/bin/python -m virtualenv --python=/usr/bin/python /home/user/.jenkins/.pyenv-python", createVirtualenvCommand.toString());

        // Ensure that ShiningPanda works as expected
        WithPythonEnvStep unixShiningPandaLocation = new WithPythonEnvStep("CPython-2.7-Unix");
        execution = new WithPythonEnvStep.Execution(unixShiningPandaLocation, mockStepContext);
        createVirtualenvCommand = execution.getCreateVirtualEnvCommand(mockStepContext, true, ".pyenv-python");
        Assert.assertEquals("/usr/bin/python27 -m virtualenv --python=/usr/bin/python27 /home/user/.jenkins/.pyenv-python", createVirtualenvCommand.toString());
    }

    @Test
    public void testGetCommandPath() throws Exception {
        DescriptorExtensionList<ToolInstallation, ToolDescriptor<?>> list = mockDescriptorList();
        WithPythonEnvStep pathExecutable = new WithPythonEnvStep("python3");
        WithPythonEnvStep.Execution execution =
                new WithPythonEnvStep.Execution(pathExecutable, null);
        Assert.assertEquals("python3", execution.getCommandPath(true, list));

        WithPythonEnvStep linuxLiteralLocation = new WithPythonEnvStep("/usr/bin/python2.7");
        execution = new WithPythonEnvStep.Execution(linuxLiteralLocation, null);
        Assert.assertEquals("/usr/bin/python2.7", execution.getCommandPath(true, list));

        // Linux ShiningPanda named plugin
        WithPythonEnvStep linuxShiningPanda = new WithPythonEnvStep("CPython-2.7-Unix");
        execution = new WithPythonEnvStep.Execution(linuxShiningPanda, mockStepContext);
        Assert.assertEquals("/usr/bin/python27", execution.getCommandPath(true, list));
    }
}