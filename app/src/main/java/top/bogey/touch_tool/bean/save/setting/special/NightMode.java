package top.bogey.touch_tool.bean.save.setting.special;

import androidx.appcompat.app.AppCompatDelegate;

import top.bogey.touch_tool.bean.save.setting.SettingSave;

public class NightMode extends SettingSave<Integer> {

    public NightMode(String key, Integer defaultValue) {
        super(key, defaultValue);
    }

    @Override
    public void set(Integer value) {
        super.set(value);
        handle();
    }

    public void handle() {
        AppCompatDelegate.setDefaultNightMode(get() - 1);
    }
}
