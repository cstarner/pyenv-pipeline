package com.github.pyenvpipeline.jenkins.steps;

import hudson.DescriptorExtensionList;
import hudson.tools.ToolDescriptor;
import hudson.tools.ToolInstallation;
import hudson.util.ArgumentListBuilder;
import jenkins.plugins.shiningpanda.tools.PythonInstallation;
import org.junit.Assert;
import org.junit.Test;

public class WithPythonEnvExecutionWindowsTest extends WithPythonEnvExecutionTestBase {

    @Override
    String getWorkspace() {
        return "C:\\Jenkins\\workspace\\foo";
    }

    @Override
    PythonInstallation getPythonInstallation() {
        return new PythonInstallation("CPython-2.7-Windows", "C:\\Python27\\", null);
    }

    @Test
    public void testGetCommandPath() throws Exception {
        DescriptorExtensionList<ToolInstallation, ToolDescriptor<?>> list = mockDescriptorList();
        WithPythonEnvStep pathExecutable = new WithPythonEnvStep("python3");

        WithPythonEnvStep.Execution execution =
                new WithPythonEnvStep.Execution(pathExecutable, mockStepContext);
        Assert.assertEquals("python3.exe", execution.getCommandPath(false, list));

        WithPythonEnvStep windowsLiteralLocation = new WithPythonEnvStep("C:\\Python34\\python");
        execution = new WithPythonEnvStep.Execution(windowsLiteralLocation, mockStepContext);
        Assert.assertEquals("C:\\Python34\\python.exe", execution.getCommandPath(false, list));

        WithPythonEnvStep windowsShiningPanda = new WithPythonEnvStep("CPython-2.7-Windows");
        execution = new WithPythonEnvStep.Execution(windowsShiningPanda, mockStepContext);
        // Here we assure that a ShiningPanda tool on a Windows system automatically appends the executable name at the
        // end
        Assert.assertEquals("C:\\Python27\\python.exe", execution.getCommandPath(false, list));
    }


    @Test
    public void testWindowsGetCreateVirtualEnvCommand() throws Exception {

        // Ensure that a python without exe is corrected
        WithPythonEnvStep windowsLiteralLocation = new WithPythonEnvStep("C:\\Python34\\python");
        WithPythonEnvStep.Execution execution = new WithPythonEnvStep.Execution(windowsLiteralLocation, mockStepContext);
        ArgumentListBuilder createVirtualenvCommand = execution.getCreateVirtualEnvCommand(mockStepContext, false, ".pyenv-python");
        Assert.assertEquals("C:\\Python34\\python.exe -m virtualenv --python=C:\\Python34\\python.exe C:\\Jenkins\\workspace\\foo\\.pyenv-python", createVirtualenvCommand.toString());

        WithPythonEnvStep windowsDefault = new WithPythonEnvStep("python");
        execution = new WithPythonEnvStep.Execution(windowsDefault, mockStepContext);
        createVirtualenvCommand = execution.getCreateVirtualEnvCommand(mockStepContext, false, ".pyenv-python");
        Assert.assertEquals("python.exe -m virtualenv --python=python.exe C:\\Jenkins\\workspace\\foo\\.pyenv-python", createVirtualenvCommand.toString());

        // Ensure that a blank installation defaults to python.exe on Windows
        WithPythonEnvStep windowsBlankLocation = new WithPythonEnvStep("");
        execution = new WithPythonEnvStep.Execution(windowsBlankLocation, mockStepContext);
        createVirtualenvCommand = execution.getCreateVirtualEnvCommand(mockStepContext, false, ".pyenv-python");
        Assert.assertEquals("python.exe -m virtualenv --python=python.exe C:\\Jenkins\\workspace\\foo\\.pyenv-python", createVirtualenvCommand.toString());

        // Ensure that ShiningPanda tools work as intended
        mockDescriptorList();

        WithPythonEnvStep windowsShiningPandaLocation = new WithPythonEnvStep("CPython-2.7-Windows");
        execution = new WithPythonEnvStep.Execution(windowsShiningPandaLocation, mockStepContext);
        createVirtualenvCommand = execution.getCreateVirtualEnvCommand(mockStepContext, false, ".pyenv-python");
        Assert.assertEquals("C:\\Python27\\python.exe -m virtualenv --python=C:\\Python27\\python.exe C:\\Jenkins\\workspace\\foo\\.pyenv-python", createVirtualenvCommand.toString());

        WithPythonEnvStep windowsPython3Location = new WithPythonEnvStep("python3");
        execution = new WithPythonEnvStep.Execution(windowsPython3Location, mockStepContext);
        createVirtualenvCommand = execution.getCreateVirtualEnvCommand(mockStepContext, false, ".pyenv-python");
        Assert.assertEquals("python3.exe -m virtualenv --python=python3.exe C:\\Jenkins\\workspace\\foo\\.pyenv-python", createVirtualenvCommand.toString());
    }
}
