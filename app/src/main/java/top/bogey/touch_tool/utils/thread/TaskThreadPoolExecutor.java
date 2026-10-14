package top.bogey.touch_tool.utils.thread;

import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class TaskThreadPoolExecutor extends ThreadPoolExecutor {
    private final AtomicInteger submittedTaskCount = new AtomicInteger(0);

    public TaskThreadPoolExecutor(int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit, TaskQueue<Runnable> workQueue) {
        super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue);
        workQueue.setExecutor(this);
    }

    public int getSubmittedTaskCount() {
        return submittedTaskCount.get();
    }

    @Override
    protected void afterExecute(Runnable r, Throwable t) {
        submittedTaskCount.decrementAndGet();
    }

    @Override
    public void execute(Runnable command) {
        if (command == null) throw new NullPointerException();
        submittedTaskCount.incrementAndGet();
        try {
            super.execute(command);
        } catch (Throwable throwable) {
            // 提交失败（容量已满被拒绝）必须如实抛出：吞掉的话调用方会以为任务已提交，
            // 而它从未执行，也不会触发任何回调
            submittedTaskCount.decrementAndGet();
            throw throwable;
        }
    }
}
