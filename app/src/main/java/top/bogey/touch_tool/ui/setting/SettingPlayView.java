package top.bogey.touch_tool.ui.setting;

import android.content.res.Configuration;
import android.graphics.Point;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import top.bogey.touch_tool.R;
import top.bogey.touch_tool.bean.save.setting.SettingSaver;
import top.bogey.touch_tool.databinding.ViewSettingPlayViewBinding;
import top.bogey.touch_tool.ui.MainActivity;
import top.bogey.touch_tool.ui.play.PlayFloatView;
import top.bogey.touch_tool.utils.DisplayUtil;

public class SettingPlayView extends Fragment {
    private ViewSettingPlayViewBinding binding;
    private String testText;

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        getParentFragmentManager().beginTransaction().detach(this).commitAllowingStateLoss();
        getParentFragmentManager().beginTransaction().attach(this).commitAllowingStateLoss();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        MainActivity activity = (MainActivity) requireActivity();
        testText = getString(R.string.preference_setting_manual_play_size_test_text);

        binding = ViewSettingPlayViewBinding.inflate(inflater, container, false);
        binding.toolBar.setNavigationOnClickListener(v -> activity.getOnBackPressedDispatcher().onBackPressed());

        // 手动执行
        binding.manualPlaySelect.setOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (isChecked) {
                View view = group.findViewById(checkedId);
                int index = group.indexOfChild(view);
                SettingSaver.MANUAL_PLAY_VIEW_SHOW_TYPE.set(index);
            }
        });
        binding.manualPlaySelect.checkIndex(SettingSaver.MANUAL_PLAY_VIEW_SHOW_TYPE.get());

        // 暂停/停止
        binding.manualPlayPauseSelect.setOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (isChecked) {
                View view = group.findViewById(checkedId);
                int index = group.indexOfChild(view);
                SettingSaver.MANUAL_PLAY_VIEW_BUTTON_PAUSE_TYPE.set(index);
            }
        });
        binding.manualPlayPauseSelect.checkIndex(SettingSaver.MANUAL_PLAY_VIEW_BUTTON_PAUSE_TYPE.get());

        // 跳转任务
        binding.manualPlayGotoTaskSwitch.setOnSwitchClickListener(v -> SettingSaver.MANUAL_PLAY_VIEW_BUTTON_LONG_PRESS_JUMP.set(binding.manualPlayGotoTaskSwitch.isChecked()));
        binding.manualPlayGotoTaskSwitch.setChecked(SettingSaver.MANUAL_PLAY_VIEW_BUTTON_LONG_PRESS_JUMP.get());

        // 隐藏悬浮窗
        binding.hideManualPlaySelect.setOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (isChecked) {
                View view = group.findViewById(checkedId);
                int index = group.indexOfChild(view);
                SettingSaver.MANUAL_PLAY_VIEW_HIDE_TYPE.set(index);
            }
        });
        binding.hideManualPlaySelect.checkIndex(SettingSaver.MANUAL_PLAY_VIEW_HIDE_TYPE.get());

        // 对截图录屏隐藏
        binding.hideManualPlaySelectWhenScreenShot.setOnSwitchClickListener(v -> SettingSaver.MANUAL_PLAY_VIEW_HIDE_WHEN_SCREENSHOT.set(binding.hideManualPlaySelectWhenScreenShot.isChecked()));
        binding.hideManualPlaySelectWhenScreenShot.setChecked(SettingSaver.MANUAL_PLAY_VIEW_HIDE_WHEN_SCREENSHOT.get());

        // 执行时隐藏悬浮窗
        binding.manualPlayingHideSwitch.setOnSwitchClickListener(v -> SettingSaver.MANUAL_PLAY_VIEW_BUTTON_HIDE_WHEN_EXECUTE.set(binding.manualPlayingHideSwitch.isChecked()));
        binding.manualPlayingHideSwitch.setChecked(SettingSaver.MANUAL_PLAY_VIEW_BUTTON_HIDE_WHEN_EXECUTE.get());

        // 未使用时淡化
        binding.notPlayHideSwitch.setOnSwitchClickListener(v -> SettingSaver.MANUAL_PLAY_VIEW_NOT_USED_FADE.set(binding.notPlayHideSwitch.isChecked()));
        binding.notPlayHideSwitch.setChecked(SettingSaver.MANUAL_PLAY_VIEW_NOT_USED_FADE.get());

        // 未使用时淡化透明度
        binding.notPlayHideAlpha.setValueFormat("%d%%");
        binding.notPlayHideAlpha.setSliderOnChangeListener((slider, value, fromUser) -> SettingSaver.MANUAL_PLAY_VIEW_NOT_USED_FADE_LEVEL.set((int) value));
        binding.notPlayHideAlpha.setValue(SettingSaver.MANUAL_PLAY_VIEW_NOT_USED_FADE_LEVEL.get());

        // 重置位置
        binding.manualPlayReset.setOnButtonClickListener(v -> {
            SettingSaver.MANUAL_PLAY_VIEW_POS.set(new Point());
            SettingSaver.MANUAL_PLAY_VIEW_EXPAND_STATE.set(true);
            Toast.makeText(activity, R.string.preference_setting_manual_play_reset_tips, Toast.LENGTH_SHORT).show();
        });

        // 手动执行悬浮窗偏移
        binding.manualPlayPadding.setSliderOnChangeListener((slider, value, fromUser) -> {
            SettingSaver.MANUAL_PLAY_VIEW_PADDING.set((int) value);
            refreshExpandView();
            refreshCloseView();
        });
        binding.manualPlayPadding.setValue(SettingSaver.MANUAL_PLAY_VIEW_PADDING.get());

        // 手动执行悬浮窗展开宽度
        binding.manualPlaySize.setSliderOnChangeListener((slider, value, fromUser) -> {
            SettingSaver.MANUAL_PLAY_VIEW_BUTTON_WIDTH.set((int) value);
            refreshExpandView();
        });
        binding.manualPlaySize.setValue(SettingSaver.MANUAL_PLAY_VIEW_BUTTON_WIDTH.get());

        // 手动执行悬浮窗按钮高度
        binding.manualPlayHeight.setSliderOnChangeListener((slider, value, fromUser) -> {
            SettingSaver.MANUAL_PLAY_VIEW_BUTTON_HEIGHT.set((int) value);
            refreshExpandView();
        });
        binding.manualPlayHeight.setValue(SettingSaver.MANUAL_PLAY_VIEW_BUTTON_HEIGHT.get());

        // 手动执行悬浮窗独立按钮大小
        binding.manualPlaySingleSize.setSliderOnChangeListener((slider, value, fromUser) -> {
            SettingSaver.MANUAL_PLAY_VIEW_SINGLE_BUTTON_SIZE.set((int) value);
            refreshSingleView();
        });
        binding.manualPlaySingleSize.setValue(SettingSaver.MANUAL_PLAY_VIEW_SINGLE_BUTTON_SIZE.get());

        // 手动执行悬浮窗关闭宽度
        binding.manualPlayCloseSize.setSliderOnChangeListener((slider, value, fromUser) -> {
            SettingSaver.MANUAL_PLAY_VIEW_ZOOM_SIZE.set((int) value);
            refreshCloseView();
        });
        binding.manualPlayCloseSize.setValue(SettingSaver.MANUAL_PLAY_VIEW_ZOOM_SIZE.get());

        refreshExpandView();
        refreshSingleView();
        refreshCloseView();

        return binding.getRoot();
    }

    private void refreshExpandView() {
        int padding = SettingSaver.MANUAL_PLAY_VIEW_PADDING.get();
        int width = SettingSaver.MANUAL_PLAY_VIEW_BUTTON_WIDTH.get();
        int height = SettingSaver.MANUAL_PLAY_VIEW_BUTTON_HEIGHT.get();

        int px = (int) DisplayUtil.dp2px(requireContext(), PlayFloatView.UNIT_DP_SIZE * padding);
        DisplayUtil.setViewMargin(binding.playButtonBox, px, 0, px, 0);
        String testText = this.testText.substring(0, Math.min(this.testText.length(), width));
        binding.playTitle.setText(testText);

        binding.circleProgress.setVisibility(width == height ? View.VISIBLE : View.GONE);
        binding.circleProgress.setIndeterminate(true);
        binding.lineProgress.setVisibility(width == height ? View.GONE : View.VISIBLE);
        binding.lineProgress.setIndeterminate(true);

        int sizePx = (int) DisplayUtil.dp2px(requireContext(), PlayFloatView.BUTTON_DP_SIZE + PlayFloatView.UNIT_GROW_DP_SIZE * (width - 1));
        int heightPx = (int) DisplayUtil.dp2px(requireContext(), PlayFloatView.BUTTON_DP_SIZE + PlayFloatView.UNIT_GROW_DP_SIZE * (height - 1));
        binding.circleProgress.setIndicatorSize(sizePx);
        DisplayUtil.setViewWidth(binding.lineProgress, sizePx);
        DisplayUtil.setViewWidth(binding.playButton, sizePx);
        DisplayUtil.setViewHeight(binding.playButton, heightPx);
    }

    private void refreshCloseView() {
        int size = SettingSaver.MANUAL_PLAY_VIEW_ZOOM_SIZE.get();
        int buttonDpSize = PlayFloatView.BUTTON_DP_SIZE * 2 / 3;
        int growDpSize = (PlayFloatView.BUTTON_DP_SIZE - buttonDpSize) / 2;
        int px = (int) DisplayUtil.dp2px(requireContext(), buttonDpSize + growDpSize * (size - 1));
        DisplayUtil.setViewWidth(binding.dragButton, px);
    }

    private void refreshSingleView() {
        int size = SettingSaver.MANUAL_PLAY_VIEW_SINGLE_BUTTON_SIZE.get();

        String testText = this.testText.substring(0, Math.min(this.testText.length(), size));
        binding.singleTitle.setText(testText);

        int px = (int) DisplayUtil.dp2px(requireContext(), PlayFloatView.BUTTON_DP_SIZE + PlayFloatView.UNIT_GROW_DP_SIZE * (size - 1));
        DisplayUtil.setViewWidth(binding.singleButtonCard, px);
        DisplayUtil.setViewHeight(binding.singleButtonCard, px);

        binding.singleProgress.setIndicatorSize(px);
        binding.singleProgress.setIndeterminate(true);
    }
}
