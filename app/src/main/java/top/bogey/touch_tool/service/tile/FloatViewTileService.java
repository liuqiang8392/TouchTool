package top.bogey.touch_tool.service.tile;

import android.content.Context;
import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;
import android.view.View;

import top.bogey.touch_tool.MainApplication;
import top.bogey.touch_tool.service.MainAccessibilityService;
import top.bogey.touch_tool.ui.custom.float_view.KeepAliveFloatView;
import top.bogey.touch_tool.utils.float_window_manager.FloatWindow;

public abstract class FloatViewTileService extends TileService {

    protected abstract String getFloatViewName();

    protected abstract void showFloatView(Context context);

    @Override
    public void onClick() {
        super.onClick();
        KeepAliveFloatView keepView = (KeepAliveFloatView) FloatWindow.getView(KeepAliveFloatView.class.getName());
        if (keepView != null) {
            String name = getFloatViewName();
            View view = FloatWindow.getView(name);
            if (view == null) {
                showFloatView(keepView.getThemeContext());
            } else {
                FloatWindow.dismiss(name);
            }
        }
        updateTile();
    }

    @Override
    public void onStartListening() {
        super.onStartListening();
        updateTile();
    }

    private void updateTile() {
        Tile tile = getQsTile();
        MainAccessibilityService service = MainApplication.getInstance().getService();
        if (service != null && service.isEnabled()) {
            View view = FloatWindow.getView(getFloatViewName());
            tile.setState(view != null ? Tile.STATE_ACTIVE : Tile.STATE_INACTIVE);
        } else {
            tile.setState(Tile.STATE_UNAVAILABLE);
        }
        tile.updateTile();
    }
}
