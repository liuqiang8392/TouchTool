package top.bogey.touch_tool.ui.tool;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import java.util.ArrayList;
import java.util.List;

import top.bogey.touch_tool.MainApplication;
import top.bogey.touch_tool.R;
import top.bogey.touch_tool.common.StaticValues;
import top.bogey.touch_tool.databinding.ViewToolBinding;
import top.bogey.touch_tool.service.MainAccessibilityService;
import top.bogey.touch_tool.service.TaskInfoSummary;
import top.bogey.touch_tool.ui.blueprint.picker.NodePickerPreview;
import top.bogey.touch_tool.ui.tool.app_info.AppInfoFloatView;
import top.bogey.touch_tool.ui.tool.log.LogFloatView;

public class ToolView extends Fragment {
    public static final String TOOL_CAPTURE_SERVICE = "capture_service";
    public static final String TOOL_PACKAGE_ACTIVITY = "package_activity";
    public static final String TOOL_NODE_PICKER = "node_picker";
    public static final String TOOL_RUNNING_LOG = "running_log";
    public static final String TOOL_YOLO_MODEL = "yolo_model";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        ViewToolBinding binding = ViewToolBinding.inflate(inflater, container, false);

        List<ToolItem> items = new ArrayList<>();
        items.add(new ToolItem(TOOL_CAPTURE_SERVICE, R.drawable.icon_videocam, R.string.capture_service));
        items.add(new ToolItem(TOOL_PACKAGE_ACTIVITY, R.drawable.icon_apps, R.string.package_activity));
        items.add(new ToolItem(TOOL_NODE_PICKER, R.drawable.icon_widgets, R.string.node_picker));
        items.add(new ToolItem(TOOL_RUNNING_LOG, R.drawable.icon_draw, R.string.running_log));

        if (TaskInfoSummary.getInstance().getAppInfo("top.bogey.yolo") != null) {
            items.add(new ToolItem(TOOL_YOLO_MODEL, R.drawable.icon_detection_and_zone, R.string.yolo_model));
        }

        ToolViewAdapter adapter = new ToolViewAdapter(items);
        binding.toolList.setAdapter(adapter);

        return binding.getRoot();
    }

    public static void openTool(Context context, String toolName) {
        if (toolName == null || toolName.isEmpty()) return;

        switch (toolName) {
            case TOOL_CAPTURE_SERVICE -> {
                MainAccessibilityService service = MainApplication.getInstance().getService();
                if (service != null && service.isEnabled()) {
                    if (service.isCaptureEnabled()) {
                        service.stopCapture();
                    } else {
                        service.startCapture(null);
                    }
                }
            }
            case TOOL_PACKAGE_ACTIVITY -> new AppInfoFloatView(context).show();
            case TOOL_NODE_PICKER -> new NodePickerPreview(context, null, null).show();
            case TOOL_RUNNING_LOG -> new LogFloatView(context).show();
            case TOOL_YOLO_MODEL -> {
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setComponent(new ComponentName(StaticValues.YOLO_APP_PACKAGE, StaticValues.YOLO_APP_ACTIVITY));
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                try {
                    MainApplication.getInstance().startActivity(intent);
                } catch (Exception ignored) {
                }
            }
        }
    }
}
