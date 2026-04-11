/*
 * Copyright 2026 Ed Bratt
 * SPDX-License-Identifier: MIT
 */
package com.processing.server;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public class EventQueue {
    private final Queue<UserInputEvent> queue = new ConcurrentLinkedQueue<>();

    public void push(UserInputEvent event) {
        queue.offer(event);
    }

    public UserInputEvent poll() {
        return queue.poll();
    }

    public boolean isEmpty() {
        return queue.isEmpty();
    }

    public int size() {
        return queue.size();
    }

    public void clear() {
        queue.clear();
    }
}