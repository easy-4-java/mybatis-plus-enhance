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
     * 查询
     */
    SELECT_IGNORE_DECRYPT_BY_ID("selectIgnoreDecryptById", "根据ID 查询一条数据", "SELECT %s FROM %s WHERE %s=#{%s} %s"),
    SELECT_IGNORE_DECRYPT_BATCH_BY_IDS("selectIgnoreDecryptBatchIds", "根据ID集合，批量查询数据", "<script>SELECT %s FROM %s WHERE %s IN (%s) %s </script>"),
    SELECT_IGNORE_DECRYPT_LIST("selectIgnoreDecryptList", "查询满足条件所有数据", "<script>%s SELECT %s FROM %s %s %s %s\n</script>"),
    SELECT_IGNORE_DECRYPT_MAPS("selectIgnoreDecryptMaps", "查询满足条件所有数据", "<script>%s SELECT %s FROM %s %s %s %s\n</script>"),
    SELECT_IGNORE_DECRYPT_OBJS("selectIgnoreDecryptObjs", "查询满足条件所有数据", "<script>%s SELECT %s FROM %s %s %s %s\n</script>");

    private final String method;
    private final String desc;
    private final String sql;

    EnhanceSqlMethod(String method, String desc, String sql) {
        this.method = method;
        this.desc = desc;
        this.sql = sql;
    }

}
