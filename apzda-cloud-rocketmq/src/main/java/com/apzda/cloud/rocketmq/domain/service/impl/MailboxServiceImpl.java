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
package com.apzda.cloud.rocketmq.domain.service.impl;

import com.apzda.cloud.rocketmq.domain.entity.Mailbox;
import com.apzda.cloud.rocketmq.domain.mapper.MailboxMapper;
import com.apzda.cloud.rocketmq.domain.service.IMailboxService;
import com.apzda.cloud.rocketmq.domain.vo.MailStatus;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.stereotype.Service;

/**
 * @author fengz (windywany@gmail.com)
 * @version 3.4.0
 * @since 3.4.0
 **/
@Slf4j
@Service
@RequiredArgsConstructor
public class MailboxServiceImpl extends ServiceImpl<MailboxMapper, Mailbox> implements IMailboxService {

    @Override
    public Mailbox getByStatusAndNextRetryAtLe(MailStatus mailStatus, long nextRetryAt) {
        val con = Wrappers.lambdaQuery(Mailbox.class);
        con.eq(Mailbox::getStatus, mailStatus);
        con.le(Mailbox::getNextRetryAt, nextRetryAt);
        con.orderByAsc(Mailbox::getNextRetryAt);
        con.last("LIMIT 1");

        return getOne(con);
    }

    @Override
    public boolean updateStatus(Mailbox mailboxTrans, MailStatus fromStatus) {
        val con = Wrappers.lambdaUpdate(Mailbox.class);
        con.eq(Mailbox::getStatus, fromStatus);
        con.eq(Mailbox::getMailId, mailboxTrans.getMailId());

        return update(mailboxTrans, con);
    }

    @Override
    public Mailbox getSentMailByMailId(String mailId) {
        val con = Wrappers.lambdaUpdate(Mailbox.class);
        con.eq(Mailbox::getStatus, MailStatus.SENT);
        con.eq(Mailbox::getMailId, mailId);
        return getOne(con);
    }

}
