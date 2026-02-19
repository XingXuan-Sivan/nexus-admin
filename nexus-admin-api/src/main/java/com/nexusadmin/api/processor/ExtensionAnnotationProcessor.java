package com.nexusadmin.api.processor;

import com.google.auto.service.AutoService;
import com.nexusadmin.api.extension.Extension;
import com.nexusadmin.api.extension.ExtensionPoint;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic;
import javax.tools.FileObject;
import javax.tools.StandardLocation;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.util.*;

/**
 * 扩展注解处理器。
 * <p>在编译期扫描所有 {@link Extension} 注解的类，自动生成扩展索引文件。</p>
 * <p>生成的索引文件位于 {@code META-INF/extensions.idx}，每行包含一个扩展实现类全限定名。</p>
 *
 * @author NexusAdmin
 * @since 1.0.0
 */
@AutoService(Processor.class)
@SupportedAnnotationTypes("com.nexusadmin.api.extension.Extension")
@SupportedSourceVersion(SourceVersion.RELEASE_17)
public class ExtensionAnnotationProcessor extends AbstractProcessor {

    /**
     * 扩展索引文件路径。
     */
    public static final String EXTENSIONS_IDX = "META-INF/extensions.idx";

    private final Set<String> extensionClasses = new HashSet<>();

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        if (!roundEnv.processingOver()) {
            // 收集所有标记了 @Extension 的类
            for (TypeElement annotation : annotations) {
                Set<? extends Element> elements = roundEnv.getElementsAnnotatedWith(annotation);
                for (Element element : elements) {
                    if (element instanceof TypeElement typeElement) {
                        processExtensionElement(typeElement);
                    }
                }
            }
        } else if (!extensionClasses.isEmpty()) {
            // 最后一轮处理时写入索引文件
            writeIndexFile();
        }

        return false;
    }

    /**
     * 处理单个扩展元素。
     *
     * @param typeElement 类型元素
     */
    private void processExtensionElement(TypeElement typeElement) {
        try {
            Extension extension = typeElement.getAnnotation(Extension.class);
            if (extension == null) {
                return;
            }

            // 检查是否启用了
            if (!extension.enabled()) {
                return;
            }

            // 验证是否实现了 ExtensionPoint 接口
            if (!implementsExtensionPoint(typeElement)) {
                processingEnv.getMessager().printMessage(
                        Diagnostic.Kind.ERROR,
                        "类 " + typeElement.getQualifiedName() + " 标记了 @Extension 但未实现 ExtensionPoint 接口",
                        typeElement);
                return;
            }

            String className = typeElement.getQualifiedName().toString();
            extensionClasses.add(className);

            processingEnv.getMessager().printMessage(
                    Diagnostic.Kind.NOTE,
                    "发现扩展实现: " + className,
                    typeElement);
        } catch (Exception e) {
            processingEnv.getMessager().printMessage(
                    Diagnostic.Kind.WARNING,
                    "处理扩展元素 " + typeElement.getQualifiedName() + " 时出错: " + e.getMessage(),
                    typeElement);
        }
    }

    /**
     * 检查类型是否实现了 ExtensionPoint 接口。
     *
     * @param typeElement 类型元素
     * @return 是否实现了 ExtensionPoint
     */
    private boolean implementsExtensionPoint(TypeElement typeElement) {
        Types typeUtils = processingEnv.getTypeUtils();

        // 获取 ExtensionPoint 接口的 TypeMirror
        TypeElement extensionPointElement = processingEnv.getElementUtils()
                .getTypeElement(ExtensionPoint.class.getCanonicalName());
        if (extensionPointElement == null) {
            return false;
        }

        TypeMirror extensionPointMirror = typeUtils.erasure(extensionPointElement.asType());

        // 检查当前类型及其所有父接口
        try {
            return checkImplementsInterface(typeUtils.erasure(typeElement.asType()),
                    extensionPointMirror, typeUtils, new HashSet<>());
        } catch (Exception e) {
            processingEnv.getMessager().printMessage(
                    Diagnostic.Kind.WARNING,
                    "检查类型 " + typeElement.getQualifiedName() + " 是否实现 ExtensionPoint 时出错: " + e.getMessage(),
                    typeElement);
            return false;
        }
    }

    /**
     * 递归检查类型是否实现了指定接口。
     *
     * @param type          待检查类型
     * @param targetInterface 目标接口
     * @param typeUtils     类型工具
     * @param visited       已访问类型集合（防止循环）
     * @return 是否实现了目标接口
     */
    private boolean checkImplementsInterface(TypeMirror type,
                                             TypeMirror targetInterface,
                                             Types typeUtils,
                                             Set<TypeMirror> visited) {
        if (type == null || targetInterface == null) {
            return false;
        }

        if (visited.contains(type)) {
            return false;
        }
        visited.add(type);

        try {
            // 使用类型擦除后的类型进行比较，避免泛型问题
            TypeMirror erasedType = typeUtils.erasure(type);
            TypeMirror erasedTarget = typeUtils.erasure(targetInterface);

            // 直接检查是否是目标接口的子类型
            if (typeUtils.isSameType(erasedType, erasedTarget)) {
                return true;
            }

            if (typeUtils.isSubtype(erasedType, erasedTarget)) {
                return true;
            }
        } catch (Exception e) {
            // 类型检查可能因为类型不完整而失败，继续检查父类型
        }

        // 获取类型的父类型和接口
        try {
            List<? extends TypeMirror> directSupertypes = typeUtils.directSupertypes(type);
            for (TypeMirror supertype : directSupertypes) {
                if (checkImplementsInterface(supertype, targetInterface, typeUtils, visited)) {
                    return true;
                }
            }
        } catch (Exception e) {
            // 获取父类型失败，可能是类型不完整
        }

        return false;
    }

    /**
     * 写入扩展索引文件。
     */
    private void writeIndexFile() {
        try {
            FileObject fileObject = processingEnv.getFiler()
                    .createResource(StandardLocation.CLASS_OUTPUT, "", EXTENSIONS_IDX);

            try (Writer writer = fileObject.openWriter()) {
                writer.write("# 扩展索引文件\n");
                writer.write("# 由 ExtensionAnnotationProcessor 自动生成，请勿手动修改\n");
                writer.write("# 生成时间: " + new Date() + "\n\n");

                // 按类名排序，保证输出稳定
                List<String> sortedClasses = new ArrayList<>(extensionClasses);
                Collections.sort(sortedClasses);

                for (String className : sortedClasses) {
                    writer.write(className);
                    writer.write("\n");
                }
            }

            processingEnv.getMessager().printMessage(
                    Diagnostic.Kind.NOTE,
                    "扩展索引文件已生成: " + EXTENSIONS_IDX + "，共 " + extensionClasses.size() + " 个扩展实现");

        } catch (IOException e) {
            StringWriter sw = new StringWriter();
            e.printStackTrace(new PrintWriter(sw));
            processingEnv.getMessager().printMessage(
                    Diagnostic.Kind.ERROR,
                    "写入扩展索引文件失败: " + e.getMessage() + "\n" + sw);
        }
    }
}
