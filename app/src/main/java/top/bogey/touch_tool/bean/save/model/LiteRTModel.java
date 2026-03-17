package top.bogey.touch_tool.bean.save.model;

import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

import java.io.File;
import java.lang.reflect.Type;
import java.util.List;
import java.util.UUID;

import top.bogey.touch_tool.utils.AppUtil;
import top.bogey.touch_tool.utils.GsonUtil;

public abstract class LiteRTModel {
    public static final String MODEL_SUFFIX = ".tflite";
    private static final String MODEL_DIR_NAME = "model";

    protected final String id;
    protected final long time;
    protected final ModelType type;

    protected String name;
    protected String description;
    protected String author;


    public LiteRTModel(ModelType type) {
        this.id = UUID.randomUUID().toString();
        this.time = System.currentTimeMillis();
        this.type = type;
    }

    protected LiteRTModel(JsonObject jsonObject) {
        id = GsonUtil.getAsString(jsonObject, "id", UUID.randomUUID().toString());
        time = GsonUtil.getAsLong(jsonObject, "time", System.currentTimeMillis());
        type = GsonUtil.getAsObject(jsonObject, "type", ModelType.class, ModelType.UNKNOWN);

        name = GsonUtil.getAsString(jsonObject, "name", "");
        description = GsonUtil.getAsString(jsonObject, "description", "");
        author = GsonUtil.getAsString(jsonObject, "author", "");
    }

    public abstract boolean importModel(Context context, Uri uri);

    public abstract List<ModelResult> execute(Context context, Bitmap bitmap, float confThreshold);

    public void removeModel(Context context) {
        String modelDirPath = getModelDirPath(context);
        File modelDir = new File(modelDirPath);
        if (!modelDir.exists()) return;
        AppUtil.deleteFile(modelDir);
    }

    public String getModelDirPath(Context context) {
        return context.getFilesDir() + File.separator + MODEL_DIR_NAME + File.separator + id;
    }

    public String getId() {
        return id;
    }

    public long getTime() {
        return time;
    }

    public ModelType getType() {
        return type;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public String getAuthor() {
        return author;
    }

    public static class LiteRTModelDeserializer implements JsonDeserializer<LiteRTModel> {

        @Override
        public LiteRTModel deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            JsonObject jsonObject = json.getAsJsonObject();
            ModelType type = GsonUtil.getAsObject(jsonObject, "type", ModelType.class, ModelType.UNKNOWN);
            return switch (type) {
                case YOLO -> new YoloModel(jsonObject);
                case OCR -> new OcrModel(jsonObject);
                default -> null;
            };
        }
    }

    public enum ModelType {
        UNKNOWN,
        YOLO,
        OCR
    }
}
