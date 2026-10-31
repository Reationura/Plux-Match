package com.pluxmatch.match;

import com.pluxmatch.model.QueueEntry;
import com.pluxmatch.model.ServerData;

/**
 * 服务器筛选扩展点：允许第三方或核心按 ELO/区域等自定义过滤候选房间。
 * 通过 {@code PluxMatchAPI#registerMatchFilter} 注册，多个过滤器以 AND 组合。
 *
 * @author Reationura
 */
@FunctionalInterface
public interface MatchFilter {

    /**
     * @return true 表示该服务器可作为该玩家的候选
     */
    boolean test(QueueEntry entry, ServerData server);
}
