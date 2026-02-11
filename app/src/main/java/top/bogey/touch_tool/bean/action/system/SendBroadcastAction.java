package top.bogey.touch_tool.bean.action.system;

import android.content.Context;
import android.content.Intent;

import com.google.gson.JsonObject;

import java.util.Map;

import top.bogey.touch_tool.MainApplication;
import top.bogey.touch_tool.R;
import top.bogey.touch_tool.bean.action.ActionType;
import top.bogey.touch_tool.bean.action.parent.ExecuteAction;
import top.bogey.touch_tool.bean.pin.Pin;
import top.bogey.touch_tool.bean.pin.pin_objects.PinMap;
import top.bogey.touch_tool.bean.pin.pin_objects.pin_string.PinString;
import top.bogey.touch_tool.service.TaskRunnable;

public class SendBroadcastAction extends ExecuteAction {
    private final transient Pin actionPin = new Pin(new PinString(), R.string.send_broadcast_action_name);
    private final transient Pin packagePin = new Pin(new PinString(), R.string.send_broadcast_action_package);
    private final transient Pin dataPin = new Pin(new PinString(), R.string.send_broadcast_action_data);
    private final transient Pin extrasPin = new Pin(new PinMap(new PinString(), new PinString()), R.string.send_broadcast_action_extra);

    public SendBroadcastAction() {
        super(ActionType.SEND_BROADCAST);
        addPins(actionPin, packagePin, dataPin, extrasPin);
    }

    public SendBroadcastAction(JsonObject jsonObject) {
        super(jsonObject);
        reAddPins(actionPin, packagePin, dataPin, extrasPin);
    }

    @Override
    public void execute(TaskRunnable runnable, Pin pin) {
        PinString action = getPinValue(runnable, actionPin);
        if (action == null || action.getValue() == null || action.getValue().isEmpty()) {
            executeNext(runnable, outPin);
            return;
        }

        Intent intent = new Intent(action.getValue());

        PinString packageName = getPinValue(runnable, packagePin);
        if (packageName != null && packageName.getValue() != null && !packageName.getValue().isEmpty()) {
            intent.setPackage(packageName.getValue());
        }

        PinString data = getPinValue(runnable, dataPin);
        if (data != null && data.getValue() != null && !data.getValue().isEmpty()) {
            intent.setData(android.net.Uri.parse(data.getValue()));
        }

        PinMap map = getPinValue(runnable, extrasPin);
        if (map != null) {
            for (Map.Entry<String, String> entry : map.getValue(PinString.class, PinString.class).entrySet()) {
                if (entry.getKey() == null) continue;
                intent.putExtra(entry.getKey(), entry.getValue());
            }
        }

        Context context = MainApplication.getInstance();
        context.sendBroadcast(intent);
        executeNext(runnable, outPin);
    }
}
