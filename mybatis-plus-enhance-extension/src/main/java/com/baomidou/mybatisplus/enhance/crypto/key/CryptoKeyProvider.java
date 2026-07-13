package com.baomidou.mybatisplus.enhance.crypto.key;

import java.util.Optional;

/**
 * 密码密钥解析端口。
 *
 * <p>生产实现可以对接 KMS、HSM 或密钥中心。加密使用当前密钥，解密和验签根据密文或签名
 * 中的 keyId 查找当前或历史密钥。</p>
 */
public interface CryptoKeyProvider {

    /** @return 当前写入使用的密钥材料 */
    CryptoKeyMaterial currentKey();

    /**
     * 按版本查找密钥材料。
     *
     * @param keyId 密钥版本标识
     * @return 当前或历史密钥
     */
    Optional<CryptoKeyMaterial> findKey(String keyId);
}
