package com.baomidou.mybatisplus.enhance.crypto.handler;

/**
 * 单字段加密、解密与 HMAC 运算端口。
 *
 * <p>业务可以实现该接口接入 KMS、硬件密码机或自定义密钥管理方案。实现不得在日志中
 * 输出明文、密钥、初始化向量或完整密文。</p>
 */
public interface EncryptedFieldHandler {

    /**
     * 将字段值序列化并加密为可持久化字符串。
     *
     * @param value 待加密字段的值
     * @param <T>   字段类型
     * @return 加密后的字符串
     */
    <T> String encrypt(T value);

    /**
     * 解密持久化字符串并转换为目标类型。
     *
     * @param value  待解密字段的值
     * @param rtType 解密结果类型
     * @param <T>    字段类型
     * @return 解密后的字段值
     */
    <T> T decrypt(String value, Class<T> rtType);

    /**
     * 计算字段值的 HMAC 签名。
     *
     * @param value 待签名的值
     * @param <T>   字段类型
     * @return 签名后的字符串
     */
    <T> String hmac(T value);

}
