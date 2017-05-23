package com.github.pyenvpipeline.jenkins.steps;
import com.google.common.collect.ImmutableSet;
import hudson.*;
import hudson.util.ArgumentListBuilder;
import org.jenkinsci.plugins.workflow.steps.*;
import org.kohsuke.stapler.DataBoundConstructor;
import javax.annotation.Nonnull;
import java.io.IOException;
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

            body = context.newBodyInvoker().
                    withContext(EnvironmentExpander.merge(context.get(EnvironmentExpander.class),
                            new ExpanderImpl(step.getCommandPrefix(context)))).
                    withCallback(BodyExecutionCallback.wrap(getContext())).
                    start();

            return false;
        }

        @Override
        public void stop(@Nonnull Throwable throwable) throws Exception {
            if (body != null ){
                body.cancel();
            }
        }
    }

    private static class ExpanderImpl extends EnvironmentExpander {

        private String venvCommand;

        public ExpanderImpl(String venvCommand){
            super();
            this.venvCommand = venvCommand;
        }

        @Override
        public void expand(@Nonnull EnvVars envVars) throws IOException, InterruptedException {
            envVars.put(PyEnvConstants.ENV_VAR_KEY, venvCommand);
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
            return ImmutableSet.of(EnvVars.class, Launcher.class);
        }
    }
}
