/*
 * The MIT License
 *
 * Copyright 2017 Colin Starner.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 *
 */

package com.github.pyenvpipeline.jenkins.steps;
import com.github.pyenvpipeline.jenkins.VirtualenvManager;
import com.google.common.collect.ImmutableSet;
import hudson.*;
import hudson.model.Run;
import org.jenkinsci.plugins.workflow.steps.*;
import org.kohsuke.stapler.DataBoundConstructor;

import javax.annotation.Nonnull;
import java.io.*;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;


public class WithPythonEnvStep extends Step implements Serializable{
    private static final Logger LOGGER = Logger.getLogger(WithPythonEnvStep.class.getName());
    private String pythonInstallation;

    @DataBoundConstructor
    public WithPythonEnvStep(String pythonInstallation){
        this.pythonInstallation = pythonInstallation;
    }

    public String getPythonInstallation() {
        return pythonInstallation.trim();
    }

    @Override
    public StepExecution start(StepContext stepContext) throws Exception {
        return new Execution(this, stepContext);
    }

    protected static class Execution extends StepExecution {

        private final WithPythonEnvStep step;
        private BodyExecution body;
        private VirtualenvManager virtualenvManager;

        protected Execution(final WithPythonEnvStep step, final StepContext context){
            super(context);
            this.step = step;
            virtualenvManager = VirtualenvManager.getInstance();
        }

        @Override
        public boolean start() throws Exception {
            StepContext context = getContext();

            ExpanderImpl expander = new ExpanderImpl(virtualenvManager.getVirtualEnvEnvVars(step, context));

            body = context.newBodyInvoker().
                    withContext(EnvironmentExpander.merge(context.get(EnvironmentExpander.class), expander)).
                    withCallback(BodyExecutionCallback.wrap(getContext())).
                    start();
            return false;
        }

        @Override
        public void stop(@Nonnull Throwable throwable) throws Exception {
            if (body != null ) {
                body.cancel();
            }
        }
    }

    private static class ExpanderImpl extends EnvironmentExpander {

        private EnvVars virtualEnvVars;

        public ExpanderImpl(EnvVars virtualenvVars) {
            super();
            virtualEnvVars = virtualenvVars;
        }

        @Override
        public void expand(@Nonnull EnvVars envVars) throws IOException, InterruptedException {

            if (virtualEnvVars != null) {

                for (Map.Entry<String, String> entry : virtualEnvVars.entrySet()) {
                    envVars.put(entry.getKey(), entry.getValue());
                }
            }
        }
    }

    @Extension
    public static final class DescriptorImpl extends StepDescriptor {

        @Override public String getDisplayName() {
            return "Code Block";
        }

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
            return ImmutableSet.of(EnvVars.class, Launcher.class, Run.class);
        }
    }
}
