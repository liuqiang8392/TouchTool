package top.bogey.touch_tool.bean.pin.pin_objects;

import android.graphics.Point;
import android.graphics.PointF;

import androidx.annotation.NonNull;

import com.google.gson.JsonObject;

import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import top.bogey.touch_tool.MainApplication;
import top.bogey.touch_tool.utils.DisplayUtil;
import top.bogey.touch_tool.utils.GsonUtil;

public class PinPoint extends PinObject {
    private PointF value;

    public PinPoint() {
        super(PinType.POINT);
        value = new PointF();
    }

    public PinPoint(int x, int y) {
        this();
        value = normalize(new PointF(x, y));
    }

    public PinPoint(float x, float y) {
        this();
        value = normalize(new PointF(x, y));
    }

    public PinPoint(JsonObject jsonObject) {
        super(jsonObject);
        value = normalize(GsonUtil.getAsObject(jsonObject, "value", PointF.class, new PointF()));
    }

    private PointF normalize(PointF point) {
        if (point.x > 1 || point.y > 1) {
            Point size = DisplayUtil.getScreenSize(MainApplication.getInstance());
            float width = size.x;
            float height = size.y;

            return new PointF(point.x / width, point.y / height);
        }
        return point;
    }

    @Override
    public void reset() {
        super.reset();
        value = new PointF();
    }

    @Override
    public void sync(PinBase value) {
        if (value instanceof PinPoint pinPoint) {
            this.value = normalize(pinPoint.value);
        }
    }

    @Override
    public boolean cast(String value) {
        Pattern pattern = Pattern.compile("\\((\\d+(?:\\.\\d+)?),(\\d+(?:\\.\\d+)?)\\)");
        Matcher matcher = pattern.matcher(value);
        if (matcher.find()) {
            try {
                float x = Float.parseFloat(Objects.requireNonNull(matcher.group(1)));
                float y = Float.parseFloat(Objects.requireNonNull(matcher.group(2)));
                this.value = normalize(new PointF(x, y));
                return true;
            } catch (NumberFormatException e) {
                e.printStackTrace();
            }
        }
        return false;
    }

    @NonNull
    @Override
    public String toString() {
        Point point = getValue();
        return super.toString() + "(" + point.x + "," + point.y + ")";
    }

    public Point getValue() {
        Point size = DisplayUtil.getScreenSize(MainApplication.getInstance());
        float width = size.x;
        float height = size.y;
        return new Point((int) (value.x * width), (int) (value.y * height));
    }

    public void setValue(int x, int y) {
        value = normalize(new PointF(x, y));
    }

    public void setValue(Point value) {
        this.value = normalize(new PointF(value.x, value.y));
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        PinPoint pinPoint = (PinPoint) o;
        return value.equals(pinPoint.value);
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + value.hashCode();
        return result;
    }
}
