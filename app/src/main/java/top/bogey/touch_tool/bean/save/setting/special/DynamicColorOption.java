package top.bogey.touch_tool.bean.save.setting.special;

import android.app.Activity;

import top.bogey.touch_tool.bean.save.setting.SettingSave;

public class DynamicColorOption extends SettingSave<Integer> {

    public DynamicColorOption(String key, Integer defaultValue) {
        super(key, defaultValue);
    }

    public void set(Activity activity, Integer value) {
        super.set(value);
        handle(activity);
    }

    public void handle(Activity activity) {
        activity.recreate();
    }
}
