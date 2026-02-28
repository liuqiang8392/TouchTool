package top.bogey.touch_tool.bean.action.system;

import com.google.gson.JsonObject;

import java.util.List;

import top.bogey.touch_tool.R;
import top.bogey.touch_tool.bean.action.ActionCheckResult;
import top.bogey.touch_tool.bean.action.ActionType;
import top.bogey.touch_tool.bean.action.parent.CalculateAction;
import top.bogey.touch_tool.bean.pin.Pin;
import top.bogey.touch_tool.bean.pin.pin_objects.pin_list.PinList;
import top.bogey.touch_tool.bean.pin.pin_objects.pin_string.PinString;
import top.bogey.touch_tool.bean.save.SettingSaver;
import top.bogey.touch_tool.bean.task.Task;
import top.bogey.touch_tool.service.TaskInfoSummary;
import top.bogey.touch_tool.service.TaskRunnable;

public class GetBluetoothDevicesAction extends CalculateAction {
    private final transient Pin devicesPin = new Pin(new PinList(new PinString()), R.string.get_bluetooth_devices_action_result, true);

    public GetBluetoothDevicesAction() {
        super(ActionType.GET_BLUETOOTH_DEVICES);
        addPin(devicesPin);
    }

    public GetBluetoothDevicesAction(JsonObject jsonObject) {
        super(jsonObject);
        reAddPin(devicesPin);
    }

    @Override
    public void calculate(TaskRunnable runnable, Pin pin) {
        PinList devices = devicesPin.getValue(PinList.class);
        List<TaskInfoSummary.BluetoothInfo> bluetoothDevices = TaskInfoSummary.getInstance().getBluetoothInfoList();
        for (TaskInfoSummary.BluetoothInfo device : bluetoothDevices) {
            devices.add(new PinString(device.bluetoothName()));
        }
    }

    @Override
    public void check(ActionCheckResult result, Task task) {
        super.check(result, task);
        if (!SettingSaver.getInstance().isBluetoothEnabled()) {
            result.addResult(ActionCheckResult.ResultType.ERROR, R.string.check_need_bluetooth_permission_error);
        }
    }
}
