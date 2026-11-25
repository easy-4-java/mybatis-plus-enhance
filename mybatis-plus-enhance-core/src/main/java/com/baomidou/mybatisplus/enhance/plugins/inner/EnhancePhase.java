package com.baomidou.mybatisplus.enhance.plugins.inner;

/**
 * Enumeration of phases for MyBatis-Plus enhanced interceptors.
 *
 * <p>Phase ordering constrains both pre-write processing and post-query processing:
 * parameters are encrypted before signing, query results are verified before decryption,
 * decryption precedes internationalization, and observation notifications execute last.
 * Custom enhancements that do not declare a phase are excluded from mandatory ordering.</p>
 *
 * @author <a href="https://github.com/loong10k">Loong Wan</a>
 */
public enum EnhancePhase {

    /** SQL structure rewriting or pre-execution protection. */
    SQL_REWRITE(100),

    /** Write-parameter encryption. */
    PARAMETER_ENCRYPTION(200),

    /** Write-time signing and query-result signature verification. */
    DATA_SIGNATURE(300),

    /** Query-result decryption. */
    RESULT_DECRYPTION(400),

    /** Query-result internationalization. */
    RESULT_I18N(500),

    /** Application processing of results after decryption and internationalization. */
    RESULT_PROCESSING(600),

    /** SQL execution observation and sidecar notifications. */
    OBSERVATION(900),

    /** Custom phase excluded from framework ordering validation. */
    UNSPECIFIED(Integer.MIN_VALUE);

    private final int order;

    EnhancePhase(int order) {
        this.order = order;
    }

    /**
     * Returns the ordinal value of this phase.
     *
     * @return the ordering value; smaller values execute earlier
     */
    public int getOrder() {
        return order;
    }
}
