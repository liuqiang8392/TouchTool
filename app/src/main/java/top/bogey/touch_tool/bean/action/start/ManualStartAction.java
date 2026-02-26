package top.bogey.touch_tool.bean.action.start;

import android.graphics.Point;

import com.google.gson.JsonObject;

import java.util.Collections;

import top.bogey.touch_tool.MainApplication;
import top.bogey.touch_tool.R;
import top.bogey.touch_tool.bean.action.ActionType;
import top.bogey.touch_tool.bean.pin.Pin;
import top.bogey.touch_tool.bean.pin.pin_objects.PinBase;
import top.bogey.touch_tool.bean.pin.pin_objects.PinBoolean;
import top.bogey.touch_tool.bean.pin.pin_objects.PinSubType;
import top.bogey.touch_tool.bean.pin.pin_objects.pin_application.PinApplication;
import top.bogey.touch_tool.bean.pin.pin_objects.pin_list.PinApplications;
import top.bogey.touch_tool.bean.pin.pin_objects.pin_scale_able.PinPoint;
import top.bogey.touch_tool.bean.pin.pin_objects.pin_string.PinSingleSelect;
import top.bogey.touch_tool.bean.pin.special_pin.NotLinkAblePin;
import top.bogey.touch_tool.bean.pin.special_pin.ShowAblePin;
import top.bogey.touch_tool.bean.task.Task;
import top.bogey.touch_tool.service.TaskInfoSummary;
import top.bogey.touch_tool.service.TaskRunnable;
import top.bogey.touch_tool.utils.DisplayUtil;
import top.bogey.touch_tool.utils.EAnchor;

public class ManualStartAction extends StartAction {
    private final transient Pin appsPin = new NotLinkAblePin(new PinApplications(PinSubType.MULTI_APP_WITH_ACTIVITY, MainApplication.getInstance().getString(R.string.common_package)), R.string.pin_app);
    private final transient Pin showTypePin = new NotLinkAblePin(new PinSingleSelect(R.array.manual_action_show_type), R.string.manual_start_action_type, false, false, true);
    private final transient Pin expandPin = new NormalShowablePin(new PinBoolean(), R.string.manual_start_action_expand, false, false, true);

    private final transient Pin anchorPin = new SingleShowablePin(new PinSingleSelect(R.array.anchor, 4), R.string.window_anchor, false, false, true);
    private final transient Pin gravityPin = new SingleShowablePin(new PinSingleSelect(R.array.anchor, 0), R.string.screen_anchor, false, false, true);
    private final transient Pin showPosPin = new SingleShowablePin(new PinPoint(), R.string.screen_anchor_pos, false, false, true);

    private final transient Pin lockPin = new SingleShowablePin(new PinBoolean(), R.string.manual_start_action_lock, false, false, true);
    private final transient Pin appPin = new Pin(new PinApplication(), R.string.manual_start_action_app, true);

    public ManualStartAction() {
        super(ActionType.MANUAL_START);
        Point size = DisplayUtil.getScreenSize(MainApplication.getInstance());
        showPosPin.getValue(PinPoint.class).setValue(new Point(size.x / 2, size.y / 2));
        addPins(appsPin, showTypePin, expandPin, showPosPin, anchorPin, gravityPin, lockPin, appPin);
    }

    public ManualStartAction(JsonObject jsonObject) {
        super(jsonObject);
        reAddPins(appsPin, showTypePin, expandPin, showPosPin, anchorPin, gravityPin, lockPin, appPin);
    }

    @Override
    public void execute(TaskRunnable runnable, Pin pin) {
        super.execute(runnable, pin);
        TaskInfoSummary handler = TaskInfoSummary.getInstance();
        TaskInfoSummary.PackageActivity packageActivity = handler.getPackageActivity();

        PinApplication application = new PinApplication(packageActivity.packageName());
        application.setActivityClasses(Collections.singletonList(packageActivity.activityName()));
        appPin.setValue(application);

        executeNext(runnable, executePin);
    }

    @Override
    public boolean ready() {
        TaskInfoSummary handler = TaskInfoSummary.getInstance();
        TaskInfoSummary.PackageActivity packageActivity = handler.getPackageActivity();

        PinApplication application = new PinApplication(packageActivity.packageName());
        application.setActivityClasses(Collections.singletonList(packageActivity.activityName()));
        return appsPin.getValue(PinApplications.class).contains(application);
    }

    public boolean isSingleShow() {
        return showTypePin.getValue(PinSingleSelect.class).getIndex() == 1;
    }

    public boolean isExpand() {
        return expandPin.getValue(PinBoolean.class).getValue();
    }

    public EAnchor getAnchor() {
        return EAnchor.values()[anchorPin.getValue(PinSingleSelect.class).getIndex()];
    }

    public EAnchor getGravity() {
        return EAnchor.values()[gravityPin.getValue(PinSingleSelect.class).getIndex()];
    }

    public Point getShowPos() {
        return showPosPin.getValue(PinPoint.class).getValue();
    }

    public boolean isLock() {
        return lockPin.getValue(PinBoolean.class).getValue();
    }


    private static class NormalShowablePin extends ShowAblePin {

        public NormalShowablePin(PinBase value, int titleId, boolean out, boolean dynamic, boolean hide) {
            super(value, titleId, out, dynamic, hide);
        }

        @Override
        public boolean showAble(Task context) {
            ManualStartAction action = (ManualStartAction) context.getAction(getOwnerId());
            return !action.isSingleShow();
        }

        @Override
        public boolean linkAble(Task context) {
            return super.linkAble(context);
        }
    }

    private static class SingleShowablePin extends ShowAblePin {

        public SingleShowablePin(PinBase value, int titleId, boolean out, boolean dynamic, boolean hide) {
            super(value, titleId, out, dynamic, hide);
        }

        @Override
        public boolean showAble(Task context) {
            ManualStartAction action = (ManualStartAction) context.getAction(getOwnerId());
            return action.isSingleShow();
        }

        @Override
        public boolean linkAble(Task context) {
            return super.linkAble(context);
        }
    }
}
