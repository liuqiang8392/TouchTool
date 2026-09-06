package top.bogey.touch_tool.bean.action.system;

import com.google.gson.JsonObject;

import java.util.Calendar;

import top.bogey.touch_tool.R;
import top.bogey.touch_tool.bean.action.ActionType;
import top.bogey.touch_tool.bean.action.parent.CalculateAction;
import top.bogey.touch_tool.bean.pin.Pin;
import top.bogey.touch_tool.bean.pin.pin_objects.pin_number.PinInteger;
import top.bogey.touch_tool.bean.pin.pin_objects.pin_number.PinLong;
import top.bogey.touch_tool.bean.pin.pin_objects.pin_number.PinNumber;
import top.bogey.touch_tool.service.TaskRunnable;

public class GetTimeAction extends CalculateAction {
    protected final transient Pin hourPin = new Pin(new PinInteger(), R.string.get_time_action_hour, true);
    protected final transient Pin minutePin = new Pin(new PinInteger(), R.string.get_time_action_minute, true);
    protected final transient Pin secondPin = new Pin(new PinInteger(), R.string.get_time_action_second, true);
    protected final transient Pin millisecondPin = new Pin(new PinInteger(), R.string.get_time_action_millisecond, true);
    protected final transient Pin timestampPin = new Pin(new PinLong(), R.string.get_time_action_timestamp, true, false, true);
    protected final transient Pin inTimestampPin = new Pin(new PinLong(), R.string.get_time_action_in_timestamp, false, false, true);

    public GetTimeAction() {
        super(ActionType.GET_CURRENT_TIME);
        addPins(hourPin, minutePin, secondPin, millisecondPin, timestampPin, inTimestampPin);
    }

    public GetTimeAction(JsonObject jsonObject) {
        super(jsonObject);
        reAddPins(hourPin, minutePin, secondPin, millisecondPin, timestampPin, inTimestampPin);
    }

    @Override
    public void calculate(TaskRunnable runnable, Pin pin) {
        PinNumber<?> timestamp = getPinValue(runnable, inTimestampPin);
        long timestampValue = timestamp.longValue();
        if (timestampValue == 0) timestampValue = System.currentTimeMillis();

        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(timestampValue);
        hourPin.getValue(PinInteger.class).setValue(calendar.get(Calendar.HOUR_OF_DAY));
        minutePin.getValue(PinInteger.class).setValue(calendar.get(Calendar.MINUTE));
        secondPin.getValue(PinInteger.class).setValue(calendar.get(Calendar.SECOND));
        millisecondPin.getValue(PinInteger.class).setValue(calendar.get(Calendar.MILLISECOND));
        timestampPin.getValue(PinLong.class).setValue(timestampValue);
    }
}
