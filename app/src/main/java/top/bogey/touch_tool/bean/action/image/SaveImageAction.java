package top.bogey.touch_tool.bean.action.image;

import com.google.gson.JsonObject;

import top.bogey.touch_tool.MainApplication;
import top.bogey.touch_tool.R;
import top.bogey.touch_tool.bean.action.ActionType;
import top.bogey.touch_tool.bean.action.parent.ExecuteAction;
import top.bogey.touch_tool.bean.pin.Pin;
import top.bogey.touch_tool.bean.pin.pin_objects.PinObject;
import top.bogey.touch_tool.bean.pin.pin_objects.pin_scale_able.PinImage;
import top.bogey.touch_tool.bean.pin.pin_objects.pin_string.PinString;
import top.bogey.touch_tool.service.TaskRunnable;
import top.bogey.touch_tool.utils.AppUtil;

public class SaveImageAction extends ExecuteAction {
    private final transient Pin namePin = new Pin(new PinString(), R.string.save_image_action_name, false, false, true);
    private final transient Pin sourcePin = new Pin(new PinImage(), R.string.pin_image);

    public SaveImageAction() {
        super(ActionType.SAVE_IMAGE);
        addPins(namePin, sourcePin);
    }

    public SaveImageAction(JsonObject jsonObject) {
        super(jsonObject);
        reAddPins(namePin, sourcePin);
    }

    @Override
    public void execute(TaskRunnable runnable, Pin pin) {
        PinImage image = getPinValue(runnable, sourcePin);
        PinObject name = getPinValue(runnable, namePin);
        String nameString = name.toString();
        if (nameString.isEmpty()) nameString = AppUtil.formatDateTime(MainApplication.getInstance(), System.currentTimeMillis(), false, false);

        AppUtil.saveImage(MainApplication.getInstance(), image.getImage(), nameString);
        executeNext(runnable, outPin);
    }
}
