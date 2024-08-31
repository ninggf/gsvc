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
package com.apzda.cloud.gsvc.utils;

import cn.hutool.core.io.FileUtil;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import lombok.Getter;
import lombok.val;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * @author fengz (windywany@gmail.com)
 * @version 1.0.0
 * @since 1.0.0
 **/
public abstract class ZipFileUtil {

    @Nonnull
    public static File createZipFile(@Nonnull File... files) throws IOException {
        val zipFile = FileUtil.createTempFile(".zip", true);
        try (val zipOut = new ZipOutputStream(new FileOutputStream(zipFile))) {
            for (val file : files) {
                addFileToZip(zipOut, file, null);
            }
        }
        return zipFile;
    }

    @Nonnull
    public static File createZipFile(@Nonnull Entry... entries) throws IOException {
        val zipFile = FileUtil.createTempFile(".zip", true);
        try (val zipOut = new ZipOutputStream(new FileOutputStream(zipFile))) {
            for (val entry : entries) {
                addFileToZip(zipOut, entry.file, entry.entryName);
            }
        }
        return zipFile;
    }

    private static void addFileToZip(@Nonnull ZipOutputStream zipOut, @Nonnull File file, @Nullable String entryName)
            throws IOException {
        entryName = StringUtils.defaultIfBlank(entryName, FileUtil.getName(file));

        zipOut.putNextEntry(new ZipEntry(entryName));

        try (val in = Files.newInputStream(file.toPath())) {
            byte[] buffer = new byte[1024];
            int len;
            while ((len = in.read(buffer)) > 0) {
                zipOut.write(buffer, 0, len);
            }
        }

        zipOut.closeEntry();
    }

    @Getter
    public static final class Entry {

        private final File file;

        private final String entryName;

        public Entry(@Nonnull File file) {
            this(file, null);
        }

        public Entry(@Nonnull File file, String entryName) {
            this.file = file;
            this.entryName = entryName;
        }

        public Entry(@Nonnull String content, String entryName) {
            this.file = FileUtil.createTempFile();
            FileUtil.writeBytes(content.getBytes(StandardCharsets.UTF_8), this.file);
            this.entryName = entryName;
        }

    }

}
