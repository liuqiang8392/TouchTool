package top.bogey.touch_tool.bean.action.image;

import android.graphics.Bitmap;
import android.graphics.Rect;
import android.graphics.RectF;

import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.List;

import top.bogey.touch_tool.MainApplication;
import top.bogey.touch_tool.R;
import top.bogey.touch_tool.bean.action.ActionType;
import top.bogey.touch_tool.bean.action.parent.ExecuteAction;
import top.bogey.touch_tool.bean.action.parent.SyncAction;
import top.bogey.touch_tool.bean.pin.Pin;
import top.bogey.touch_tool.bean.pin.pin_objects.pin_list.PinList;
import top.bogey.touch_tool.bean.pin.pin_objects.pin_number.PinInteger;
import top.bogey.touch_tool.bean.pin.pin_objects.pin_number.PinNumber;
import top.bogey.touch_tool.bean.pin.pin_objects.pin_scale_able.PinArea;
import top.bogey.touch_tool.bean.pin.pin_objects.pin_scale_able.PinImage;
import top.bogey.touch_tool.bean.pin.pin_objects.pin_string.PinSingleSelect;
import top.bogey.touch_tool.bean.pin.pin_objects.pin_string.PinString;
import top.bogey.touch_tool.bean.save.model.LiteRTModel;
import top.bogey.touch_tool.bean.save.model.ModelResult;
import top.bogey.touch_tool.bean.save.model.ModelSaver;
import top.bogey.touch_tool.bean.task.Task;
import top.bogey.touch_tool.service.MainAccessibilityService;
import top.bogey.touch_tool.service.TaskRunnable;

public class YoloDetectAction extends ExecuteAction implements SyncAction {
    private final transient Pin sourcePin = new Pin(new PinImage(), R.string.pin_image_full, false, false, true);
    private final transient Pin modelPin = new Pin(new PinSingleSelect(), R.string.yolo_detect_action_model);
    private final transient Pin similarityPin = new Pin(new PinInteger(80), R.string.yolo_detect_action_similarity);
    private final transient Pin targetsPin = new Pin(new PinList(new PinString()), R.string.yolo_detect_action_target, true);
    private final transient Pin areasPin = new Pin(new PinList(new PinArea()), true);
    private final transient Pin confPin = new Pin(new PinList(new PinInteger()), R.string.yolo_detect_action_similarity, true);

    public YoloDetectAction() {
        super(ActionType.YOLO_DETECT);
        addPins(sourcePin, modelPin, similarityPin, targetsPin, areasPin, confPin);
    }

    public YoloDetectAction(JsonObject jsonObject) {
        super(jsonObject);
        reAddPins(sourcePin, modelPin, similarityPin, targetsPin, areasPin, confPin);
    }

    @Override
    public void execute(TaskRunnable runnable, Pin pin) {
        sync(runnable.getTask());
        MainAccessibilityService service = MainApplication.getInstance().getService();
        Bitmap bitmap;
        if (sourcePin.isLinked()) {
            PinImage source = getPinValue(runnable, sourcePin);
            bitmap = source.getImage();
        } else {
            bitmap = service.tryGetScreenShot();
        }

        PinSingleSelect model = getPinValue(runnable, modelPin);
        PinNumber<?> similarity = getPinValue(runnable, similarityPin);

        List<LiteRTModel> models = ModelSaver.getInstance().getModelList(LiteRTModel.ModelType.YOLO);
        if (!models.isEmpty()) {
            if (models.size() > model.getIndex()) {
                LiteRTModel yoloModel = models.get(model.getIndex());
                List<ModelResult> results = yoloModel.execute(service, bitmap, similarity.floatValue() / 100f);
                results.forEach(result -> {
                    targetsPin.getValue(PinList.class).add(new PinString(result.getText()));
                    RectF area = result.getArea();
                    Rect rect = new Rect((int) area.left, (int) area.top, (int) area.right, (int) area.bottom);
                    areasPin.getValue(PinList.class).add(new PinArea(rect));
                    confPin.getValue(PinList.class).add(new PinInteger((int) (result.getValue() * 100)));
                });
            }
        }
        executeNext(runnable, outPin);
    }

    @Override
    public void sync(Task context) {
        List<LiteRTModel> models = ModelSaver.getInstance().getModelList(LiteRTModel.ModelType.YOLO);
        if (models.isEmpty()) return;
        List<String> modelNames = new ArrayList<>();
        for (LiteRTModel model : models) {
            modelNames.add(model.getName());
        }
        modelPin.getValue(PinSingleSelect.class).setOptions(modelNames);
        modelPin.setValue(context, modelPin.getValue());
    }
}
