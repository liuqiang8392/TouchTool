package top.bogey.touch_tool.bean.action.system;

import com.google.gson.JsonObject;

import top.bogey.touch_tool.MainApplication;
import top.bogey.touch_tool.R;
import top.bogey.touch_tool.bean.action.ActionType;
import top.bogey.touch_tool.bean.action.parent.CalculateAction;
import top.bogey.touch_tool.bean.pin.Pin;
import top.bogey.touch_tool.bean.pin.pin_objects.PinBoolean;
import top.bogey.touch_tool.bean.pin.pin_objects.pin_scale_able.PinArea;
import top.bogey.touch_tool.bean.pin.pin_objects.pin_string.PinSingleSelect;
import top.bogey.touch_tool.service.TaskInfoSummary;
import top.bogey.touch_tool.service.TaskRunnable;
import top.bogey.touch_tool.utils.AppUtil;
import top.bogey.touch_tool.utils.DisplayUtil;

public class GetScreenStatusAction extends CalculateAction {
    private final transient Pin statePin = new Pin(new PinSingleSelect(R.array.screen_state), R.string.get_screen_status_action_state, true);
    private final transient Pin sizePin = new Pin(new PinArea(), R.string.get_screen_status_action_size, true);
    private final transient Pin orientationPin = new Pin(new PinBoolean(), R.string.get_screen_status_action_orientation, true);

    public GetScreenStatusAction() {
        super(ActionType.GET_SCREEN_STATUS);
        addPins(statePin, sizePin, orientationPin);
    }

    public GetScreenStatusAction(JsonObject jsonObject) {
        super(jsonObject);
        reAddPins(statePin, sizePin, orientationPin);
    }

    @Override
    public void calculate(TaskRunnable runnable, Pin pin) {
        TaskInfoSummary.PhoneState state = AppUtil.getPhoneState(MainApplication.getInstance());
        statePin.getValue(PinSingleSelect.class).setIndex(state.ordinal());
        sizePin.getValue(PinArea.class).setValue(DisplayUtil.getScreenArea(MainApplication.getInstance().getService()));
        orientationPin.getValue(PinBoolean.class).setValue(DisplayUtil.isPortrait(MainApplication.getInstance().getService()));
    }
}
