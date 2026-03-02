package top.bogey.touch_tool.bean.action.system;

import android.graphics.Bitmap;

import com.google.gson.JsonObject;

import top.bogey.touch_tool.R;
import top.bogey.touch_tool.bean.action.ActionType;
import top.bogey.touch_tool.bean.action.parent.ExecuteAction;
import top.bogey.touch_tool.bean.pin.Pin;
import top.bogey.touch_tool.bean.pin.pin_objects.PinBase;
import top.bogey.touch_tool.bean.pin.pin_objects.pin_scale_able.PinImage;
import top.bogey.touch_tool.bean.pin.pin_objects.pin_string.PinString;
import top.bogey.touch_tool.bean.pin.special_pin.ShowAblePin;
import top.bogey.touch_tool.bean.task.Task;
import top.bogey.touch_tool.service.TaskRunnable;
import top.bogey.touch_tool.ui.custom.float_view.ReadClipboardFloatView;

public class ReadFromClipboardAction extends ExecuteAction {
    private final transient Pin textPin = new ResultShowablePin(new PinString(), R.string.pin_string, true);
    private final transient Pin imagePin = new ResultShowablePin(new PinImage(), R.string.pin_image, true);

    public ReadFromClipboardAction() {
        super(ActionType.READ_FROM_CLIPBOARD);
        addPins(textPin, imagePin);
    }

    public ReadFromClipboardAction(JsonObject jsonObject) {
        super(jsonObject);
        reAddPins(textPin, imagePin);
    }

    @Override
    public void execute(TaskRunnable runnable, Pin pin) {
        Object result = ReadClipboardFloatView.getClipboardData();
        if (result instanceof String string) {
            textPin.getValue(PinString.class).setValue(string);
        } else if (result instanceof Bitmap bitmap) {
            imagePin.getValue(PinImage.class).setImage(bitmap);
        }
        executeNext(runnable, outPin);
    }

    public Pin getTextPin() {
        return textPin;
    }

    public Pin getImagePin() {
        return imagePin;
    }

    private static class ResultShowablePin extends ShowAblePin {
        public ResultShowablePin(PinBase value, int titleId, boolean out) {
            super(value, titleId, out);
        }

        @Override
        public boolean showAble(Task context) {
            ReadFromClipboardAction action = (ReadFromClipboardAction) context.getAction(getOwnerId());
            Pin textPin = action.getTextPin();
            Pin imagePin = action.getImagePin();
            if (textPin.isLinked() || imagePin.isLinked()) {
                if (textPin.isLinked() && textPin.equals(this)) return true;
                return imagePin.isLinked() && imagePin.equals(this);
            } else {
                return true;
            }
        }
    }
}
