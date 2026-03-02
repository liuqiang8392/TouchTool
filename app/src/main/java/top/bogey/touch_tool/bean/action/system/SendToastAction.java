package top.bogey.touch_tool.bean.action.system;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

import com.google.gson.JsonObject;

import top.bogey.touch_tool.MainApplication;
import top.bogey.touch_tool.R;
import top.bogey.touch_tool.bean.action.ActionCheckResult;
import top.bogey.touch_tool.bean.action.ActionType;
import top.bogey.touch_tool.bean.action.parent.ExecuteAction;
import top.bogey.touch_tool.bean.pin.Pin;
import top.bogey.touch_tool.bean.pin.pin_objects.PinObject;
import top.bogey.touch_tool.bean.pin.pin_objects.pin_string.PinSingleSelect;
import top.bogey.touch_tool.bean.pin.pin_objects.pin_string.PinString;
import top.bogey.touch_tool.bean.task.Task;
import top.bogey.touch_tool.service.TaskRunnable;
import top.bogey.touch_tool.ui.custom.float_view.KeepAliveFloatView;
import top.bogey.touch_tool.utils.float_window_manager.FloatWindow;

public class SendToastAction extends ExecuteAction {
    private final transient Pin contentPin = new Pin(new PinString(), R.string.send_toast_action_content);
    private final transient Pin lengthPin = new Pin(new PinSingleSelect(R.array.toast_length_type), R.string.send_toast_action_duration, false, false, true);

    public SendToastAction() {
        super(ActionType.SEND_TOAST);
        addPins(contentPin, lengthPin);
    }

    public SendToastAction(JsonObject jsonObject) {
        super(jsonObject);
        reAddPins(contentPin, lengthPin);
    }

    @Override
    public void execute(TaskRunnable runnable, Pin pin) {
        PinObject content = getPinValue(runnable, contentPin);
        PinSingleSelect length = getPinValue(runnable, lengthPin);

        KeepAliveFloatView keepView = (KeepAliveFloatView) FloatWindow.getView(KeepAliveFloatView.class.getName());
        if (keepView != null) {
            new Handler(Looper.getMainLooper()).post(() -> Toast.makeText(keepView.getThemeContext(), content.toString(), length.getIndex()).show());
        }
        executeNext(runnable, outPin);
    }

    @Override
    public void check(ActionCheckResult result, Task task) {
        super.check(result, task);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (MainApplication.getInstance().checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                result.addResult(ActionCheckResult.ResultType.WARNING, R.string.check_need_notification_warning);
            }
        }
    }
}
