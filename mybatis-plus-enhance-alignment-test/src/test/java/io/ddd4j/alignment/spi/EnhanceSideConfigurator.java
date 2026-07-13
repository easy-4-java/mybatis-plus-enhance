package io.ddd4j.alignment.spi;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;

/**
 * non-Plus 端注解反射实现：访问 mybatis-enhance-annotation 3.0.x 中的
 * {@code @EncryptedField} / {@code @IgnoreEncrypted} / {@code @TableSignatureField}。
 *
 * <p>通过 {@code Class.forName()} 反射加载（不直接 import），避免与
 * 旧路径 2.0.x annotation 编译期类路径冲突。</p>
 */
public class EnhanceSideConfigurator implements SharedEntityConfigurator {

    private static final String PREFIX = "org.apache.ibatis.enhance.annotation.crypto.";

    private final Class<? extends Annotation> tableSignatureFieldClass;
    private final Class<? extends Annotation> encryptedFieldClass;
    private final Class<? extends Annotation> ignoreEncryptedClass;
    private final Class<?> entityClass;

    public EnhanceSideConfigurator(Class<?> entityClass) {
        this.entityClass = entityClass;
        this.tableSignatureFieldClass = loadAnno(PREFIX + "TableSignatureField");
        this.encryptedFieldClass = loadAnno(PREFIX + "EncryptedField");
        this.ignoreEncryptedClass = loadAnno(PREFIX + "IgnoreEncrypted");
    }

    @SuppressWarnings("unchecked")
    private static Class<? extends Annotation> loadAnno(String name) {
        try {
            return (Class<? extends Annotation>) Class.forName(name, false, EnhanceSideConfigurator.class.getClassLoader());
        } catch (ClassNotFoundException e) {
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
        return "enhance";
    }
}
