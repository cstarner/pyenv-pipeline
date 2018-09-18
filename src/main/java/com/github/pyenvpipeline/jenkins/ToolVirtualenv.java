package com.github.pyenvpipeline.jenkins;

import hudson.DescriptorExtensionList;
import hudson.tools.ToolDescriptor;
import hudson.tools.ToolInstallation;

import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;

public class ToolVirtualenv extends WorkspaceVirtualenv {

    private static final Logger LOGGER = Logger.getLogger(ToolVirtualenv.class.getName());
    private String installationHome;

    public ToolVirtualenv(String withPythonEnvBlockArgument, boolean isUnix, String workspaceDirectory, String installationHome) {
        super(withPythonEnvBlockArgument, isUnix, workspaceDirectory);
        this.installationHome = installationHome;
    }

    @Override
    public String getPythonInstallationPath() {
        // ShiningPanda returns the full installation (executable included) on Unix, but on the directory on Windows
        if (isUnix) {
            return installationHome;
        } else {
            String copy = withPythonEnvBlockArgument;
            withPythonEnvBlockArgument = installationHome;
            String result = super.getPythonInstallationPath();
            withPythonEnvBlockArgument = copy;
            return result;
        }
    }

    public static class Factory extends AbstractVirtualenvFactory<ToolVirtualenv> {

        private static final String[] VALID_TOOL_DESCRIPTOR_IDS = {"jenkins.plugins.shiningpanda.tools.PythonInstallation"};

        String installationHome;

        @Override
        public ToolVirtualenv build(String withPythonEnvBlockArgument, StepContextWrapper stepContextWrapper) {
            ToolVirtualenv result = new ToolVirtualenv(withPythonEnvBlockArgument, stepContextWrapper.isUnix(), stepContextWrapper.getWorkspaceDirectory(), installationHome);

            // Reset installation home, since we are likely reusing the factory
            installationHome = null;

            return result;
        }

        @Override
        public boolean canBeBuilt(String withPythonEnvArgument, StepContextWrapper stepContextWrapper) throws Exception {
            DescriptorExtensionList<ToolInstallation, ToolDescriptor<?>> descriptors = ToolInstallation.all();

            List<String> validToolDescriptors = Arrays.asList(VALID_TOOL_DESCRIPTOR_IDS);
            for (ToolDescriptor<?> desc : descriptors) {

                if (!validToolDescriptors.contains(desc.getId())) {
                    continue;
                }
                LOGGER.info("Found Python ToolDescriptor: " + desc.getId());
                ToolInstallation[] installations = unboxToolInstallations(desc);
                for (ToolInstallation installation : installations) {
                    if (installation.getName().equals(withPythonEnvArgument)) {
                        String notification = "Matched ShiningPanda tool name: " + installation.getName();
                        stepContextWrapper.logger().println(notification);
                        LOGGER.info(notification);
                        installationHome =  installation.getHome();
                        return true;
                    } else {
                        LOGGER.info("Skipping ToolInstallation: " + installation.getName());
                    }
                }
            }

            return false;
        }

        private<T extends ToolInstallation> T[] unboxToolInstallations(ToolDescriptor<T> descriptor) {
            return descriptor.getInstallations();
        }
    }
}
