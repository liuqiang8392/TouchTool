package top.bogey.touch_tool.bean.action.string;

import com.google.gson.JsonObject;

import top.bogey.touch_tool.R;
import top.bogey.touch_tool.bean.action.ActionType;
import top.bogey.touch_tool.bean.action.parent.CalculateAction;
import top.bogey.touch_tool.bean.pin.Pin;
import top.bogey.touch_tool.bean.pin.pin_objects.PinObject;
import top.bogey.touch_tool.bean.pin.pin_objects.pin_number.PinInteger;
import top.bogey.touch_tool.bean.pin.pin_objects.pin_number.PinNumber;
import top.bogey.touch_tool.bean.pin.pin_objects.pin_string.PinString;
import top.bogey.touch_tool.service.TaskRunnable;

public class StringSubStringAction extends CalculateAction {
    private final transient Pin textPin = new Pin(new PinString(), R.string.pin_string);
    private final transient Pin startPin = new Pin(new PinInteger(1), R.string.string_substring_action_start);
    private final transient Pin endPin = new Pin(new PinInteger(1), R.string.string_substring_action_end);
    private final transient Pin resultPin = new Pin(new PinString(), R.string.string_substring_action_result, true);

    public StringSubStringAction() {
        super(ActionType.STRING_SUBSTRING);
        addPins(textPin, startPin, endPin, resultPin);
    }

    public StringSubStringAction(JsonObject jsonObject) {
        super(jsonObject);
        reAddPins(textPin, startPin, endPin, resultPin);
    }

    @Override
    public void calculate(TaskRunnable runnable, Pin pin) {
        PinObject text = getPinValue(runnable, textPin);
        PinNumber<?> start = getPinValue(runnable, startPin);
        PinNumber<?> end = getPinValue(runnable, endPin);
        int max = text.toString().length();
        int startPos = start.intValue();
        if (startPos < 0) {
            startPos = Math.max(0, max + startPos + 1);
        }
        startPos = Math.max(1, Math.min(max, startPos));
        int endPos = end.intValue();
        if (endPos < 0) {
            endPos = Math.max(0, max + endPos + 1);
        }
        endPos = Math.max(1, Math.min(max, endPos));
        if (startPos > endPos) {
            int temp = startPos;
            startPos = endPos;
            endPos = temp;
        }

        if (text.toString().isEmpty()) {
            resultPin.getValue(PinString.class).setValue("");
        } else {
            String result = text.toString().substring(startPos - 1, endPos);
            resultPin.getValue(PinString.class).setValue(result);
        }
    }
}
