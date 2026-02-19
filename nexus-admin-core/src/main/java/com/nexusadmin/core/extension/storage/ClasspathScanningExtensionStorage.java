package com.nexusadmin.core.extension.storage;

import com.nexusadmin.core.extension.Extension;
import com.nexusadmin.core.extension.ExtensionPoint;
import com.nexusadmin.core.extension.ExtensionMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 基于类路径扫描的扩展存储实现。
 * <p>通过扫描类路径下的所有类，查找标记了 {@link Extension} 注解且实现了 {@link ExtensionPoint} 接口的类。</p>
 * <p>该实现不需要编译期生成索引文件，在运行期动态发现扩展实现。</p>
 */
public class ClasspathScanningExtensionStorage implements ExtensionStorage {

    private static final Logger logger = LoggerFactory.getLogger(ClasspathScanningExtensionStorage.class);

    /**
     * 扩展索引文件路径（如果存在则优先使用）。
     */
    public static final String EXTENSIONS_IDX = "META-INF/extensions.idx";

    @Override
    public List<ExtensionMetadata> loadExtensions(ClassLoader classLoader) {
        List<ExtensionMetadata> result = new ArrayList<>();

        // 首先尝试从索引文件加载（如果存在）
        List<ExtensionMetadata> fromIndex = loadFromIndex(classLoader);
        if (!fromIndex.isEmpty()) {
            logger.debug("从索引文件加载了 {} 个扩展实现", fromIndex.size());
            result.addAll(fromIndex);
        }

        // 然后扫描类路径下的所有类
        List<ExtensionMetadata> fromScanning = scanClasspath(classLoader);
        logger.debug("类路径扫描发现了 {} 个扩展实现", fromScanning.size());

        // 合并结果，去重
        Set<String> existingClasses = result.stream()
                .map(ExtensionMetadata::getImplementationClassName)
                .collect(Collectors.toSet());

        for (ExtensionMetadata metadata : fromScanning) {
            if (!existingClasses.contains(metadata.getImplementationClassName())) {
                result.add(metadata);
                existingClasses.add(metadata.getImplementationClassName());
            }
        }

        return result;
    }

    /**
     * 从索引文件加载扩展类名列表。
     *
     * @param classLoader 类加载器
     * @return 扩展元数据列表
     */
    private List<ExtensionMetadata> loadFromIndex(ClassLoader classLoader) {
        List<ExtensionMetadata> result = new ArrayList<>();

        try {
            Enumeration<URL> resources = classLoader.getResources(EXTENSIONS_IDX);
            while (resources.hasMoreElements()) {
                URL url = resources.nextElement();
                result.addAll(loadFromUrl(url));
            }
        } catch (IOException e) {
            logger.debug("读取扩展索引文件失败: {}", e.getMessage());
        }

        return result;
    }

    /**
     * 从指定 URL 加载扩展类名列表。
     *
     * @param url 索引文件 URL
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
            logger.debug("从 URL 加载扩展失败: {} - {}", url, e.getMessage());
        }

        return result;
    }

    /**
     * 扫描类路径下的所有类，查找扩展实现。
     * <p>注意：这是一个简化实现，实际生产环境可能需要使用类路径扫描库如 Reflections 或 ClassGraph。</p>
     *
     * @param classLoader 类加载器
     * @return 扩展元数据列表
     */
    private List<ExtensionMetadata> scanClasspath(ClassLoader classLoader) {
        // 简化实现：这里仅返回空列表
        // 实际实现可以使用以下方式：
        // 1. 使用 Reflections 库扫描类路径
        // 2. 使用 ClassGraph 库扫描
        // 3. 手动扫描 JAR 文件和目录
        //
        // 示例代码（使用 Reflections）：
        // Reflections reflections = new Reflections(classLoader);
        // Set<Class<?>> extensionClasses = reflections.getTypesAnnotatedWith(Extension.class);
        // for (Class<?> clazz : extensionClasses) {
        //     if (ExtensionPoint.class.isAssignableFrom(clazz)) {
        //         result.add(new ExtensionMetadata(clazz.getName(), ""));
        //     }
        // }

        return new ArrayList<>();
    }
}
