package top.bogey.touch_tool.service.super_user;

import top.bogey.touch_tool.utils.callback.BooleanResultCallback;

public interface ISuperUser {
    void init(BooleanResultCallback callback);

    void exit();

    boolean isValid();

    CmdResult runCommand(String cmd);
}
