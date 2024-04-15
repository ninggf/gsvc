package com.apzda.cloud.gsvc.utils;

import com.apzda.cloud.gsvc.autoconfigure.GsvcCoreAutoConfiguration;
import lombok.val;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.test.autoconfigure.json.JsonTest;
import org.springframework.test.context.TestPropertySource;

import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author fengz (windywany@gmail.com)
 * @version 1.0.0
 * @since 1.0.0
 **/
@JsonTest
@ImportAutoConfiguration(GsvcCoreAutoConfiguration.class)
@TestPropertySource(properties = "spring.messages.basename=messages-core")
class I18nHelperTest {

    @Test
    void trans_with_args_should_be_correct() {
        // given
        val zhCn = Locale.SIMPLIFIED_CHINESE;
        System.out.println("zhCn = " + zhCn.toLanguageTag());
        // when
        val msg = I18nUtils.t("{} login successfully", new Object[] { "admin" });
        val msgZhCN = I18nUtils.t("{} login successfully", new Object[] { "admin" }, zhCn);
        val errMsg = I18nUtils.t("{} login failure:    :{}", new Object[] { "admin", "test" });
        val errMsgZhCN = I18nUtils.t("{} login failure: {}", new Object[] { "admin", "test" }, zhCn);
        val okTxt = I18nUtils.t("hello.ok");
        // then
        assertThat(msg).isEqualTo("admin login successfully");
        assertThat(msgZhCN).isEqualTo("admin登录成功。");
        assertThat(errMsg).isEqualTo("admin login failed: test.");
        assertThat(errMsgZhCN).isEqualTo("admin登录失败, 原因如下: test。");
        assertThat(okTxt).isEqualTo("Hello is Ok.");
    }

}
