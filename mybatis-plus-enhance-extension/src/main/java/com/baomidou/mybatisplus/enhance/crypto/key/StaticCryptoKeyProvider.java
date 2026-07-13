package com.baomidou.mybatisplus.enhance.crypto.key;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * 适用于本地配置和测试的静态密钥提供者。
 */
public class StaticCryptoKeyProvider implements CryptoKeyProvider {

    private final CryptoKeyMaterial currentKey;
    private final Map<String, CryptoKeyMaterial> keys;

    /**
     * 创建包含当前密钥和历史密钥的提供者。
     *
     * @param currentKey 当前写入密钥
     * @param historicalKeys 可用于解密旧数据的历史密钥
     */
    public StaticCryptoKeyProvider(CryptoKeyMaterial currentKey,
                                   Collection<CryptoKeyMaterial> historicalKeys) {
        this.currentKey = Objects.requireNonNull(currentKey, "currentKey must not be null");
        Map<String, CryptoKeyMaterial> configuredKeys = new LinkedHashMap<>();
        configuredKeys.put(currentKey.getKeyId(), currentKey);
        if (Objects.nonNull(historicalKeys)) {
            for (CryptoKeyMaterial historicalKey : historicalKeys) {
                CryptoKeyMaterial key = Objects.requireNonNull(historicalKey,
                        "historical key must not be null");
                configuredKeys.put(key.getKeyId(), key);
            }
        }
        this.keys = java.util.Collections.unmodifiableMap(configuredKeys);
    }

    public StaticCryptoKeyProvider(CryptoKeyMaterial currentKey) {
        this(currentKey, java.util.Collections.emptyList());
    }

    @Override
    public CryptoKeyMaterial currentKey() {
        return currentKey;
    }

    @Override
    public Optional<CryptoKeyMaterial> findKey(String keyId) {
        return Optional.ofNullable(keys.get(keyId));
    }
}
