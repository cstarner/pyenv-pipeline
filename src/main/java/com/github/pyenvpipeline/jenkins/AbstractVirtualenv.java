package com.github.pyenvpipeline.jenkins;

import java.io.Serializable;

public abstract class AbstractVirtualenv implements Serializable {

    private static final String DEFAULT_DIR_PREFIX = ".pyenv";

    String withPythonEnvBlockArgument;
    boolean isUnix;

    AbstractVirtualenv(String withPythonEnvBlockArgument, boolean isUnix) {
        this.withPythonEnvBlockArgument = withPythonEnvBlockArgument;
        this.isUnix = isUnix;
    }

    String getRelativePythonEnvDirectory(String pythonInstallation){
        String postfix = pythonInstallation.replaceAll("/", "-")
                .replaceFirst("[a-zA-Z]:\\\\", "")
                .replaceAll("\\\\", "-");

        if (!postfix.startsWith("-")) {
            postfix = "-" + postfix;
        }

        return DEFAULT_DIR_PREFIX + postfix;
    }

    public boolean canCreate() {
        return true;
    }

    public abstract String getVirtualEnvPath();
    public abstract String getPythonInstallationPath();

    @Override
    public String toString() {
        return getVirtualEnvPath() + " " + getPythonInstallationPath();
    }
}
