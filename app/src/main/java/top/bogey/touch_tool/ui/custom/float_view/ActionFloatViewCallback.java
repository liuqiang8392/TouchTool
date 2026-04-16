package top.bogey.touch_tool.ui.custom.float_view;

import android.graphics.Point;

import top.bogey.touch_tool.bean.save.setting.SettingSaver;
import top.bogey.touch_tool.ui.blueprint.picker.FloatBaseCallback;
import top.bogey.touch_tool.utils.float_window_manager.FloatWindow;
import top.bogey.touch_tool.utils.float_window_manager.FloatWindowHelper;

public class ActionFloatViewCallback extends FloatBaseCallback {
    private final String tag;

    public ActionFloatViewCallback(String tag) {
        this.tag = tag;
    }

    @Override
    public void onShow(String tag) {

    }

    @Override
    public void onDragEnd() {
        super.onDragEnd();
        FloatWindowHelper helper = FloatWindow.getHelper(tag);
        if (helper != null) {
            Point point = helper.getRelativePoint();
            SettingSaver.FLOAT_VIEW_POS.set(point);
        }
    }

    @Override
    public void onDismiss() {

    }
}
