package top.bogey.touch_tool.bean.action.image;

import android.graphics.Bitmap;
import android.graphics.Rect;

import com.google.gson.JsonObject;

import top.bogey.touch_tool.R;
import top.bogey.touch_tool.bean.action.ActionType;
import top.bogey.touch_tool.bean.action.parent.ExecuteAction;
import top.bogey.touch_tool.bean.pin.Pin;
import top.bogey.touch_tool.bean.pin.pin_objects.pin_scale_able.PinArea;
import top.bogey.touch_tool.bean.pin.pin_objects.pin_scale_able.PinImage;
import top.bogey.touch_tool.bean.pin.pin_objects.pin_scale_able.PinPoint;
import top.bogey.touch_tool.service.TaskRunnable;
import top.bogey.touch_tool.utils.DisplayUtil;

public class CropImageAction extends ExecuteAction {
    private final transient Pin sourcePin = new Pin(new PinImage(), R.string.pin_image);
    private final transient Pin areaPin = new Pin(new PinArea(), R.string.pin_area);
    private final transient Pin imagePin = new Pin(new PinImage(), R.string.pin_image, true);
    private final transient Pin offsetPin = new Pin(new PinPoint(), R.string.crop_image_action_offset, true);

    public CropImageAction() {
        super(ActionType.CROP_IMAGE);
        addPins(sourcePin, areaPin, imagePin, offsetPin);
    }

    public CropImageAction(JsonObject jsonObject) {
        super(jsonObject);
        reAddPins(sourcePin, areaPin, imagePin, offsetPin);
    }

    @Override
    public void execute(TaskRunnable runnable, Pin pin) {
        PinImage source = getPinValue(runnable, sourcePin);
        PinArea area = getPinValue(runnable, areaPin);
        Rect areaRect = area.getValue();
        Bitmap clipBitmap = DisplayUtil.safeClipBitmap(source.getImage(), areaRect.left, areaRect.top, areaRect.width(), areaRect.height());
        if (clipBitmap != null) {
            imagePin.getValue(PinImage.class).setImage(clipBitmap);
            offsetPin.getValue(PinPoint.class).setValue(areaRect.left, areaRect.top);
        }
        executeNext(runnable, outPin);
    }
}
