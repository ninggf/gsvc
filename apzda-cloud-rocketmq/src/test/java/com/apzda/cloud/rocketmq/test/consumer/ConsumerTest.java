/*
 * Copyright (C) 2023-2025 Fengz Ning (windywany@gmail.com)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.apzda.cloud.rocketmq.test.consumer;

import com.apzda.cloud.mybatis.autoconfigure.MyBatisPlusAutoConfiguration;
import com.apzda.cloud.rocketmq.autoconfigure.RocketMqAutoconfiguration;
import com.apzda.cloud.rocketmq.listener.IMessageListener;
import com.apzda.cloud.rocketmq.listener.ISendCallback;
import com.apzda.cloud.rocketmq.test.Tags;
import com.apzda.cloud.rocketmq.test.TestApp;
import com.apzda.cloud.rocketmq.test.message.DelayMessage;
import com.apzda.cloud.rocketmq.test.message.SimpleMessage;
import com.apzda.cloud.rocketmq.test.message.SyncMessage;
import com.apzda.cloud.rocketmq.test.producer.SimpleMessageProducer;
import com.baomidou.mybatisplus.test.autoconfigure.MybatisPlusTest;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apache.rocketmq.client.apis.ClientException;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.json.AutoConfigureJsonTesters;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.transaction.TestTransaction;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * @author fengz (windywany@gmail.com)
 * @version 3.4.0
 * @since 3.4.0
 **/
@Slf4j
@MybatisPlusTest
@ContextConfiguration(classes = TestApp.class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@AutoConfigureJsonTesters
@ImportAutoConfiguration({ MyBatisPlusAutoConfiguration.class, RocketMqAutoconfiguration.class })
@Sql("classpath:/mailbox.sql")
@Disabled
public class ConsumerTest {

    @Autowired
    @Qualifier("simpleMessageProducer")
    SimpleMessageProducer producer;

    @Autowired
    @Qualifier("directMessageProducer")
    SimpleMessageProducer directProducer;

    @Autowired
    @Qualifier("transMessageProducer")
    SimpleMessageProducer transProducer;

    @Autowired
    @Qualifier("delayMessageProducer")
    SimpleMessageProducer delayMessageProducer;

    @Autowired
    @Qualifier("failedMessageProducer")
    SimpleMessageProducer failedMessageProducer;

    @Autowired
    @MockitoSpyBean
    ISendCallback callback;

    @Autowired
    @MockitoSpyBean
    IMessageListener<SimpleMessage, Tags> delayListener;

    @Autowired
    @MockitoSpyBean
    IMessageListener<SimpleMessage, Tags> demoListener;

    @Test
    @DisplayName("通过信使发送普通消息")
    void testOk() throws InterruptedException {

        // given
        val simpleMessage = new SimpleMessage();
        simpleMessage.setContent("test");

        // when
        TestTransaction.flagForCommit();
        TestTransaction.end();
        producer.send(simpleMessage, Tags.TEST);

        TimeUnit.SECONDS.sleep(3);
        verify(callback, times(1)).onSuccess(any());
        verify(demoListener, times(1)).onMessage(any(), any(Tags.class), any());
    }

    @Test
    @DisplayName("通过信使发送普通消息应该抛异常")
    void testShouldFailed() throws InterruptedException, ClientException {
        // given
        val simpleMessage = new SimpleMessage();
        simpleMessage.setContent("test");

        // when
        TestTransaction.flagForCommit();
        TestTransaction.end();
        failedMessageProducer.send(simpleMessage, Tags.TEST);

        TimeUnit.SECONDS.sleep(10);
        verify(callback, times(2)).onError(any(), any());
    }

    @Test
    @DisplayName("直接发送普通消息")
    void sendSimpleMessageDirectlyOk() throws InterruptedException {

        // given
        val simpleMessage = new SimpleMessage();
        simpleMessage.setContent("direct");

        // when
        TestTransaction.flagForCommit();
        TestTransaction.end();
        directProducer.send(simpleMessage, Tags.TEST);

        TimeUnit.SECONDS.sleep(5);
        verify(callback, times(1)).onSuccess(any());
        verify(demoListener, times(1)).onMessage(any(), any(), any());
    }

    @Test
    @DisplayName("直接发送普通消息-限流器工作")
    void sendSimpleMessageDirectly2Ok() throws InterruptedException {

        // given
        val simpleMessage = new SyncMessage();
        simpleMessage.setContent("direct");

        // when
        TestTransaction.flagForCommit();
        TestTransaction.end();
        final CountDownLatch countDownLatch = new CountDownLatch(10);

        for (int i = 0; i < 10; i++) {
            CompletableFuture.runAsync(() -> {
                try {
                    directProducer.send(simpleMessage, Tags.TEST);
                }
                catch (Exception e) {
                    log.error(e.getMessage());
                }
                finally {
                    countDownLatch.countDown();
                }
            });
        }

        assertThat(countDownLatch.await(15, TimeUnit.SECONDS)).isTrue();

        TimeUnit.SECONDS.sleep(3);
        verify(demoListener, atLeast(6)).onMessage(any(), any(), any());
    }

    @Test()
    @DisplayName("直接发送普通消息应该抛异常")
    void sendSimpleMessageDirectlyFailed() {

        // given
        val simpleMessage = new SyncMessage();
        simpleMessage.setContent("direct");

        // when
        TestTransaction.flagForCommit();
        TestTransaction.end();
        assertThatThrownBy(() -> {
            failedMessageProducer.send(simpleMessage, Tags.TEST);
        }).hasMessageContaining("No topic route");
    }

    @Test
    @DisplayName("通过信使发送事务消息")
    @Timeout(value = 120, unit = TimeUnit.SECONDS)
    void sendTransMessageDirectlyOk() throws InterruptedException {

        // given
        val simpleMessage = new SimpleMessage();
        simpleMessage.setContent("trans");

        // when
        TestTransaction.flagForCommit();
        TestTransaction.end();
        transProducer.send(simpleMessage, Tags.TEST);

        TimeUnit.SECONDS.sleep(30);
        verify(callback, times(1)).onSuccess(any());
        verify(demoListener, times(1)).onMessage(any(), any(), any());
    }

    @Test
    @DisplayName("通过信使发送延时消息")
    void sendDelayMessageDirectlyOk() throws InterruptedException {

        // given
        val delayMessage = new DelayMessage();
        delayMessage.setContent("Delay");
        delayMessage.setDelay(Duration.ofSeconds(10));
        // when
        TestTransaction.flagForCommit();
        TestTransaction.end();
        delayMessageProducer.send(delayMessage, Tags.TEST);

        TimeUnit.SECONDS.sleep(15);

        verify(callback, times(1)).onSuccess(any());
        verify(delayListener, times(1)).onMessage(any(), any(), any());
    }

}
