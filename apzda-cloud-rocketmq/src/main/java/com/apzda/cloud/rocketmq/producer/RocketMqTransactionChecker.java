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
package com.apzda.cloud.rocketmq.producer;

import com.apzda.cloud.rocketmq.domain.service.IMailboxService;
import com.apzda.cloud.rocketmq.listener.ISendCallback;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apache.rocketmq.client.apis.message.MessageView;
import org.apache.rocketmq.client.apis.producer.TransactionChecker;
import org.apache.rocketmq.client.apis.producer.TransactionResolution;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * @author fengz (windywany@gmail.com)
 * @version 3.4.0
 * @since 3.4.0
 **/
@RequiredArgsConstructor
@Slf4j
public class RocketMqTransactionChecker implements TransactionChecker {

    private final IMailboxService mailboxService;

    private final ObjectMapper objectMapper;

    private final List<ISendCallback> callbacks;

    @Override
    public TransactionResolution check(MessageView messageView) {
        val mailId = messageView.getKeys().stream().findFirst().orElse("");
        log.info("Check transaction for mailId: {}", mailId);

        if (!StringUtils.hasText(mailId)) {
            return TransactionResolution.ROLLBACK;
        }
        try {
            val mail = mailboxService.getSentMailByMailId(mailId);
            if (mail != null) {
                try {
                    mailboxService.removeById(mail);
                }
                catch (Exception e) {
                    log.warn("Failed to remove mail: {}. please remove it manually: {}", mail, e.getMessage());
                }
                ISendCallback.onSuccess(mail, callbacks, objectMapper);
                return TransactionResolution.COMMIT;
            }
            return TransactionResolution.ROLLBACK;
        }
        catch (Exception e) {
            return TransactionResolution.UNKNOWN;
        }
    }

}
