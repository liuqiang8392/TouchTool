package top.bogey.touch_tool.bean.action.app;

import com.google.gson.JsonObject;

import java.util.Collections;

import top.bogey.touch_tool.R;
import top.bogey.touch_tool.bean.action.ActionType;
import top.bogey.touch_tool.bean.action.parent.FindExecuteAction;
import top.bogey.touch_tool.bean.pin.Pin;
import top.bogey.touch_tool.bean.pin.pin_objects.PinBoolean;
import top.bogey.touch_tool.bean.pin.pin_objects.pin_application.PinApplication;
import top.bogey.touch_tool.bean.pin.pin_objects.pin_list.PinApplications;
import top.bogey.touch_tool.service.TaskInfoSummary;
import top.bogey.touch_tool.service.TaskRunnable;

public class WaitInAppAction extends FindExecuteAction {
    private final transient Pin appsPin = new Pin(new PinApplications(), R.string.pin_app);
    private final transient Pin exactMatchPin = new Pin(new PinBoolean(false), R.string.wait_in_app_action_exact_match);

    public WaitInAppAction() {
        super(ActionType.WAIT_IN_APPLICATION);
        addPins(appsPin, exactMatchPin);
    }

    public WaitInAppAction(JsonObject jsonObject) {
        super(jsonObject);
        reAddPins(appsPin, exactMatchPin);
    }

    @Override
    public boolean find(TaskRunnable runnable) {
        TaskInfoSummary summary = TaskInfoSummary.getInstance();
        TaskInfoSummary.PackageActivity packageActivity = summary.getPackageActivity();
        String currentPackage = packageActivity.packageName();
        String currentActivity = packageActivity.activityName();

        PinBoolean exactMatch = getPinValue(runnable, exactMatchPin);
        PinApplications apps = PinApplications.convertAppList(getPinValue(runnable, appsPin));
        PinApplication currentApp = new PinApplication();
        currentApp.setPackageName(currentPackage);
        if (exactMatch.getValue()) currentApp.setActivityClasses(Collections.singletonList(currentActivity));
        return apps.contains(currentApp);
    }
}