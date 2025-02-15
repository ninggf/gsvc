package com.apzda.cloud.gsvc.infra;

import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author fengz (windywany@gmail.com)
 * @version 1.0.0
 * @since 1.0.0
 **/
@Slf4j
class LocalInfraImplTest {

    private static final LocalInfraImpl LOCAL_INFRA_IMPL = new LocalInfraImpl(Duration.ofDays(1));

    @Test
    void underscore2camel() {
        // given
        String abc = "abc_def_ghi";
        // when

        val newStr = Pattern.compile("_([a-z])")
            .matcher(abc)
            .replaceAll((matchResult -> matchResult.group(1).toUpperCase()));

        // then
        assertThat(newStr).isEqualTo("abcDefGhi");

        // given
        String abc1 = "abcDefGhi_A";
        // when

        val newStr1 = Pattern.compile("_([a-z])")
            .matcher(abc1)
            .replaceAll((matchResult -> matchResult.group(1).toUpperCase()));

        // then
        assertThat(newStr1).isEqualTo("abcDefGhi_A");
    }

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
        TimeUnit.SECONDS.sleep(2);
        assertThat(LOCAL_INFRA_IMPL.getDuration("biz1.leo_ning").toSeconds()).isLessThan(5);
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

        val duration = LOCAL_INFRA_IMPL.getDuration("biz1.leo_ning");
        assertThat(duration.toSeconds()).isLessThanOrEqualTo(0);
    }

    @Test
    void lock_should_be_worked() throws Exception {
        // given
        val storage = LOCAL_INFRA_IMPL;

        // when
        val countDownLatch = new CountDownLatch(3);
        val error = new AtomicInteger();
        // when
        for (var i = 0; i < 3; i++) {
            new Thread(() -> {
                val testLock = storage.getLock("test_lock");
                try {
                    if (testLock.tryLock(20, TimeUnit.SECONDS)) {
                        log.info("done");
                    }
                    else {
                        throw new IllegalStateException("");
                    }
                    TimeUnit.MILLISECONDS.sleep(200);
                }
                catch (Exception e) {
                    error.incrementAndGet();
                }
                finally {
                    countDownLatch.countDown();
                    testLock.unlock();
                }
            }).start();
        }
        countDownLatch.await();
        // then
        assertThat(error.get()).isEqualTo(0);

        storage.deleteLock("test_lock");
    }

}
