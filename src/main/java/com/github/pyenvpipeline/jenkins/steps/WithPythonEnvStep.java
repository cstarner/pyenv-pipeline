package com.github.pyenvpipeline.jenkins.steps;
import com.google.common.base.Function;
import com.google.common.collect.ImmutableSet;
import hudson.*;
import hudson.model.TaskListener;
import hudson.util.ArgumentListBuilder;
import org.apache.tools.ant.taskdefs.Parallel;
import org.jenkinsci.plugins.durabletask.DurableTask;
import org.jenkinsci.plugins.workflow.steps.*;
import org.jenkinsci.plugins.workflow.steps.durable_task.DurableTaskStep;
import org.kohsuke.stapler.DataBoundConstructor;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.PrintStream;
import java.io.Serializable;
import java.util.Set;

public class WithPythonEnvStep extends Step implements Serializable{

    private String pythonVersion;
    private static final String DEFAULT_DIR_PREFIX = ".pyenv";


    @DataBoundConstructor
    public WithPythonEnvStep(String pythonVersion){
        this.pythonVersion = pythonVersion;
    }

    public String getPythonVersion() {
        return pythonVersion;
    }

    public String getCommandPrefix(StepContext stepContext) throws Exception {
        return ". "+getFullyQualifiedPythonEnvDirectoryName(stepContext) + "/bin/activate; ";
    }

    public String getRelativePythonEnvDirectory(){
        return DEFAULT_DIR_PREFIX +"-"+ pythonVersion.replaceAll("/", "-");
    }

    public String getFullyQualifiedPythonEnvDirectoryName(StepContext stepContext) throws Exception{
        EnvVars envVars = stepContext.get(EnvVars.class);
        String workspace = envVars.get("WORKSPACE");
        return workspace + "/" + getRelativePythonEnvDirectory();
    }

    @Override
    public StepExecution start(StepContext stepContext) throws Exception {
        return new Execution(this, stepContext);
    }


    private static class Execution extends StepExecution {

        private final WithPythonEnvStep step;
        private BodyExecution body;

        protected Execution(final WithPythonEnvStep step, final StepContext context){
            super(context);
            this.step = step;
        }

        private void createPythonEnv(StepContext stepContext) throws Exception{
            ArgumentListBuilder command = new ArgumentListBuilder();

            command.add("virtualenv");
            command.add("--python="+step.getPythonVersion());
            command.add(step.getFullyQualifiedPythonEnvDirectoryName(stepContext));

            Launcher launcher = stepContext.get(Launcher.class);
            Launcher.ProcStarter procStarter = launcher.launch();
            procStarter.cmds(command);
            procStarter.join();
        }

        @Override
        public boolean start() throws Exception {
            StepContext context = getContext();

            // Setup environment properly

            if (!context.get(FilePath.class).child(step.getRelativePythonEnvDirectory()).exists()) {
                createPythonEnv(context);
            }

            // Create a merged LauncherDecorator, add it to the BodyInvoker, and run the code block

            WithPythonEnvLauncherDecorator launcherDecorator = new WithPythonEnvLauncherDecorator(step.getCommandPrefix(context), step.getPythonVersion());
            LauncherDecorator merged = BodyInvoker.mergeLauncherDecorators(context.get(LauncherDecorator.class), launcherDecorator);

           PrintStream logger = context.get(TaskListener.class).getLogger();

            body = context.newBodyInvoker().
                    withContext(merged).
                    withCallback(BodyExecutionCallback.wrap(getContext())).
                    start();

            for (StepExecution stepExecution : body.getCurrentExecutions()) {
                logger.println(stepExecution.getClass().getCanonicalName());
            }

            return false;
        }

        @Override
        public void stop(@Nonnull Throwable throwable) throws Exception {
            if (body != null ){
                body.cancel();
            }
        }
    }

    @Extension
    public static class DescriptorImpl extends StepDescriptor {

        @Override
        public String getFunctionName() {
            return "withPythonEnv";
        }

        @Override
        public boolean takesImplicitBlockArgument() {
            return true;
        }

        @Override
        public Set<? extends Class<?>> getRequiredContext() {
            return ImmutableSet.of(TaskListener.class, FilePath.class, Launcher.class, EnvVars.class);
        }
    }
}
