package com.pluxmatch.redis;

/**
 * Redis 协议常量。
 *
 * @author Reationura
 */
public class Protocol {
    public static final int DEFAULT_TIMEOUT = 2000;

    public static final String ACTION_QUEUE_JOIN = "QUEUE_JOIN";
    public static final String ACTION_QUEUE_LEAVE = "QUEUE_LEAVE";
    public static final String ACTION_MATCH_START = "MATCH_START";
    public static final String ACTION_MATCH_END = "MATCH_END";
}
