package com.baomidou.mybatisplus.enhance.enums;

import lombok.Getter;

/**
 * 由 {@code EnhanceSqlInjector} 注入的原始数据查询 SQL 模板。
 * <p>
 * 这些方法使用 {@code IgnoreEncrypted} 语义跳过查询结果解密，主要供表签名补签、
 * 验签以及密文运维场景使用，不建议直接暴露给业务层。
 */
@Getter
public enum EnhanceSqlMethod {

    /**
     * 按主键查询单个实体的原始持久化数据。
     */
    SELECT_IGNORE_DECRYPT_BY_ID("selectIgnoreDecryptById", "根据ID 查询一条数据", "SELECT %s FROM %s WHERE %s=#{%s} %s"),

    /**
     * 按主键集合批量查询实体原始持久化数据。
     */
    SELECT_IGNORE_DECRYPT_BATCH_BY_IDS("selectIgnoreDecryptBatchIds", "根据ID集合，批量查询数据", "<script>SELECT %s FROM %s WHERE %s IN (%s) %s </script>"),

    /**
     * 根据 Wrapper 条件查询实体原始持久化数据列表。
     */
    SELECT_IGNORE_DECRYPT_LIST("selectIgnoreDecryptList", "查询满足条件所有数据", "<script>%s SELECT %s FROM %s %s %s %s\n</script>"),

    /**
     * 根据 Wrapper 条件查询 Map 形式的原始持久化数据列表。
     */
    SELECT_IGNORE_DECRYPT_MAPS("selectIgnoreDecryptMaps", "查询满足条件所有数据", "<script>%s SELECT %s FROM %s %s %s %s\n</script>"),

    /**
     * 根据 Wrapper 条件查询首列原始持久化值列表。
     */
    SELECT_IGNORE_DECRYPT_OBJS("selectIgnoreDecryptObjs", "查询满足条件所有数据", "<script>%s SELECT %s FROM %s %s %s %s\n</script>"),

    /**
     * 仅按主键更新表签名存储列，不改写其他业务列。
     */
    UPDATE_SIGNATURE_BY_ID("updateSignatureById", "根据ID仅更新表签名", "<script>UPDATE %s SET %s=#{et.%s} WHERE %s=#{et.%s} %s</script>");

    /**
     * 注入到 Mapper 的方法名。
     */
    private final String method;

    /**
     * 方法用途说明。
     */
    private final String desc;

    /**
     * 供 MyBatis-Plus SQL 注入器格式化的脚本模板。
     */
    private final String sql;

    /**
     * 创建增强 SQL 方法定义。
     *
     * @param method Mapper 方法名
     * @param desc   方法用途说明
     * @param sql    SQL 脚本模板
     */
    EnhanceSqlMethod(String method, String desc, String sql) {
        this.method = method;
        this.desc = desc;
        this.sql = sql;
    }

}
