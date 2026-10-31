package com.pluxmatch.event;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * 轻量事件总线，供核心内部发布及第三方插件订阅。
 * 线程安全，订阅回调在发布者线程同步执行。
 *
 * @author Reationura
 */
public class EventBus {

    private final Map<Class<?>, List<Consumer<?>>> listeners = new ConcurrentHashMap<>();

    @SuppressWarnings("unchecked")
    public <T> void subscribe(Class<T> type, Consumer<T> handler) {
        listeners.computeIfAbsent(type, k -> new CopyOnWriteArrayList<>()).add(handler);
    }

    public <T> void unsubscribe(Class<T> type, Consumer<T> handler) {
        List<Consumer<?>> list = listeners.get(type);
        if (list != null) {
            list.remove(handler);
        }
    }

    @SuppressWarnings("unchecked")
    public <T> void publish(T event) {
        List<Consumer<?>> list = listeners.get(event.getClass());
        if (list == null) {
            return;
        }
        for (Consumer<?> consumer : list) {
            ((Consumer<T>) consumer).accept(event);
        }
    }

    public void clear() {
        listeners.clear();
    }
}
