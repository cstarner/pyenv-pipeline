package com.github.pyenvpipeline.jenkins.containers;

import hudson.model.Computer;
import hudson.model.Node;
import hudson.model.Slave;
import hudson.model.TaskListener;
import hudson.plugins.sshslaves.SSHLauncher;
import hudson.slaves.ComputerListener;
import hudson.slaves.DumbSlave;
import hudson.slaves.NodeProperty;
import hudson.slaves.RetentionStrategy;
import org.jenkinsci.test.acceptance.docker.DockerContainer;
import org.jenkinsci.test.acceptance.docker.DockerFixture;
import org.jenkinsci.test.acceptance.docker.fixtures.JavaContainer;
import org.jenkinsci.test.acceptance.docker.fixtures.SshdContainer;
import org.jvnet.hudson.test.JenkinsRule;

import java.util.Collections;
import java.util.concurrent.CountDownLatch;

@DockerFixture(id = "linux-python", ports = 22)
public class UnixPythonContainer extends JavaContainer {

    // Taken verbatim from the Mercurial Plugin tests
    @SuppressWarnings("deprecation")
    public Slave createSlave(JenkinsRule r) throws Exception {
        DumbSlave slave = new DumbSlave("slave" + r.jenkins.getNodes().size(),
                "dummy", "/home/test/slave", "1", Node.Mode.NORMAL, "pyenv-pipeline",
                new SSHLauncher(ipBound(22), port(22), "test", "test", "", ""),
                RetentionStrategy.INSTANCE, Collections.<NodeProperty<?>>emptyList());
        r.jenkins.addNode(slave);
        // Copied from JenkinsRule:
        final CountDownLatch latch = new CountDownLatch(1);
        ComputerListener waiter = new ComputerListener() {
            @Override public void onOnline(Computer C, TaskListener t) {
                latch.countDown();
                unregister();
            }
        };
        waiter.register();
        latch.await();
        return slave;
    }

}
