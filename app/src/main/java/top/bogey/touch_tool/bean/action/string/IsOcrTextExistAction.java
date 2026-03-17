package top.bogey.touch_tool.bean.action.string;

import android.graphics.Bitmap;

import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.List;

import top.bogey.touch_tool.MainApplication;
import top.bogey.touch_tool.R;
import top.bogey.touch_tool.bean.action.ActionCheckResult;
import top.bogey.touch_tool.bean.action.ActionType;
import top.bogey.touch_tool.bean.action.parent.CalculateAction;
import top.bogey.touch_tool.bean.action.parent.SyncAction;
import top.bogey.touch_tool.bean.pin.Pin;
import top.bogey.touch_tool.bean.pin.pin_objects.PinBoolean;
import top.bogey.touch_tool.bean.pin.pin_objects.PinObject;
import top.bogey.touch_tool.bean.pin.pin_objects.pin_number.PinInteger;
import top.bogey.touch_tool.bean.pin.pin_objects.pin_number.PinNumber;
import top.bogey.touch_tool.bean.pin.pin_objects.pin_scale_able.PinImage;
import top.bogey.touch_tool.bean.pin.pin_objects.pin_string.PinSingleLineString;
import top.bogey.touch_tool.bean.pin.pin_objects.pin_string.PinSingleSelect;
import top.bogey.touch_tool.bean.save.model.LiteRTModel;
import top.bogey.touch_tool.bean.save.model.ModelResult;
import top.bogey.touch_tool.bean.save.model.ModelSaver;
import top.bogey.touch_tool.bean.task.Task;
import top.bogey.touch_tool.service.MainAccessibilityService;
import top.bogey.touch_tool.service.TaskRunnable;
import top.bogey.touch_tool.utils.AppUtil;

public class IsOcrTextExistAction extends CalculateAction implements SyncAction {
    private final transient Pin sourcePin = new Pin(new PinImage(), R.string.pin_image);
    private final transient Pin textPin = new Pin(new PinSingleLineString(), R.string.pin_string);
    private final transient Pin similarPin = new Pin(new PinInteger(60), R.string.is_ocr_text_exist_action_similar);
    private final transient Pin typePin = new Pin(new PinSingleSelect(), R.string.is_ocr_text_exist_action_type, false, false, true);
    private final transient Pin resultPin = new Pin(new PinBoolean(), R.string.pin_boolean_result, true);

    public IsOcrTextExistAction() {
        super(ActionType.IS_OCR_TEXT_EXIST);
        addPins(sourcePin, textPin, similarPin, typePin, resultPin);
    }

    public IsOcrTextExistAction(JsonObject jsonObject) {
        super(jsonObject);
        reAddPins(sourcePin, textPin, similarPin, typePin, resultPin);
    }

    @Override
    public void calculate(TaskRunnable runnable, Pin pin) {
        sync(runnable.getTask());
        PinImage source = getPinValue(runnable, sourcePin);
        PinObject text = getPinValue(runnable, textPin);
        PinNumber<?> similar = getPinValue(runnable, similarPin);
        PinSingleSelect type = getPinValue(runnable, typePin);

        String value = text.toString();
        if (value.isEmpty()) return;

        Bitmap bitmap = source.getImage();
        if (bitmap == null) return;

        List<LiteRTModel> models = ModelSaver.getInstance().getModelList(LiteRTModel.ModelType.OCR);
        if (models.size() <= type.getIndex()) return;

        MainAccessibilityService service = MainApplication.getInstance().getService();
        LiteRTModel model = models.get(type.getIndex());
        List<ModelResult> results = model.execute(service, bitmap, similar.floatValue() / 100f);
        if (results.isEmpty()) return;

        for (ModelResult result : results) {
            if (result.getValue() * 100 < similar.intValue()) continue;
            if (AppUtil.isStringContains(result.getText(), value)) {
                resultPin.getValue(PinBoolean.class).setValue(true);
                return;
            }
        }
    }

    @Override
    public void check(ActionCheckResult result, Task task) {
        super.check(result, task);
        List<LiteRTModel> models = ModelSaver.getInstance().getModelList(LiteRTModel.ModelType.OCR);
        if (models.isEmpty()) {
            result.addResult(ActionCheckResult.ResultType.ERROR, R.string.check_need_ocr_module_error);
        }
    }

    @Override
    public void sync(Task context) {
        List<LiteRTModel> models = ModelSaver.getInstance().getModelList(LiteRTModel.ModelType.OCR);
        if (models.isEmpty()) return;

        List<String> modelNames = new ArrayList<>();
        for (LiteRTModel model : models) {
            modelNames.add(model.getName());
        }
        typePin.getValue(PinSingleSelect.class).setOptions(modelNames);
        typePin.setValue(context, typePin.getValue());
    }
}
