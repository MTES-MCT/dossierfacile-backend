package fr.dossierfacile.common.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class FeatureFlagServiceImplHashTest {

    @InjectMocks
    private FeatureFlagServiceImpl featureFlagService;

    @Test
    void should_return_positive_bucket_even_with_negative_hash_byte() {
        // GIVEN
        // 0x80000000 was causing Integer.MIN_VALUE issue
        byte[] hash = new byte[]{(byte) 0x80, 0, 0, 0};

        // WHEN
        int bucket = featureFlagService.getBucketFromHash(hash);

        // THEN
        // (0x80 & 0x7F) is 0
        assertThat(bucket).isNotNegative().isZero();
    }

    @Test
    void should_return_correct_bucket_for_normal_hashes() {
        // GIVEN
        byte[] hash = new byte[]{0, 0, 0, 50};

        // WHEN
        int bucket = featureFlagService.getBucketFromHash(hash);

        // THEN
        assertThat(bucket).isEqualTo(50);
    }
}
