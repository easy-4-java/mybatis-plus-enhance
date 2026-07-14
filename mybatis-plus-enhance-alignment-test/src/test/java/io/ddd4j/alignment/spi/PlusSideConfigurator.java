package io.ddd4j.alignment.spi;

import org.apache.ibatis.enhance.annotation.crypto.EncryptedField;
import org.apache.ibatis.enhance.annotation.crypto.IgnoreEncrypted;
import org.apache.ibatis.enhance.annotation.crypto.TableSignatureField;

import java.lang.reflect.Field;

/**
 * Plus 端注解反射实现：与 EnhanceSideConfigurator 完全一致（统一路径后）。
 */
public class PlusSideConfigurator implements SharedEntityConfigurator {

    private final Class<?> entityClass;

    public PlusSideConfigurator(Class<?> entityClass) {
        this.entityClass = entityClass;
    }

    @Override
    public Integer signatureOrder(String fieldName) {
        try {
            Field f = entityClass.getDeclaredField(fieldName);
            TableSignatureField anno = f.getAnnotation(TableSignatureField.class);
            return anno == null ? null : anno.order();
        } catch (NoSuchFieldException e) {
            return null;
        }
    }

    @Override
    public Boolean isEncrypted(String fieldName) {
        try {
            Field f = entityClass.getDeclaredField(fieldName);
            return f.getAnnotation(EncryptedField.class) != null;
        } catch (NoSuchFieldException e) {
            return false;
        }
    }

    @Override
    public Boolean isIgnoreDecrypted(String fieldName) {
        try {
            Field f = entityClass.getDeclaredField(fieldName);
            return f.getAnnotation(IgnoreEncrypted.class) != null;
        } catch (NoSuchFieldException e) {
            return false;
        }
    }

    @Override
    public String side() {
        return "plus";
    }
}
