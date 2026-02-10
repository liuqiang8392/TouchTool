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

    public WaitInAppAction() {
        super(ActionType.WAIT_IN_APPLICATION);
        addPin(appsPin);
    }

    public WaitInAppAction(JsonObject jsonObject) {
        super(jsonObject);
        reAddPin(appsPin);
    }

    @Override
    public boolean find(TaskRunnable runnable) {
        TaskInfoSummary summary = TaskInfoSummary.getInstance();
        TaskInfoSummary.PackageActivity packageActivity = summary.getPackageActivity();
        String currentPackage = packageActivity.packageName();
        String currentActivity = packageActivity.activityName();

        PinApplications apps = PinApplications.convertAppList(getPinValue(runnable, appsPin));
        PinApplication currentApp = new PinApplication();
        currentApp.setPackageName(currentPackage);
        currentApp.setActivityClasses(Collections.singletonList(currentActivity));
        return apps.contains(currentApp);
    }
}