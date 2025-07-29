package org.kingdoms.utils.internal;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;

/**
 * A lock that can be hold at only one position at any given time regardless of the current thread.
 * This is mostly used for Folia as the same thread is used for different tasks which are not executed
 * in the order they were queued.
 */
public final class NonReentrantLock implements Lock {
    private Thread lockingThread;

    public boolean isLocked() {
        return lockingThread != null;
    }

    public Thread getLockingThread() {
        return lockingThread;
    }

    // Maybe change this to "public HeldLock void lock()" and only allow unlocking from that object?
    @Override
    public synchronized void lock() {
        while (isLocked()) {
            try {
                wait();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
        }
        lockingThread = Thread.currentThread();
    }

    @Override
    public synchronized void lockInterruptibly() throws InterruptedException {
        if (Thread.interrupted()) {
            throw new InterruptedException();
        }
        while (isLocked()) {
            wait();
        }
        lockingThread = Thread.currentThread();
    }

    @Override
    public synchronized boolean tryLock() {
        if (isLocked()) {
            return false;
        }
        lockingThread = Thread.currentThread();
        return true;
    }

    @Override
    public synchronized boolean tryLock(long time, TimeUnit unit) throws InterruptedException {
        if (Thread.interrupted()) {
            throw new InterruptedException();
        }
        if (!isLocked()) {
            lockingThread = Thread.currentThread();
            return true;
        }
        if (time <= 0) {
            return false;
        }
        long deadline = System.nanoTime() + unit.toNanos(time);
        while (isLocked()) {
            long remaining = deadline - System.nanoTime();
            if (remaining <= 0) {
                return false;
            }
            wait(remaining / 1_000_000, (int) (remaining % 1_000_000));
        }
        lockingThread = Thread.currentThread();
        return true;
    }

    @Override
    public synchronized void unlock() {
        if (lockingThread != Thread.currentThread()) {
            throw new IllegalMonitorStateException("Calling thread does not hold the lock:" +
                    "\n  Owner: " + lockingThread + "\n  Current: " + Thread.currentThread());
        }
        lockingThread = null;
        notify();
    }

    @Override
    public synchronized Condition newCondition() {
        throw new UnsupportedOperationException("Conditions are not supported for non-reentrant locks");
    }
}