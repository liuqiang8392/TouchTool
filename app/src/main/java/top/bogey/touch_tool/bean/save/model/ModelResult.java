package top.bogey.touch_tool.bean.save.model;

import android.graphics.RectF;

import androidx.annotation.NonNull;

public class ModelResult {
    private final String text;
    private final float value;
    private final RectF area;

    ModelResult(String text, float value, RectF area) {
        this.text = text;
        this.value = value;
        this.area = area;
    }

    public ModelResult(String text, float value) {
        this(text, value, new RectF());
    }

    @NonNull
    @Override
    public String toString() {
        return "ModelResult{" + "text='" + text + '\'' + ", value=" + value + ", area=" + area + '}';
    }

    public String getText() {
        return text;
    }

    public float getValue() {
        return value;
    }

    public RectF getArea() {
        return area;
    }
}
