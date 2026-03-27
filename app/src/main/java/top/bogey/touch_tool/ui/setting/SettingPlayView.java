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
import top.bogey.touch_tool.bean.save.SettingSaver;
import top.bogey.touch_tool.databinding.ViewSettingPlayViewBinding;
import top.bogey.touch_tool.ui.MainActivity;
import top.bogey.touch_tool.ui.play.PlayFloatView;
import top.bogey.touch_tool.utils.DisplayUtil;

public class SettingPlayView extends Fragment {
    private ViewSettingPlayViewBinding binding;
    private String testText;
    private final SettingSaver saver = SettingSaver.getInstance();


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
                saver.setManualPlayShowType(index);
            }
        });
        binding.manualPlaySelect.checkIndex(saver.getManualPlayShowType());

        // 暂停/停止
        binding.manualPlayPauseSelect.setOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (isChecked) {
                View view = group.findViewById(checkedId);
                int index = group.indexOfChild(view);
                saver.setManualPlayPauseType(index);
            }
        });
        binding.manualPlayPauseSelect.checkIndex(saver.getManualPlayPauseType());

        // 跳转任务
        binding.manualPlayGotoTaskSwitch.setOnSwitchClickListener(v -> saver.setManualPlayGotoTask(binding.manualPlayGotoTaskSwitch.isChecked()));
        binding.manualPlayGotoTaskSwitch.setChecked(saver.isManualPlayGotoTask());

        // 隐藏悬浮窗
        binding.hideManualPlaySelect.setOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (isChecked) {
                View view = group.findViewById(checkedId);
                int index = group.indexOfChild(view);
                saver.setManualPlayHideType(index);
            }
        });
        binding.hideManualPlaySelect.checkIndex(saver.getManualPlayHideType());

        // 对截图录屏隐藏
        binding.hideManualPlaySelectWhenScreenShot.setOnSwitchClickListener(v -> saver.setManualPlayHideWhenScreenshot(binding.hideManualPlaySelectWhenScreenShot.isChecked()));
        binding.hideManualPlaySelectWhenScreenShot.setChecked(saver.isManualPlayHideWhenScreenshot());

        // 执行时隐藏悬浮窗
        binding.manualPlayingHideSwitch.setOnSwitchClickListener(v -> saver.setManualPlayingHide(binding.manualPlayingHideSwitch.isChecked()));
        binding.manualPlayingHideSwitch.setChecked(saver.isManualPlayingHide());

        // 未使用时淡化
        binding.notPlayHideSwitch.setOnSwitchClickListener(v -> saver.setNotPlayHide(binding.notPlayHideSwitch.isChecked()));
        binding.notPlayHideSwitch.setChecked(saver.isNotPlayHide());

        // 未使用时淡化透明度
        binding.notPlayHideAlpha.setValueFormat("%d%%");
        binding.notPlayHideAlpha.setSliderOnChangeListener((slider, value, fromUser) -> saver.setNotPlayHideAlpha((int) value));
        binding.notPlayHideAlpha.setValue(saver.getNotPlayHideAlpha());

        // 重置位置
        binding.manualPlayReset.setOnButtonClickListener(v -> {
            saver.setManualPlayViewPos(new Point());
            saver.setManualPlayViewState(true);
            Toast.makeText(activity, R.string.preference_setting_manual_play_reset_tips, Toast.LENGTH_SHORT).show();
        });

        // 手动执行悬浮窗偏移
        binding.manualPlayPadding.setSliderOnChangeListener((slider, value, fromUser) -> {
            saver.setManualPlayViewPadding((int) value);
            refreshExpandView();
            refreshCloseView();
        });
        binding.manualPlayPadding.setValue(saver.getManualPlayViewPadding());

        // 手动执行悬浮窗展开宽度
        binding.manualPlaySize.setSliderOnChangeListener((slider, value, fromUser) -> {
            saver.setManualPlayViewExpandSize((int) value);
            refreshExpandView();
        });
        binding.manualPlaySize.setValue(saver.getManualPlayViewExpandSize());

        // 手动执行悬浮窗关闭宽度
        binding.manualPlayCloseSize.setSliderOnChangeListener((slider, value, fromUser) -> {
            saver.setManualPlayViewCloseSize((int) value);
            refreshCloseView();
        });
        binding.manualPlayCloseSize.setValue(saver.getManualPlayViewCloseSize());

        // 手动执行悬浮窗按钮高度
        binding.manualPlayHeight.setSliderOnChangeListener((slider, value, fromUser) -> {
            saver.setManualPlayViewButtonHeight((int) value);
            refreshExpandView();
        });
        binding.manualPlayHeight.setValue(saver.getManualPlayViewButtonHeight());

        // 手动执行悬浮窗独立按钮大小
        binding.manualPlaySingleSize.setSliderOnChangeListener((slider, value, fromUser) -> {
            saver.setManualPlayViewSingleSize((int) value);
            refreshSingleView();
        });
        binding.manualPlaySingleSize.setValue(saver.getManualPlayViewSingleSize());

        refreshExpandView();
        refreshSingleView();
        refreshCloseView();

        return binding.getRoot();
    }

    private void refreshExpandView() {
        int padding = saver.getManualPlayViewPadding();
        int size = saver.getManualPlayViewExpandSize();
        int height = saver.getManualPlayViewButtonHeight();

        int px = (int) DisplayUtil.dp2px(requireContext(), PlayFloatView.UNIT_DP_SIZE * padding);
        DisplayUtil.setViewMargin(binding.playButtonBox, px, 0, px, 0);
        String testText = this.testText.substring(0, Math.min(this.testText.length(), size));
        binding.playTitle.setText(testText);

        binding.circleProgress.setVisibility(size == height ? View.VISIBLE : View.GONE);
        binding.circleProgress.setIndeterminate(true);
        binding.lineProgress.setVisibility(size == height ? View.GONE : View.VISIBLE);
        binding.lineProgress.setIndeterminate(true);

        int sizePx = (int) DisplayUtil.dp2px(requireContext(), PlayFloatView.BUTTON_DP_SIZE + PlayFloatView.UNIT_GROW_DP_SIZE * (size - 1));
        int heightPx = (int) DisplayUtil.dp2px(requireContext(), PlayFloatView.BUTTON_DP_SIZE + PlayFloatView.UNIT_GROW_DP_SIZE * (height - 1));
        binding.circleProgress.setIndicatorSize(sizePx);
        DisplayUtil.setViewWidth(binding.lineProgress, sizePx);
        DisplayUtil.setViewWidth(binding.playButton, sizePx);
        DisplayUtil.setViewHeight(binding.playButton, heightPx);
    }

    private void refreshCloseView() {
        int size = saver.getManualPlayViewCloseSize();
        int buttonDpSize = PlayFloatView.BUTTON_DP_SIZE * 2 / 3;
        int growDpSize = (PlayFloatView.BUTTON_DP_SIZE - buttonDpSize) / 2;
        int px = (int) DisplayUtil.dp2px(requireContext(), buttonDpSize + growDpSize * (size - 1));
        DisplayUtil.setViewWidth(binding.dragButton, px);
    }

    private void refreshSingleView() {
        int size = saver.getManualPlayViewSingleSize();

        String testText = this.testText.substring(0, Math.min(this.testText.length(), size));
        binding.singleTitle.setText(testText);

        int px = (int) DisplayUtil.dp2px(requireContext(), PlayFloatView.BUTTON_DP_SIZE + PlayFloatView.UNIT_GROW_DP_SIZE * (size - 1));
        DisplayUtil.setViewWidth(binding.singleButtonCard, px);
        DisplayUtil.setViewHeight(binding.singleButtonCard, px);

        binding.singleProgress.setIndicatorSize(px);
        binding.singleProgress.setIndeterminate(true);
    }
}
