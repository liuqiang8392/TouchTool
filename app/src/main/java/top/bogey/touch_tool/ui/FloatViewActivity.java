package top.bogey.touch_tool.ui;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;

import androidx.annotation.Nullable;

import top.bogey.touch_tool.MainApplication;
import top.bogey.touch_tool.service.MainAccessibilityService;
import top.bogey.touch_tool.service.TaskInfoSummary;
import top.bogey.touch_tool.ui.custom.KeepAliveFloatView;
import top.bogey.touch_tool.utils.float_window_manager.FloatWindow;

public class FloatViewActivity extends BaseActivity {
    public static final String INTENT_KEY_AUTO_START = "INTENT_KEY_AUTO_START";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        MainAccessibilityService.enabled.observe(this, enabled -> {
            if (MainApplication.getInstance().getService() == null) return;

            // 只监听启动，关闭服务时会强制移除悬浮窗
            if (enabled) {
                View view = FloatWindow.getView(KeepAliveFloatView.class.getName());
                if (view != null) return;
                new KeepAliveFloatView(getThemeContext()).show();
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        TaskInfoSummary.getInstance().resetApps();

        MainAccessibilityService service = MainApplication.getInstance().getService();
        if (service != null && service.isEnabled()) {
            View view = FloatWindow.getView(KeepAliveFloatView.class.getName());
            if (view != null) return;
            new KeepAliveFloatView(getThemeContext()).show();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        Intent intent = getIntent();
        if (intent == null) return;
        String action = intent.getAction();
        if (INTENT_KEY_AUTO_START.equals(action)) {
            new Handler(getMainLooper()).postDelayed(this::finish, 100);
        }
    }

    @Override
    protected void onNightModeChanged(int mode) {
        super.onNightModeChanged(mode);
        FloatWindow.dismiss(KeepAliveFloatView.class.getName());
    }

    protected Context getThemeContext() {
        return this;
    }

}
