package com.apzda.cloud.boot.sanitize;

import lombok.val;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author fengz (windywany@gmail.com)
 * @version 1.0.0
 * @since 1.0.0
 **/
class RegexpSanitizerTest {

    @Test
    void sanitize() {
        // given
        String phone = "13166666666";
        String phone1 = "+86 13166666666";
        String phone2 = "+886 13166666666";
        String[] configure = new String[] { "^((\\+\\d{2,4}\\s+)?\\d{3})\\d{4}(\\d{4})$", "$1****$3" };

        val sanitizer = new RegexpSanitizer();
        // when
        val sanitized = sanitizer.sanitize(phone, configure);
        val sanitized1 = sanitizer.sanitize(phone1, configure);
        val sanitized2 = sanitizer.sanitize(phone2, configure);
        // then
        assertThat(sanitized).isEqualTo("131****6666");
        assertThat(sanitized1).isEqualTo("+86 131****6666");
        assertThat(sanitized2).isEqualTo("+886 131****6666");
    }

}
