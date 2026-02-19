package com.nexusadmin.core.extension.finder;

import com.nexusadmin.core.extension.Extension;
import com.nexusadmin.core.extension.ExtensionPoint;
import com.nexusadmin.core.extension.ExtensionMetadata;
import com.nexusadmin.core.extension.ExtensionWrapper;
import com.nexusadmin.core.extension.storage.ExtensionStorage;

import java.util.*;

/**
 * 组合扩展发现器实现。
 * <p>组合多个 {@link ExtensionStorage} 实现，从不同来源发现扩展实现。</p>
 * <p>支持自动推断扩展点类型、优先级排序、启用状态过滤。</p>
 */
public class CompositeExtensionFinder implements ExtensionFinder {

    private final List<ExtensionStorage> storages;

    /**
     * 构造组合扩展发现器。
     *
     * @param storages 扩展存储实现列表
     */
    public CompositeExtensionFinder(List<ExtensionStorage> storages) {
        this.storages = storages != null ? List.copyOf(storages) : List.of();
    }

    /**
     * 构造组合扩展发现器（单存储）。
     *
     * @param storage 扩展存储实现
     */
    public CompositeExtensionFinder(ExtensionStorage storage) {
        this.storages = storage != null ? List.of(storage) : List.of();
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends ExtensionPoint> List<ExtensionWrapper<T>> find(
            Class<T> pointType,
            ClassLoader classLoader,
            String pluginId) {

        if (pointType == null || classLoader == null) {
            return List.of();
        }

        List<ExtensionWrapper<T>> result = new ArrayList<>();
        Set<String> processedClasses = new HashSet<>();

        for (ExtensionStorage storage : storages) {
            if (!storage.supports(classLoader)) {
                continue;
            }

            List<ExtensionMetadata> metadatas = storage.loadExtensions(classLoader);
            for (ExtensionMetadata metadata : metadatas) {
                // 避免重复处理同一实现类
                if (processedClasses.contains(metadata.getImplementationClassName())) {
                    continue;
                }

                try {
                    Class<?> implClass = Class.forName(
                            metadata.getImplementationClassName(), true, classLoader);

                    // 检查是否实现了目标扩展点
                    if (!pointType.isAssignableFrom(implClass)) {
                        continue;
                    }

                    // 读取 @Extension 注解
                    Extension extAnno = implClass.getAnnotation(Extension.class);

                    // 如果标记了 @Extension 且 disabled，则跳过
                    if (extAnno != null && !extAnno.enabled()) {
                        continue;
                    }

                    // 如果指定了 points，检查是否匹配
                    if (extAnno != null && extAnno.points().length > 0) {
                        boolean matched = Arrays.stream(extAnno.points())
                                .anyMatch(p -> p.equals(pointType));
                        if (!matched) {
                            continue;
                        }
                    }

                    // 实例化
                    Class<? extends T> impl = (Class<? extends T>) implClass;
                    T instance = impl.getDeclaredConstructor().newInstance();

                    // 提取元信息
                    int priority = extAnno != null ? extAnno.priority() : 0;
                    String name = extAnno != null && !extAnno.name().isEmpty()
                            ? extAnno.name() : implClass.getSimpleName();
                    String description = extAnno != null ? extAnno.description() : "";

                    // 创建包装器
                    ExtensionWrapper<T> wrapper = new ExtensionWrapper<>(
                            instance,
                            pointType,
                            pluginId != null ? pluginId : metadata.getPluginId(),
                            priority,
                            true,
                            name,
                            description
                    );

                    result.add(wrapper);
                    processedClasses.add(metadata.getImplementationClassName());

                } catch (ClassNotFoundException e) {
                    // 类不存在，跳过
                } catch (Exception e) {
                    // 实例化失败，跳过
                }
            }
        }

        // 排序：优先级降序
        result.sort(Comparator.reverseOrder());

        return Collections.unmodifiableList(result);
    }
}
