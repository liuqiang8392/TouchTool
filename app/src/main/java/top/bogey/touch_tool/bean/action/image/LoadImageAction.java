package top.bogey.touch_tool.bean.action.image;

import android.graphics.Bitmap;
import android.graphics.Rect;

import com.google.gson.JsonObject;

import top.bogey.touch_tool.R;
import top.bogey.touch_tool.bean.action.ActionType;
import top.bogey.touch_tool.bean.action.parent.CalculateAction;
import top.bogey.touch_tool.bean.pin.Pin;
import top.bogey.touch_tool.bean.pin.pin_objects.PinSubType;
import top.bogey.touch_tool.bean.pin.pin_objects.pin_scale_able.PinArea;
import top.bogey.touch_tool.bean.pin.pin_objects.pin_scale_able.PinImage;
import top.bogey.touch_tool.bean.pin.special_pin.NotLinkAblePin;
import top.bogey.touch_tool.service.TaskRunnable;

public class LoadImageAction extends CalculateAction {
    private final transient Pin filePin = new NotLinkAblePin(new PinImage(PinSubType.FILE_CONTENT), R.string.pin_image);
    private final transient Pin imagePin = new Pin(new PinImage(), R.string.pin_boolean_result, true);
    private final transient Pin areaPin = new Pin(new PinArea(), R.string.pin_area, true);

    public LoadImageAction() {
        super(ActionType.LOAD_IMAGE);
        addPins(filePin, imagePin, areaPin);
    }

    public LoadImageAction(JsonObject jsonObject) {
        super(jsonObject);
        reAddPins(filePin, imagePin, areaPin);
    }

    @Override
    public void calculate(TaskRunnable runnable, Pin pin) {
        PinImage image = getPinValue(runnable, filePin);
        Bitmap bitmap = image.getImage();
        if (bitmap == null) return;
        imagePin.getValue(PinImage.class).setImage(bitmap);
        areaPin.getValue(PinArea.class).setValue(new Rect(0, 0, bitmap.getWidth(), bitmap.getHeight()));
    }
}
