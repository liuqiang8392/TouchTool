package top.bogey.touch_tool.service.super_user.shizuku;

import android.content.Context;
import android.util.Log;

import androidx.annotation.Keep;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;

import top.bogey.touch_tool.service.super_user.CmdResult;

public class ShizukuService extends IShizukuService.Stub {
    private final static String EXIT_MARKER = "EXIT_MARKER";

    private Process process = null;
    private BufferedWriter cmdWriter = null;
    private BufferedReader outputReader = null;

    public ShizukuService() {
        try {
            ProcessBuilder builder = new ProcessBuilder("sh");
            builder.redirectErrorStream(true);
            process = builder.start();
            cmdWriter = new BufferedWriter(new OutputStreamWriter(process.getOutputStream()));
            outputReader = new BufferedReader(new InputStreamReader(process.getInputStream()));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Keep
    public ShizukuService(Context context) {
        this();
    }

    @Override
    public void destory() {
        try {
            if (cmdWriter != null) {
                cmdWriter.write("exit\n");
                cmdWriter.flush();
                cmdWriter.close();
            }
            if (outputReader != null) outputReader.close();
            if (process != null) process.destroy();
        } catch (Exception e) {
            e.printStackTrace();
        }
        System.exit(0);
    }

    @Override
    public CmdResult runCommand(String cmd) {
        if (process == null) return new CmdResult(false, "");

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
            return new CmdResult(false, e.getMessage());
        }
    }
}