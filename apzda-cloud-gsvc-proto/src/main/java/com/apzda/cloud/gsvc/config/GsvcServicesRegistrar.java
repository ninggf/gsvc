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
package com.apzda.cloud.gsvc.config;

import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.context.annotation.ImportSelector;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;

import javax.annotation.Nonnull;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * @author fengz (windywany@gmail.com)
 * @version 1.0.0
 * @since 1.0.0
 **/
@Slf4j
class GsvcServicesRegistrar implements ImportSelector {

    private static final Set<String> IMPORTED_PACKAGES = new LinkedHashSet<>();

    @Override
    @Nonnull
    public String[] selectImports(@Nonnull AnnotationMetadata metadata) {
        val annName = EnableGsvcServices.class.getName();
        if (!metadata.hasAnnotation(annName) && !metadata.hasMetaAnnotation(annName)) {
            return new String[0];
        }

        final Set<String> imports = new LinkedHashSet<>();
        val annotation = (EnableGsvcServices) metadata.getAnnotations().get(EnableGsvcServices.class).synthesize();
        val service = annotation.value();

        if (service != null) {
            Arrays.stream(service).forEach((s) -> {
                val gsvc = s.getName() + "Gsvc";
                if (!IMPORTED_PACKAGES.contains(gsvc)
                        && ClassUtils.isPresent(gsvc, GsvcServicesRegistrar.class.getClassLoader())) {
                    log.trace("Found Gsvc Service Configuration Class: {}", gsvc);
                    imports.add(gsvc);
                    IMPORTED_PACKAGES.add(gsvc);
                }
            });
        }

        return StringUtils.toStringArray(imports);
    }

}
