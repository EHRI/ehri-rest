package eu.ehri.project.commands;

import org.apache.commons.cli.CommandLine;

public interface Command {
    public String getHelp();
    public String getUsage();
    public int execWithOptions(CommandLine cmdLine) throws Exception;
    public int exec(String[] args) throws Exception;
}
