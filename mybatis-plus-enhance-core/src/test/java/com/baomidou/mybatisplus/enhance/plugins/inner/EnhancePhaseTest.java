package com.baomidou.mybatisplus.enhance.plugins.inner;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link EnhancePhase}.
 *
 * @author <a href="https://github.com/loong10k">Loong Wan</a>
 */
@DisplayName("EnhancePhase")
class EnhancePhaseTest {

    @Test
    @DisplayName("should define phases in ascending order")
    void shouldDefinePhasesInAscendingOrder() {
        assertThat(EnhancePhase.SQL_REWRITE.getOrder()).isLessThan(EnhancePhase.PARAMETER_ENCRYPTION.getOrder());
        assertThat(EnhancePhase.PARAMETER_ENCRYPTION.getOrder()).isLessThan(EnhancePhase.DATA_SIGNATURE.getOrder());
        assertThat(EnhancePhase.DATA_SIGNATURE.getOrder()).isLessThan(EnhancePhase.RESULT_DECRYPTION.getOrder());
        assertThat(EnhancePhase.RESULT_DECRYPTION.getOrder()).isLessThan(EnhancePhase.RESULT_I18N.getOrder());
        assertThat(EnhancePhase.RESULT_I18N.getOrder()).isLessThan(EnhancePhase.RESULT_PROCESSING.getOrder());
        assertThat(EnhancePhase.RESULT_PROCESSING.getOrder()).isLessThan(EnhancePhase.OBSERVATION.getOrder());
    }

    @Test
    @DisplayName("should have UNSPECIFIED with minimum integer value")
    void shouldHaveUnspecifiedWithMinValue() {
        assertThat(EnhancePhase.UNSPECIFIED.getOrder()).isEqualTo(Integer.MIN_VALUE);
    }

    @Test
    @DisplayName("should have SQL_REWRITE at order 100")
    void shouldHaveSqlRewriteAt100() {
        assertThat(EnhancePhase.SQL_REWRITE.getOrder()).isEqualTo(100);
    }

    @Test
    @DisplayName("should have PARAMETER_ENCRYPTION at order 200")
    void shouldHaveParameterEncryptionAt200() {
        assertThat(EnhancePhase.PARAMETER_ENCRYPTION.getOrder()).isEqualTo(200);
    }

    @Test
    @DisplayName("should have DATA_SIGNATURE at order 300")
    void shouldHaveDataSignatureAt300() {
        assertThat(EnhancePhase.DATA_SIGNATURE.getOrder()).isEqualTo(300);
    }

    @Test
    @DisplayName("should have RESULT_DECRYPTION at order 400")
    void shouldHaveResultDecryptionAt400() {
        assertThat(EnhancePhase.RESULT_DECRYPTION.getOrder()).isEqualTo(400);
    }

    @Test
    @DisplayName("should have RESULT_I18N at order 500")
    void shouldHaveResultI18nAt500() {
        assertThat(EnhancePhase.RESULT_I18N.getOrder()).isEqualTo(500);
    }

    @Test
    @DisplayName("should have OBSERVATION at order 900")
    void shouldHaveObservationAt900() {
        assertThat(EnhancePhase.OBSERVATION.getOrder()).isEqualTo(900);
    }

    @Test
    @DisplayName("should have exactly eight enum constants")
    void shouldHaveEightConstants() {
        assertThat(EnhancePhase.values()).hasSize(8);
    }

    @Test
    @DisplayName("should resolve UNSPECIFIED by name")
    void shouldResolveUnspecifiedByName() {
        assertThat(EnhancePhase.valueOf("UNSPECIFIED")).isSameAs(EnhancePhase.UNSPECIFIED);
    }
}
