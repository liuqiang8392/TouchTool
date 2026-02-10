package top.bogey.touch_tool.ui;

import android.content.Intent;
import android.graphics.BitmapFactory;
import android.net.Uri;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import top.bogey.touch_tool.MainApplication;
import top.bogey.touch_tool.bean.action.Action;
import top.bogey.touch_tool.bean.action.start.InnerStartAction;
import top.bogey.touch_tool.bean.action.start.StartAction;
import top.bogey.touch_tool.bean.action.start.TimeStartAction;
import top.bogey.touch_tool.bean.pin.Pin;
import top.bogey.touch_tool.bean.pin.pin_objects.PinBase;
import top.bogey.touch_tool.bean.pin.pin_objects.PinObject;
import top.bogey.touch_tool.bean.save.task.TaskSaver;
import top.bogey.touch_tool.bean.task.Task;
import top.bogey.touch_tool.bean.task.Variable;
import top.bogey.touch_tool.service.MainAccessibilityService;
import top.bogey.touch_tool.service.TaskInfoSummary;
import top.bogey.touch_tool.ui.tool.ToolView;
import top.bogey.touch_tool.utils.AppUtil;

public class InstantActivity extends BaseActivity {
    public static final String INTENT_KEY_DO_ACTION = "INTENT_KEY_DO_ACTION";
    public static final String TASK_ID = "TASK_ID";
    public static final String ACTION_ID = "ACTION_ID";
    public static final String PIN_ID = "PIN_ID";

    @Override
    protected void onStart() {
        super.onStart();

        Intent intent = getIntent();
        handleIntent(intent);
        finish();
    }

    private void handleIntent(Intent intent) {
        if (intent == null) return;
        setIntent(null);

        String action = intent.getAction();

        if (Intent.ACTION_VIEW.equals(action)) {
            Uri uri = intent.getData();
            if (uri != null) {
                String scheme = uri.getScheme();
                if ("tt".equals(scheme)) {
                    if ("do_action".equals(uri.getHost()) && uri.getQuery() != null) {
                        HashMap<String, String> params = new HashMap<>();
                        for (String name : uri.getQueryParameterNames()) {
                            params.put(name, uri.getQueryParameter(name));
                        }
                        String taskId = params.remove(TASK_ID);
                        String actionId = params.remove(ACTION_ID);
                        doAction(taskId, actionId, null, params);
                    }

                    if ("open_tool".equals(uri.getHost())) {
                        ToolView.openTool(this, uri.getQuery());
                    }
                }
            }
        } else if (Intent.ACTION_SEND.equals(action)) {
            Object object = null;
            Uri uri = intent.getParcelableExtra(Intent.EXTRA_STREAM);
            if (uri != null) {
                String type = getContentResolver().getType(uri);
                if (type != null) {
                    if (type.startsWith("image")) {
                        try (InputStream inputStream = getContentResolver().openInputStream(uri)) {
                            object = BitmapFactory.decodeStream(inputStream);
                        } catch (IOException ignored) {
                        }
                    } else if (type.startsWith("text")) {
                        object = new String(AppUtil.readFile(this, uri));
                    }

                }
            } else {
                object = intent.getStringExtra(Intent.EXTRA_TEXT);
            }
            if (object != null) {
                PinBase pinBase = PinBase.parseValue(object);
                if (pinBase instanceof PinObject pinObject) {
                    TaskInfoSummary.getInstance().tryStartShareTask(pinObject);
                }
            }
        } else if (INTENT_KEY_DO_ACTION.equals(action)) {
            String taskId = intent.getStringExtra(TASK_ID);
            String actionId = intent.getStringExtra(ACTION_ID);
            String pinId = intent.getStringExtra(PIN_ID);
            doAction(taskId, actionId, pinId, null);
        }
    }

    public static void doAction(String taskId, String actionId, String pinId, Map<String, String> params) {
        if (taskId == null || actionId == null) return;

        Task task = TaskSaver.getInstance().getTask(taskId);
        if (task == null) return;
        Task copy = task.copy();

        Action action = copy.getAction(actionId);
        if (action == null || (action instanceof StartAction startAction && !startAction.isEnable())) return;

        MainAccessibilityService service = MainApplication.getInstance().getService();
        if (service == null || !service.isEnabled()) return;

        if (action instanceof TimeStartAction timeStartAction) {
            service.addAlarm(copy, timeStartAction);
        }

        if (action instanceof StartAction startAction) {
            if (params != null) {
                params.forEach((key, value) -> {
                    Variable var = copy.findVariableByName(key);
                    if (var != null) var.getSaveValue().cast(value);

                    Pin pin = startAction.getPinByTitle(key);
                    if (pin != null && pin.getValue() instanceof PinObject pinObject) {
                        pinObject.cast(value);
                    }
                });
            }
            service.runTask(copy, startAction);
        } else {
            if (pinId == null) return;
            Pin pin = action.getPinById(pinId);
            if (pin == null) return;

            InnerStartAction innerStartAction = new InnerStartAction(pin);
            service.runTask(task, innerStartAction);
        }
    }
}
