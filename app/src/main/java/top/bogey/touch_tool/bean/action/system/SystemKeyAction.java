package top.bogey.touch_tool.bean.action.system;

import android.os.Build;
import android.widget.Toast;

import com.google.gson.JsonObject;

import top.bogey.touch_tool.MainApplication;
import top.bogey.touch_tool.R;
import top.bogey.touch_tool.bean.action.ActionType;
import top.bogey.touch_tool.bean.action.parent.ExecuteAction;
import top.bogey.touch_tool.bean.pin.Pin;
import top.bogey.touch_tool.bean.pin.pin_objects.pin_string.PinSingleSelect;
import top.bogey.touch_tool.service.MainAccessibilityService;
import top.bogey.touch_tool.service.TaskRunnable;

public class SystemKeyAction extends ExecuteAction {
    private final transient Pin keyPin = new Pin(new PinSingleSelect(R.array.system_key), R.string.system_key_action_key);

    public SystemKeyAction() {
        super(ActionType.SYSTEM_KEY);
        addPin(keyPin);
    }

    public SystemKeyAction(JsonObject jsonObject) {
        super(jsonObject);
        reAddPin(keyPin);
    }

    @Override
    public void execute(TaskRunnable runnable, Pin pin) {
        PinSingleSelect key = getPinValue(runnable, keyPin);
        MainAccessibilityService service = MainApplication.getInstance().getService();
        switch (key.getIndex()) {
            case 0 -> service.performGlobalAction(MainAccessibilityService.GLOBAL_ACTION_BACK);
            case 1 -> service.performGlobalAction(MainAccessibilityService.GLOBAL_ACTION_HOME);
            case 2 -> service.performGlobalAction(MainAccessibilityService.GLOBAL_ACTION_RECENTS);
            case 3 -> service.performGlobalAction(MainAccessibilityService.GLOBAL_ACTION_NOTIFICATIONS);
            case 4 -> service.performGlobalAction(MainAccessibilityService.GLOBAL_ACTION_QUICK_SETTINGS);
            case 5 -> service.performGlobalAction(MainAccessibilityService.GLOBAL_ACTION_POWER_DIALOG);
            case 6 -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) service.performGlobalAction(MainAccessibilityService.GLOBAL_ACTION_LOCK_SCREEN);
                else Toast.makeText(service, R.string.device_not_support_lock, Toast.LENGTH_SHORT).show();
            }
            case 7 -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) service.performGlobalAction(MainAccessibilityService.GLOBAL_ACTION_TAKE_SCREENSHOT);
                else Toast.makeText(service, R.string.device_not_support_snap, Toast.LENGTH_SHORT).show();
            }
        }
        executeNext(runnable, outPin);
    }
}
