package top.bogey.touch_tool.bean.action.number;

import com.google.gson.JsonObject;

import top.bogey.touch_tool.R;
import top.bogey.touch_tool.bean.action.ActionType;
import top.bogey.touch_tool.bean.action.parent.CalculateAction;
import top.bogey.touch_tool.bean.pin.Pin;
import top.bogey.touch_tool.bean.pin.pin_objects.pin_number.PinDouble;
import top.bogey.touch_tool.bean.pin.pin_objects.pin_number.PinNumber;
import top.bogey.touch_tool.service.TaskRunnable;

public class NumberRandomAction extends CalculateAction {
    // 精度换算出的下标上限，避免超出 long 范围
    private final static double MAX_INDEX = 9E15;

    protected final transient Pin firstPin = new Pin(new PinDouble(0), R.string.pin_value_area_min);
    protected final transient Pin secondPin = new Pin(new PinDouble(1), R.string.pin_value_area_max);
    protected final transient Pin resultPin = new Pin(new PinDouble(), R.string.pin_number_double, true);
    private final transient Pin offsetPin = new Pin(new PinDouble(0.0001), R.string.number_random_action_offset, false, false, true);

    public NumberRandomAction() {
        super(ActionType.NUMBER_RANDOM);
        addPins(firstPin, secondPin, resultPin);
        addPin(offsetPin);
    }

    public NumberRandomAction(JsonObject jsonObject) {
        super(jsonObject);
        reAddPins(firstPin, secondPin, resultPin);
        reAddPin(offsetPin);
    }

    @Override
    public void calculate(TaskRunnable runnable, Pin pin) {
        PinNumber<?> first = getPinValue(runnable, firstPin);
        PinNumber<?> second = getPinValue(runnable, secondPin);
        PinNumber<?> offset = getPinValue(runnable, offsetPin);
        double start = first.doubleValue();
        double end = second.doubleValue();
        double min = Math.min(start, end);
        double max = Math.max(start, end);

        // 精度只看小数位数，如 0.91 表示保留两位小数
        double pow = Math.pow(10, getScale(offset.doubleValue()));
        // 区间内符合精度的取值下标范围，以 0 为基准对齐
        double lowRaw = min * pow;
        double highRaw = max * pow;
        double lowIndex = Math.ceil(lowRaw - Math.ulp(lowRaw));
        double highIndex = Math.floor(highRaw + Math.ulp(highRaw));
        double count = highIndex - lowIndex;

        double result;
        if (count >= 0 && count <= MAX_INDEX && Math.abs(lowIndex) <= MAX_INDEX) {
            // 在符合精度的取值中随机取一个
            long index = (long) lowIndex + (long) (Math.random() * (count + 1));
            result = index / pow;
        } else if (count < 0) {
            // 区间内没有符合精度的值，只能取区间下限
            result = min;
        } else {
            // 精度过小或数值过大，退化为连续随机
            result = Math.random() * (end - start) + start;
        }
        resultPin.getValue(PinDouble.class).setValue(Math.clamp(result, min, max));
    }

    // 取数值的小数位数，如 0.91 为 2
    private int getScale(double value) {
        if (!Double.isFinite(value)) return 0;
        double abs = Math.abs(value);
        int scale = 0;
        while (scale < 15 && Math.abs(abs - Math.round(abs)) > Math.ulp(abs) * 4) {
            abs *= 10;
            scale++;
        }
        return scale;
    }
}
