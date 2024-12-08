package com.matteo.AppAPIserver.Tools.ThreadPool;

import java.util.Vector;

public class ThreadExecutor implements Runnable {
    private int maxRunningThread = 0;
    private Vector<Thread> threads = new Vector<Thread>();
    private Vector<Thread> runningThreads = new Vector<Thread>();
    private boolean paused = true;
    private boolean finished = false;

    protected ThreadExecutor(int maxRunningThread, Vector<Thread> threads) {
        this.maxRunningThread = maxRunningThread;
        this.threads = threads;
        new Thread(this).start();
    }

    @Override
    public void run() {
        boolean interrupted = false;
        while(!Thread.interrupted() && !interrupted) {
            synchronized(this) {
                while(paused) {
                    try {
                        wait();
                    } catch (InterruptedException e) {
                        interrupted = true;
                        break;
                    }
                }
            }

            if(threads.size() == 0) {
                if(runningThreads.size() > 0) {
                    while(runningThreads.size() > 0) {
                        Thread runningThread = runningThreads.remove(0);
                        if(runningThread != null && runningThread.isAlive()) {
                            runningThread.setPriority(Thread.MAX_PRIORITY);
                            try {
                                runningThread.join();
                            } catch (InterruptedException e) {
                                interrupted = true;
                            }
                        }
                    }
                    System.out.println("FINISH");
                } else {
                    break;
                }
            } else {
                while(runningThreads.size() >= maxRunningThread) {
                    Thread runningThread = runningThreads.remove(0);
                    if(runningThread != null && runningThread.isAlive()) {
                        runningThread.setPriority(Thread.MAX_PRIORITY);
                        try {
                            runningThread.join();
                        } catch (InterruptedException e) {
                            interrupted = true;
                        }
                    }
                }

                Thread t = threads.remove(0);
                t.start();
                runningThreads.add(t);
            }
        }
        finished = true;
    }

    public synchronized void pause() {
        this.paused = true;
    }

    public synchronized void resume() {
        this.paused = false;
        notifyAll();
    }

    public void waitForFinish() {
        while (!finished) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }
}
