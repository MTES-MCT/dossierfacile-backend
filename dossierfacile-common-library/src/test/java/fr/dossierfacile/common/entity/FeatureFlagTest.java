package fr.dossierfacile.common.entity;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class FeatureFlagTest {

    @Test
    void optedInPartnerNames_isEmptyForNullOrBlank() {
        assertThat(FeatureFlag.builder().optedInPartners(null).build().getOptedInPartnerNames()).isEmpty();
        assertThat(FeatureFlag.builder().optedInPartners("  ").build().getOptedInPartnerNames()).isEmpty();
    }

    @Test
    void optedInPartnerNames_trimsSkipsBlanksAndDeduplicatesKeepingOrder() {
        FeatureFlag flag = FeatureFlag.builder().optedInPartners(" b , a ,, b ,").build();

        assertThat(flag.getOptedInPartnerNames()).containsExactly("b", "a");
    }

    @Test
    void joinPartnerNames_normalizes() {
        assertThat(FeatureFlag.joinPartnerNames(null)).isNull();
        assertThat(FeatureFlag.joinPartnerNames(List.of())).isEmpty();
        assertThat(FeatureFlag.joinPartnerNames(Arrays.asList(" a ", null, "b", "a", ""))).isEqualTo("a,b");
    }
}
