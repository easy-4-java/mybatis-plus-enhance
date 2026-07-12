# mybatis-plus-enhance-crypto

提供字段透明加解密、HMAC、表级签名验签、原始密文 Mapper 与 SQL Injector。

写入拦截器顺序必须为“加密后签名”；读取顺序必须为“验签后解密”。
密钥和 IV 必须由应用从 KMS 或受保护配置中提供。
