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
package com.apzda.cloud.gsvc.security.resolver;

import com.apzda.cloud.gsvc.dto.CurrentUser;
import com.apzda.cloud.gsvc.security.dto.CardDto;
import com.apzda.cloud.gsvc.security.dto.StaffDto;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * @author fengz (windywany@gmail.com)
 * @version 1.0.0
 * @since 1.0.0
 **/

@RestController
public class TestController {

    @GetMapping("/test/ok")
    public String ok(CurrentUser currentUser) {
        return currentUser.getUid();
    }

    @PostMapping("/test/card")
    @PreAuthorize("@authz.isMine(#card)")
    public String isMine(@RequestBody CardDto card) {
        return card.getUid();
    }

    @GetMapping("/test/card/{id}")
    @PreAuthorize("@authz.isMine(#id)")
    public String isMine(@PathVariable String id) {
        return id;
    }

    @PostMapping("/test/staff")
    @PreAuthorize("authenticated && @authz.isOwned(#staffDto)")
    public String isOwned(@RequestBody StaffDto staffDto) {
        return staffDto.getTenantId().toString();
    }

    @GetMapping("/test/staff/{id}")
    @PreAuthorize("authenticated && @authz.isOwned(#id)")
    public String isOwned(@PathVariable String id) {
        return id;
    }

}
