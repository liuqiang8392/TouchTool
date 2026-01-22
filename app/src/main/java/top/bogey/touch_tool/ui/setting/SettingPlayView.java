package top.bogey.touch_tool.ui.setting;

import static top.bogey.touch_tool.ui.play.PlayFloatView.BUTTON_DP_SIZE;
import static top.bogey.touch_tool.ui.play.PlayFloatView.UNIT_DP_SIZE;
import static top.bogey.touch_tool.ui.play.PlayFloatView.UNIT_GROW_DP_SIZE;

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
import top.bogey.touch_tool.bean.save.SettingSaver;
import top.bogey.touch_tool.databinding.ViewSettingPlayViewBinding;
import top.bogey.touch_tool.ui.MainActivity;
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
                SettingSaver.getInstance().setManualPlayShowType(index);
            }
        });
        binding.manualPlaySelect.checkIndex(SettingSaver.getInstance().getManualPlayShowType());

        // 暂停/停止
        binding.manualPlayPauseSelect.setOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (isChecked) {
                View view = group.findViewById(checkedId);
                int index = group.indexOfChild(view);
                SettingSaver.getInstance().setManualPlayPauseType(index);
            }
        });
        binding.manualPlayPauseSelect.checkIndex(SettingSaver.getInstance().getManualPlayPauseType());

        // 隐藏悬浮窗
        binding.hideManualPlaySelect.setOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (isChecked) {
                View view = group.findViewById(checkedId);
                int index = group.indexOfChild(view);
                SettingSaver.getInstance().setManualPlayHideType(index);
            }
        });
        binding.hideManualPlaySelect.checkIndex(SettingSaver.getInstance().getManualPlayHideType());

        // 对截图录屏隐藏
        binding.hideManualPlaySelectWhenScreenShot.setOnSwitchClickListener(v -> SettingSaver.getInstance().setManualPlayHideWhenScreenshot(binding.hideManualPlaySelectWhenScreenShot.isChecked()));
        binding.hideManualPlaySelectWhenScreenShot.setChecked(SettingSaver.getInstance().isManualPlayHideWhenScreenshot());

        // 执行时隐藏悬浮窗
        binding.manualPlayingHideSwitch.setOnSwitchClickListener(v -> SettingSaver.getInstance().setManualPlayingHide(binding.manualPlayingHideSwitch.isChecked()));
        binding.manualPlayingHideSwitch.setChecked(SettingSaver.getInstance().isManualPlayingHide());

        // 未使用时淡化
        binding.notPlayHideSwitch.setOnSwitchClickListener(v -> SettingSaver.getInstance().setNotPlayHide(binding.notPlayHideSwitch.isChecked()));
        binding.notPlayHideSwitch.setChecked(SettingSaver.getInstance().isNotPlayHide());

        // 未使用时淡化透明度
        binding.notPlayHideAlpha.setValueFormat("%d%%");
        binding.notPlayHideAlpha.setSliderOnChangeListener((slider, value, fromUser) -> SettingSaver.getInstance().setNotPlayHideAlpha((int) value));
        binding.notPlayHideAlpha.setValue(SettingSaver.getInstance().getNotPlayHideAlpha());

        // 重置位置
        binding.manualPlayReset.setOnButtonClickListener(v -> {
            SettingSaver.getInstance().setManualPlayViewPos(new Point());
            SettingSaver.getInstance().setManualPlayViewState(true);
            Toast.makeText(activity, R.string.preference_setting_manual_play_reset_tips, Toast.LENGTH_SHORT).show();
        });

        // 手动执行悬浮窗偏移
        binding.manualPlayPadding.setSliderOnChangeListener((slider, value, fromUser) -> {
            SettingSaver.getInstance().setManualPlayViewPadding((int) value);
            refreshExpandView();
            refreshCloseView();
        });
        binding.manualPlayPadding.setValue(SettingSaver.getInstance().getManualPlayViewPadding());

        // 手动执行悬浮窗展开宽度
        binding.manualPlaySize.setSliderOnChangeListener((slider, value, fromUser) -> {
            SettingSaver.getInstance().setManualPlayViewExpandSize((int) value);
            refreshExpandView();
        });
        binding.manualPlaySize.setValue(SettingSaver.getInstance().getManualPlayViewExpandSize());

        // 手动执行悬浮窗关闭宽度
        binding.manualPlayCloseSize.setSliderOnChangeListener((slider, value, fromUser) -> {
            SettingSaver.getInstance().setManualPlayViewCloseSize((int) value);
            refreshCloseView();
        });
        binding.manualPlayCloseSize.setValue(SettingSaver.getInstance().getManualPlayViewCloseSize());

        // 手动执行悬浮窗按钮高度
        binding.manualPlayHeight.setSliderOnChangeListener((slider, value, fromUser) -> {
            SettingSaver.getInstance().setManualPlayViewButtonHeight((int) value);
            refreshExpandView();
        });
        binding.manualPlayHeight.setValue(SettingSaver.getInstance().getManualPlayViewButtonHeight());

        // 手动执行悬浮窗独立按钮大小
        binding.manualPlaySingleSize.setSliderOnChangeListener((slider, value, fromUser) -> {
            SettingSaver.getInstance().setManualPlayViewSingleSize((int) value);
            refreshSingleView();
        });
        binding.manualPlaySingleSize.setValue(SettingSaver.getInstance().getManualPlayViewSingleSize());

        refreshExpandView();
        refreshSingleView();
        refreshCloseView();

        return binding.getRoot();
    }

    private void refreshExpandView() {
        int padding = SettingSaver.getInstance().getManualPlayViewPadding();
        int size = SettingSaver.getInstance().getManualPlayViewExpandSize();
        int height = SettingSaver.getInstance().getManualPlayViewButtonHeight();

        int px = (int) DisplayUtil.dp2px(requireContext(), UNIT_DP_SIZE * padding);
        DisplayUtil.setViewMargin(binding.playButtonBox, px, 0, px, 0);
        String testText = this.testText.substring(0, Math.min(this.testText.length(), size));
        binding.playTitle.setText(testText);

        binding.circleProgress.setVisibility(size == height ? View.VISIBLE : View.GONE);
        binding.circleProgress.setIndeterminate(true);
        binding.lineProgress.setVisibility(size == height ? View.GONE : View.VISIBLE);
        binding.lineProgress.setIndeterminate(true);

        int sizePx = (int) DisplayUtil.dp2px(requireContext(), BUTTON_DP_SIZE + UNIT_GROW_DP_SIZE * (size - 1));
        int heightPx = (int) DisplayUtil.dp2px(requireContext(), BUTTON_DP_SIZE + UNIT_GROW_DP_SIZE * (height - 1));
        binding.circleProgress.setIndicatorSize(sizePx);
        DisplayUtil.setViewWidth(binding.lineProgress, sizePx);
        DisplayUtil.setViewWidth(binding.playButton, sizePx);
        DisplayUtil.setViewHeight(binding.playButton, heightPx);
    }

    private void refreshCloseView() {
        int size = SettingSaver.getInstance().getManualPlayViewCloseSize();
        int buttonDpSize = BUTTON_DP_SIZE * 2 / 3;
        int growDpSize = (BUTTON_DP_SIZE - buttonDpSize) / 2;
        int px = (int) DisplayUtil.dp2px(requireContext(), buttonDpSize + growDpSize * (size - 1));
        DisplayUtil.setViewWidth(binding.dragButton, px);
    }

    private void refreshSingleView() {
        int size = SettingSaver.getInstance().getManualPlayViewSingleSize();

        String testText = this.testText.substring(0, Math.min(this.testText.length(), size));
        binding.singleTitle.setText(testText);

        int px = (int) DisplayUtil.dp2px(requireContext(), BUTTON_DP_SIZE + UNIT_GROW_DP_SIZE * (size - 1));
        DisplayUtil.setViewWidth(binding.singleButtonCard, px);
        DisplayUtil.setViewHeight(binding.singleButtonCard, px);

        binding.singleProgress.setIndicatorSize(px);
        binding.singleProgress.setIndeterminate(true);
    }
}
