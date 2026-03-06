package com.nexusadmin.core.context;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

/**
 * 插件运行时工作空间。
 *
 * <p>
 * Workspace 表示插件运行期间可使用的隔离目录，
 * 用于存储插件运行产生的配置、数据、缓存及日志等信息。
 * </p>
 *
 * <p>
 * Workspace 采用懒加载机制，只有在首次访问时才会创建物理目录，
 * 避免未使用插件产生无用的文件系统资源。
 * </p>
 *
 * <p>默认包含以下标准子目录：</p>
 * <ul>
 *   <li>config：插件配置文件</li>
 *   <li>data：插件运行数据</li>
 *   <li>cache：插件缓存</li>
 *   <li>logs：插件日志</li>
 * </ul>
 */
public final class PluginWorkspace {

    private final Path root;

    private volatile boolean initialized = false;

    /**
     * 构造插件工作空间。
     *
     * @param root workspace 根目录
     */
    public PluginWorkspace(Path root) {
        this.root = Objects.requireNonNull(root, "工作空间根目录不能为空");
    }

    /**
     * 返回 workspace 根目录。
     * <p>首次调用时触发目录初始化。</p>
     *
     * @return workspace 根目录路径
     */
    public Path root() {
        ensureInitialized();
        return root;
    }

    /**
     * 解析 workspace 下的相对路径。
     * <p>首次调用时触发目录初始化。</p>
     *
     * @param path 相对路径
     * @return 解析后的绝对路径
     */
    public Path resolve(String path) {
        ensureInitialized();
        return root.resolve(path);
    }

    /**
     * 返回配置目录（config）。
     * <p>首次调用时触发目录初始化。</p>
     *
     * @return 配置目录路径
     */
    public Path config() {
        ensureInitialized();
        return root.resolve("config");
    }

    /**
     * 返回数据目录（data）。
     * <p>首次调用时触发目录初始化。</p>
     *
     * @return 数据目录路径
     */
    public Path data() {
        ensureInitialized();
        return root.resolve("data");
    }

    /**
     * 返回缓存目录（cache）。
     * <p>首次调用时触发目录初始化。</p>
     *
     * @return 缓存目录路径
     */
    public Path cache() {
        ensureInitialized();
        return root.resolve("cache");
    }

    /**
     * 返回日志目录（logs）。
     * <p>首次调用时触发目录初始化。</p>
     *
     * @return 日志目录路径
     */
    public Path logs() {
        ensureInitialized();
        return root.resolve("logs");
    }

    /**
     * 检查 workspace 根目录是否已存在于文件系统。
     * <p>此方法不会触发目录初始化。</p>
     *
     * @return 如果根目录存在则返回 true
     */
    public boolean exists() {
        return Files.exists(root);
    }

    /**
     * 删除 workspace 目录及其下所有内容。
     * <p>如果 workspace 不存在则无操作。</p>
     *
     * @throws IOException 如果删除失败
     */
    public void delete() throws IOException {
        if (!Files.exists(root)) {
            return;
        }
        try (var stream = Files.walk(root)) {
            stream.sorted(java.util.Comparator.reverseOrder())
                    .forEach(path -> {
                        try {
                            Files.delete(path);
                        } catch (IOException e) {
                            throw new java.io.UncheckedIOException(e);
                        }
                    });
        }
    }

    /**
     * 懒加载初始化，使用 double-checked locking 保证线程安全。
     * <p>初始化时统一创建根目录和所有标准子目录。</p>
     */
    private void ensureInitialized() {
        if (initialized) {
            return;
        }

        synchronized (this) {
            if (initialized) {
                return;
            }

            try {
                Files.createDirectories(root);
                Files.createDirectories(root.resolve("config"));
                Files.createDirectories(root.resolve("data"));
                Files.createDirectories(root.resolve("cache"));
                Files.createDirectories(root.resolve("logs"));
            } catch (IOException e) {
                throw new RuntimeException("初始化插件工作空间失败: " + root, e);
            }

            initialized = true;
        }
    }
}
