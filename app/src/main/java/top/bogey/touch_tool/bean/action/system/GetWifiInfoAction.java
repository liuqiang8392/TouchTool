package top.bogey.touch_tool.bean.action.system;

import com.google.gson.JsonObject;

import top.bogey.touch_tool.R;
import top.bogey.touch_tool.bean.action.ActionCheckResult;
import top.bogey.touch_tool.bean.action.ActionType;
import top.bogey.touch_tool.bean.action.parent.CalculateAction;
import top.bogey.touch_tool.bean.pin.Pin;
import top.bogey.touch_tool.bean.pin.pin_objects.pin_string.PinString;
import top.bogey.touch_tool.bean.save.SettingSaver;
import top.bogey.touch_tool.bean.task.Task;
import top.bogey.touch_tool.service.TaskInfoSummary;
import top.bogey.touch_tool.service.TaskRunnable;

public class GetWifiInfoAction extends CalculateAction {
    private final transient Pin wifiNamePin = new Pin(new PinString(), R.string.get_wifi_info_action_wifi_name, true);
    private final transient Pin gatewayPin = new Pin(new PinString(), R.string.get_wifi_info_action_gateway, true);
    private final transient Pin ipPin = new Pin(new PinString(), R.string.get_wifi_info_action_ip, true);

    public GetWifiInfoAction() {
        super(ActionType.GET_WIFI_INFO);
        addPins(wifiNamePin, gatewayPin, ipPin);
    }

    public GetWifiInfoAction(JsonObject jsonObject) {
        super(jsonObject);
        reAddPins(wifiNamePin, gatewayPin, ipPin);
    }

    @Override
    public void calculate(TaskRunnable runnable, Pin pin) {
        TaskInfoSummary.WifiInfo wifiInfo = TaskInfoSummary.getInstance().getWifiInfo();
        if (wifiInfo == null) return;

        wifiNamePin.getValue(PinString.class).setValue(wifiInfo.wifiName());
        gatewayPin.getValue(PinString.class).setValue(wifiInfo.gateway());
        ipPin.getValue(PinString.class).setValue(wifiInfo.ip());
    }

    @Override
    public void check(ActionCheckResult result, Task task) {
        super.check(result, task);
        if (!SettingSaver.getInstance().isLocationEnabled()) {
            result.addResult(ActionCheckResult.ResultType.ERROR, R.string.check_need_fine_location_permission_error);
        }
    }
}
