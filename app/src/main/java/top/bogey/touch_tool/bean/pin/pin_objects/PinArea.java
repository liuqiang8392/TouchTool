package top.bogey.touch_tool.bean.pin.pin_objects;

import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.RectF;

import androidx.annotation.NonNull;

import com.google.gson.JsonObject;

import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import top.bogey.touch_tool.MainApplication;
import top.bogey.touch_tool.utils.DisplayUtil;
import top.bogey.touch_tool.utils.GsonUtil;

public class PinArea extends PinObject {
    private RectF value;

    public PinArea() {
        super(PinType.AREA);
        value = new RectF(0, 0, 1, 1);
    }

    public PinArea(Rect area) {
        super(PinType.AREA);
        value = normalize(new RectF(area));
    }

    public PinArea(JsonObject jsonObject) {
        super(jsonObject);
        value = normalize(GsonUtil.getAsObject(jsonObject, "value", RectF.class, new RectF(0, 0, 1, 1)));
    }

    private RectF normalize(RectF rectF) {
        if (rectF.left > 1 || rectF.top > 1 || rectF.right > 1 || rectF.bottom > 1) {
            Point size = DisplayUtil.getScreenSize(MainApplication.getInstance());
            float width = size.x;
            float height = size.y;

            return new RectF(rectF.left / width, rectF.top / height, rectF.right / width, rectF.bottom / height);
        }
        return rectF;
    }

    @Override
    public void reset() {
        super.reset();
        value = new RectF(0, 0, 1, 1);
    }

    @Override
    public void sync(PinBase value) {
        if (value instanceof PinArea pinArea) {
            this.value = normalize(pinArea.value);
        }
    }

    @Override
    public boolean cast(String value) {
        Pattern pattern = Pattern.compile("\\((\\d+(?:\\.\\d+)?),(\\d+(?:\\.\\d+)?),(\\d+(?:\\.\\d+)?),(\\d+(?:\\.\\d+)?)\\)");
        Matcher matcher = pattern.matcher(value);
        if (matcher.find()) {
            try {
                RectF area = new RectF();
                area.left = Float.parseFloat(Objects.requireNonNull(matcher.group(1)));
                area.top = Float.parseFloat(Objects.requireNonNull(matcher.group(2)));
                area.right = Float.parseFloat(Objects.requireNonNull(matcher.group(3)));
                area.bottom = Float.parseFloat(Objects.requireNonNull(matcher.group(4)));
                this.value = normalize(area);
                return true;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return false;
    }

    @NonNull
    @Override
    public String toString() {
        Rect area = getValue();
        return super.toString() + "(" + area.left + "," + area.top + "," + area.right + "," + area.bottom + ")";
    }

    public Rect getValue() {
        Point size = DisplayUtil.getScreenSize(MainApplication.getInstance());
        float width = size.x;
        float height = size.y;
        return new Rect((int) (value.left * width), (int) (value.top * height), (int) (value.right * width), (int) (value.bottom * height));
    }

    public void setValue(Rect value) {
        this.value = normalize(new RectF(value));
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        PinArea pinArea = (PinArea) o;
        return value.equals(pinArea.value);
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + value.hashCode();
        return result;
    }
}
