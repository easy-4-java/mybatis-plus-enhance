package io.ddd4j.alignment.spi;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;

/**
 * Plus 端注解反射实现：访问 Plus 端 {@code mybatis-enhance-annotation} 2.0.x 中的
 * {@code @EncryptedField} / {@code @IgnoreEncrypted} / {@code @TableSignatureField}。
 *
 * <p>由于 Plus 端 2.0.x annotation jar 的包路径在远端仓库且不与 mybatis-enhance 3.0.x
 * 共存，本实现通过 {@code Class.forName()} 反射加载目标类，避免编译期硬依赖。</p>
 */
public class PlusSideConfigurator implements SharedEntityConfigurator {

    private static final String PREFIX = "org.apache.mybatis.enhance.annotation.crypto.";

    private final Class<? extends Annotation> tableSignatureFieldClass;
    private final Class<? extends Annotation> encryptedFieldClass;
    private final Class<? extends Annotation> ignoreEncryptedClass;
    private final Class<?> entityClass;

    public PlusSideConfigurator(Class<?> entityClass) {
        this.entityClass = entityClass;
        this.tableSignatureFieldClass = loadAnno(PREFIX + "TableSignatureField");
        this.encryptedFieldClass = loadAnno(PREFIX + "EncryptedField");
        this.ignoreEncryptedClass = loadAnno(PREFIX + "IgnoreEncrypted");
    }

    @SuppressWarnings("unchecked")
    private static Class<? extends Annotation> loadAnno(String name) {
        try {
            return (Class<? extends Annotation>) Class.forName(name, false, PlusSideConfigurator.class.getClassLoader());
        } catch (ClassNotFoundException e) {
            // 旧路径 annotation 不在 classpath（mybatis-enhance-annotation 2.0.x 在远端），
            // 用 Object 占位，所有查询都返回 null（语义：未标注）
            return null;
        }
    }

    @Override
    public Integer signatureOrder(String fieldName) {
        if (tableSignatureFieldClass == null) return null;
        try {
            Field f = entityClass.getDeclaredField(fieldName);
            Annotation anno = f.getAnnotation(tableSignatureFieldClass);
            if (anno == null) return null;
            return (Integer) tableSignatureFieldClass.getMethod("order").invoke(anno);
        } catch (ReflectiveOperationException e) {
            return null;
        }
    }

    @Override
    public Boolean isEncrypted(String fieldName) {
        if (encryptedFieldClass == null) return false;
        try {
            Field f = entityClass.getDeclaredField(fieldName);
            return f.getAnnotation(encryptedFieldClass) != null;
        } catch (NoSuchFieldException e) {
            return false;
        }
    }

    @Override
    public Boolean isIgnoreDecrypted(String fieldName) {
        if (ignoreEncryptedClass == null) return false;
        try {
            Field f = entityClass.getDeclaredField(fieldName);
            return f.getAnnotation(ignoreEncryptedClass) != null;
        } catch (NoSuchFieldException e) {
            return false;
        }
    }

    @Override
    public String side() {
        return "plus";
    }
}
