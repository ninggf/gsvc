package com.apzda.cloud.boot.sanitize;

import com.apzda.cloud.boot.vo.TestVo;
import lombok.val;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author fengz (windywany@gmail.com)
 * @version 1.0.0
 * @since 1.0.0
 **/
class SanitizeUtilsTest {

    @Test
    void getSanitizers() {
        val sanitizer = SanitizeUtils.getSanitizer(RegexpSanitizer.class);
        assertThat(sanitizer).isNotNull();
    }

    @Test
    void sanitize() {
        // given
        TestVo testVo = new TestVo();
        testVo.setPhone("13166666666");
        testVo.setPhone1("13166666666");
        // when
        val sanitized = SanitizeUtils.sanitize(testVo);
        // then
        assertThat(sanitized).isNotNull();
        assertThat(sanitized.getPhone()).isEqualTo("131****6666");
        assertThat(sanitized.getPhone1()).isEqualTo("131****6666");
    }

}
