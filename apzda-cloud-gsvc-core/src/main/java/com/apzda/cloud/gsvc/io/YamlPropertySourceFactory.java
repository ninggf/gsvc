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
package com.apzda.cloud.gsvc.io;

import jakarta.annotation.Nonnull;
import lombok.val;
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.core.env.PropertiesPropertySource;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.support.EncodedResource;
import org.springframework.core.io.support.PropertySourceFactory;

import java.io.IOException;
import java.util.Objects;

/**
 * @author fengz (windywany@gmail.com)
 * @version 1.0.0
 * @since 1.0.0
 **/
public class YamlPropertySourceFactory implements PropertySourceFactory {

    @Override
    @Nonnull
    public PropertySource<?> createPropertySource(String name, EncodedResource encodedResource) throws IOException {
        val factory = new YamlPropertiesFactoryBean();
        factory.setResources(encodedResource.getResource());
        val properties = factory.getObject();

        assert properties != null;
        return (name != null) ? new PropertiesPropertySource(name, properties) : new PropertiesPropertySource(
                Objects.requireNonNull(encodedResource.getResource().getFilename()), properties);
    }

}
