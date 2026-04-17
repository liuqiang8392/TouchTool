package top.bogey.touch_tool.bean.action.string;

import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import top.bogey.touch_tool.R;
import top.bogey.touch_tool.bean.action.ActionType;
import top.bogey.touch_tool.bean.action.parent.CalculateAction;
import top.bogey.touch_tool.bean.pin.Pin;
import top.bogey.touch_tool.bean.pin.pin_objects.PinBase;
import top.bogey.touch_tool.bean.pin.pin_objects.PinObject;
import top.bogey.touch_tool.bean.pin.pin_objects.pin_list.PinList;
import top.bogey.touch_tool.bean.pin.pin_objects.pin_string.PinSingleSelect;
import top.bogey.touch_tool.bean.pin.pin_objects.pin_string.PinString;
import top.bogey.touch_tool.service.TaskRunnable;

public class StringToSingleSelectAction extends CalculateAction {
    private final transient Pin optionsPin = new Pin(new PinList(new PinString()), R.string.string_to_single_select_action_options);
    private final transient Pin textPin = new Pin(new PinString(), R.string.string_to_single_select_action_select);
    private final transient Pin singleSelectPin = new Pin(new PinSingleSelect(), R.string.pin_string_select, true);

    public StringToSingleSelectAction() {
        super(ActionType.STRING_TO_SINGLE_SELECT);
        addPins(optionsPin, textPin, singleSelectPin);
    }

    public StringToSingleSelectAction(JsonObject jsonObject) {
        super(jsonObject);
        reAddPins(optionsPin, textPin, singleSelectPin);
    }

    @Override
    public void calculate(TaskRunnable runnable, Pin pin) {
        PinList options = getPinValue(runnable, optionsPin);
        PinObject text = getPinValue(runnable, textPin);
        PinSingleSelect select = singleSelectPin.getValue();

        Pin linkedPin = singleSelectPin.getLinkedPin(runnable.getTask());
        PinBase value = linkedPin.getValue();

        if (value instanceof PinSingleSelect pinSingleSelect) {
            // 如果单选连接的是动态单选，则以options为准，否则以连接的单选为准
            if (pinSingleSelect.isDynamic()) {
                if (options.isEmpty()) {
                    select.setOptions(new ArrayList<>(Collections.singletonList(text.toString())));
                    select.setValue(text.toString());
                } else {
                    List<String> optionsList = new ArrayList<>();
                    for (PinObject option : options) {
                        optionsList.add(option.toString());
                    }
                    select.setOptions(optionsList);
                    select.setValue(text.toString());
                }
            } else {
                select.setOptions(pinSingleSelect.getOptions());
                if (pinSingleSelect.getOptions().contains(text.toString())) {
                    select.setValue(text.toString());
                } else {
                    select.setValue(pinSingleSelect.getValue());
                }
            }
        } else if (value instanceof PinString) {
            select.setOptions(new ArrayList<>(Collections.singletonList(text.toString())));
            select.setValue(text.toString());
        }
    }
}
