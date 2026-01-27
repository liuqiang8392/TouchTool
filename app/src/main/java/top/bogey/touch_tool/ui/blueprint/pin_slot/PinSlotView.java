package top.bogey.touch_tool.ui.blueprint.pin_slot;

import android.content.Context;
import android.graphics.Color;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;

import top.bogey.touch_tool.bean.pin.Pin;
import top.bogey.touch_tool.bean.pin.PinInfo;
import top.bogey.touch_tool.bean.pin.pin_objects.PinBase;
import top.bogey.touch_tool.bean.pin.pin_objects.PinMap;
import top.bogey.touch_tool.bean.pin.pin_objects.pin_list.PinList;
import top.bogey.touch_tool.utils.DisplayUtil;

public abstract class PinSlotView extends FrameLayout {
    protected final Pin pin;

    public PinSlotView(@NonNull Context context, Pin pin) {
        super(context);
        this.pin = pin;
        ViewGroup.LayoutParams params = getLayoutParams();
        if (params == null) params = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        setLayoutParams(params);
    }

    public abstract void setLinked(boolean linked);

    public @ColorInt int getPinColor() {
        return getPinColor(pin);
    }

    protected static @ColorInt int getPinColor(Pin pin) {
        if (pin.linkAble()) {
            PinBase pinBase = pin.getValue();
            if (pinBase instanceof PinList pinList) {
                return getPinColor(new Pin(pinList.getValueType()));
            }
            if (pinBase instanceof PinMap pinMap) {
                int keyColor = getPinColor(new Pin(pinMap.getKeyType()));
                int valueColor = getPinColor(new Pin(pinMap.getValueType()));
                return DisplayUtil.blendColor(keyColor, valueColor, 0.7f);
            }
            return PinInfo.getPinInfo(pinBase.getType(), pinBase.getSubType()).getColor();
        }
        return Color.GRAY;
    }
}
