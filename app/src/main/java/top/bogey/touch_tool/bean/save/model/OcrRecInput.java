package top.bogey.touch_tool.bean.save.model;

import android.graphics.Rect;

public class OcrRecInput {
    public float[] input;
    public int index;
    public float x;
    public float y;
    public Rect area;

    public OcrRecInput(float[] input, int index, float x, float y, int left, int top, int right, int bottom) {
        this.input = input;
        this.index = index;
        this.x = x;
        this.y = y;
        this.area = new Rect(left, top, right, bottom);
    }
}
