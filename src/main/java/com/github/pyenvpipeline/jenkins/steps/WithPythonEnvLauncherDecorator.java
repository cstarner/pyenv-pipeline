package com.github.pyenvpipeline.jenkins.steps;

import hudson.FilePath;
import hudson.Launcher;
import hudson.LauncherDecorator;
import hudson.Proc;
import hudson.model.Node;
import hudson.model.TaskListener;
import hudson.remoting.Channel;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.Serializable;
import java.util.List;
import java.util.Map;


public class WithPythonEnvLauncherDecorator extends LauncherDecorator implements Serializable{

    private String commandPrefix;
    private String pythonVersion;

    public WithPythonEnvLauncherDecorator(String commandPrefix, String pythonVersion){
        super();
        this.commandPrefix = commandPrefix;
        this.pythonVersion = pythonVersion;
    }

    @Override
    public Launcher decorate(final Launcher base, Node node) {
        return new Launcher(base) {
            @Override
            public Proc launch(ProcStarter procStarter) throws IOException {
                TaskListener taskListener = getListener();
                PrintStream logger = taskListener.getLogger();
                logger.println("Switching Commands to "+pythonVersion);

                //List<String> commands = procStarter.cmds();
                //String firstCommand = commandPrefix + commands.get(0);
                //commands.set(0, firstCommand);
                //procStarter.cmds(commands);

                return base.launch(procStarter);
            }

            @Override
            public Channel launchChannel(String[] strings, OutputStream outputStream, FilePath filePath, Map<String, String> map) throws IOException, InterruptedException {
                return base.launchChannel(strings, outputStream, filePath, map);
            }

            @Override
            public void kill(Map<String, String> map) throws IOException, InterruptedException {
                base.kill(map);
            }
        };
    }
}
