package com.nexusadmin.core.registry;

import java.util.List;

/**
 * 组合器接口：将多个组件组合成一个统一视图。
 * <p>所有 Composite 实现类应实现此接口，而非直接硬编码组合逻辑。</p>
 * <p>通过实现此接口，组件可以透明地以单个组件或组合形式参与系统运行。</p>
 *
 * @param <T> 被组合的组件类型，必须继承 {@link Composable}
 */
public interface Composite<T extends Composable> extends Composable {

    /**
     * 获取当前组合的所有成员。
     *
     * @return 成员列表，不会返回 null
     */
    List<T> getMembers();

    /**
     * 添加成员到组合。
     *
     * @param member 要添加的成员，不能为 null
     * @throws NullPointerException 如果 member 为 null
     */
    void addMember(T member);

    /**
     * 从组合中移除成员。
     *
     * @param member 要移除的成员
     */
    void removeMember(T member);

    /**
     * 检查是否包含某成员。
     *
     * @param member 要检查的成员
     * @return 如果包含该成员返回 true
     */
    boolean containsMember(T member);

    /**
     * 获取成员数量。
     *
     * @return 当前组合中的成员数量
     */
    int memberCount();
}
