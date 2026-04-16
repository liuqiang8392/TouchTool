package top.bogey.touch_tool.bean.save.setting.special;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;

import java.util.List;

import top.bogey.touch_tool.bean.save.setting.SettingSave;

public class AppHideActivityBackground extends SettingSave<Boolean> {

    public AppHideActivityBackground(String key, Boolean defaultValue) {
        super(key, defaultValue);
    }

    public void set(Activity activity, Boolean value) {
        super.set(value);
        handle(activity);
    }

    public void handle(Activity activity) {
        int taskId = activity.getTaskId();
        ActivityManager manager = (ActivityManager) activity.getSystemService(Context.ACTIVITY_SERVICE);
        if (manager != null) {
            List<ActivityManager.AppTask> taskList = manager.getAppTasks();
            if (taskList != null) {
                for (ActivityManager.AppTask task : taskList) {
                    if (task.getTaskInfo().id == taskId) task.setExcludeFromRecents(get());
                }
            }
        }
    }
}
