package com.nexusadmin.core.config.binder;

import com.nexusadmin.core.config.ConfigManager;
import com.nexusadmin.core.config.event.ConfigChangedEvent;
import com.nexusadmin.core.config.event.ConfigListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 配置绑定器，将配置自动绑定到对象字段。
 * <p>支持配置热更新时自动重新绑定。</p>
 * <p>以 scope 为核心，可用于插件配置或平台配置的绑定。</p>
 */
public class ConfigBinder {

    private static final Logger log = LoggerFactory.getLogger(ConfigBinder.class);

    /**
     * 配置管理器。
     */
    private final ConfigManager configManager;

    /**
     * 绑定记录，key 为 scope，value 为绑定对象集合。
     */
    private final Map<String, Map<Object, BindingInfo>> bindings = new ConcurrentHashMap<>();

    /**
     * 构造配置绑定器。
     *
     * @param configManager 配置管理器
     */
    public ConfigBinder(ConfigManager configManager) {
        this.configManager = Objects.requireNonNull(configManager, "配置管理器不能为空");
    }

    /**
     * 绑定配置到对象。
     * <p>对象字段名将作为配置键名，从配置管理器获取值并设置到字段。</p>
     *
     * @param scope  配置作用域
     * @param target 目标对象
     */
    public void bind(String scope, Object target) {
        Objects.requireNonNull(scope, "作用域不能为空");
        Objects.requireNonNull(target, "目标对象不能为空");

        Class<?> clazz = target.getClass();

        // 执行初始绑定
        performBinding(scope, target, clazz);

        // 记录绑定信息
        BindingInfo bindingInfo = new BindingInfo(target, clazz);
        bindings.computeIfAbsent(scope, k -> new ConcurrentHashMap<>()).put(target, bindingInfo);

        log.debug("配置已绑定: {} -> {}", scope, clazz.getSimpleName());
    }

    /**
     * 解绑配置。
     *
     * @param scope  配置作用域
     * @param target 目标对象
     */
    public void unbind(String scope, Object target) {
        Map<Object, BindingInfo> scopeBindings = bindings.get(scope);
        if (scopeBindings != null) {
            scopeBindings.remove(target);
            log.debug("配置已解绑: {} -> {}", scope, target.getClass().getSimpleName());
        }
    }

    /**
     * 解绑指定作用域的所有配置。
     *
     * @param scope 配置作用域
     */
    public void unbindAll(String scope) {
        bindings.remove(scope);
        log.debug("作用域所有配置已解绑: {}", scope);
    }

    /**
     * 重新绑定指定作用域的所有对象。
     *
     * @param scope 配置作用域
     */
    public void rebind(String scope) {
        Map<Object, BindingInfo> scopeBindings = bindings.get(scope);
        if (scopeBindings == null || scopeBindings.isEmpty()) {
            return;
        }

        for (Map.Entry<Object, BindingInfo> entry : scopeBindings.entrySet()) {
            Object target = entry.getKey();
            BindingInfo info = entry.getValue();
            performBinding(scope, target, info.clazz);
        }

        log.debug("配置已重新绑定: {} ({} 个对象)", scope, scopeBindings.size());
    }

    /**
     * 创建配置变更监听器。
     * <p>当配置变更时自动重新绑定。</p>
     *
     * @param scope 配置作用域
     * @return 配置监听器
     */
    public ConfigListener createListener(String scope) {
        return new AutoRebindListener(scope);
    }

    /**
     * 执行绑定操作。
     * <p>遍历目标对象的所有字段，以字段名为键从配置管理器获取值并设置。</p>
     *
     * @param scope  配置作用域
     * @param target 目标对象
     * @param clazz  目标类
     */
    private void performBinding(String scope, Object target, Class<?> clazz) {
        for (Field field : clazz.getDeclaredFields()) {
            String key = field.getName();
            Class<?> type = field.getType();

            // 复用 ConfigManager 的类型转换能力
            Optional<?> value = configManager.get(scope, key, type);

            if (value.isPresent()) {
                try {
                    field.setAccessible(true);
                    field.set(target, value.get());
                    log.trace("字段绑定: {}.{} = {}", scope, key, value.get());
                } catch (Exception e) {
                    log.warn("字段绑定失败: {}.{}", scope, key, e);
                }
            }
        }
    }

    /**
     * 绑定信息。
     */
    private static class BindingInfo {
        final Object target;
        final Class<?> clazz;

        BindingInfo(Object target, Class<?> clazz) {
            this.target = target;
            this.clazz = clazz;
        }
    }

    /**
     * 自动重新绑定监听器。
     */
    private class AutoRebindListener implements ConfigListener {
        private final String scope;

        AutoRebindListener(String scope) {
            this.scope = scope;
        }

        @Override
        public void onConfigChanged(ConfigChangedEvent event) {
            // 监听该 scope 及其子键的变更
            if (scope.equals(event.configScope()) || event.configScope().startsWith(scope + ".")) {
                rebind(scope);
            }
        }

        @Override
        public String interestedScope() {
            return scope;
        }
    }
}
