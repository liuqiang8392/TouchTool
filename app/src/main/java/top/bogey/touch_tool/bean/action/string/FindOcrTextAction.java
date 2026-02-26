package top.bogey.touch_tool.bean.action.string;

import android.graphics.Bitmap;

import com.google.gson.JsonObject;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import top.bogey.touch_tool.MainApplication;
import top.bogey.touch_tool.R;
import top.bogey.touch_tool.bean.action.ActionCheckResult;
import top.bogey.touch_tool.bean.action.ActionType;
import top.bogey.touch_tool.bean.action.parent.FindExecuteAction;
import top.bogey.touch_tool.bean.action.parent.SyncAction;
import top.bogey.touch_tool.bean.pin.Pin;
import top.bogey.touch_tool.bean.pin.pin_objects.PinObject;
import top.bogey.touch_tool.bean.pin.pin_objects.pin_number.PinInteger;
import top.bogey.touch_tool.bean.pin.pin_objects.pin_number.PinNumber;
import top.bogey.touch_tool.bean.pin.pin_objects.pin_scale_able.PinArea;
import top.bogey.touch_tool.bean.pin.pin_objects.pin_scale_able.PinImage;
import top.bogey.touch_tool.bean.pin.pin_objects.pin_string.PinSingleLineString;
import top.bogey.touch_tool.bean.pin.pin_objects.pin_string.PinSingleSelect;
import top.bogey.touch_tool.bean.pin.pin_objects.pin_string.PinString;
import top.bogey.touch_tool.bean.task.Task;
import top.bogey.touch_tool.service.MainAccessibilityService;
import top.bogey.touch_tool.service.OcrResult;
import top.bogey.touch_tool.service.TaskInfoSummary;
import top.bogey.touch_tool.service.TaskRunnable;
import top.bogey.touch_tool.utils.AppUtil;

public class FindOcrTextAction extends FindExecuteAction implements SyncAction {
    private final transient Pin sourcePin = new Pin(new PinImage(), R.string.pin_image);
    private final transient Pin textPin = new Pin(new PinSingleLineString(), R.string.pin_string);
    private final transient Pin similarPin = new Pin(new PinInteger(60), R.string.find_ocr_text_action_similar);
    private final transient Pin typePin = new Pin(new PinSingleSelect(), R.string.find_ocr_text_action_type);
    private final transient Pin resultAreaPin = new Pin(new PinArea(), R.string.pin_area, true);
    private final transient Pin resultTextPin = new Pin(new PinString(), R.string.pin_string, true);

    public FindOcrTextAction() {
        super(ActionType.FIND_OCR_TEXT);
        addPins(sourcePin, textPin, similarPin, typePin, resultAreaPin, resultTextPin);
    }

    public FindOcrTextAction(JsonObject jsonObject) {
        super(jsonObject);
        reAddPins(sourcePin, textPin, similarPin, typePin, resultAreaPin, resultTextPin);
    }

    @Override
    public boolean find(TaskRunnable runnable) {
        sync(runnable.getTask());
        PinImage source = getPinValue(runnable, sourcePin);
        PinObject text = getPinValue(runnable, textPin);
        PinNumber<?> similar = getPinValue(runnable, similarPin);
        PinSingleSelect type = getPinValue(runnable, typePin);

        String value = text.toString();
        if (value.isEmpty()) return false;

        Bitmap bitmap = source.getImage();
        if (bitmap == null) return false;
        List<String> ocrApps = TaskInfoSummary.getInstance().getOcrApps();
        if (ocrApps.size() <= type.getIndex()) return false;

        AtomicReference<List<OcrResult>> ocrResultsReference = new AtomicReference<>();
        AtomicBoolean pause = new AtomicBoolean(true);

        MainAccessibilityService service = MainApplication.getInstance().getService();
        String packageName = ocrApps.get(type.getIndex());
        service.runOcr(bitmap, packageName, result -> {
            ocrResultsReference.set(result);
            pause.set(false);
            runnable.resume();
        });
        if (pause.get()) runnable.await();

        List<OcrResult> ocrResults = ocrResultsReference.get();
        if (ocrResults == null) return false;

        for (OcrResult ocrResult : ocrResults) {
            if (ocrResult.getSimilar() < similar.intValue()) continue;
            if (AppUtil.isStringContains(ocrResult.getText(), value)) {
                resultAreaPin.getValue(PinArea.class).setValue(ocrResult.getArea());
                resultTextPin.getValue(PinString.class).setValue(ocrResult.getText());
                return true;
            }
        }

        return false;
    }

    @Override
    public void check(ActionCheckResult result, Task task) {
        super.check(result, task);
        List<String> ocrApps = TaskInfoSummary.getInstance().getOcrApps();
        if (ocrApps.isEmpty()) {
            result.addResult(ActionCheckResult.ResultType.ERROR, R.string.check_need_ocr_module_error);
        }
    }

    @Override
    public void sync(Task context) {
        typePin.getValue(PinSingleSelect.class).setOptions(TaskInfoSummary.getInstance().getOcrAppNames());
    }
}
