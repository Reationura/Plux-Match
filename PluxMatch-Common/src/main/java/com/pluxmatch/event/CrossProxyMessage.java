package com.pluxmatch.event;

/**
 * 跨代理消息事件：Redis 订阅收到其他网关广播的状态同步时发布到事件总线，
 * 供核心或第三方插件处理真正的分布式同步逻辑（避免各处只打日志）。
 *
 * @author Reationura
 */
public class CrossProxyMessage {

    private final String action;
    private final String data;

    public CrossProxyMessage(String action, String data) {
        this.action = action;
        this.data = data;
    }

    public String getAction() {
        return action;
    }

    public String getData() {
        return data;
    }

    /**
     * data 字段以逗号分隔时的解析。
     */
    public String[] parts() {
        return data == null ? new String[0] : data.split(",");
    }
}
