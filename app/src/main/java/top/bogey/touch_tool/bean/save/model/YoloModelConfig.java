package top.bogey.touch_tool.bean.save.model;

public class YoloModelConfig extends ModelConfig {
    private final boolean needNMS;

    public YoloModelConfig(String modelName, boolean needNMS) {
        super(modelName);
        this.needNMS = needNMS;
    }

    public boolean isNeedNMS() {
        return needNMS;
    }
}
