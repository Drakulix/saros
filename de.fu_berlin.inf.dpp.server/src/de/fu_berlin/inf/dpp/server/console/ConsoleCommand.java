package de.fu_berlin.inf.dpp.server.console;

import java.io.PrintStream;

public abstract class ConsoleCommand {
    public abstract String identifier();

    public abstract String help();

    public boolean matches(String command) {
        return command.startsWith(identifier());
    }

    public abstract void execute(String command, PrintStream out);
}
