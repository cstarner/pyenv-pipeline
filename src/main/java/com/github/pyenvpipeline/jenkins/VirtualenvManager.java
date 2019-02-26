package com.github.pyenvpipeline.jenkins;

import com.github.pyenvpipeline.jenkins.steps.WithPythonEnvStep;
import hudson.*;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.util.ArgumentListBuilder;
import org.jenkinsci.plugins.durabletask.BourneShellScript;
import org.jenkinsci.plugins.durabletask.Controller;
import org.jenkinsci.plugins.durabletask.DurableTask;
import org.jenkinsci.plugins.durabletask.WindowsBatchScript;
import org.jenkinsci.plugins.workflow.steps.StepContext;

import java.io.ByteArrayOutputStream;
import java.io.Serializable;
import java.util.*;
import java.util.logging.Logger;
import java.util.regex.Pattern;


public class VirtualenvManager implements Serializable {

    private static final Logger LOGGER = Logger.getLogger(VirtualenvManager.class.getName());
    private static final List<String> ENVVARS_TO_IGNORE = Arrays.asList("HUDSON_COOKIE");
    private static HashMap<String, EnvVars> virtualenvMap = new HashMap<>();

    private static VirtualenvManager instance;
    private List<? extends AbstractVirtualenvFactory> factories;

    private VirtualenvManager() {
        // This represents the priority order of the factories to be applied
        // We will use Tool first, Managed second, etc
        factories = Arrays.asList(
            new ToolVirtualenv.Factory(),
            new ManagedVirtualenv.Factory(),
            new WorkspaceVirtualenv.Factory()
        );
    }

    public static VirtualenvManager getInstance() {
        if (instance == null) {
            instance = new VirtualenvManager();
        }

        return instance;
    }

    public EnvVars getVirtualEnvEnvVars(WithPythonEnvStep step, StepContext stepContext) throws Exception {
        String withPythonEnvBlockArgument = step.getPythonInstallation();
        EnvVars result = virtualenvMap.get(step.getPythonInstallation());

        if (result == null) {
            StepContextWrapper stepContextWrapper = createStepContextWrapper(stepContext);
            AbstractVirtualenv abstractVirtualenv = generateVirtualenv(withPythonEnvBlockArgument, stepContextWrapper);

            if (abstractVirtualenv != null) {
                if (!stepContext.get(FilePath.class).child(abstractVirtualenv.getVirtualEnvPath()).exists()) {

                    if (abstractVirtualenv.canCreate()) {
                        createPythonEnv(stepContext, abstractVirtualenv);
                    }
                }

                result = diffEnvironments(stepContextWrapper, abstractVirtualenv.getVirtualEnvPath());
            } else {
                stepContextWrapper.logger().println("Could not determine virtualenv for argument \"" + withPythonEnvBlockArgument + "\"" );
            }
        }

        return result;
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

    private void createPythonEnv(StepContext stepContext, AbstractVirtualenv abstractVirtualenv) throws Exception{
        ArgumentListBuilder command = getCreateVirtualEnvCommand(stepContext, abstractVirtualenv);
        runCommandList(command, stepContext);
    }

    protected ArgumentListBuilder getCreateVirtualEnvCommand(StepContext context, AbstractVirtualenv abstractVirtualenv) throws Exception {
        String fullQualifiedDirectoryName = abstractVirtualenv.getVirtualEnvPath();
        String commandPath = abstractVirtualenv.getPythonInstallationPath();
        LOGGER.info("Creating virtualenv at " + fullQualifiedDirectoryName + " using Python installation " +
                "found at " + commandPath);

        // Checking version of python
        ArgumentListBuilder versionChecker = new ArgumentListBuilder();
        versionChecker.add(commandPath);
        versionChecker.add("--version");
        String versionOutput = runCommandList(versionChecker, context);

        String[] outputPortions = versionOutput.split(Pattern.quote(" "));
        String versionPortion = outputPortions[outputPortions.length-1];
        String[] versionsPortions = versionPortion.split(Pattern.quote("."));

        boolean prePEP405 = true;

        if (versionsPortions.length >= 3) {
            Integer major = Integer.parseInt(versionsPortions[0]);
            Integer minor = Integer.parseInt(versionsPortions[1]);

            if ((major > 3) || ((major == 3) && (minor >= 6))) {
                prePEP405 = false;
            }
        }

        // Composing command
        ArgumentListBuilder command = new ArgumentListBuilder();
        command.add(commandPath);

        if (prePEP405) {
            command.add("-m");
            command.add("virtualenv");
            command.add("--python="+commandPath);
        } else {
            command.add("-m");
            command.add("venv");
        }
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
        String script = ". \"" + directoryName + "/bin/activate\"; env";
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

    private StepContextWrapper createStepContextWrapper(StepContext stepContext) throws Exception {
        boolean isUnix = stepContext.get(Launcher.class).isUnix();
        EnvVars envVars = stepContext.get(EnvVars.class);
        String workspace = envVars.get("WORKSPACE");

        String directoryCharacter = isUnix ? "/" : "\\";

        if (!workspace.endsWith(directoryCharacter)) {
            workspace += directoryCharacter;
        }

        return new StepContextWrapper(stepContext, isUnix, workspace);
    }

    protected AbstractVirtualenv generateVirtualenv(String withPythonEnvBlockArgument, StepContextWrapper stepContextWrapper) throws Exception {

        AbstractVirtualenv result = null;

        for (AbstractVirtualenvFactory factory : factories) {
            if (factory.canBeBuilt(withPythonEnvBlockArgument, stepContextWrapper)) {
                result = factory.build(withPythonEnvBlockArgument, stepContextWrapper);
                break;
            }
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

    protected EnvVars diffEnvironments(StepContextWrapper stepContextWrapper, String relativeDir) throws Exception {
        StepContext stepContext = stepContextWrapper.getStepContext();
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
                String pathAddition = processPathValues(originalValue, entry.getValue(), stepContextWrapper);
                result.put("PATH+PYTHON", pathAddition);
            } else {
                if (originalValue == null || !originalValue.equals(entry.getValue())) {
                    result.put(entry.getKey(), entry.getValue());
                }
            }
        }
        return result;
    }

    protected String processPathValues(String originalPath, String newPath, StepContextWrapper stepContextWrapper) throws Exception {
        List<String> newPortions = new ArrayList<>();
        String pathSeparator = stepContextWrapper.isUnix() ? getUnixPathSeparator() : getWindowsPathSeparator();

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

}
