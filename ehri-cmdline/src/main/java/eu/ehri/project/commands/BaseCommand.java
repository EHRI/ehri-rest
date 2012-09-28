package eu.ehri.project.commands;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.PosixParser;

public abstract class BaseCommand implements Command {
    
    Options options = new Options();
    CommandLineParser parser = new PosixParser();
        
    protected void setCustomOptions() {};    
    public abstract String getHelp();
    public abstract String getUsage();
    public final int exec(String[] args) throws Exception {
        setCustomOptions();
        return execWithOptions(parser.parse(options, args));
    }
    public abstract int execWithOptions(CommandLine cmdLine) throws Exception;
}
