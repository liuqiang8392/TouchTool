package top.bogey.touch_tool.bean.action.image;

import android.graphics.Bitmap;
import android.graphics.Rect;
import android.graphics.RectF;

import com.google.gson.JsonObject;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import top.bogey.touch_tool.MainApplication;
import top.bogey.touch_tool.R;
import top.bogey.touch_tool.bean.action.ActionType;
import top.bogey.touch_tool.bean.action.parent.ExecuteAction;
import top.bogey.touch_tool.bean.action.parent.SyncAction;
import top.bogey.touch_tool.bean.pin.Pin;
import top.bogey.touch_tool.bean.pin.pin_objects.pin_list.PinList;
import top.bogey.touch_tool.bean.pin.pin_objects.pin_number.PinInteger;
import top.bogey.touch_tool.bean.pin.pin_objects.pin_number.PinNumber;
import top.bogey.touch_tool.bean.pin.pin_objects.PinArea;
import top.bogey.touch_tool.bean.pin.pin_objects.pin_scale_able.PinImage;
import top.bogey.touch_tool.bean.pin.pin_objects.pin_string.PinSingleSelect;
import top.bogey.touch_tool.bean.pin.pin_objects.pin_string.PinString;
import top.bogey.touch_tool.bean.task.Task;
import top.bogey.touch_tool.service.MainAccessibilityService;
import top.bogey.touch_tool.service.TaskRunnable;
import top.bogey.touch_tool.service.YoloResult;

public class YoloDetectAction extends ExecuteAction implements SyncAction {
    private final transient Pin sourcePin = new Pin(new PinImage(), R.string.pin_image_full, false, false, true);
    private final transient Pin modelPin = new Pin(new PinSingleSelect(), R.string.yolo_detect_action_model);
    private final transient Pin similarityPin = new Pin(new PinInteger(80), R.string.yolo_detect_action_similarity);
    private final transient Pin targetsPin = new Pin(new PinList(new PinString()), R.string.yolo_detect_action_target, true);
    private final transient Pin areasPin = new Pin(new PinList(new PinArea()), true);

    public YoloDetectAction() {
        super(ActionType.YOLO_DETECT);
        addPins(sourcePin, modelPin, similarityPin, targetsPin, areasPin);
    }

    public YoloDetectAction(JsonObject jsonObject) {
        super(jsonObject);
        reAddPins(sourcePin, modelPin, similarityPin, targetsPin, areasPin);
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

        AtomicReference<List<YoloResult>> yoloResultsReference = new AtomicReference<>();
        AtomicBoolean pause = new AtomicBoolean(true);
        service.runYolo(bitmap, model.getValue(), similarity.intValue(), result -> {
            yoloResultsReference.set(result);
            pause.set(false);
            runnable.resume();
        });
        if (pause.get()) runnable.await();

        List<YoloResult> yoloResults = yoloResultsReference.get();
        if (yoloResults != null && !yoloResults.isEmpty()) {
            yoloResults.forEach(yoloResult -> {
                targetsPin.getValue(PinList.class).add(new PinString(yoloResult.getName()));
                RectF area = yoloResult.getArea();
                Rect rect = new Rect((int) area.left, (int) area.top, (int) area.right, (int) area.bottom);
                areasPin.getValue(PinList.class).add(new PinArea(rect));
            });
        }

        executeNext(runnable, outPin);
    }

    @Override
    public void sync(Task context) {
        MainAccessibilityService service = MainApplication.getInstance().getService();
        if (service == null || !service.isEnabled()) return;
        service.getYoloModelList(result -> {
            if (result.isEmpty()) return;
            modelPin.getValue(PinSingleSelect.class).setOptions(result);
            // 刷新针脚
            modelPin.setValue(context, modelPin.getValue());
        });
    }
}
