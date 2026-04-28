package top.bogey.touch_tool.service.super_user.root;

import android.util.Log;

import androidx.annotation.Keep;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;

import top.bogey.touch_tool.service.super_user.CmdResult;
import top.bogey.touch_tool.service.super_user.ISuperUser;

public class RootSuperUser implements ISuperUser {
    private final static String EXIT_MARKER = "EXIT_MARKER";

    private boolean existRoot = false;
    private Process rootProcess = null;
    private BufferedWriter cmdWriter = null;
    private BufferedReader outputReader = null;

    @Keep
    public RootSuperUser() {

    }

    @Override
    public boolean init() {
        return openRootSession();
    }

    @Override
    public void tryInit() {
        openRootSession();
    }

    @Override
    public void exit() {
        try {
            if (cmdWriter != null) {
                cmdWriter.write("exit\n");
                cmdWriter.flush();
                cmdWriter.close();
            }
            if (outputReader != null) outputReader.close();
            if (rootProcess != null) rootProcess.destroy();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            existRoot = false;
        }
    }

    @Override
    public boolean isValid() {
        return openRootSession();
    }

    @Override
    public CmdResult runCommand(String cmd) {
        if (!existRoot) return null;

        try {
            cmdWriter.write(cmd + "\n");
            cmdWriter.write("echo \n");
            cmdWriter.write("echo " + EXIT_MARKER + ":$?\n");
            cmdWriter.flush();

            StringBuilder output = new StringBuilder();
            String line;
            while ((line = outputReader.readLine()) != null) {
                Log.d("TAG", "runCommand: " + line);
                if (line.startsWith(EXIT_MARKER)) {
                    int exitCode = Integer.parseInt(line.substring(EXIT_MARKER.length() + 1));
                    return new CmdResult(exitCode == 0, output.toString().trim());
                }
                output.append(line).append("\n");
            }

            return new CmdResult(false, "");

        } catch (Exception e) {
            e.printStackTrace();
            exit();
            return new CmdResult(false, e.getMessage());
        }
    }

    public boolean openRootSession() {
        if (existRoot) return true;
        try {
            ProcessBuilder builder = new ProcessBuilder("su");
            builder.redirectErrorStream(true);
            rootProcess = builder.start();
            cmdWriter = new BufferedWriter(new OutputStreamWriter(rootProcess.getOutputStream()));
            outputReader = new BufferedReader(new InputStreamReader(rootProcess.getInputStream()));

            existRoot = true;
            CmdResult result = runCommand("echo root");
            existRoot = result.getResult();
            if (!existRoot) exit();
            return existRoot;
        } catch (Exception e) {
            e.printStackTrace();
            exit();
            return false;
        }
    }
}
