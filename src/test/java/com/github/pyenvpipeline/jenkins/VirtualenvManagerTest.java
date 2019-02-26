package com.github.pyenvpipeline.jenkins;

import hudson.Launcher;
import org.jenkinsci.plugins.workflow.steps.StepContext;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.modules.junit4.PowerMockRunner;

import static org.junit.Assert.*;

@RunWith(PowerMockRunner.class)
public class VirtualenvManagerTest {

    @Mock
    StepContext mockStepContext;

    @Mock
    Launcher mockLauncher;

    @Before
    public void setUp() throws Exception {
        PowerMockito.when(mockStepContext.get(Launcher.class)).thenReturn(mockLauncher);
    }

    @Test
    public void testWindowsVirtualEnvPathHandling() throws Exception {
        PowerMockito.when(mockLauncher.isUnix()).thenReturn(false);
        VirtualenvManager manager = VirtualenvManager.getInstance();
        StepContextWrapper stepContextWrapper = new StepContextWrapper(mockStepContext, false, "");

        String originalPath = "C:\\Python27\\;C:\\Python27\\Scripts;C:\\Program Files (x86)\\Common Files\\Oracle\\Java\\javapath;";
        String newPath = "C:\\Programming\\Python3\\test\\venv\\Scripts;C:\\Python27\\;C:\\Python27\\Scripts;C:\\Program Files (x86)\\Common Files\\Oracle\\Java\\javapath;";

        String result = manager.processPathValues(originalPath, newPath, stepContextWrapper);
        assertEquals(result, "C:\\Programming\\Python3\\test\\venv\\Scripts");

        // Near as I can tell, virtualenv doesn't currently prepend more than one variable to the front of the
        // PATH variable. All the same, can't hurt to make sure

        newPath = "C:\\Programming\\Python3\\test\\venv;" + newPath;
        result = manager.processPathValues(originalPath, newPath, stepContextWrapper);
        assertEquals(result, "C:\\Programming\\Python3\\test\\venv;C:\\Programming\\Python3\\test\\venv\\Scripts");
    }

    @Test
    public void testUnixVirtualEnvPathHandling() throws Exception {
        PowerMockito.when(mockLauncher.isUnix()).thenReturn(true);
        VirtualenvManager manager = VirtualenvManager.getInstance();

        StepContextWrapper stepContextWrapper = new StepContextWrapper(mockStepContext, true, "");

        String originalPath = "/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin";
        String newPath = "/venv/bin:/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin";

        String result = manager.processPathValues(originalPath, newPath, stepContextWrapper);
        assertEquals(result, "/venv/bin");

        newPath = "/venv:" + newPath;
        result = manager.processPathValues(originalPath, newPath, stepContextWrapper);
        assertEquals(result, "/venv:/venv/bin");
    }
}
