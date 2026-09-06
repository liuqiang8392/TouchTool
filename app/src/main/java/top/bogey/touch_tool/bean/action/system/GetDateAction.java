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

public class GetDateAction extends CalculateAction {
    private final transient Pin yearPin = new Pin(new PinInteger(), R.string.get_date_action_year, true);
    private final transient Pin monthPin = new Pin(new PinInteger(), R.string.get_date_action_month, true);
    private final transient Pin dayPin = new Pin(new PinInteger(), R.string.get_date_action_day, true);
    private final transient Pin weekPin = new Pin(new PinInteger(), R.string.get_date_action_week, true);

    private final transient Pin timestampPin = new Pin(new PinLong(), R.string.get_date_action_timestamp, false, false, true);

    public GetDateAction() {
        super(ActionType.GET_CURRENT_DATE);
        addPins(yearPin, monthPin, dayPin, weekPin, timestampPin);
    }

    public GetDateAction(JsonObject jsonObject) {
        super(jsonObject);
        reAddPins(yearPin, monthPin, dayPin, weekPin, timestampPin);
    }

    @Override
    public void calculate(TaskRunnable runnable, Pin pin) {
        PinNumber<?> timestamp = getPinValue(runnable, timestampPin);
        long timestampValue = timestamp.longValue();
        if (timestampValue == 0) timestampValue = System.currentTimeMillis();

        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(timestampValue);
        yearPin.getValue(PinInteger.class).setValue(calendar.get(Calendar.YEAR));
        monthPin.getValue(PinInteger.class).setValue(calendar.get(Calendar.MONTH) + 1);
        dayPin.getValue(PinInteger.class).setValue(calendar.get(Calendar.DAY_OF_MONTH));
        int weekDay = calendar.get(Calendar.DAY_OF_WEEK) + 1 - Calendar.MONDAY;
        if (weekDay <= 0) weekDay += 7;
        weekPin.getValue(PinInteger.class).setValue(weekDay);
    }
}
