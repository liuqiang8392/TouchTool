package top.bogey.touch_tool.bean.action.string;

import com.google.gson.JsonObject;

import java.util.Objects;
import java.util.regex.Pattern;

import top.bogey.touch_tool.R;
import top.bogey.touch_tool.bean.action.ActionType;
import top.bogey.touch_tool.bean.action.parent.CalculateAction;
import top.bogey.touch_tool.bean.pin.Pin;
import top.bogey.touch_tool.bean.pin.pin_objects.PinBoolean;
import top.bogey.touch_tool.bean.pin.pin_objects.PinObject;
import top.bogey.touch_tool.bean.pin.pin_objects.pin_number.PinInteger;
import top.bogey.touch_tool.bean.pin.pin_objects.pin_string.PinString;
import top.bogey.touch_tool.service.TaskRunnable;

public class StringEqualAction extends CalculateAction {
    private final transient Pin firstPin = new Pin(new PinString(), R.string.pin_string);
    private final transient Pin secondPin = new Pin(new PinString(), R.string.pin_string);
    private final transient Pin ignoreCasePin = new Pin(new PinBoolean(), R.string.string_equal_action_ignore_case);
    private final transient Pin resultPin = new Pin(new PinBoolean(), R.string.pin_boolean_result, true);
    private final transient Pin regexPin = new Pin(new PinBoolean(), R.string.string_equal_action_regex);
    private final transient Pin compareValuePin = new Pin(new PinInteger(), R.string.string_equal_action_value, true);

    public StringEqualAction() {
        super(ActionType.STRING_EQUAL);
        addPins(firstPin, secondPin, ignoreCasePin, resultPin, regexPin, compareValuePin);
    }

    public StringEqualAction(JsonObject jsonObject) {
        super(jsonObject);
        reAddPins(firstPin, secondPin, ignoreCasePin, resultPin, regexPin, compareValuePin);
    }

    @Override
    public void calculate(TaskRunnable runnable, Pin pin) {
        PinObject first = getPinValue(runnable, firstPin);
        PinObject second = getPinValue(runnable, secondPin);
        PinBoolean ignoreCase = getPinValue(runnable, ignoreCasePin);
        PinBoolean useRegex = getPinValue(runnable, regexPin);

        boolean result;
        String firstValue = first.toString();
        String secondValue = second.toString();

        if (useRegex.getValue()) {
            // 正则匹配模式
            try {
                int flags = ignoreCase.getValue() ? Pattern.CASE_INSENSITIVE : 0;
                Pattern pattern = Pattern.compile(secondValue, flags);
                result = pattern.matcher(firstValue).matches();
            } catch (Exception e) {
                result = false;
            }
        } else {
            // 普通字符串比较
            if (ignoreCase.getValue()) {
                result = firstValue.equalsIgnoreCase(secondValue);
            } else {
                result = Objects.equals(firstValue, secondValue);
            }
        }

        if (ignoreCase.getValue()) {
            compareValuePin.getValue(PinInteger.class).setValue(firstValue.compareToIgnoreCase(secondValue));
        } else {
            compareValuePin.getValue(PinInteger.class).setValue(firstValue.compareTo(secondValue));
        }

        resultPin.getValue(PinBoolean.class).setValue(result);
    }
}
