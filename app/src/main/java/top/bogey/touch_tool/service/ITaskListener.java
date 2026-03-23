package top.bogey.touch_tool.service;

import top.bogey.touch_tool.bean.action.Action;

public interface ITaskListener {
    default void onStart(TaskRunnable runnable) {
    }

    default void onExecute(TaskRunnable runnable, Action action, int progress) {
    }

    default void onCalculate(TaskRunnable runnable, Action action) {
    }

    default void onFinish(TaskRunnable runnable) {
    }

    default void onPauseChanged(TaskRunnable runnable, boolean paused) {
    }
}
