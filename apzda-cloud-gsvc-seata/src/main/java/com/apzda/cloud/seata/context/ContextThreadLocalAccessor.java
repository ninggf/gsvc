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
package com.apzda.cloud.seata.context;

import io.micrometer.context.ThreadLocalAccessor;
import io.seata.core.context.RootContext;
import jakarta.annotation.Nonnull;
import lombok.val;
import org.apache.commons.lang3.StringUtils;

/**
 * @author fengz (windywany@gmail.com)
 * @version 1.0.0
 * @since 1.0.0
 **/
public class ContextThreadLocalAccessor implements ThreadLocalAccessor<SeataContext> {

    public static final String KEY = "gsvc.seata";

    @Override
    @Nonnull
    public Object key() {
        return KEY;
    }

    @Override
    public SeataContext getValue() {
        val xid = RootContext.getXID();
        if (StringUtils.isNotBlank(xid)) {
            SeataContext context = new SeataContext();
            context.setXid(xid);
            context.setTimeout(RootContext.getTimeout());
            context.setBranchType(RootContext.getBranchType());
            return context;
        }
        return null;
    }

    @Override
    public void setValue(@Nonnull SeataContext seataContext) {
        val xid = seataContext.getXid();
        if (seataContext.getTimeout() != null) {
            RootContext.setTimeout(seataContext.getTimeout());
        }
        if (seataContext.getBranchType() != null) {
            RootContext.bindBranchType(seataContext.getBranchType());
        }

        RootContext.bind(xid);
    }

    @Override
    public void setValue() {
        RootContext.unbind();
    }

}
