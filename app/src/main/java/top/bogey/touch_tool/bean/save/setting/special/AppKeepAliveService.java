package top.bogey.touch_tool.bean.save.setting.special;

import android.content.Context;
import android.content.Intent;

import top.bogey.touch_tool.bean.save.setting.SettingSave;
import top.bogey.touch_tool.service.KeepAliveService;

public class AppKeepAliveService extends SettingSave<Boolean> {

    public AppKeepAliveService(String key, Boolean defaultValue) {
        super(key, defaultValue);
    }

    public void set(Context context, Boolean value) {
        super.set(value);
        handle(context);
    }

    public void handle(Context context) {
        Intent intent = new Intent(context, KeepAliveService.class);
        if (get()) context.startService(intent);
        else context.stopService(intent);
    }
}
