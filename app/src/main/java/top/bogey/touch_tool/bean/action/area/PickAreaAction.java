package top.bogey.touch_tool.bean.action.area;

import com.google.gson.JsonObject;

import java.util.concurrent.atomic.AtomicBoolean;

import top.bogey.touch_tool.R;
import top.bogey.touch_tool.bean.action.ActionType;
import top.bogey.touch_tool.bean.action.parent.ExecuteAction;
import top.bogey.touch_tool.bean.pin.Pin;
import top.bogey.touch_tool.bean.pin.pin_objects.pin_execute.PinExecute;
import top.bogey.touch_tool.bean.pin.pin_objects.PinArea;
import top.bogey.touch_tool.service.TaskRunnable;
import top.bogey.touch_tool.ui.blueprint.picker.AreaPicker;

public class PickAreaAction extends ExecuteAction {
    private final transient Pin areaPin = new Pin(new PinArea(), R.string.pin_area, true);
    private final transient Pin elsePin = new Pin(new PinExecute(), R.string.pick_area_action_else, true);

    public PickAreaAction() {
        super(ActionType.PICK_AREA);
        addPins(areaPin, elsePin);
    }

    public PickAreaAction(JsonObject jsonObject) {
        super(jsonObject);
        reAddPins(areaPin, elsePin);
    }

    @Override
    public void execute(TaskRunnable runnable, Pin pin) {
        AtomicBoolean aBoolean = new AtomicBoolean(false);
        AreaPicker.showPicker(result -> {
            if (result != null) {
                areaPin.getValue(PinArea.class).setValue(result);
                aBoolean.set(true);
            }
            runnable.resume();
        });
        runnable.await();
        if (aBoolean.get()) executeNext(runnable, outPin);
        else executeNext(runnable, elsePin);
    }
}
