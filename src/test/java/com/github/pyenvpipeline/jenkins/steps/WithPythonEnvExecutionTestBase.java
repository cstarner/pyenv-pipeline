package com.github.pyenvpipeline.jenkins.steps;

import hudson.DescriptorExtensionList;
import hudson.EnvVars;
import hudson.model.TaskListener;
import hudson.tools.ToolDescriptor;
import hudson.tools.ToolInstallation;
import jenkins.plugins.shiningpanda.tools.PythonInstallation;
import org.jenkinsci.plugins.workflow.steps.StepContext;
import org.junit.Before;
import org.junit.Rule;
import org.junit.runner.RunWith;
import org.jvnet.hudson.test.JenkinsRule;
import org.mockito.Mock;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.modules.junit4.PowerMockRunner;

import java.io.PrintStream;

@RunWith(PowerMockRunner.class)
@PowerMockIgnore({"javax.crypto.*" })
public abstract class WithPythonEnvExecutionTestBase {

    @Mock
    StepContext mockStepContext;

    @Mock
    TaskListener mockTaskListener;

    @Mock
    PrintStream mockPrintStream;

    @Mock
    EnvVars mockEnvVars;

    @Rule
    public JenkinsRule j = new JenkinsRule();

    @Before
    public void setUp() throws Exception {
        PowerMockito.when(mockStepContext.get(TaskListener.class)).thenReturn(mockTaskListener);
        PowerMockito.when(mockStepContext.get(EnvVars.class)).thenReturn(mockEnvVars);
        PowerMockito.when(mockTaskListener.getLogger()).thenReturn(mockPrintStream);
        PowerMockito.when(mockEnvVars.get("WORKSPACE")).thenReturn(getWorkspace());
        mockDescriptorList();
    }

    abstract String getWorkspace();
    abstract PythonInstallation getPythonInstallation();


    DescriptorExtensionList<ToolInstallation, ToolDescriptor<?>> mockDescriptorList() {
        // Here we mock the ShiningPanda installations. For these, we need a StepContext that actually "works".

        PythonInstallation.DescriptorImpl descriptor = j.jenkins.getDescriptorByType(PythonInstallation.DescriptorImpl.class);

        PythonInstallation installation = getPythonInstallation();

        // This crashes when it tries to persist the new installations to file. We swallow the exception and move on
        try {
            descriptor.setInstallations(installation);
        } catch (Exception e) {

        }

        DescriptorExtensionList<ToolInstallation, ToolDescriptor<?>> list =
                DescriptorExtensionList.createDescriptorList(j.jenkins, ToolInstallation.class);
        list.add(descriptor);

        return list;
    }
}
