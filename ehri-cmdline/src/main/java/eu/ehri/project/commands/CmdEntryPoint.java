package eu.ehri.project.commands;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.ParseException;

public class CmdEntryPoint extends BaseCommand {

    /**
     * Constructor.
     * 
     * @param args
     * @throws ParseException
     */
    public CmdEntryPoint() {
        super();
    }

    private static final Map<String, Command> COMMANDS;
    static {
        Map<String, Command> mmap = new HashMap<String, Command>();
        mmap.put("import-ead", new EadImport());
        COMMANDS = Collections.unmodifiableMap(mmap);
    };

    @Override
    public String getHelp() {
        String sep = System.getProperty("line.separator");
        String help = "Command line interface for the EHRI graph database."
                + sep + sep + "The following commands are available:" + sep
                + sep;
        for (String key : CmdEntryPoint.COMMANDS.keySet()) {
            help += "  " + key + sep;
        }
        return help;
    }

    @Override
    public String getUsage() {
        return "Usage: ehri <command> <command-args ... >";
    }

    @Override
    public int execWithOptions(CommandLine cmdLine) throws Exception {
        System.err.println(getHelp());
        return 1;
    }
    
    public static int main(String[] args) throws Exception {
        
        if (args.length < 1) {
            CmdEntryPoint cmd = new CmdEntryPoint();
            return cmd.exec(args);                        
        } else {
            if (CmdEntryPoint.COMMANDS.containsKey(args[0])) {
                List<String> newArgs = new LinkedList<String>();
                for (int i = 1; i < args.length; i++) {
                    newArgs.add(args[i]);
                }
                Command cmd = CmdEntryPoint.COMMANDS.get(args[0]);
                cmd.exec(newArgs.toArray(new String[newArgs.size()]));
            }
        }
        return 0;
    }
}
