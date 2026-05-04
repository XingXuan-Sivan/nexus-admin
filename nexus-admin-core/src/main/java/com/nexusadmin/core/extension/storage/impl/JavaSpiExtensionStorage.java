package com.nexusadmin.core.extension.storage.impl;

import com.nexusadmin.core.extension.ExtensionMetadata;
import com.nexusadmin.core.extension.storage.ExtensionStorage;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

/**
 * 兼容 Java SPI 机制的扩展存储实现。
 * <p>从类路径下的 META-INF/services/ 目录读取扩展实现类名列表，兼容标准 Java SPI 格式。</p>
 * <p>该实现允许逐步迁移旧的 SPI 实现到新的 ExtensionPoint 体系，无需立即修改所有插件。</p>
 */
public class JavaSpiExtensionStorage implements ExtensionStorage {

    /**
     * Java SPI 服务目录前缀。
     */
    public static final String SERVICES_PREFIX = "META-INF/services/";

    @Override
    public List<ExtensionMetadata> loadExtensions(ClassLoader classLoader) {
        // 注意：Java SPI 需要知道具体的接口名才能加载
        // 此实现仅扫描 META-INF/services 目录结构，实际使用时需要配合其他机制
        // 或者通过特定约定来识别哪些接口是扩展点
        return new ArrayList<>();
    }

    /**
     * 加载指定接口类型的 SPI 实现。
     *
     * @param classLoader 类加载器
     * @param spiType     SPI 接口类型
     * @return 扩展元数据列表
     */
    public List<ExtensionMetadata> loadExtensions(ClassLoader classLoader, Class<?> spiType) {
        List<ExtensionMetadata> result = new ArrayList<>();
        String resourceName = SERVICES_PREFIX + spiType.getName();

        try {
            Enumeration<URL> resources = classLoader.getResources(resourceName);
            while (resources.hasMoreElements()) {
                URL url = resources.nextElement();
                result.addAll(loadFromUrl(url));
            }
        } catch (IOException e) {
            // 读取失败时返回空列表
        }

        return result;
    }

    /**
     * 从指定 URL 加载扩展类名列表。
     *
     * @param url SPI 配置文件 URL
     * @return 扩展元数据列表
     */
    private List<ExtensionMetadata> loadFromUrl(URL url) {
        List<ExtensionMetadata> result = new ArrayList<>();

        try (InputStream is = url.openStream();
             BufferedReader reader = new BufferedReader(
                     new InputStreamReader(is, StandardCharsets.UTF_8))) {

            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                // 忽略空行和注释行
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }
                result.add(new ExtensionMetadata(line, ""));
            }
        } catch (IOException e) {
            // 单个文件读取失败不影响整体流程
        }

        return result;
    }
}
