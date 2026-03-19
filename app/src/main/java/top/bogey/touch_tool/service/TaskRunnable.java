package top.bogey.touch_tool.service;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.Stack;
import java.util.concurrent.Future;

import top.bogey.touch_tool.bean.action.Action;
import top.bogey.touch_tool.bean.action.start.InnerStartAction;
import top.bogey.touch_tool.bean.action.start.StartAction;
import top.bogey.touch_tool.bean.other.log.ActionLog;
import top.bogey.touch_tool.bean.other.log.DateTimeLog;
import top.bogey.touch_tool.bean.other.log.LogInfo;
import top.bogey.touch_tool.bean.other.log.NormalLog;
import top.bogey.touch_tool.bean.save.SettingSaver;
import top.bogey.touch_tool.bean.save.log.LogSaver;
import top.bogey.touch_tool.bean.task.Task;

public class TaskRunnable implements Runnable {
    private final Stack<TaskContext> taskContextStack = new Stack<>();
    private final Set<ITaskListener> listeners = new HashSet<>();

    private final Task task;
    private final StartAction startAction;
    private boolean debug;


    private int progress = 0;

    private Future<?> future;
    private volatile boolean interrupt = false;
    private volatile long pauseTime = -1;

    private boolean cacheLog = false;
    private final List<LogInfo> cacheLogList = new ArrayList<>();

    private boolean logged = false;
    private final Stack<LogInfo> logStack = new Stack<>();
    private final Stack<Integer> logStackIndex = new Stack<>();

    public TaskRunnable(Task task, StartAction startAction) {
        this.task = task;
        this.startAction = startAction;
        this.debug = task.hasFlag(Task.FLAG_DEBUG) && SettingSaver.getInstance().isDetailLog();
    }

    @Override
    public void run() {
        if (startAction instanceof InnerStartAction) {
            cacheLog = true;
        }
        try {
            if (SettingSaver.getInstance().isLogResetOnStart()) {
                LogSaver.getInstance().clearLog(task.getId());
            }

            task.execute(this, startAction, result -> {
                if (result) listeners.stream().filter(Objects::nonNull).forEach(listener -> listener.onStart(this));
            });
        } catch (Exception e) {
            e.printStackTrace();

            String errorInfo = e.toString();
            try {
                StringWriter stringWriter = new StringWriter();
                PrintWriter printWriter = new PrintWriter(stringWriter);
                e.printStackTrace(printWriter);
                errorInfo = stringWriter.toString();
            } catch (Exception ignored) {
            }
            addLog(errorInfo);
        }

        while (!logStack.isEmpty()) {
            addLog(logStack.pop(), 0);
        }
        if (logged) addLog(new LogInfo(new DateTimeLog()), 0);

        interrupt = true;
        listeners.stream().filter(Objects::nonNull).forEach(listener -> listener.onFinish(this));
    }

    public void pushStack(Task task, Action action) {
        taskContextStack.push(new TaskContext(task, action));
        if (debug) logStackIndex.push(logStack.size());
    }

    public void popStack() {
        taskContextStack.pop();
        if (debug) {
            int index = logStackIndex.pop();
            while (logStack.size() > index) {
                addLog(logStack.pop(), 0);
            }
        }
    }

    public Task getTask() {
        return taskContextStack.peek().getTask();
    }

    public Action getAction() {
        return taskContextStack.peek().getStartAction();
    }

    public Task getStartTask() {
        return task;
    }

    public StartAction getStartAction() {
        return startAction;
    }

    public void addListener(ITaskListener listener) {
        listeners.add(listener);
    }

    public void addExecuteProgress(Action action) {
        progress++;
        listeners.stream().filter(Objects::nonNull).forEach(listener -> listener.onExecute(this, action, progress));

        StartAction startAction = getStartAction();
        if (startAction == null || startAction.stop(this)) stop();
        else checkStatus();
    }

    public void addCalculateProgress(Action action) {
        listeners.stream().filter(Objects::nonNull).forEach(listener -> listener.onCalculate(this, action));
        checkStatus();
    }

    public void addLog(String log) {
        addLog(new LogInfo(new NormalLog(log)), 0);
    }

    public void addLog(LogInfo logInfo, int stackOption) {
        switch (stackOption) {
            case -1 -> {
                LogInfo info = logStack.pop();
                info.syncLog(logInfo.getLogObject());
                addLog(info, 0);
            }
            case 0 -> {
                if (logStack.isEmpty()) {
                    if (!cacheLog) {
                        LogSaver.getInstance().addLog(task.getId(), logInfo, true);
                        logged = true;
                    }
                    cacheLogList.add(logInfo);
                } else {
                    logStack.peek().addChild(logInfo);
                    if (!cacheLog) {
                        LogSaver.getInstance().addLog(task.getId(), logInfo, false);
                        logged = true;
                    }
                }
            }
            case 1 -> logStack.push(logInfo);
        }
    }

    public void addDebugLog(Action action, int stackOption) {
        if (action instanceof InnerStartAction) return;
        if (debug) addLog(new LogInfo(new ActionLog(progress + 1, getTask(), action, stackOption != 0)), stackOption);
    }

    public List<LogInfo> getCacheLogList() {
        return cacheLogList;
    }

    public int getProgress() {
        return progress;
    }

    public synchronized void stop() {
        interrupt = true;
        if (pauseTime >= 0) resume();
        if (future != null) future.cancel(true);
        taskContextStack.forEach(taskContext -> taskContext.setInterrupt(true));
    }

    public void stopCurrent() {
        taskContextStack.peek().setInterrupt(true);
    }

    public boolean isInterrupt() {
        return interrupt;
    }

    public boolean isCurrentInterrupt() {
        return interrupt || taskContextStack.peek().isInterrupt();
    }

    private synchronized void checkStatus() {
        while (pauseTime >= 0 && !interrupt) {
            try {
                long currentPauseTime = pauseTime;
                wait(currentPauseTime);
                if (pauseTime == currentPauseTime) {
                    pauseTime = -1;
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public void sleep(long time) {
        if (time <= 0) return;
        long remainTime = time;
        long sleepTime = Math.min(remainTime, 100);
        while (sleepTime > 0) {
            try {
                Thread.sleep(sleepTime);
                remainTime = remainTime - 100;
                sleepTime = Math.min(remainTime, 100);
                checkStatus();
            } catch (InterruptedException e) {
                stop();
                break;
            }
        }
    }

    public void await() {
        await(0);
    }

    public synchronized void await(long ms) {
        if (pauseTime >= 0) {
            // 当前正处于暂停状态，则跳过当前暂停，执行新的暂停
            pauseTime = ms;
            notifyAll();
        } else {
            // 处于正常状态，则暂停并等待
            pauseTime = ms;
            checkStatus();
        }
    }

    // 其他线程告诉当前任务要暂停了
    public synchronized boolean pause() {
        if (pauseTime >= 0) return false;
        pauseTime = 0;
        notifyAll();
        return true;
    }

    public synchronized void resume() {
        pauseTime = -1;
        notifyAll();
    }

    public boolean isDebug() {
        return debug;
    }

    public void setDebug(boolean debug) {
        this.debug = debug;
    }

    public void setFuture(Future<?> future) {
        this.future = future;
    }

    private static class TaskContext {
        private final Task task;
        private final Action startAction;

        private boolean interrupt = false;

        public TaskContext(Task task, Action startAction) {
            this.task = task;
            this.startAction = startAction;
        }

        public void setInterrupt(boolean interrupt) {
            this.interrupt = interrupt;
        }

        public boolean isInterrupt() {
            return interrupt;
        }

        public Task getTask() {
            return task;
        }

        public Action getStartAction() {
            return startAction;
        }
    }
}
