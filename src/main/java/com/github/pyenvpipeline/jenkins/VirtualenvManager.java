package com.github.pyenvpipeline.jenkins;

import com.github.pyenvpipeline.jenkins.steps.WithPythonEnvStep;
import hudson.*;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.tools.ToolDescriptor;
import hudson.tools.ToolInstallation;
import hudson.util.ArgumentListBuilder;
import hudson.util.LogTaskListener;
import org.jenkinsci.plugins.durabletask.BourneShellScript;
import org.jenkinsci.plugins.durabletask.Controller;
import org.jenkinsci.plugins.durabletask.DurableTask;
import org.jenkinsci.plugins.durabletask.WindowsBatchScript;
import org.jenkinsci.plugins.workflow.steps.StepContext;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.io.Serializable;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;


public class VirtualenvManager implements Serializable {

    private static final Logger LOGGER = Logger.getLogger(VirtualenvManager.class.getName());
    private static final List<String> ENVVARS_TO_IGNORE = Arrays.asList("HUDSON_COOKIE");
    private static HashMap<String, EnvVars> virtualenvMap = new HashMap<>();
    static final String[] VALID_TOOL_DESCRIPTOR_IDS = {"jenkins.plugins.shiningpanda.tools.PythonInstallation"};
    private static final String DEFAULT_DIR_PREFIX = ".pyenv";

    private static VirtualenvManager instance;

    private VirtualenvManager() {
    }

    public static VirtualenvManager getInstance() {
        if (instance == null) {
            instance = new VirtualenvManager();
        }

        return instance;
    }

    private String runCommandList(ArgumentListBuilder command, StepContext stepContext) throws Exception {
        // TODO: Rewrite this to work with DurableTask
        ByteArrayOutputStream outputBaos = new ByteArrayOutputStream();

        Launcher launcher = stepContext.get(Launcher.class);

        Launcher.ProcStarter procStarter = launcher.launch();
        procStarter.cmds(command);
        procStarter.stderr(outputBaos);
        procStarter.stdout(outputBaos);

        Proc proc = procStarter.start();

        int exitCode = proc.join();

        Run run = stepContext.get(Run.class);
        String capturedOutput = outputBaos.toString(run.getCharset().name());

        if (exitCode != 0) {
            String errorMessage = "Error while creating virtualenv: " + capturedOutput;
            stepContext.onFailure(new AbortException(errorMessage));
            LOGGER.warning(errorMessage);
        } else {
            LOGGER.fine(capturedOutput);
            LOGGER.info("Created virtualenv");
        }

        return capturedOutput;
    }

    private void createPythonEnv(StepContext stepContext, String pythonInstallation) throws Exception{
        ArgumentListBuilder command = getCreateVirtualEnvCommand(stepContext, pythonInstallation);
        runCommandList(command, stepContext);
    }

    public String getFullyQualifiedPythonEnvDirectoryName(StepContext stepContext, String pythonInstallation) throws Exception{
        EnvVars envVars = stepContext.get(EnvVars.class);
        String workspace = envVars.get("WORKSPACE");
        boolean isUnix = isUnix(stepContext);
        String relativeDir = getRelativePythonEnvDirectory(pythonInstallation);

        if (isUnix) {
            return workspace + "/" + relativeDir;
        } else {
            return workspace + "\\" + relativeDir;
        }
    }

    public ArgumentListBuilder getCreateVirtualEnvCommand(StepContext context, String pythonInstallation) throws Exception {
        String fullQualifiedDirectoryName = getFullyQualifiedPythonEnvDirectoryName(context, pythonInstallation);
        String commandPath = getCommandPath(pythonInstallation, context, ToolInstallation.all());
        LOGGER.info("Creating virtualenv at " + fullQualifiedDirectoryName + " using Python installation " +
                "found at " + commandPath);

        ArgumentListBuilder command = new ArgumentListBuilder();

        command.add(commandPath);
        command.add("-m");
        command.add("virtualenv");
        command.add("--python="+commandPath);
        command.add(fullQualifiedDirectoryName);

        return command;
    }

    private DurableTask getVirtualenvDurableTask(String directoryName, boolean isUnix) {
        if (isUnix) {
            return getVirtualenvUnixDurableTask(directoryName);
        } else {
            return getVirtualenvWindowsDurableTask(directoryName);
        }
    }

    private WindowsBatchScript getVirtualenvWindowsDurableTask(String directoryName) {
        String script = "@CALL \"" + directoryName + "\\Scripts\\activate.bat\"\nSET";
        return new WindowsBatchScript(script);
    }

    private BourneShellScript getVirtualenvUnixDurableTask(String directoryName) {
        String script = ". " + directoryName + "/bin/activate; env";
        return new BourneShellScript(script);
    }

    private DurableTask getPreVirtualenvTask(boolean isUnix) {
        if (isUnix) {
            return getUnixPreVirtualenvTask();
        } else {
            return getWindowsPreVirtualenvTask();
        }
    }

    private WindowsBatchScript getWindowsPreVirtualenvTask() {
        return new WindowsBatchScript("SET");
    }

    private BourneShellScript getUnixPreVirtualenvTask() {
        return new BourneShellScript("env");
    }

    private EnvVars fromEnvOutput(String commandOutput) {
        EnvVars result = new EnvVars();

        for (String line: commandOutput.split(Pattern.quote("\n"))) {

            boolean skip = false;

            for (String envVarToIgnore : ENVVARS_TO_IGNORE) {
                skip = line.contains(envVarToIgnore);

                if (skip) {
                    break;
                }
            }

            if (!skip) {
                result.addLine(line);
            }
        }

        return result;
    }

    public EnvVars getVirtualEnvEnvVars(WithPythonEnvStep step, StepContext stepContext) throws Exception {
        EnvVars result = virtualenvMap.get(step.getPythonInstallation());

        if (result == null) {
            String pythonInstallation = step.getPythonInstallation();
            String virtualenvDirectory = getFullyQualifiedPythonEnvDirectoryName(stepContext, pythonInstallation);

            if (!stepContext.get(FilePath.class).child(virtualenvDirectory).exists()) {
                createPythonEnv(stepContext, step.getPythonInstallation());
            }

            result = diffEnvironments(stepContext, virtualenvDirectory);
        }

        return result;
    }

    private String runTaskAndCapture(DurableTask task, StepContext context) throws Exception {
        task.captureOutput();

        FilePath filePath = context.get(FilePath.class);
        Launcher launcher = context.get(Launcher.class);

        Controller controller = task.launch(context.get(EnvVars.class), filePath, launcher, context.get(TaskListener.class));

        Integer exitCode = null;

        do {
            Thread.sleep(100);
            exitCode = controller.exitStatus(filePath, launcher);
        } while(exitCode == null);

       return new String(controller.getOutput(filePath, launcher), context.get(Run.class).getCharset());
    }

    protected EnvVars diffEnvironments(StepContext stepContext, String relativeDir) throws Exception {
        Launcher launcher = stepContext.get(Launcher.class);

        DurableTask preEnvChangeTask = getPreVirtualenvTask(launcher.isUnix());
        String preEnvChangeOutput = runTaskAndCapture(preEnvChangeTask, stepContext);
        EnvVars preEnvChange = fromEnvOutput(preEnvChangeOutput);

        DurableTask activateTask = getVirtualenvDurableTask(relativeDir, launcher.isUnix());
        String postChangeOutput = runTaskAndCapture(activateTask, stepContext);
        EnvVars postEnvChange = fromEnvOutput(postChangeOutput);

        EnvVars result = new EnvVars();

        for (Map.Entry<String, String> entry : postEnvChange.entrySet()) {
            String originalValue = preEnvChange.get(entry.getKey());

            if (entry.getKey().equals("PATH")) {
                // This is so we comply with how Jenkins expects the PATH variable to be modified within the
                // pipeline. We also know that the PATH variable is present in any circumstance, as well as
                // changed by virtualenv.
                String pathAddition = processPathValues(originalValue, entry.getValue(), stepContext);
                result.put("PATH+PYTHON", pathAddition);
            } else {
                if (originalValue == null || !originalValue.equals(entry.getValue())) {
                    result.put(entry.getKey(), entry.getValue());
                }
            }
        }
        return result;
    }

    protected String processPathValues(String originalPath, String newPath, StepContext context) throws Exception {
        List<String> newPortions = new ArrayList<>();
        String pathSeparator = isUnix(context) ? getUnixPathSeparator() : getWindowsPathSeparator();

        List<String> originalPathEntries = new ArrayList<>(Arrays.asList(originalPath.split(pathSeparator)));
        List<String> newPathEntries = new ArrayList<>(Arrays.asList(newPath.split(pathSeparator)));

        // It appears that virtualenv modifies the PATH variable by appending the path to the appropriate virtualenv
        // folder to the front of the path variable. In order to grab these values, we will pop the front of the new
        // path off, until the first values of both lists match.

        while (!originalPathEntries.get(0).equals(newPathEntries.get(0))) {
            String entry = newPathEntries.remove(0);
            newPortions.add(entry);
        }

        StringBuilder builder = new StringBuilder();

        for (String pathPortion: newPortions) {
            builder.append(pathPortion);
            builder.append(pathSeparator);
        }

        String result = builder.toString();

        if (result.length() > 1) {
            result =  result.substring(0, result.length() - pathSeparator.length());
        }

        return result;
    }

    private String getWindowsPathSeparator() {
        return ";";
    }

    private String getUnixPathSeparator() {
        return ":";
    }

    protected String getWindowsCommandPath(String baseToolDirectory, String pythonInstallation) throws Exception {

        String result;

        // ShiningPanda on Windows only gives us the directory, not the full path. However, we will catch this
        // further down. Here we only care if no ShiningPanda tool was found
        if (baseToolDirectory.equals("")) {
            result = pythonInstallation;
        } else {
            result = baseToolDirectory;
        }

        String[] portions = result.split(Pattern.quote("\\"));
        String pathEnd = portions[portions.length-1];

        if (!pathEnd.contains("python")) {
            if (result.length() > 0 && !result.endsWith("\\")) {
                result += "\\";
            }

            result += "python.exe";
        }

        if (!result.endsWith(".exe")) {
            result += ".exe";
        }

        return result;
    }

    private boolean isUnix(StepContext context) throws Exception {
        return context.get(Launcher.class).isUnix();
    }

    protected String getCommandPath(String pythonInstallation, StepContext context, DescriptorExtensionList<ToolInstallation, ToolDescriptor<?>> descriptors) throws Exception {

        boolean isUnix = isUnix(context);
        String baseToolDirectory = getBaseToolDirectory(pythonInstallation, context, descriptors);
        if (isUnix) {
            return getUnixCommandPath(baseToolDirectory, pythonInstallation);
        } else {
            return getWindowsCommandPath(baseToolDirectory, pythonInstallation);
        }
    }

    protected String getUnixCommandPath(String baseToolDirectory, String pythonInstallation) throws Exception {
        if (!baseToolDirectory.equals("")) {
            // ShiningPanda, on Linux, returns direct links to Python Exceutables
            return baseToolDirectory;
        } else {
            // This either points to a little python executable, or a directory that contains one
            String commandPathBase = pythonInstallation;

            String[] portions = commandPathBase.split(Pattern.quote("/"));
            String lastPortion = portions[portions.length-1];

            if (!lastPortion.contains("python")) {
                if (!commandPathBase.endsWith("/")) {
                    commandPathBase += "/";
                }

                commandPathBase += "python";
            }

            return commandPathBase;
        }
    }

    private String getBaseToolDirectory(String pythonInstallation, StepContext context, DescriptorExtensionList<ToolInstallation, ToolDescriptor<?>> descriptors) {
        List<String> validToolDescriptors = Arrays.asList(VALID_TOOL_DESCRIPTOR_IDS);
        for (ToolDescriptor<?> desc : descriptors) {

            if (!validToolDescriptors.contains(desc.getId())) {
                LOGGER.info("Skipping ToolDescriptor: "+ desc.getId());
                continue;
            }
            LOGGER.info("Found Python ToolDescriptor: " + desc.getId());
            ToolInstallation[] installations = unboxToolInstallations(desc);
            for (ToolInstallation installation : installations) {
                if (installation.getName().equals(pythonInstallation)) {
                    String notification = "Matched ShiningPanda tool name: " + installation.getName();
                    logger(context).println(notification);
                    LOGGER.info(notification);
                    return installation.getHome();
                } else {
                    LOGGER.info("Skipping ToolInstallation: "+pythonInstallation);
                }
            }
        }

        return "";
    }

    private<T extends ToolInstallation> T[] unboxToolInstallations(ToolDescriptor<T> descriptor) {
        return descriptor.getInstallations();
    }

    private PrintStream logger(StepContext context) {
        TaskListener l;
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

    public String getRelativePythonEnvDirectory(String pythonInstallation){
        String postfix = pythonInstallation.replaceAll("/", "-")
                .replaceFirst("[a-zA-Z]:\\\\", "")
                .replaceAll("\\\\", "-");

        if (!postfix.startsWith("-")) {
            postfix = "-" + postfix;
        }

        return DEFAULT_DIR_PREFIX + postfix;
    }

}
