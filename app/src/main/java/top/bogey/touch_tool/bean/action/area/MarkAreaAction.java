package top.bogey.touch_tool.bean.action.area;

import com.google.gson.JsonObject;

import top.bogey.touch_tool.R;
import top.bogey.touch_tool.bean.action.ActionType;
import top.bogey.touch_tool.bean.action.parent.ExecuteAction;
import top.bogey.touch_tool.bean.pin.Pin;
import top.bogey.touch_tool.bean.pin.pin_objects.pin_scale_able.PinArea;
import top.bogey.touch_tool.service.TaskRunnable;
import top.bogey.touch_tool.ui.custom.MarkTargetFloatView;

public class MarkAreaAction extends ExecuteAction {
    private final transient Pin areaPin = new Pin(new PinArea(), R.string.pin_area);

    public MarkAreaAction() {
        super(ActionType.MARK_AREA);
        addPin(areaPin);
    }

    public MarkAreaAction(JsonObject jsonObject) {
        super(jsonObject);
        reAddPin(areaPin);
    }

    @Override
    public void execute(TaskRunnable runnable, Pin pin) {
        PinArea area = getPinValue(runnable, areaPin);
        MarkTargetFloatView.showMarkArea(area.getValue());
        executeNext(runnable, outPin);
    }
}
