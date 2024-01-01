package com.apzda.cloud.gsvc.infra;

import com.apzda.TestApp;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.val;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.boot.test.autoconfigure.json.JsonTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author fengz (windywany@gmail.com)
 * @version 1.0.0
 * @since 1.0.0
 **/
@JsonTest
@ContextConfiguration(classes = TestApp.class)
@ImportAutoConfiguration({ RedisAutoConfiguration.class })
@TestPropertySource(properties = { "spring.data.redis.database=1" })
class RedisInfraImplTest {

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void count() throws InterruptedException {
        // given
        Counter counter = new RedisInfraImpl(stringRedisTemplate, objectMapper);
        val countDownLatch = new CountDownLatch(10);

        // when
        for (var i = 0; i < 10; i++) {
            new Thread(() -> {
                counter.count("abc", 5);
                countDownLatch.countDown();
            }).start();
        }
        countDownLatch.await();
        val count = counter.count("abc", 5);
        // then
        if (count != 1) {
            assertThat(count).isEqualTo(11);
        }
        // when
        TimeUnit.SECONDS.sleep(5);
        val count1 = counter.count("abc", 5);
        assertThat(count1).isEqualTo(1);
    }

    @Test
    void storage_should_be_worked() throws Exception {
        // given
        val storage = new RedisInfraImpl(stringRedisTemplate, objectMapper);
        val data = new TestData();
        data.setAge(18);
        data.setName("Leo Ning");
        // when
        val saved = storage.save("biz1.leo_ning", data);
        // then
        assertThat(saved).isNotNull();
        assertThat(saved).isSameAs(data);

        // when
        val tData = storage.load("biz1.leo_ning", TestData.class);
        // then
        assertThat(tData).isPresent();
        assertThat(tData.get().getAge()).isEqualTo(18);

        // when
        TimeUnit.SECONDS.sleep(6);
        val tData2 = storage.load("biz1.leo_ning", TestData.class);

        // then
        assertThat(tData2).isNotPresent();
    }

}
