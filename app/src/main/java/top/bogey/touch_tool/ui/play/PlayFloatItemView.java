package top.bogey.touch_tool.ui.play;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import top.bogey.touch_tool.MainApplication;
import top.bogey.touch_tool.R;
import top.bogey.touch_tool.bean.action.Action;
import top.bogey.touch_tool.bean.action.start.StartAction;
import top.bogey.touch_tool.bean.save.SettingSaver;
import top.bogey.touch_tool.bean.task.Task;
import top.bogey.touch_tool.databinding.FloatPlayItemBinding;
import top.bogey.touch_tool.service.ITaskListener;
import top.bogey.touch_tool.service.MainAccessibilityService;
import top.bogey.touch_tool.service.TaskRunnable;
import top.bogey.touch_tool.ui.InstantActivity;
import top.bogey.touch_tool.ui.MainActivity;
import top.bogey.touch_tool.utils.DisplayUtil;

@SuppressLint("ViewConstructor")
public class PlayFloatItemView extends FrameLayout implements ITaskListener {
    protected static final int PLAY_STATE_STOPPED = 0;
    protected static final int PLAY_STATE_RUNNING = 1;
    protected static final int PLAY_STATE_PAUSED = 2;

    protected final FloatPlayItemBinding binding;
    protected int size;
    protected int height;

    protected Task task;
    protected StartAction startAction;

    protected int playState = PLAY_STATE_STOPPED;
    protected TaskRunnable runnable;
    protected boolean remove = false;

    public PlayFloatItemView(@NonNull Context context, Task task, StartAction action) {
        super(context);
        binding = FloatPlayItemBinding.inflate(LayoutInflater.from(context), this, true);
        size = SettingSaver.getInstance().getManualPlayViewExpandSize();
        height = SettingSaver.getInstance().getManualPlayViewButtonHeight();
        int pauseType = SettingSaver.getInstance().getManualPlayPauseType();

        this.task = task;
        this.startAction = action;

        binding.title.setText(getTitle(task, action));

        binding.getRoot().setOnClickListener(v -> {
            if (playState == PLAY_STATE_RUNNING) {
                if (pauseType == 0) pause();
                else stop();
            } else {
                if (!resume()) {
                    MainAccessibilityService service = MainApplication.getInstance().getService();
                    if (service == null || !service.isEnabled()) return;
                    runnable = service.runTask(task, startAction, this);
                }
            }
        });

        binding.getRoot().setOnLongClickListener(v -> {
            if (playState == PLAY_STATE_STOPPED) {
                if (SettingSaver.getInstance().isManualPlayGotoTask()) {
                    Intent intent = new Intent(context, MainActivity.class);
                    intent.setAction(MainActivity.INTENT_KEY_OPEN_TASK);
                    intent.putExtra(InstantActivity.TASK_ID, task.getId());
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    context.startActivity(intent);
                }
            } else {
                if (pauseType == 0) stop();
                else pause();
            }
            return true;
        });

        binding.circleProgress.setVisibility(View.GONE);
        binding.lineProgress.setVisibility(View.GONE);


        int sizePx = (int) DisplayUtil.dp2px(context, PlayFloatView.BUTTON_DP_SIZE + PlayFloatView.UNIT_GROW_DP_SIZE * (size - 1));
        int heightPx = (int) DisplayUtil.dp2px(context, PlayFloatView.BUTTON_DP_SIZE + PlayFloatView.UNIT_GROW_DP_SIZE * (height - 1));
        binding.circleProgress.setIndicatorSize(sizePx);
        DisplayUtil.setViewWidth(binding.lineProgress, sizePx);

        DisplayUtil.setViewWidth(binding.cardLayout, sizePx);
        DisplayUtil.setViewHeight(binding.cardLayout, heightPx);
    }

    public boolean check(Task task, StartAction startAction) {
        return this.task.getId().equals(task.getId()) && this.startAction.getId().equals(startAction.getId());
    }

    public void tryRemoveFromParent() {
        if (playState == PLAY_STATE_STOPPED) {
            ((ViewGroup) getParent()).removeView(this);
        } else {
            remove = true;
        }
    }

    public void setNeedRemove(boolean needRemove) {
        this.remove = needRemove;
    }

    private void pause() {
        if (runnable == null) return;
        runnable.pause();
        playState = PLAY_STATE_PAUSED;

        binding.circleProgress.setIndeterminate(false);
        binding.lineProgress.setIndeterminate(false);
        binding.circleProgress.setVisibility(View.GONE);
        binding.lineProgress.setVisibility(View.GONE);
        binding.icon.setImageResource(R.drawable.icon_play_arrow);
    }

    private void setPlaying() {
        playState = PLAY_STATE_RUNNING;

        binding.circleProgress.setIndeterminate(true);
        binding.lineProgress.setIndeterminate(true);
        binding.circleProgress.setVisibility(size == height ? View.VISIBLE : View.GONE);
        binding.lineProgress.setVisibility(size == height ? View.GONE : View.VISIBLE);
        binding.icon.setImageResource(R.drawable.icon_pause);
    }

    private boolean resume() {
        if (runnable == null) return false;
        runnable.resume();
        setPlaying();
        return true;
    }

    protected void stop() {
        if (runnable == null) return;
        runnable.stop();
        runnable = null;
        playState = PLAY_STATE_STOPPED;

        binding.circleProgress.setIndeterminate(false);
        binding.lineProgress.setIndeterminate(false);
        binding.circleProgress.setVisibility(View.GONE);
        binding.lineProgress.setVisibility(View.GONE);
        binding.icon.setImageResource(0);
        binding.title.setText(getTitle(task, startAction));
    }

    private String getTitle(Task task, StartAction action) {
        String description = action.getDescription();
        if (description == null || description.isEmpty()) description = task.getTitle();
        if (description == null || description.isEmpty()) return "?";
        Pattern pattern = Pattern.compile("[\"|“](.+)[\"|”]");
        Matcher matcher = pattern.matcher(description);
        if (matcher.find()) description = matcher.group(1);
        if (description == null || description.isEmpty()) return "?";
        description = description.substring(0, Math.min(size, description.length()));
        return description;
    }

    public Task getTask() {
        return task;
    }

    public StartAction getStartAction() {
        return startAction;
    }

    @Override
    public void onStart(TaskRunnable runnable) {
        this.runnable = runnable;
        post(this::setPlaying);
    }

    @Override
    public void onExecute(TaskRunnable runnable, Action action, int progress) {
        post(() -> {
            if (playState == PLAY_STATE_RUNNING) binding.title.setText(String.valueOf(progress));
        });
    }

    @Override
    public void onCalculate(TaskRunnable runnable, Action action) {
    }

    @Override
    public void onFinish(TaskRunnable runnable) {
        post(() -> {
            stop();
            if (remove) ((ViewGroup) getParent()).removeView(this);
        });
    }
}
