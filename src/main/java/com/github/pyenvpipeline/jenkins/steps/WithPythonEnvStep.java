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
import com.google.common.collect.ImmutableSet;
import hudson.*;
import hudson.model.TaskListener;
import hudson.tools.ToolDescriptor;
import hudson.tools.ToolInstallation;
import hudson.util.ArgumentListBuilder;
import hudson.util.LogTaskListener;
import org.jenkinsci.plugins.workflow.steps.*;
import org.kohsuke.stapler.DataBoundConstructor;

import javax.annotation.Nonnull;
import java.io.*;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.github.pyenvpipeline.jenkins.steps.PyEnvConstants.VALID_TOOL_DESCRIPTOR_IDS;

public class WithPythonEnvStep extends Step implements Serializable{

    private static final Logger LOGGER = Logger.getLogger(WithPythonEnvStep.class.getName());
    private static final String DEFAULT_DIR_PREFIX = ".pyenv";

    private String pythonInstallation;

    @DataBoundConstructor
    public WithPythonEnvStep(String pythonInstallation){
        this.pythonInstallation = pythonInstallation;
    }

    public String getPythonInstallation() {
        return pythonInstallation;
    }

    @Override
    public StepExecution start(StepContext stepContext) throws Exception {
        return new Execution(this, stepContext);
    }

    public String getRelativePythonEnvDirectory(){

        String postfix = pythonInstallation.replaceAll("/", "-")
                .replaceFirst("C:\\\\", "")
                .replaceAll("\\\\", "-");

        if (!postfix.startsWith("-")) {
            postfix = "-" + postfix;
        }

        return DEFAULT_DIR_PREFIX + postfix;
    }

    private static class Execution extends StepExecution {

        private boolean usingShiningPanda;

        public String getFullyQualifiedPythonEnvDirectoryName(StepContext stepContext, boolean isUnix, String relativeDir) throws Exception{
            EnvVars envVars = stepContext.get(EnvVars.class);
            String workspace = envVars.get("WORKSPACE");

            if (isUnix) {
                return workspace + "/" + relativeDir;
            } else {
                return workspace + "\\" + relativeDir;
            }
        }

        private String getBaseToolDirectory() {
            List<String> validToolDescriptors = Arrays.asList(VALID_TOOL_DESCRIPTOR_IDS);
            for (ToolDescriptor<?> desc : ToolInstallation.all()) {

                if (!validToolDescriptors.contains(desc.getId())) {
                    LOGGER.info("Skipping ToolDescriptor: "+ desc.getId());
                    continue;
                }
                LOGGER.info("Found Python ToolDescriptor: " + desc.getId());
                ToolInstallation[] installations = unboxToolInstallations(desc);
                for (ToolInstallation installation : installations) {
                    if (installation.getName().equals(step.getPythonInstallation())) {
                        String notification = "Matched ShiningPanda tool name: " + installation.getName();
                        logger().println(notification);
                        LOGGER.info(notification);
                        usingShiningPanda = true;
                        return installation.getHome();
                    } else {
                        LOGGER.info("Skipping ToolInstallation: "+step.getPythonInstallation());
                    }
                }
            }

            return "";
        }

        private<T extends ToolInstallation> T[] unboxToolInstallations(ToolDescriptor<T> descriptor) {
            return descriptor.getInstallations();
        }

        private final WithPythonEnvStep step;
        private BodyExecution body;

        protected Execution(final WithPythonEnvStep step, final StepContext context){
            super(context);
            this.step = step;
            usingShiningPanda = false;
        }

        private void createPythonEnv(StepContext stepContext, boolean isUnix, String relativeDir) throws Exception{
            String fullQualifiedDirectoryName = getFullyQualifiedPythonEnvDirectoryName(stepContext, isUnix, relativeDir);
            String baseToolDirectory = getBaseToolDirectory();

            String commandPath;

            if (!baseToolDirectory.equals("")) {

                // ShiningPanda returns actual Python instances for Linux, but only returns folders for Windows
                if (usingShiningPanda && !isUnix) {
                    baseToolDirectory += "\\python";
                }

                commandPath = baseToolDirectory;
            } else {
                commandPath = step.getPythonInstallation();
            }

            if (!commandPath.contains("python")) {
                commandPath += "python";
            }

            LOGGER.info("Creating virtualenv at " + fullQualifiedDirectoryName + " using Python installation " +
                    "found at " + commandPath);

            ArgumentListBuilder command = new ArgumentListBuilder();

            command.add(commandPath);
            command.add("-m");
            command.add("virtualenv");
            command.add(fullQualifiedDirectoryName);

            Launcher launcher = stepContext.get(Launcher.class);

            Launcher.ProcStarter procStarter = launcher.launch();
            procStarter.cmds(command);
            Proc proc = procStarter.start();

            proc.join();
            LOGGER.info("Created virtualenv");
        }

        private PrintStream logger() {
            TaskListener l;
            StepContext context = getContext();
            try {
                l = context.get(TaskListener.class);
                if (l != null) {
                    LOGGER.log(Level.FINEST, "JENKINS-34021: DurableTaskStep.Execution.listener present in {0}", context);
                } else {
                    LOGGER.log(Level.WARNING, "JENKINS-34021: TaskListener not available upon request in {0}", context);
                    l = new LogTaskListener(LOGGER, Level.FINE);
                }
            } catch (Exception x) {
                LOGGER.log(Level.FINE, "JENKINS-34021: could not get TaskListener in " + context, x);
                l = new LogTaskListener(LOGGER, Level.FINE);
            }
            return l.getLogger();
        }

        @Override
        public boolean start() throws Exception {
            StepContext context = getContext();

            boolean isUnix = context.get(Launcher.class).isUnix();

            String relativeDir = step.getRelativePythonEnvDirectory();

            if (!context.get(FilePath.class).child(step.getRelativePythonEnvDirectory()).exists()) {
                createPythonEnv(context, isUnix, relativeDir);
            }

            String absoluteDir = getFullyQualifiedPythonEnvDirectoryName(context, isUnix, relativeDir);

            body = context.newBodyInvoker().
                    withContext(EnvironmentExpander.merge(context.get(EnvironmentExpander.class),
                            new ExpanderImpl(relativeDir, absoluteDir))).
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

        private String relativeDir;
        private String absoluteLocation;

        public ExpanderImpl(String relativeDir, String absoluteLocation) {
            super();
            this.relativeDir = relativeDir;
            this.absoluteLocation = absoluteLocation;
        }

        @Override
        public void expand(@Nonnull EnvVars envVars) throws IOException, InterruptedException {
            envVars.put(PyEnvConstants.VIRTUALENV_LOCATION_ENV_VAR_KEY, absoluteLocation);
            envVars.put(PyEnvConstants.VIRTUALENV_RELATIVE_DIRECTORY_NAME_ENV_VAR_KEY, relativeDir);
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
            return ImmutableSet.of(EnvVars.class, Launcher.class);
        }
    }
}
