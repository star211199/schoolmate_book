package com.schoolmate.utils;

import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * 事务辅助工具。
 *
 * @author Albot
 */
public final class TransactionUtil {

    private TransactionUtil() {
    }

    /**
     * 在当前事务提交之后再执行指定动作，若当前没有事务则立即执行。
     *
     * <p><b>为什么需要它：</b>聊天场景下「先落库再推送」是原则，但如果推送写在事务体内，
     * 一旦后续操作抛异常导致事务回滚，对方已经收到了这条并不存在于数据库的消息
     * （俗称「幽灵消息」，刷新后消失）。
     * 把推送挂到 afterCommit，可以保证「消息一定已落库」才对外可见。
     *
     * @param action 待执行动作
     */
    public static void afterCommit(Runnable action) {
        if (action == null) {
            return;
        }
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    action.run();
                }
            });
        } else {
            action.run();
        }
    }
}
