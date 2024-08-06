/*
 * Copyright (C) 2023-2023 Fengz Ning (windywany@gmail.com)
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
package com.apzda.cloud.gsvc.ext;

import com.google.common.io.Files;
import lombok.val;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.MimeTypeUtils;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Nonnull;
import java.io.File;
import java.io.IOException;
import java.net.URLConnection;

/**
 * @author fengz (windywany@gmail.com)
 * @version 1.0.0
 * @since 1.0.0
 **/
public abstract class FileUploadUtils {

    @Nonnull
    public static GsvcExt.UploadFile create(@Nonnull String file) {
        return create(new File(file));
    }

    @Nonnull
    public static GsvcExt.UploadFile create(@Nonnull File file) {
        final GsvcExt.UploadFile.Builder builder = GsvcExt.UploadFile.newBuilder();
        final String fileName = file.getName();
        builder.setName(Files.getNameWithoutExtension(fileName));
        if (file.exists()) {
            if (file.isFile()) {
                builder.setFile(file.getAbsolutePath());
                builder.setContentType(URLConnection.guessContentTypeFromName(fileName));
                builder.setFilename(fileName);
                builder.setExt(Files.getFileExtension(fileName));
                try {
                    builder.setSize(java.nio.file.Files.size(file.toPath()));
                }
                catch (Exception e) {
                    builder.setError(e.getMessage());
                }
            }
            else {
                builder.setError(String.format("'%s' is not file!", file));
            }
        }
        else {
            builder.setError(String.format("'%s' is not exist!", file));
        }
        return builder.build();
    }

    @Nonnull
    public static GsvcExt.UploadFile create(@Nonnull MultipartFile file) throws IOException {
        GsvcExt.UploadFile.Builder builder = GsvcExt.UploadFile.newBuilder();
        String fileName = file.getOriginalFilename();
        val contentType = file.getContentType();

        if (StringUtils.isBlank(fileName) && StringUtils.isNotBlank(contentType)) {
            val ext = MimeTypeUtils.parseMimeType(contentType).getSubtype();
            fileName = file.getName() + "." + ext;
        }

        if (fileName == null) {
            throw new NullPointerException("originalFilename is null");
        }

        File tmpFile = File.createTempFile("G_UP_", ".dat");
        file.transferTo(tmpFile);
        builder.setName(Files.getNameWithoutExtension(fileName));
        builder.setFile(tmpFile.getAbsolutePath());
        builder.setContentType(contentType);
        builder.setFilename(fileName);
        builder.setExt(Files.getFileExtension(fileName));
        try {
            builder.setSize(file.getSize());
        }
        catch (Exception e) {
            builder.setError(e.getMessage());
        }

        return builder.build();
    }

}
