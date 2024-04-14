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
package com.apzda.cloud.test.autoconfig;

import com.apzda.cloud.gsvc.config.IServiceConfigure;
import com.apzda.cloud.gsvc.exception.IExceptionHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.NotImplementedException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.JsonTest;
import org.springframework.test.context.ContextConfiguration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * @author fengz (windywany@gmail.com)
 * @version 1.0.0
 * @since 1.0.0
 **/
@JsonTest
@ContextConfiguration(classes = TestApp.class)
@AutoConfigureGsvcTest
public class WithAutoConfigureGsvcTest {

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private IServiceConfigure serviceConfigure;

    @Autowired
    private IExceptionHandler exceptionHandler;

    @Test
    void object_mapper_should_inject() {
        assertThat(objectMapper).isNotNull();
    }

    @Test
    void config_is_stub() {
        assertThrows(NotImplementedException.class, () -> exceptionHandler.handle(null, null));
    }

}
