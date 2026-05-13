package top.bogey.touch_tool.service.super_user.shell;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;

public class ShellSession {
    protected final Process process;
    protected final BufferedWriter cmdWriter;
    protected final BufferedReader outputReader;

    public ShellSession(String cmd) throws Exception {
        ProcessBuilder builder = new ProcessBuilder(cmd);
        builder.redirectErrorStream(true);
        process = builder.start();
        cmdWriter = new BufferedWriter(new OutputStreamWriter(process.getOutputStream()));
        outputReader = new BufferedReader(new InputStreamReader(process.getInputStream()));
    }

    public synchronized void writeLine(String cmd) throws Exception {
        cmdWriter.write(cmd);
        cmdWriter.write("\n");
        cmdWriter.flush();
    }

    public synchronized void close() {
        try {
            writeLine("exit");
        } catch (Exception ignored) {
        }

        try {
            cmdWriter.close();
        } catch (Exception ignored) {
        }

        try {
            outputReader.close();
        } catch (Exception ignored) {
        }

        try {
            process.destroy();
        } catch (Exception ignored) {
        }
    }
}
