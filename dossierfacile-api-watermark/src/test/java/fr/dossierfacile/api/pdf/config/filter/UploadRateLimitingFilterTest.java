package fr.dossierfacile.api.pdf.config.filter;

import fr.dossierfacile.common.config.filter.AbstractDownloadRateLimitingFilter;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class UploadRateLimitingFilterTest {

    @Test
    void filter_should_extend_abstract_rate_limiting_filter() {
        assertThat(UploadRateLimitingFilter.class.getSuperclass())
                .isEqualTo(AbstractDownloadRateLimitingFilter.class);
    }
}
