package com.quertimizer.judge.adapter.out.execution;

import com.quertimizer.judge.domain.model.DbmsType;
import com.quertimizer.judge.domain.model.JudgeQueuePriority;
import com.quertimizer.judge.domain.model.JudgeQueueStatusListener;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

class LvmSnapshotWorkQueue {

    private static final long WAIT_REPORT_INTERVAL_MILLIS = 1000L;

    private final Object monitor = new Object();
    private final Deque<QueueTicket> queue = new ArrayDeque<>();

    LvmSnapshotRuntimeSlot awaitTurn(DbmsType dbmsType, JudgeQueuePriority priority,
                                     JudgeQueueStatusListener listener,
                                     Supplier<Optional<LvmSnapshotRuntimeSlot>> slotSupplier) {
        // 대기열 ticket 등록
        QueueTicket ticket = new QueueTicket(dbmsType, priority);
        JudgeQueueStatusListener statusListener = listener != null ? listener : JudgeQueueStatusListener.noop();
        synchronized (monitor) {
            if (priority == JudgeQueuePriority.FIRST) {
                addPriorityTicket(ticket);
            } else {
                queue.addLast(ticket);
            }
            monitor.notifyAll();
        }

        // 순번과 runner slot 확보까지 대기
        while (true) {
            int remainingTasks;
            synchronized (monitor) {
                try {
                    if (isFirstForDbms(ticket)) {
                        Optional<LvmSnapshotRuntimeSlot> slot = slotSupplier.get();
                        if (slot.isPresent()) {
                            queue.remove(ticket);
                            monitor.notifyAll();
                            return slot.get();
                        }
                    }
                } catch (RuntimeException exception) {
                    queue.remove(ticket);
                    monitor.notifyAll();
                    throw exception;
                }
                remainingTasks = countBefore(ticket);
            }

            // 현재 ticket 앞의 작업 수 전달
            statusListener.onWaiting(remainingTasks);
            synchronized (monitor) {
                try {
                    monitor.wait(WAIT_REPORT_INTERVAL_MILLIS);
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    queue.remove(ticket);
                    monitor.notifyAll();
                    throw new IllegalStateException("LVM 스냅샷 작업 대기 중 중단", exception);
                }
            }
        }
    }

    void notifyAvailableSlot() {
        synchronized (monitor) {
            monitor.notifyAll();
        }
    }

    private void addPriorityTicket(QueueTicket ticket) {
        // 기존 우선 작업 뒤와 일반 작업 앞 사이에 ticket 삽입
        Deque<QueueTicket> reorderedQueue = new ArrayDeque<>();
        boolean added = false;
        while (!queue.isEmpty()) {
            QueueTicket queuedTicket = queue.removeFirst();
            if (!added && queuedTicket.priority != JudgeQueuePriority.FIRST) {
                reorderedQueue.addLast(ticket);
                added = true;
            }
            reorderedQueue.addLast(queuedTicket);
        }
        if (!added) {
            reorderedQueue.addLast(ticket);
        }
        queue.addAll(reorderedQueue);
    }

    private boolean isFirstForDbms(QueueTicket ticket) {
        // 같은 DBMS 기준 선두 ticket 여부 확인
        for (QueueTicket queuedTicket : queue) {
            if (queuedTicket.dbmsType == ticket.dbmsType) {
                return queuedTicket.equals(ticket);
            }
        }

        return false;
    }

    private int countBefore(QueueTicket ticket) {
        // 같은 DBMS 기준 현재 ticket 앞 대기 작업 수 계산
        int count = 0;
        for (QueueTicket queuedTicket : queue) {
            if (queuedTicket.equals(ticket)) {
                return count;
            }
            if (queuedTicket.dbmsType == ticket.dbmsType) {
                count++;
            }
        }

        return count;
    }

    private static final class QueueTicket {
        private final String id = UUID.randomUUID().toString();
        private final DbmsType dbmsType;
        private final JudgeQueuePriority priority;

        private QueueTicket(DbmsType dbmsType, JudgeQueuePriority priority) {
            this.dbmsType = Objects.requireNonNull(dbmsType, "필수 값이 없습니다.");
            this.priority = Objects.requireNonNull(priority, "필수 값이 없습니다.");
        }

        @Override
        public boolean equals(Object object) {
            if (this == object) {
                return true;
            }
            if (!(object instanceof QueueTicket ticket)) {
                return false;
            }

            return id.equals(ticket.id);
        }

        @Override
        public int hashCode() {
            return id.hashCode();
        }
    }
}
