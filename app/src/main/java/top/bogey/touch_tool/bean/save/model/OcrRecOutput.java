package top.bogey.touch_tool.bean.save.model;

public class OcrRecOutput {
    private final int[] index;
    private final float value;

    public OcrRecOutput(int[] index, float value) {
        this.index = index;
        this.value = value;
    }

    public int[] getIndex() {
        return index;
    }

    public float getValue() {
        return value;
    }
}
