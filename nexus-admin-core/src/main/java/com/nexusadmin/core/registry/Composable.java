package com.nexusadmin.core.registry;

/**
 * 标记接口：表示该组件可以被注册中心管理。
 * <p>实现此接口的组件可以通过 {@link ComponentRegistry} 进行统一注册和配置。</p>
 * <p>所有具备多实现能力的系统核心组件（如 parser、resolver、reader、loader 等）
 * 均应实现此接口，以支持统一的注册中心管理。</p>
 *
 * <p>该接口不包含任何方法，仅作为类型标记使用。</p>
 *
 * @see Composite
 */
public interface Composable {
    // 标记接口，无需方法
}
