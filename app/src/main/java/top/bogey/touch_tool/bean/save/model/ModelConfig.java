package top.bogey.touch_tool.bean.save.model;

public abstract class ModelConfig {
    private final String modelName;

    public ModelConfig(String modelName) {
        modelName = modelName.replace(LiteRTModel.MODEL_SUFFIX, "");
        this.modelName = modelName;
    }

    public String getModelName() {
        return modelName;
    }
}
