package com.nexusadmin.core.config.binder;

import com.nexusadmin.core.config.event.ConfigListener;
import com.nexusadmin.core.config.ConfigManager;
import com.nexusadmin.core.config.event.ConfigChangedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 插件配置绑定器，将配置自动绑定到对象字段。
 * <p>支持配置热更新时重新绑定。</p>
 */
public class PluginConfigBinder {

    private static final Logger log = LoggerFactory.getLogger(PluginConfigBinder.class);

    /**
     * 配置管理器。
     */
    private final ConfigManager configManager;

    /**
     * 绑定记录，key 为 pluginId，value 为绑定对象集合。
     */
    private final Map<String, Map<Object, BindingInfo>> bindings = new ConcurrentHashMap<>();

    /**
     * 构造配置绑定器。
     *
     * @param configManager 配置管理器
     */
    public PluginConfigBinder(ConfigManager configManager) {
        this.configManager = Objects.requireNonNull(configManager, "配置管理器不能为空");
    }

    /**
     * 绑定配置到对象。
     * <p>对象字段名将作为配置键名，从配置服务获取值并设置到字段。</p>
     *
     * @param pluginId 插件ID
     * @param target   目标对象
     */
    public void bind(String pluginId, Object target) {
        Objects.requireNonNull(pluginId, "插件ID不能为空");
        Objects.requireNonNull(target, "目标对象不能为空");

        String scope = "plugin." + pluginId;
        Class<?> clazz = target.getClass();

        // 执行初始绑定
        performBinding(scope, target, clazz);

        // 记录绑定信息
        BindingInfo bindingInfo = new BindingInfo(target, clazz);
        bindings.computeIfAbsent(pluginId, k -> new ConcurrentHashMap<>()).put(target, bindingInfo);

        log.debug("配置已绑定: {} -> {}", pluginId, clazz.getSimpleName());
    }

    /**
     * 解绑配置。
     *
     * @param pluginId 插件ID
     * @param target   目标对象
     */
    public void unbind(String pluginId, Object target) {
        Map<Object, BindingInfo> pluginBindings = bindings.get(pluginId);
        if (pluginBindings != null) {
            pluginBindings.remove(target);
            log.debug("配置已解绑: {} -> {}", pluginId, target.getClass().getSimpleName());
        }
    }

    /**
     * 解绑插件的所有配置。
     *
     * @param pluginId 插件ID
     */
    public void unbindAll(String pluginId) {
        bindings.remove(pluginId);
        log.debug("插件所有配置已解绑: {}", pluginId);
    }

    /**
     * 重新绑定指定插件的所有对象。
     *
     * @param pluginId 插件ID
     */
    public void rebind(String pluginId) {
        Map<Object, BindingInfo> pluginBindings = bindings.get(pluginId);
        if (pluginBindings == null || pluginBindings.isEmpty()) {
            return;
        }

        String scope = "plugin." + pluginId;
        for (Map.Entry<Object, BindingInfo> entry : pluginBindings.entrySet()) {
            Object target = entry.getKey();
            BindingInfo info = entry.getValue();
            performBinding(scope, target, info.clazz);
        }

        log.debug("配置已重新绑定: {} ({} 个对象)", pluginId, pluginBindings.size());
    }

    /**
     * 创建配置变更监听器。
     * <p>当配置变更时自动重新绑定。</p>
     *
     * @param pluginId 插件ID
     * @return 配置监听器
     */
    public ConfigListener createListener(String pluginId) {
        return new AutoRebindListener(pluginId);
    }

    /**
     * 执行绑定操作。
     *
     * @param scope  配置作用域
     * @param target 目标对象
     * @param clazz  目标类
     */
    private void performBinding(String scope, Object target, Class<?> clazz) {
        for (Field field : clazz.getDeclaredFields()) {
            String key = field.getName();
            Optional<String> value = configManager.get(scope, key);

            if (value.isPresent()) {
                try {
                    field.setAccessible(true);
                    Object convertedValue = convertValue(value.get(), field.getType());
                    field.set(target, convertedValue);
                    log.trace("字段绑定: {}.{} = {}", scope, key, convertedValue);
                } catch (Exception e) {
                    log.warn("字段绑定失败: {}.{}", scope, key, e);
                }
            }
        }
    }

    /**
     * 转换配置值到目标类型。
     */
    private Object convertValue(String value, Class<?> type) {
        if (type == String.class) {
            return value;
        }
        if (type == Integer.class || type == int.class) {
            return Integer.parseInt(value);
        }
        if (type == Long.class || type == long.class) {
            return Long.parseLong(value);
        }
        if (type == Boolean.class || type == boolean.class) {
            return Boolean.parseBoolean(value);
        }
        if (type == Double.class || type == double.class) {
            return Double.parseDouble(value);
        }
        if (type == Float.class || type == float.class) {
            return Float.parseFloat(value);
        }
        if (type.isEnum()) {
            @SuppressWarnings({"rawtypes", "unchecked"})
            Enum enumValue = Enum.valueOf((Class<Enum>) type, value);
            return enumValue;
        }
        throw new IllegalArgumentException("不支持的类型: " + type.getName());
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
        private final String pluginId;

        AutoRebindListener(String pluginId) {
            this.pluginId = pluginId;
        }

        @Override
        public void onConfigChanged(ConfigChangedEvent event) {
            if (event.scope().equals("plugin." + pluginId)) {
                rebind(pluginId);
            }
        }

        @Override
        public String interestedScope() {
            return "plugin." + pluginId;
        }
    }
}
