package com.github.pyenvpipeline.jenkins.steps;

import com.github.pyenvpipeline.jenkins.containers.UnixPythonContainer;
import hudson.model.Action;
import hudson.model.Slave;
import org.jenkinsci.plugins.workflow.actions.WorkspaceAction;
import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition;
import org.jenkinsci.plugins.workflow.cps.nodes.StepStartNode;
import org.jenkinsci.plugins.workflow.flow.FlowExecution;
import org.jenkinsci.plugins.workflow.graph.FlowGraphWalker;
import org.jenkinsci.plugins.workflow.graph.FlowNode;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.jenkinsci.test.acceptance.docker.DockerClassRule;
import org.jenkinsci.test.acceptance.docker.DockerFixture;
import org.junit.*;
import org.jvnet.hudson.test.JenkinsRule;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.assertTrue;

public class UnixWithPythonEnvStepIntergrationTest {

    @Rule
    public JenkinsRule jenkins = new JenkinsRule();

    @ClassRule
    public static DockerClassRule<UnixPythonContainer> docker = new DockerClassRule<>(UnixPythonContainer.class);

    private Slave slave;

    @Before
    public void setUpClass() throws Exception {
        UnixPythonContainer container = docker.create();
        slave = container.createSlave(jenkins);
    }

    private String getWorkflowRunWorkspace(WorkflowRun run) {
        FlowExecution exec = run.getExecution();
        if(exec == null)
            return null;
        FlowGraphWalker w = new FlowGraphWalker(exec);
        for (FlowNode n : w) {
            if (n instanceof StepStartNode) {
                WorkspaceAction action = (WorkspaceAction) n.getAction(WorkspaceAction.class);
                if(action != null) {
                    return action.getPath();
                }
            }
        }

        return null;
    }

    private String generatePythonNodeBlock(String virtualEnvArgument) {
        return "node('" + slave.getNodeName() + "') {\n" +
                "withPythonEnv('" + virtualEnvArgument + "') {\n" +
                "def pythonOutput = sh(script: 'python -c \\\"import platform; import sys; sys.stdout.write(platform.python_version()+\\'\\\\n\\')\\\"', returnStdout: true)\n" +
                "def pipOutput = sh(script: 'pip -V', returnStdout: true)\n" +
                "echo \"pythonOutput: $pythonOutput\"\n" +
                "echo \"pipOutput: $pipOutput\"" +
                "}\n" +
                " }";
    }

    private String joinString(String delimiter, List<String> portions) {
        StringBuilder stringBuilder = new StringBuilder();

        for (String portion: portions) {
            stringBuilder.append(portion);
            stringBuilder.append(delimiter);
        }

        int lastIndex = (2 * portions.size()) - 1;
        stringBuilder.replace(lastIndex, lastIndex, "");
        return stringBuilder.toString();
    }

    private void runTest(String virtualEnvArgument, String expectedOutput) throws Exception {

        WorkflowJob job = jenkins.jenkins.createProject(WorkflowJob.class, virtualEnvArgument);
        String script = generatePythonNodeBlock(virtualEnvArgument);
        job.setDefinition(new CpsFlowDefinition(script, true));
        System.out.println(jenkins.jenkins.getWorkspaceFor(job).getRemote());

        //jenkins.jenkins.setNodes(Arrays.asList(slave));
        WorkflowRun run = jenkins.assertBuildStatusSuccess(job.scheduleBuild2(0));
        System.out.println(getWorkflowRunWorkspace(run));
        List<String> runLogs = run.getLog(100);

        String errorMessage = String.format("Failed test for virtualenv argument \"%s\":\n%s",
                virtualEnvArgument, joinString("\n", runLogs));
        assertTrue(errorMessage, runLogs.contains(expectedOutput));
    }

    @Test
    public void withPythonEnvIntegrationTest() throws Exception {
        runTest("python3.5", "pythonOutput: 3.5.6");
    }

    @Test
    public void withPythonEnvIntegrationTest3_4() throws Exception {
        runTest("python3.4", "pythonOutput: 3.4.9");
    }

    @Test
    public void withPythonEnvIntegrationTest2_7() throws Exception {
        runTest("python2.7", "pythonOutput: 2.7.15rc1");
    }

    @Test
    public void withPythonEnvIntegrationTestManagedVirtualenv() throws Exception {
        runTest("/var/managed_virtualenv/", "pythonOutput: 3.4.9");
    }
}
