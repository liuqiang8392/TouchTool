package top.bogey.touch_tool.bean.action.image;

import android.net.Uri;

import com.google.gson.JsonObject;

import top.bogey.touch_tool.MainApplication;
import top.bogey.touch_tool.R;
import top.bogey.touch_tool.bean.action.ActionType;
import top.bogey.touch_tool.bean.action.parent.ExecuteAction;
import top.bogey.touch_tool.bean.pin.Pin;
import top.bogey.touch_tool.bean.pin.pin_objects.PinObject;
import top.bogey.touch_tool.bean.pin.pin_objects.pin_number.PinNumber;
import top.bogey.touch_tool.bean.pin.pin_objects.pin_number.PinPeriodic;
import top.bogey.touch_tool.bean.pin.pin_objects.pin_scale_able.PinImage;
import top.bogey.touch_tool.bean.pin.pin_objects.pin_string.PinString;
import top.bogey.touch_tool.bean.pin.special_pin.NotLinkAblePin;
import top.bogey.touch_tool.service.TaskRunnable;
import top.bogey.touch_tool.utils.AppUtil;

public class SaveImageAction extends ExecuteAction {
    private final transient Pin namePin = new Pin(new PinString(), R.string.save_image_action_name, false, false, true);
    private final transient Pin sourcePin = new Pin(new PinImage(), R.string.pin_image);
    private final transient Pin deletePin = new NotLinkAblePin(new PinPeriodic(), R.string.save_image_action_delay_delete, false, false, true);

    public SaveImageAction() {
        super(ActionType.SAVE_IMAGE);
        addPins(namePin, sourcePin, deletePin);
    }

    public SaveImageAction(JsonObject jsonObject) {
        super(jsonObject);
        reAddPins(namePin, sourcePin, deletePin);
    }

    @Override
    public void execute(TaskRunnable runnable, Pin pin) {
        PinImage image = getPinValue(runnable, sourcePin);
        PinObject name = getPinValue(runnable, namePin);
        PinNumber<?> delayDelete = getPinValue(runnable, deletePin);

        String nameString = name.toString();
        Uri uri;
        if (nameString.isEmpty()) {
            uri = AppUtil.writePictureImage(MainApplication.getInstance(), image.getImage());
        } else {
            uri = AppUtil.writePictureImage(MainApplication.getInstance(), MainApplication.appName, nameString, image.getImage());
        }

        if (delayDelete.longValue() > 0) {
            MainApplication.getInstance().getService().addAlarm(uri, delayDelete.longValue());
        }
        executeNext(runnable, outPin);
    }
}
