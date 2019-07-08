package com.github.pyenvpipeline.jenkins;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class UnitTests {

    @Test
    public void testPathSeparatorHandling() {

        // Demonstrating that the presence of the path separator at the end of the workspace directory has no bearing
        // on the output

        WorkspaceVirtualenv workspaceVirtualenv1 = new WorkspaceVirtualenv("Python3.7", true,
                "/var/lib/jenkins/workspace/test-lib-dev-216/");

        assertEquals("/var/lib/jenkins/workspace/test-lib-dev-216/.pyenv-Python3.7", workspaceVirtualenv1.getVirtualEnvPath());

        WorkspaceVirtualenv workspaceVirtualenv2 = new WorkspaceVirtualenv("Python3.7", true,
                "/var/lib/jenkins/workspace/test-lib-dev-216");

        assertEquals("/var/lib/jenkins/workspace/test-lib-dev-216/.pyenv-Python3.7", workspaceVirtualenv2.getVirtualEnvPath());

        // Doing the same for Windows

        WorkspaceVirtualenv windowsWorkspaceVirtualenv1 = new WorkspaceVirtualenv("Python3.7", false,
                "C:\\jenkins\\workspace\\test-lib-dev-216\\");

        assertEquals("C:\\jenkins\\workspace\\test-lib-dev-216\\.pyenv-Python3.7", windowsWorkspaceVirtualenv1.getVirtualEnvPath());

        WorkspaceVirtualenv windowsWorkspaceVirtualenv2 = new WorkspaceVirtualenv("Python3.7", false,
                "C:\\jenkins\\workspace\\test-lib-dev-216");

        assertEquals("C:\\jenkins\\workspace\\test-lib-dev-216\\.pyenv-Python3.7", windowsWorkspaceVirtualenv2.getVirtualEnvPath());
    }
}
