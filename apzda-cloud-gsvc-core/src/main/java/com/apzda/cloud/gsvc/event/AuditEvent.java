/*
 * Copyright (C) 2023-2024 Fengz Ning (windywany@gmail.com)
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
package com.apzda.cloud.gsvc.event;

import com.apzda.cloud.gsvc.dto.Audit;
import jakarta.annotation.Nonnull;
import org.springframework.context.ApplicationEvent;

import java.time.Clock;

/**
 * @author fengz (windywany@gmail.com)
 * @version 1.0.0
 * @since 1.0.0
 **/
public class AuditEvent extends ApplicationEvent {

    public AuditEvent(@Nonnull Audit source) {
        super(source);
    }

    public AuditEvent(@Nonnull Audit source, @Nonnull Clock clock) {
        super(source, clock);
    }

    public Audit getAudit() {
        return (Audit) source;
    }

}
