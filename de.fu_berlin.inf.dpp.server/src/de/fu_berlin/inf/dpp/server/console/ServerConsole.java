package de.fu_berlin.inf.dpp.server.console;

import java.io.InputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class ServerConsole implements Runnable {
    private Scanner input;
    private PrintStream output;
    private List<ConsoleCommand> commands;

    public ServerConsole(InputStream in, PrintStream out) {
        input = new Scanner(in);
        output = out;
        commands = new ArrayList<>();
    }

    public void registerCommand(ConsoleCommand command) {
        commands.add(command);
    }

    @Override
    public void run() {
        output
            .println("# Welcome to Saros Server (type 'help' for available commands)");
        while (true) {
            output.print("> ");
            String line = input.nextLine();

            if (!line.startsWith("#")) {
                if (line.equalsIgnoreCase("quit")) {
                    break;
                } else if (line.equalsIgnoreCase("help")) {
                    for (ConsoleCommand command : commands) {
                        output.println(command.help());
                    }
                    output.println("help - Print this help");
                    output.println("quit - Quit Saros Server");
                } else {
                    found: {
                        for (ConsoleCommand command : commands) {
                            if (command.matches(line)) {
                                command.execute(line, output);
                                break found;
                            }
                        }
                        output
                            .printf(
                                "Command '%s' is not known. (Type 'help' for available commands)",
                                line);
                    }
                }
            }
        }
    }
}
