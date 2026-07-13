package io.ddd4j.alignment.spi;

/**
 * 对齐测试 SPI：暴露给两个框架的"统一字段契约"。
 *
 * <p>实现方（Plus 端 / non-Plus 端）通过反射各自的注解类
 * （{@code @TableSignatureField} 在两个框架中包路径不同）来回答这些方法。</p>
 *
 * <p>语义对应：</p>
 * <ul>
 *   <li>{@link #signatureOrder(String)} 返回 {@code @TableSignatureField.order} 值（null = 不参与签名）</li>
 *   <li>{@link #isEncrypted(String)} 返回 {@code @EncryptedField} 是否标注（null = 不加密）</li>
 *   <li>{@link #isIgnoreDecrypted(String)} 返回 {@code @IgnoreEncrypted} 是否标注（null = 不跳过解密）</li>
 * </ul>
 */
public interface SharedEntityConfigurator {

    Integer signatureOrder(String fieldName);

    Boolean isEncrypted(String fieldName);

    Boolean isIgnoreDecrypted(String fieldName);

    /**
     * 框架侧标识：返回 "plus" 或 "enhance"，便于测试日志输出。
     */
    String side();
}
