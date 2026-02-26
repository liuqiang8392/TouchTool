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
import top.bogey.touch_tool.bean.action.parent.ExecuteOrCalculateAction;
import top.bogey.touch_tool.bean.action.parent.SyncAction;
import top.bogey.touch_tool.bean.pin.Pin;
import top.bogey.touch_tool.bean.pin.pin_objects.pin_list.PinList;
import top.bogey.touch_tool.bean.pin.pin_objects.pin_number.PinInteger;
import top.bogey.touch_tool.bean.pin.pin_objects.pin_number.PinNumber;
import top.bogey.touch_tool.bean.pin.pin_objects.pin_scale_able.PinArea;
import top.bogey.touch_tool.bean.pin.pin_objects.pin_scale_able.PinImage;
import top.bogey.touch_tool.bean.pin.pin_objects.pin_string.PinSingleSelect;
import top.bogey.touch_tool.bean.pin.pin_objects.pin_string.PinString;
import top.bogey.touch_tool.bean.task.Task;
import top.bogey.touch_tool.service.MainAccessibilityService;
import top.bogey.touch_tool.service.OcrResult;
import top.bogey.touch_tool.service.TaskInfoSummary;
import top.bogey.touch_tool.service.TaskRunnable;

public class GetOcrTextAction extends ExecuteOrCalculateAction implements SyncAction {
    private final transient Pin sourcePin = new Pin(new PinImage(), R.string.pin_image);
    private final transient Pin similarPin = new Pin(new PinInteger(60), R.string.get_ocr_text_action_similar);
    private final transient Pin typePin = new Pin(new PinSingleSelect(), R.string.get_ocr_text_action_type, false, false, true);
    private final transient Pin textPin = new Pin(new PinString(), R.string.get_ocr_text_action_text, true);
    private final transient Pin textArrayPin = new Pin(new PinList(new PinString()), true);
    private final transient Pin areaArrayPin = new Pin(new PinList(new PinArea()), true);

    public GetOcrTextAction() {
        super(ActionType.GET_OCR_TEXT);
        addPins(sourcePin, similarPin, typePin, textPin, textArrayPin, areaArrayPin);
    }

    public GetOcrTextAction(JsonObject jsonObject) {
        super(jsonObject);
        reAddPins(sourcePin, similarPin, typePin, textPin, textArrayPin, areaArrayPin);
    }

    @Override
    protected void doAction(TaskRunnable runnable, Pin pin) {
        sync(runnable.getTask());
        PinImage source = getPinValue(runnable, sourcePin);
        PinNumber<?> similar = getPinValue(runnable, similarPin);
        PinSingleSelect type = getPinValue(runnable, typePin);

        Bitmap bitmap = source.getImage();

        AtomicReference<List<OcrResult>> ocrResultsReference = new AtomicReference<>();
        AtomicBoolean pause = new AtomicBoolean(true);

        List<String> ocrApps = TaskInfoSummary.getInstance().getOcrApps();
        if (ocrApps.size() > type.getIndex()) {
            MainAccessibilityService service = MainApplication.getInstance().getService();
            String packageName = ocrApps.get(type.getIndex());
            service.runOcr(bitmap, packageName, result -> {
                ocrResultsReference.set(result);
                pause.set(false);
                runnable.resume();
            });
            if (pause.get()) runnable.await();
        }

        PinList textArray = textArrayPin.getValue(PinList.class);
        PinList areaArray = areaArrayPin.getValue(PinList.class);
        List<OcrResult> ocrResults = ocrResultsReference.get();
        if (ocrResults != null) {
            StringBuilder builder = new StringBuilder();
            for (OcrResult ocrResult : ocrResults) {
                if (ocrResult.getSimilar() < similar.intValue()) continue;
                builder.append(ocrResult.getText()).append("\n");
                textArray.add(new PinString(ocrResult.getText()));
                areaArray.add(new PinArea(ocrResult.getArea()));
            }
            textPin.getValue(PinString.class).setValue(builder.toString().trim());
        } else {
            textPin.getValue(PinString.class).setValue("");
            textArray.clear();
            areaArray.clear();
        }
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
