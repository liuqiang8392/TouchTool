package top.bogey.touch_tool.bean.action.system;

import com.google.gson.JsonObject;

import top.bogey.touch_tool.MainApplication;
import top.bogey.touch_tool.R;
import top.bogey.touch_tool.bean.action.ActionType;
import top.bogey.touch_tool.bean.action.parent.ExecuteAction;
import top.bogey.touch_tool.bean.pin.Pin;
import top.bogey.touch_tool.bean.pin.pin_objects.PinBoolean;
import top.bogey.touch_tool.bean.pin.pin_objects.pin_string.PinString;
import top.bogey.touch_tool.service.MainAccessibilityService;
import top.bogey.touch_tool.service.TaskRunnable;

public class TextToSpeechAction extends ExecuteAction {
    private final transient Pin textPin = new Pin(new PinString(), R.string.pin_string);
    private final transient Pin modePin = new Pin(new PinBoolean(false), R.string.text_to_speak_action_mode);

    public TextToSpeechAction() {
        super(ActionType.TEXT_TO_SPEECH);
        addPins(textPin, modePin);
    }

    public TextToSpeechAction(JsonObject jsonObject) {
        super(jsonObject);
        reAddPins(textPin, modePin);
    }

    @Override
    public void execute(TaskRunnable runnable, Pin pin) {
        PinString text = getPinValue(runnable, textPin);
        PinBoolean mode = getPinValue(runnable, modePin);

        MainAccessibilityService service = MainApplication.getInstance().getService();
        service.speak(text.getValue(), result -> {
            if (mode.getValue()) runnable.resume();
        });

        if (mode.getValue()) runnable.await();
        executeNext(runnable, outPin);
    }
}
