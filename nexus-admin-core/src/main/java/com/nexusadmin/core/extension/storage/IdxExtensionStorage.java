package com.nexusadmin.core.extension.storage;

import com.nexusadmin.core.extension.ExtensionMetadata;

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
 * 基于 META-INF/extensions.idx 文件的扩展存储实现。
 * <p>从类路径下的 META-INF/extensions.idx 文件中读取扩展实现类名列表。</p>
 * <p>索引文件格式：每行一个扩展实现类全限定名，忽略空行和注释行（以 # 开头）。</p>
 */
public class IdxExtensionStorage implements ExtensionStorage {

    /**
     * 扩展索引文件路径。
     */
    public static final String EXTENSIONS_IDX = "META-INF/extensions.idx";

    @Override
    public List<ExtensionMetadata> loadExtensions(ClassLoader classLoader) {
        List<ExtensionMetadata> result = new ArrayList<>();

        try {
            Enumeration<URL> resources = classLoader.getResources(EXTENSIONS_IDX);
            while (resources.hasMoreElements()) {
                URL url = resources.nextElement();
                result.addAll(loadFromUrl(url));
            }
        } catch (IOException e) {
            // 读取失败时返回空列表，不影响其他存储实现
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
            // 单个文件读取失败不影响整体流程
        }

        return result;
    }
}
