package com.matteo.AppAPIserver.Tools.ThreadPool;

import java.util.Vector;

public class ThreadPool {
    private Vector<Thread> threads = new Vector<Thread>();
    private ThreadExecutor executor;

    public ThreadPool() {
        this(Math.max(Runtime.getRuntime().availableProcessors() * 2, 1));
    }

    public ThreadPool(int maxRunningThread) {
        if(maxRunningThread < 1) {
            maxRunningThread = 1;
        }
        executor = new ThreadExecutor(maxRunningThread, threads);
    }

    public void add(Thread t) {
        threads.add(t);
        executor.resume();
    }

    public void waitAllThreads() {
        executor.waitForFinish();
    }
}
