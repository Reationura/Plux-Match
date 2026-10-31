package com.pluxmatch.model;

/**
 * 加入队列的结果枚举，替代原先的布尔返回，便于命令端给出精确提示。
 *
 * @author Reationura
 */
public enum QueueResult {
    SUCCESS,
    QUEUE_NOT_FOUND,
    QUEUE_DISABLED,
    COOLDOWN,
    QUEUE_FULL,
    NO_PERMISSION,
    PARTY_NOT_ALLOWED,
    PARTY_SIZE_MISMATCH,
    ALREADY_IN_QUEUE
}
