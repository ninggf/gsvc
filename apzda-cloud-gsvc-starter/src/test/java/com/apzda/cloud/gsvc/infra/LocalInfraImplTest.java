package com.apzda.cloud.gsvc.infra;

import lombok.val;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author fengz (windywany@gmail.com)
 * @version 1.0.0
 * @since 1.0.0
 **/
class LocalInfraImplTest {

    private static final LocalInfraImpl LOCAL_INFRA_IMPL = new LocalInfraImpl(Duration.ofDays(1));

    @Test
    void count() throws InterruptedException {
        // given
        val countDownLatch = new CountDownLatch(10);

        // when
        for (var i = 0; i < 10; i++) {
            new Thread(() -> {
                LOCAL_INFRA_IMPL.count("abc", 3);
                countDownLatch.countDown();
            }).start();
        }
        countDownLatch.await();
        val count = LOCAL_INFRA_IMPL.count("abc", 3);
        // then
        if (count != 1) {
            assertThat(count).isEqualTo(11);
        }
    }

    @Test
    void storage_should_be_worked() throws Exception {
        // given
        val data = new TestData();
        data.setAge(18);
        data.setName("Leo Ning");
        // when
        val saved = LOCAL_INFRA_IMPL.save("biz1.leo_ning", data);
        // then
        assertThat(saved).isNotNull();
        assertThat(saved).isSameAs(data);

        // when
        val tData = LOCAL_INFRA_IMPL.load("biz1.leo_ning", TestData.class);
        // then
        assertThat(tData).isPresent();
        assertThat(tData.get().getAge()).isEqualTo(18);

        // when
        TimeUnit.SECONDS.sleep(10);
        val tData2 = LOCAL_INFRA_IMPL.load("biz1.leo_ning", TestData.class);

        // then
        assertThat(tData2).isNotPresent();
    }

}
