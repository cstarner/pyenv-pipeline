package com.github.pyenvpipeline.jenkins.steps;

import hudson.DescriptorExtensionList;
import hudson.model.TaskListener;
import hudson.tools.ToolDescriptor;
import hudson.tools.ToolInstallation;
import jenkins.plugins.shiningpanda.tools.PythonInstallation;
import org.jenkinsci.plugins.workflow.steps.StepContext;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.jvnet.hudson.test.JenkinsRule;
import org.mockito.Mock;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.modules.junit4.PowerMockRunner;

import java.io.PrintStream;

@RunWith(PowerMockRunner.class)
@PowerMockIgnore({"javax.crypto.*" })
public class WithPythonEnvGetCommandPathTest {

    @Mock
    StepContext mockStepContext;

    @Mock
    TaskListener mockTaskListener;

    @Mock
    PrintStream mockPrintStream;

    @Rule
    public JenkinsRule j = new JenkinsRule();

    @Before
    public void setUp() throws Exception {
        PowerMockito.when(mockStepContext.get(TaskListener.class)).thenReturn(mockTaskListener);
        PowerMockito.when(mockTaskListener.getLogger()).thenReturn(mockPrintStream);
    }

    @Test
    public void testGetCommandPath() throws Exception {

        // Here we mock the ShiningPanda installations. For these, we need a StepContext that actually "works".

        PythonInstallation.DescriptorImpl descriptor = j.jenkins.getDescriptorByType(PythonInstallation.DescriptorImpl.class);
        PythonInstallation linuxInstallation = new PythonInstallation("CPython-2.7-Unix", "/usr/bin/python27", null);
        PythonInstallation windowsInstallation = new PythonInstallation("CPython-2.7-Windows", "C:\\Python27\\", null);

        // This crashes when it tries to persist the new installations to file. We swallow the exception and move on
        try {
            descriptor.setInstallations(linuxInstallation, windowsInstallation);
        } catch (Exception e) {

        }

        DescriptorExtensionList<ToolInstallation, ToolDescriptor<?>> list =
                DescriptorExtensionList.createDescriptorList(j.jenkins, ToolInstallation.class);
        list.add(descriptor);

        WithPythonEnvStep pathExecutable = new WithPythonEnvStep("python3");

        WithPythonEnvStep.Execution execution =
                new WithPythonEnvStep.Execution(pathExecutable, null);
        Assert.assertEquals("python3", execution.getCommandPath(true, list));

        WithPythonEnvStep linuxLiteralLocation = new WithPythonEnvStep("/usr/bin/python2.7");
        execution = new WithPythonEnvStep.Execution(linuxLiteralLocation, null);
        Assert.assertEquals("/usr/bin/python2.7", execution.getCommandPath(true, list));

        WithPythonEnvStep windowsLiteralLocation = new WithPythonEnvStep("C:\\Python34\\python");
        execution = new WithPythonEnvStep.Execution(windowsLiteralLocation, null);
        Assert.assertEquals("C:\\Python34\\python", execution.getCommandPath(false, list));

        // Linux ShiningPanda named plugin
        WithPythonEnvStep linuxShiningPanda = new WithPythonEnvStep("CPython-2.7-Unix");
        execution = new WithPythonEnvStep.Execution(linuxShiningPanda, mockStepContext);
        Assert.assertEquals("/usr/bin/python27", execution.getCommandPath(true, list));

        WithPythonEnvStep windowsShiningPanda = new WithPythonEnvStep("CPython-2.7-Windows");
        execution = new WithPythonEnvStep.Execution(windowsShiningPanda, mockStepContext);
        // Here we assure that a ShiningPanda tool on a Windows system automatically appends the executable name at the
        // end
        Assert.assertEquals("C:\\Python27\\python", execution.getCommandPath(false, list));
    }
}
