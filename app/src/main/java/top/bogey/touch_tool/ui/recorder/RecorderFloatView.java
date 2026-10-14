package top.bogey.touch_tool.ui.recorder;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.view.LayoutInflater;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;

import top.bogey.touch_tool.MainApplication;
import top.bogey.touch_tool.R;
import top.bogey.touch_tool.bean.action.Action;
import top.bogey.touch_tool.bean.action.image.TouchImageAction;
import top.bogey.touch_tool.bean.action.node.FindNodeAction;
import top.bogey.touch_tool.bean.action.node.NodeTouchAction;
import top.bogey.touch_tool.bean.action.normal.DelayAction;
import top.bogey.touch_tool.bean.action.normal.LoggerAction;
import top.bogey.touch_tool.bean.action.parent.ExecuteAction;
import top.bogey.touch_tool.bean.action.point.TouchAction;
import top.bogey.touch_tool.bean.action.start.InnerStartAction;
import top.bogey.touch_tool.bean.action.system.SystemKeyAction;
import top.bogey.touch_tool.bean.action.task.CustomEndAction;
import top.bogey.touch_tool.bean.action.task.CustomStartAction;
import top.bogey.touch_tool.bean.other.NodeInfo;
import top.bogey.touch_tool.bean.pin.Pin;
import top.bogey.touch_tool.bean.pin.pin_objects.PinValueArea;
import top.bogey.touch_tool.bean.pin.pin_objects.pin_scale_able.PinImage;
import top.bogey.touch_tool.bean.pin.pin_objects.pin_scale_able.PinTouchPath;
import top.bogey.touch_tool.bean.pin.pin_objects.pin_string.PinNodePathString;
import top.bogey.touch_tool.bean.pin.pin_objects.pin_string.PinString;
import top.bogey.touch_tool.bean.task.Task;
import top.bogey.touch_tool.databinding.FloatRecorderBinding;
import top.bogey.touch_tool.service.ITaskListener;
import top.bogey.touch_tool.service.MainAccessibilityService;
import top.bogey.touch_tool.service.TaskRunnable;
import top.bogey.touch_tool.ui.blueprint.picker.FloatBaseCallback;
import top.bogey.touch_tool.ui.blueprint.picker.ImagePicker;
import top.bogey.touch_tool.ui.blueprint.picker.NodePicker;
import top.bogey.touch_tool.ui.blueprint.picker.TextPickerPreview;
import top.bogey.touch_tool.ui.custom.float_view.ToastFloatView;
import top.bogey.touch_tool.utils.callback.ResultCallback;
import top.bogey.touch_tool.utils.float_window_manager.FloatInterface;
import top.bogey.touch_tool.utils.float_window_manager.FloatWindow;

@SuppressLint("ViewConstructor")
public class RecorderFloatView extends FrameLayout implements FloatInterface {
    private final static int ACTION_OFFSET = 30;

    private final FloatRecorderBinding binding;
    private final TouchRecorderFloatView touchRecorderFloatView;

    private final List<RecorderStep> steps = new ArrayList<>();
    private final List<RecorderStep> history = new ArrayList<>();

    private long delayStartTime, pauseStartTime;
    private boolean recording = false;

    public RecorderFloatView(@NonNull Context context, ResultCallback<Task> callback) {
        super(context);
        binding = FloatRecorderBinding.inflate(LayoutInflater.from(context), this, true);
        touchRecorderFloatView = new TouchRecorderFloatView(context, result -> {
            if (isRecording()) {
                RecorderStep step = new RecorderStep();
                step.addTouchAction(getDelay(), result);
                addStep(step);
            }
        });
        touchRecorderFloatView.show();

        binding.closeButton.setOnClickListener(v -> dismiss());

        binding.saveButton.setOnClickListener(v -> {
            if (steps.isEmpty()) callback.onResult(null);
            else {
                Task task = new Task();
                CustomStartAction startAction = new CustomStartAction();
                task.addAction(startAction);

                int index = 1;
                RecorderStep lastStep = null;
                for (RecorderStep step : steps) {
                    // 首个步骤需要移除延迟
                    if (index == 1) step.detachDelayAction();

                    if (lastStep != null) linkPin(lastStep.getOutPin(), step.getInPin());
                    else linkPin(startAction.getOutPin(), step.getInPin());
                    lastStep = step;

                    for (Action action : step.getActions()) {
                        task.addAction(action);
                        action.setPos(0, index * ACTION_OFFSET);
                        index++;
                    }
                }

                CustomEndAction endAction = new CustomEndAction();
                task.addAction(endAction);
                endAction.setPos(0, index * ACTION_OFFSET);
                linkPin(steps.get(steps.size() - 1).getOutPin(), endAction.getInPin());

                callback.onResult(task);
            }
            dismiss();
        });

        binding.recodeButton.setOnClickListener(v -> {
            if (recording) {
                pauseStartTime = System.currentTimeMillis();
            } else {
                if (pauseStartTime > 0) {
                    delayStartTime = System.currentTimeMillis() - pauseStartTime + delayStartTime;
                } else {
                    delayStartTime = System.currentTimeMillis();
                }
            }
            recording = !recording;
            binding.recodeButton.setChecked(recording);
            showTouchRecorder(recording);
        });

        binding.backButton.setOnClickListener(v -> addHistory());
        binding.nextButton.setOnClickListener(v -> removeHistory());
        binding.countText.setText("0");

        binding.widgetButton.setOnClickListener(v -> {
            if (isRecording()) {
                pauseStartTime = System.currentTimeMillis();
                new NodePicker(getContext(), result -> {
                    delayStartTime = System.currentTimeMillis() - pauseStartTime + delayStartTime;
                    RecorderStep step = new RecorderStep();
                    step.addNodeAction(getDelay(), result);
                    addStep(step);
                }, null).show();
            }
        });

        binding.textButton.setOnClickListener(v -> {
            if (isRecording()) {
                pauseStartTime = System.currentTimeMillis();
                new TextPickerPreview(getContext(), result -> {
                    delayStartTime = System.currentTimeMillis() - pauseStartTime + delayStartTime;
                    RecorderStep step = new RecorderStep();
                    step.addTextAction(getDelay(), result);
                    addStep(step);
                }, "").show();
            }
        });

        binding.imageButton.setOnClickListener(v -> {
            if (isRecording()) {
                pauseStartTime = System.currentTimeMillis();
                new ImagePicker(getContext(), result -> {
                    delayStartTime = System.currentTimeMillis() - pauseStartTime + delayStartTime;
                    RecorderStep step = new RecorderStep();
                    step.addImageAction(getDelay(), result);
                    addStep(step);
                }, null).show();
            }
        });

        binding.logButton.setOnClickListener(v -> {
            if (isRecording()) {
                pauseStartTime = System.currentTimeMillis();
                new TextPickerPreview(getContext(), result -> {
                    delayStartTime = System.currentTimeMillis() - pauseStartTime + delayStartTime;
                    RecorderStep step = new RecorderStep();
                    step.addLogAction(result);
                    addStep(step);
                }, "").show();
            }
        });

        binding.backKeyButton.setOnClickListener(v -> {
            if (isRecording()) {
                RecorderStep step = new RecorderStep();
                step.addBackAction(getDelay());
                addStep(step);
            }
        });
    }

    private void addStep(RecorderStep step) {
        steps.add(step);
        binding.backButton.setVisibility(VISIBLE);
        binding.countText.setText(String.valueOf(steps.size()));

        history.clear();
        binding.nextButton.setVisibility(GONE);
        runStep(step);
    }

    private void runStep(RecorderStep step) {
        MainAccessibilityService service = MainApplication.getInstance().getService();
        if (service == null || !service.isEnabled()) return;

        Task task = new Task();
        InnerStartAction innerStartAction = null;
        for (Action action : step.getActions()) {
            if (action instanceof LoggerAction) continue;
            if (action instanceof ExecuteAction executeAction) {
                if (innerStartAction == null && !executeAction.getInPin().isLinked()) {
                    innerStartAction = new InnerStartAction(executeAction.getOutPin());
                }
            }
            task.addAction(action);
        }
        if (innerStartAction == null) {
            delayStartTime = System.currentTimeMillis();
            return;
        }

        task.addAction(innerStartAction);

        FloatWindow.hide(RecorderFloatView.class.getName());
        InnerStartAction finalAction = innerStartAction;
        postDelayed(() -> service.runTask(task, finalAction, new ITaskListener() {
            @Override
            public void onFinish(TaskRunnable runnable) {
                delayStartTime = System.currentTimeMillis();
                post(() -> FloatWindow.show(RecorderFloatView.class.getName()));
            }
        }), 100);
    }

    private void addHistory() {
        RecorderStep step = steps.remove(steps.size() - 1);
        history.add(step);

        binding.nextButton.setVisibility(VISIBLE);
        binding.backButton.setVisibility(steps.isEmpty() ? GONE : VISIBLE);
        binding.countText.setText(String.valueOf(steps.size()));
        delayStartTime = System.currentTimeMillis();
    }

    private void removeHistory() {
        RecorderStep step = history.remove(history.size() - 1);
        steps.add(step);

        binding.backButton.setVisibility(VISIBLE);
        binding.nextButton.setVisibility(history.isEmpty() ? GONE : VISIBLE);
        binding.countText.setText(String.valueOf(steps.size()));
        delayStartTime = System.currentTimeMillis();
    }

    private boolean isRecording() {
        if (recording) return true;
        ToastFloatView.showToast(getContext().getString(R.string.task_recorder_not_recording_tips));
        return false;
    }

    private int getDelay() {
        if (delayStartTime == 0) return 0;
        return (int) (System.currentTimeMillis() - delayStartTime);
    }

    public void showTouchRecorder(boolean show) {
        if (show && recording) {
            FloatWindow.show(touchRecorderFloatView.getTag());
        } else {
            FloatWindow.hide(touchRecorderFloatView.getTag());
        }
    }

    @Override
    public void show() {
        FloatWindow.with(MainApplication.getInstance().getService())
                .setLayout(this)
                .setTag(RecorderFloatView.class.getName())
                .setCallback(new RecorderFloatCallback())
                .setSpecial(true)
                .show();
    }

    @Override
    public void dismiss() {
        FloatWindow.dismiss(RecorderFloatView.class.getName());
    }

    private static void linkPin(Pin inPin, Pin outPin) {
        inPin.directAddLink(outPin);
        outPin.directAddLink(inPin);
    }

    private static class RecorderStep {
        private final List<Action> actions = new ArrayList<>();
        private Pin inPin;
        private Pin outPin;

        private void attachDelayAction(int delay) {
            if (delay <= 0) return;
            DelayAction delayAction = new DelayAction();
            delayAction.getDelay().getValue(PinValueArea.class).setMin(delay);
            delayAction.getDelay().getValue(PinValueArea.class).setMax(delay);

            linkPin(delayAction.getOutPin(), inPin);
            inPin = delayAction.getInPin();
            actions.add(0, delayAction);
        }

        public void detachDelayAction() {
            for (Action action : actions) {
                if (action instanceof DelayAction delayAction) {
                    Pin delayOutPin = delayAction.getOutPin();
                    Pin linkedPin = delayOutPin.getLinkedPin(actions);
                    if (linkedPin != null) {
                        inPin = linkedPin;
                        actions.remove(delayAction);
                        return;
                    }
                }
            }
        }

        public void addTouchAction(int delay, PinTouchPath touchPath) {
            TouchAction touchAction = new TouchAction();
            inPin = touchAction.getInPin();
            outPin = touchAction.getOutPin();
            touchAction.getTouchPin().getValue(PinTouchPath.class).sync(touchPath);

            actions.add(touchAction);
            attachDelayAction(delay);
        }

        public void addNodeAction(int delay, NodeInfo nodeInfo) {
            FindNodeAction findNodeAction = new FindNodeAction();
            inPin = findNodeAction.getInPin();
            findNodeAction.getPathPin().getValue(PinNodePathString.class).setValue(nodeInfo);

            NodeTouchAction nodeTouchAction = new NodeTouchAction();
            outPin = nodeTouchAction.getOutPin();

            linkPin(nodeTouchAction.getInPin(), findNodeAction.getOutPin());
            linkPin(findNodeAction.getNodePin(), nodeTouchAction.getNodePin());

            actions.add(findNodeAction);
            actions.add(nodeTouchAction);

            attachDelayAction(delay);
        }

        public void addTextAction(int delay, String text) {
            FindNodeAction findNodeAction = new FindNodeAction();
            inPin = findNodeAction.getInPin();
            findNodeAction.setTypeValue(1);
            findNodeAction.getTextPin().getValue(PinString.class).setValue(text);

            NodeTouchAction nodeTouchAction = new NodeTouchAction();
            outPin = nodeTouchAction.getOutPin();

            linkPin(nodeTouchAction.getInPin(), findNodeAction.getOutPin());
            linkPin(findNodeAction.getNodePin(), nodeTouchAction.getNodePin());

            actions.add(findNodeAction);
            actions.add(nodeTouchAction);

            attachDelayAction(delay);
        }

        public void addImageAction(int delay, Bitmap bitmap) {
            TouchImageAction touchImageAction = new TouchImageAction();
            inPin = touchImageAction.getInPin();
            outPin = touchImageAction.getOutPin();
            touchImageAction.getTemplatePin().getValue(PinImage.class).setImage(bitmap);

            actions.add(touchImageAction);
            attachDelayAction(delay);
        }

        public void addLogAction(String log) {
            LoggerAction loggerAction = new LoggerAction();
            inPin = loggerAction.getInPin();
            outPin = loggerAction.getOutPin();
            loggerAction.getLogPin().getValue(PinString.class).setValue(log);

            actions.add(loggerAction);
        }

        public void addBackAction(int delay) {
            SystemKeyAction systemKeyAction = new SystemKeyAction();
            inPin = systemKeyAction.getInPin();
            outPin = systemKeyAction.getOutPin();

            actions.add(systemKeyAction);
            attachDelayAction(delay);
        }

        public List<Action> getActions() {
            return actions;
        }

        public Pin getInPin() {
            return inPin;
        }

        public Pin getOutPin() {
            return outPin;
        }
    }

    private class RecorderFloatCallback extends FloatBaseCallback {
        @Override
        public void onShow(String tag) {
            super.onShow(tag);
            showTouchRecorder(true);
        }

        @Override
        public void onHide() {
            super.onHide();
            showTouchRecorder(false);
        }

        @Override
        public void onDismiss() {
            touchRecorderFloatView.dismiss();
            super.onDismiss();
        }

        @Override
        public void onDragEnd() {
            super.onDragEnd();
            delayStartTime = System.currentTimeMillis();
        }
    }
}
