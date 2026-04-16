package top.bogey.touch_tool.bean.save.setting.special;

import android.os.Build;

import top.bogey.touch_tool.bean.save.setting.SettingSave;

public class PermissionExactAlarm extends SettingSave<Boolean> {

    public PermissionExactAlarm(String key, Boolean defaultValue) {
        super(key, defaultValue);
    }

    @Override
    public Boolean get() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return true;
        return super.get();
    }
}
