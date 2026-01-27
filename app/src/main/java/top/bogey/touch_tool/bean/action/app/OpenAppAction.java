package top.bogey.touch_tool.bean.action.app;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import com.google.gson.JsonObject;

import java.util.Arrays;
import java.util.List;

import top.bogey.touch_tool.MainApplication;
import top.bogey.touch_tool.R;
import top.bogey.touch_tool.bean.action.ActionType;
import top.bogey.touch_tool.bean.action.parent.ExecuteAction;
import top.bogey.touch_tool.bean.pin.Pin;
import top.bogey.touch_tool.bean.pin.pin_objects.PinMap;
import top.bogey.touch_tool.bean.pin.pin_objects.PinObject;
import top.bogey.touch_tool.bean.pin.pin_objects.PinSubType;
import top.bogey.touch_tool.bean.pin.pin_objects.pin_application.PinApplication;
import top.bogey.touch_tool.bean.pin.pin_objects.pin_list.PinList;
import top.bogey.touch_tool.bean.pin.pin_objects.pin_list.PinMultiSelect;
import top.bogey.touch_tool.bean.pin.pin_objects.pin_number.PinInteger;
import top.bogey.touch_tool.bean.pin.pin_objects.pin_number.PinNumber;
import top.bogey.touch_tool.bean.pin.pin_objects.pin_string.PinString;
import top.bogey.touch_tool.service.TaskRunnable;
import top.bogey.touch_tool.utils.AppUtil;

public class OpenAppAction extends ExecuteAction {
    private final static List<Integer> FLAGS = Arrays.asList(
            Intent.FLAG_ACTIVITY_NEW_TASK,
            Intent.FLAG_ACTIVITY_NO_HISTORY,
            Intent.FLAG_ACTIVITY_SINGLE_TOP,
            Intent.FLAG_ACTIVITY_MULTIPLE_TASK,
            Intent.FLAG_ACTIVITY_CLEAR_TOP,
            Intent.FLAG_ACTIVITY_FORWARD_RESULT,
            Intent.FLAG_ACTIVITY_PREVIOUS_IS_TOP,
            Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS,
            Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT,
            Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED,
            Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY,
            Intent.FLAG_ACTIVITY_NEW_DOCUMENT,
            Intent.FLAG_ACTIVITY_NO_USER_ACTION,
            Intent.FLAG_ACTIVITY_REORDER_TO_FRONT,
            Intent.FLAG_ACTIVITY_NO_ANIMATION,
            Intent.FLAG_ACTIVITY_CLEAR_TASK,
            Intent.FLAG_ACTIVITY_TASK_ON_HOME,
            Intent.FLAG_ACTIVITY_RETAIN_IN_RECENTS,
            Intent.FLAG_ACTIVITY_LAUNCH_ADJACENT
    );
    private static final List<String> CATEGORIES = Arrays.asList(
            Intent.CATEGORY_DEFAULT,
            Intent.CATEGORY_BROWSABLE,
            Intent.CATEGORY_VOICE,
            Intent.CATEGORY_ALTERNATIVE,
            Intent.CATEGORY_SELECTED_ALTERNATIVE,
            Intent.CATEGORY_TAB,
            Intent.CATEGORY_LAUNCHER,
            Intent.CATEGORY_LEANBACK_LAUNCHER,
            Intent.CATEGORY_INFO,
            Intent.CATEGORY_HOME,
            Intent.CATEGORY_PREFERENCE,
            Intent.CATEGORY_DEVELOPMENT_PREFERENCE,
            Intent.CATEGORY_EMBED,
            Intent.CATEGORY_APP_MARKET,
            Intent.CATEGORY_MONKEY,
            Intent.CATEGORY_TEST,
            Intent.CATEGORY_UNIT_TEST,
            Intent.CATEGORY_SAMPLE_CODE,
            Intent.CATEGORY_OPENABLE,
            Intent.CATEGORY_FRAMEWORK_INSTRUMENTATION_TEST,
            Intent.CATEGORY_CAR_DOCK,
            Intent.CATEGORY_DESK_DOCK,
            Intent.CATEGORY_LE_DESK_DOCK,
            Intent.CATEGORY_HE_DESK_DOCK,
            Intent.CATEGORY_CAR_MODE,

            // App categories
            Intent.CATEGORY_APP_BROWSER,
            Intent.CATEGORY_APP_CALCULATOR,
            Intent.CATEGORY_APP_CALENDAR,
            Intent.CATEGORY_APP_CONTACTS,
            Intent.CATEGORY_APP_EMAIL,
            Intent.CATEGORY_APP_GALLERY,
            Intent.CATEGORY_APP_MAPS,
            Intent.CATEGORY_APP_MESSAGING,
            Intent.CATEGORY_APP_MUSIC
    );

    private final transient Pin appPin = new Pin(new PinApplication(PinSubType.SINGLE_APP_WITH_ACTIVITY), R.string.pin_app);

    private final transient Pin actionPin = new Pin(new PinString(), R.string.open_app_action_action, false, false, true);
    private final transient Pin flagPin = new Pin(new PinMultiSelect(new PinInteger()), R.string.open_app_action_flags, false, false, true);
    private final transient Pin categoryPin = new Pin(new PinMultiSelect(new PinString()), R.string.open_app_action_category, false, false, true);
    private final transient Pin uriPin = new Pin(new PinString(), R.string.open_app_action_uri_data, false, false, true);
    private final transient Pin paramsPin = new Pin(new PinMap(new PinString(), new PinString()), R.string.open_app_action_params, false, false, true);
    private final transient Pin bundlePin = new Pin(new PinMap(new PinString(), new PinString()), R.string.open_app_action_bundle, false, false, true);

    public OpenAppAction() {
        super(ActionType.OPEN_APP);
        addPins(appPin, actionPin, flagPin, categoryPin, uriPin, paramsPin, bundlePin);
        initFlagSelection();
        initCategorySelection();
    }

    public OpenAppAction(JsonObject jsonObject) {
        super(jsonObject);
        reAddPins(appPin, actionPin, flagPin, categoryPin, uriPin, paramsPin, bundlePin);
        initFlagSelection();
        initCategorySelection();
    }

    @Override
    public void execute(TaskRunnable runnable, Pin pin) {
        PinApplication app = getPinValue(runnable, appPin);
        String packageName = app.getPackageName();
        String activityClass = app.getFirstActivity();

        PinString action = getPinValue(runnable, actionPin);
        String actionString = action.getValue();

        PinList flags = getPinValue(runnable, flagPin);
        int flag = 0;
        for (PinObject object : flags) {
            PinNumber<?> number = (PinNumber<?>) object;
            flag |= number.intValue();
        }
        if (flag == 0) flag = Intent.FLAG_ACTIVITY_NEW_TASK;

        PinList category = getPinValue(runnable, categoryPin);

        PinMap map = getPinValue(runnable, paramsPin);
        Bundle params = new Bundle();
        map.forEach((key, value) -> putBundle(params, key.toString(), value.toString()));

        PinMap bundleMap = getPinValue(runnable, bundlePin);
        Bundle bundle = new Bundle();
        bundleMap.forEach((key, value) -> putBundle(bundle, key.toString(), value.toString()));

        PinString uri = getPinValue(runnable, uriPin);

        Context context = MainApplication.getInstance().getService();
        Intent intent;
        if (activityClass == null) {
            if (actionString == null || actionString.isEmpty()) {
                intent = context.getPackageManager().getLaunchIntentForPackage(packageName);
                if (intent != null) intent.addCategory(Intent.CATEGORY_LAUNCHER);
            } else {
                intent = new Intent(actionString);
                intent.setPackage(packageName);
            }
        } else {
            intent = new Intent(Intent.ACTION_MAIN);
            intent.setClassName(packageName, activityClass);
        }

        if (intent != null) {
            intent.setFlags(flag);
            for (PinObject object : category) {
                PinString pinString = (PinString) object;
                intent.addCategory(pinString.getValue());
            }
            if (uri.getValue() != null && !uri.getValue().isEmpty()) {
                intent.setData(Uri.parse(uri.getValue()));
            }
            intent.putExtras(params);

            AppUtil.startActivity(context, intent, bundle);
        }

        executeNext(runnable, outPin);
    }

    private void initFlagSelection() {
        PinMultiSelect multiSelect = flagPin.getValue(PinMultiSelect.class);
        multiSelect.resetSelectObjects();
        Context context = MainApplication.getInstance();
        String[] names = context.getResources().getStringArray(R.array.activity_flag);
        String[] desc = context.getResources().getStringArray(R.array.activity_flag_desc);
        for (int i = 0; i < FLAGS.size(); i++) {
            Integer value = FLAGS.get(i);
            PinMultiSelect.MultiSelectObject object = new PinMultiSelect.MultiSelectObject(names[i], desc[i], value);
            multiSelect.addSelectObject(object);
        }
    }

    private void initCategorySelection() {
        PinMultiSelect multiSelect = categoryPin.getValue(PinMultiSelect.class);
        multiSelect.resetSelectObjects();
        Context context = MainApplication.getInstance();

        String[] names = context.getResources().getStringArray(R.array.intent_category);
        String[] desc = context.getResources().getStringArray(R.array.intent_category_desc);
        for (int i = 0, categoriesSize = CATEGORIES.size(); i < categoriesSize; i++) {
            String value = CATEGORIES.get(i);
            PinMultiSelect.MultiSelectObject object = new PinMultiSelect.MultiSelectObject(names[i], desc[i], value);
            multiSelect.addSelectObject(object);
        }
    }

    private void putBundle(Bundle bundle, String key, String value) {
        if (value == null || value.isEmpty()) return;
        try {
            int i = Integer.parseInt(value);
            bundle.putInt(key, i);
            return;
        } catch (NumberFormatException ignored) {
        }

        try {
            double d = Double.parseDouble(value);
            bundle.putDouble(key, d);
            return;
        } catch (NumberFormatException ignored) {
        }

        if (value.equalsIgnoreCase("true") || value.equalsIgnoreCase("false")) {
            bundle.putBoolean(key, Boolean.parseBoolean(value));
            return;
        }
        bundle.putString(key, value);
    }
}
