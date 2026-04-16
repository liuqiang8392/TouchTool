package top.bogey.touch_tool.bean.save.setting.special;

import top.bogey.touch_tool.bean.save.setting.SettingSave;

public class AppRunTimes extends SettingSave<Integer> {

    public AppRunTimes(String key, Integer defaultValue) {
        super(key, defaultValue);
    }

    public void add() {
        set(get() + 1);
    }
}
