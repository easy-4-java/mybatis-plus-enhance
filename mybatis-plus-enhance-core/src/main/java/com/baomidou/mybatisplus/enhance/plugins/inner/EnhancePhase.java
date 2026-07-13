package com.baomidou.mybatisplus.enhance.plugins.inner;

/**
 * MyBatis-Plus 增强拦截器阶段。
 *
 * <p>阶段顺序同时约束写入前处理和查询后处理：参数先加密再签名，查询结果先验签再解密，
 * 解密后才能执行国际化，观测通知最后执行。未声明阶段的自定义增强不参与强制排序。</p>
 */
public enum EnhancePhase {

    /** SQL 结构改写或前置保护。 */
    SQL_REWRITE(100),

    /** 写入参数加密。 */
    PARAMETER_ENCRYPTION(200),

    /** 写入签名及查询结果验签。 */
    DATA_SIGNATURE(300),

    /** 查询结果解密。 */
    RESULT_DECRYPTION(400),

    /** 查询结果国际化。 */
    RESULT_I18N(500),

    /** SQL 执行观测与旁路通知。 */
    OBSERVATION(900),

    /** 不参与框架顺序校验的自定义阶段。 */
    UNSPECIFIED(Integer.MIN_VALUE);

    private final int order;

    EnhancePhase(int order) {
        this.order = order;
    }

    /**
     * 获取阶段排序值。
     *
     * @return 数值越小越先执行
     */
    public int getOrder() {
        return order;
    }
}
