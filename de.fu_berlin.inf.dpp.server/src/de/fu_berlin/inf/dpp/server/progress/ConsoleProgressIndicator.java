package de.fu_berlin.inf.dpp.server.progress;

import de.fu_berlin.inf.dpp.monitoring.IProgressMonitor;

import java.io.PrintStream;

public class ConsoleProgressIndicator implements IProgressMonitor {
    private PrintStream out;
    private boolean canceled = false;
    
    private int total = 0;
    private int worked = 0;

    private String task;
    private String subTask;

    private int animationFrame = 0;

    public ConsoleProgressIndicator(PrintStream out) {
        this.out = out;
    }

    @Override
    public void done() {
        out.print('\n');
    }

    @Override
    public void subTask(String name) {
        if (!name.equals(subTask)) {
            subTask = name;
            print();
        }
    }

    @Override
    public void setTaskName(String name) {
        if (!name.equals(task)) {
            task = name;
            print();
        }
    }

    @Override
    public void worked(int amount) {
        if (amount != worked) {
            worked = amount;
            print();
        }
    }

    @Override
    public void setCanceled(boolean canceled) {
        if (canceled != this.canceled) {
            this.canceled = canceled;
            print();
        }
    }

    @Override
    public boolean isCanceled() {
        return canceled;
    }

    @Override
    public void beginTask(String name, int size) {
        task = name;
        total = size;
        print();
    }

    private void print() {
        if (canceled) {
            out.print("[CANCELED] ");
        } else if (total == IProgressMonitor.UNKNOWN) {
            switch (animationFrame) {
            case 0:
                out.print("[-] ");
                break;
            case 1:
                out.print("[\\] ");
                break;
            case 2:
                out.print("[|] ");
                break;
            case 3:
                out.print("[/] ");
                break;
            }
            animationFrame = (animationFrame + 1) % 4;
        } else {
            out.printf("[%2d%%] ", worked / total);
        }
        out.print(task);
        if (subTask != null) {
            out.printf(" - %s", subTask);
        }
        out.print('\r');
    }
}
