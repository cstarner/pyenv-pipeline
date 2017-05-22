package com.github.pyenvpipeline.jenkins.steps;
import hudson.*;
import hudson.model.Descriptor;
import hudson.model.Node;
import hudson.remoting.Channel;
import net.sf.json.JSONObject;
import org.jenkinsci.plugins.workflow.steps.*;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.StaplerRequest;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

public class WithPythonEnvStep extends Step {

    private String pythonEnvLocation;
    private static final String DEFAULT_DIR = ".pyenv";

    @Override
    public StepExecution start(StepContext stepContext) throws Exception {
        return new Execution(this, stepContext);
    }

    @DataBoundSetter
    public void setPythonEnvLocation(String pythonEnvLocation){
        if ("".equals(pythonEnvLocation)) {
            this.pythonEnvLocation = DEFAULT_DIR;
        } else {
            this.pythonEnvLocation = pythonEnvLocation;
        }
    }

    public String getPythonEnvLocation(){
        return this.pythonEnvLocation;
    }


    private static class Execution extends StepExecution {

        private final WithPythonEnvStep step;

        protected Execution(final WithPythonEnvStep step, final StepContext context){
            super(context);
            this.step = step;
        }

        @Override
        public boolean start() throws Exception {
            StepContext context = getContext();
            context.newBodyInvoker().
                    withContext(context).
                    withCallback(BodyExecutionCallback.wrap(getContext())).
                    start();
            return false;
        }

        @Override
        public void stop(@Nonnull Throwable throwable) throws Exception {

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
            return Collections.emptySet();
        }
    }

    private class PyenvLauncherDecorate extends LauncherDecorator {

        @Override
        public Launcher decorate(final Launcher base, Node node) {

            return new Launcher(base) {
                @Override
                public Proc launch(ProcStarter procStarter) throws IOException {
                    return null;
                }

                @Override
                public Channel launchChannel(String[] strings, OutputStream outputStream, FilePath filePath, Map<String, String> map) throws IOException, InterruptedException {
                    return null;
                }

                @Override
                public void kill(Map<String, String> map) throws IOException, InterruptedException {
                    base.kill(map);
                }
            };
        }
    }
}
