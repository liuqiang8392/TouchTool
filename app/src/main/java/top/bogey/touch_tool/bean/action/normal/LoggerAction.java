package top.bogey.touch_tool.bean.action.normal;

import android.graphics.Point;

import com.google.gson.JsonObject;

import top.bogey.touch_tool.MainApplication;
import top.bogey.touch_tool.R;
import top.bogey.touch_tool.bean.action.ActionType;
import top.bogey.touch_tool.bean.action.parent.ExecuteAction;
import top.bogey.touch_tool.bean.other.log.LogInfo;
import top.bogey.touch_tool.bean.other.log.NormalLog;
import top.bogey.touch_tool.bean.pin.Pin;
import top.bogey.touch_tool.bean.pin.pin_objects.PinBoolean;
import top.bogey.touch_tool.bean.pin.pin_objects.PinObject;
import top.bogey.touch_tool.bean.pin.pin_objects.pin_scale_able.PinPoint;
import top.bogey.touch_tool.bean.pin.pin_objects.pin_string.PinSingleSelect;
import top.bogey.touch_tool.bean.pin.pin_objects.pin_string.PinString;
import top.bogey.touch_tool.service.TaskRunnable;
import top.bogey.touch_tool.ui.custom.ToastFloatView;
import top.bogey.touch_tool.utils.DisplayUtil;
import top.bogey.touch_tool.utils.EAnchor;

public class LoggerAction extends ExecuteAction {
    private final transient Pin logPin = new Pin(new PinString(), R.string.log_action_text);
    private final transient Pin showPin = new Pin(new PinBoolean(true), R.string.log_action_show, false, false, true);
    private final transient Pin anchorPin = new Pin(new PinSingleSelect(R.array.anchor, 7), R.string.window_anchor, false, false, true);
    private final transient Pin gravityPin = new Pin(new PinSingleSelect(R.array.anchor, 7), R.string.screen_anchor, false, false, true);
    private final transient Pin showPosPin = new Pin(new PinPoint(), R.string.screen_anchor_pos, false, false, true);
    private final transient Pin savePin = new Pin(new PinBoolean(true), R.string.log_action_save, false, false, true);

    public LoggerAction() {
        super(ActionType.LOG);
        Point size = DisplayUtil.getScreenSize(MainApplication.getInstance());
        showPosPin.getValue(PinPoint.class).setValue(new Point(0, -size.y / 5));
        addPins(logPin, showPin, anchorPin, gravityPin, showPosPin, savePin);
    }

    public LoggerAction(JsonObject jsonObject) {
        super(jsonObject);
        reAddPins(logPin, showPin, anchorPin, gravityPin, showPosPin, savePin);
    }

    @Override
    public void execute(TaskRunnable runnable, Pin pin) {
        PinObject logObject = getPinValue(runnable, logPin);
        PinBoolean show = getPinValue(runnable, showPin);
        PinSingleSelect anchor = getPinValue(runnable, anchorPin);
        PinSingleSelect gravity = getPinValue(runnable, gravityPin);
        PinPoint showPos = getPinValue(runnable, showPosPin);
        PinBoolean save = getPinValue(runnable, savePin);

        if (show.getValue()) {
            ToastFloatView.showToast(logObject.toString(), EAnchor.values()[anchor.getIndex()], EAnchor.values()[gravity.getIndex()], showPos.getValue());
        }

        if (save.getValue()) {
            runnable.addLog(new LogInfo(new NormalLog(logObject.toString())), 0);
        }

        executeNext(runnable, outPin);
    }

    public Pin getLogPin() {
        return logPin;
    }

    public boolean switchLog() {
        boolean show = showPin.getValue(PinBoolean.class).getValue();
        show = !show;
        showPin.setValue(new PinBoolean(show));
        savePin.setValue(new PinBoolean(show));
        return show;
    }

    public boolean getLogSwitch() {
        return showPin.getValue(PinBoolean.class).getValue();
    }
}
