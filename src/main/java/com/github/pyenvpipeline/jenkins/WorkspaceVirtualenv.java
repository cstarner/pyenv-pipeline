package com.github.pyenvpipeline.jenkins;
import java.util.regex.Pattern;

public class WorkspaceVirtualenv extends AbstractVirtualenv {

    private String workspaceDirectory;

    public WorkspaceVirtualenv(String withPythonEnvBlockArgument, boolean isUnix, String worksapceDirectory) {
        super(withPythonEnvBlockArgument, isUnix);
        this.workspaceDirectory = worksapceDirectory;
    }

    @Override
    public String getPythonInstallationPath() {
        return isUnix ? getUnixCommandPath() : getWindowsCommandPath();
    }

    protected String getUnixCommandPath() {
        // This either points to a little python executable, or a directory that contains one
        String commandPathBase = withPythonEnvBlockArgument;

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

    protected String getWindowsCommandPath() {

        String result = withPythonEnvBlockArgument;

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

    @Override
    public String getVirtualEnvPath() {
        String pathSeparator = isUnix ? "/" : "\\";

        String result = workspaceDirectory;
        String relativePythonEnvDirectory = getRelativePythonEnvDirectory(withPythonEnvBlockArgument);

        if (!result.endsWith(pathSeparator) && !relativePythonEnvDirectory.startsWith(pathSeparator)) {
            result += pathSeparator;
        }

        result += relativePythonEnvDirectory;

        return result;
    }

    public static class Factory extends AbstractVirtualenvFactory<WorkspaceVirtualenv> {

        // This is the default, fall-back case. It can always be built
        @Override
        public boolean canBeBuilt(String withPythonEnvArgument, StepContextWrapper stepContext) throws Exception {
            return true;
        }

        @Override
        public WorkspaceVirtualenv build(String withPythonEnvBlockArgument, StepContextWrapper stepContext) {
            return new WorkspaceVirtualenv(withPythonEnvBlockArgument, stepContext.isUnix(),
                    stepContext.getWorkspaceDirectory());
        }
    }
}
