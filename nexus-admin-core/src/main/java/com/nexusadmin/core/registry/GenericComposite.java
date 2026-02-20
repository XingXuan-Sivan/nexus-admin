package com.nexusadmin.core.registry;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Function;

/**
 * 通用组合器实现，适用于所有 Composable 组件类型。
 * <p>支持按优先级排序、短路或全执行策略，无需为每个组件类型单独实现 Composite 类。</p>
 *
 * <p>使用示例：</p>
 * <pre>
 * // 创建带优先级排序的组合器
 * GenericComposite&lt;PluginLoader&gt; loaderComposite = 
 *     new GenericComposite&lt;&gt;(loader -&gt; 100);
 * 
 * loaderComposite.addMember(jarLoader);
 * loaderComposite.addMember(classpathLoader);
 * 
 * // 执行第一个支持的成员
 * Optional&lt;PluginWapper&gt; result = loaderComposite.executeFirstSupported(
 *     loader -&gt; loader.supports(candidate),
 *     loader -&gt; loader.load(candidate, registry)
 * );
 * </pre>
 *
 * @param <T> 被组合的组件类型，必须继承 {@link Composable}
 * @see Composite
 * @see Composable
 */
public class GenericComposite<T extends Composable> implements Composite<T> {

    private final List<T> members = new CopyOnWriteArrayList<>();
    private final Comparator<T> comparator;

    /**
     * 创建无排序的通用组合器。
     * <p>成员按添加顺序执行。</p>
     */
    public GenericComposite() {
        this.comparator = null;
    }

    /**
     * 创建带优先级排序的通用组合器。
     * <p>成员按优先级从高到低执行。</p>
     *
     * @param priorityExtractor 优先级提取函数，返回优先级数值（数值越大优先级越高）
     */
    public GenericComposite(Function<T, Integer> priorityExtractor) {
        Objects.requireNonNull(priorityExtractor, "优先级提取函数不能为 null");
        this.comparator = Comparator.comparingInt(priorityExtractor::apply).reversed();
    }

    // ==================== Composite 接口实现 ====================

    @Override
    public List<T> getMembers() {
        List<T> result = new ArrayList<>(members);
        if (comparator != null) {
            result.sort(comparator);
        }
        return List.copyOf(result);
    }

    @Override
    public void addMember(T member) {
        Objects.requireNonNull(member, "成员不能为 null");
        if (!members.contains(member)) {
            members.add(member);
        }
    }

    @Override
    public void removeMember(T member) {
        members.remove(member);
    }

    @Override
    public boolean containsMember(T member) {
        return members.contains(member);
    }

    @Override
    public int memberCount() {
        return members.size();
    }

    // ==================== 通用执行策略 ====================

    /**
     * 按顺序执行所有成员，返回第一个非空结果（短路策略）。
     * <p>适用于解析器、查找器等场景，找到第一个有效结果即返回。</p>
     *
     * @param executor 执行函数，接收成员返回 Optional 结果
     * @param <R>      返回类型
     * @return 第一个非空结果，如果所有成员都返回空则返回 empty
     */
    public <R> Optional<R> executeFirst(Function<T, Optional<R>> executor) {
        Objects.requireNonNull(executor, "执行函数不能为 null");
        return getMembers().stream()
                .map(executor)
                .flatMap(Optional::stream)
                .findFirst();
    }

    /**
     * 执行所有成员，返回所有非空结果（全执行策略）。
     * <p>适用于监听器、通知器等场景，需要执行所有成员。</p>
     *
     * @param executor 执行函数，接收成员返回 Optional 结果
     * @param <R>      返回类型
     * @return 所有非空结果列表
     */
    public <R> List<R> executeAll(Function<T, Optional<R>> executor) {
        Objects.requireNonNull(executor, "执行函数不能为 null");
        return getMembers().stream()
                .map(executor)
                .flatMap(Optional::stream)
                .toList();
    }

    /**
     * 执行第一个支持的成员（支持检查 + 短路策略）。
     * <p>适用于条件匹配场景，先检查是否支持，再执行第一个支持的成员。</p>
     *
     * @param supporter 支持检查函数，判断成员是否支持当前操作
     * @param executor  执行函数，执行支持的成员
     * @param <R>       返回类型
     * @return 第一个支持的成员的执行结果，如果没有支持的成员则返回 empty
     */
    public <R> Optional<R> executeFirstSupported(
            Function<T, Boolean> supporter,
            Function<T, R> executor) {
        Objects.requireNonNull(supporter, "支持检查函数不能为 null");
        Objects.requireNonNull(executor, "执行函数不能为 null");
        return getMembers().stream()
                .filter(supporter::apply)
                .findFirst()
                .map(executor);
    }

    /**
     * 执行所有支持的成员（支持检查 + 全执行策略）。
     * <p>适用于需要执行所有匹配成员的场景。</p>
     *
     * @param supporter 支持检查函数，判断成员是否支持当前操作
     * @param executor  执行函数，执行支持的成员
     * @param <R>       返回类型
     * @return 所有支持的成员的执行结果列表
     */
    public <R> List<R> executeAllSupported(
            Function<T, Boolean> supporter,
            Function<T, R> executor) {
        Objects.requireNonNull(supporter, "支持检查函数不能为 null");
        Objects.requireNonNull(executor, "执行函数不能为 null");
        return getMembers().stream()
                .filter(supporter::apply)
                .map(executor)
                .toList();
    }
}
