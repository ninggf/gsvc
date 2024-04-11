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
package com.apzda.cloud.demo.bar.facade;

import com.apzda.cloud.demo.bar.proto.FileInfo;
import com.apzda.cloud.demo.bar.proto.FileReq;
import com.apzda.cloud.demo.bar.proto.FileService;
import com.apzda.cloud.demo.bar.proto.UploadRes;
import com.apzda.cloud.gsvc.ext.GsvcExt;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.val;
import org.springframework.stereotype.Service;

/**
 * @author fengz (windywany@gmail.com)
 * @version 1.0.0
 * @since 1.0.0
 **/
@Service
@Tag(name = "FileService")
public class FileServiceImpl implements FileService {

    @Override
    public UploadRes upload(FileReq request) {
        if (request.hasFile()) {
            val builder = FileReq.newBuilder(request);
            builder.addFiles(request.getFile());
            request = builder.build();
        }
        val builder = UploadRes.newBuilder();

        int i = 0;

        for (GsvcExt.UploadFile uploadFile : request.getFilesList()) {
            val fb = FileInfo.newBuilder();
            if (uploadFile.hasError()) {
                fb.setError(1);
                fb.setMessage(uploadFile.getError());
            }
            else {
                fb.setError(0);
                fb.setLength(uploadFile.getSize());
            }
            fb.setIndex(i++);
            fb.setPath(uploadFile.getFilename());
            builder.addFiles(fb.build());
        }

        builder.setCount(request.getFilesCount());
        builder.addAllNames(request.getNamesList());
        builder.setErrCode(0);
        return builder.build();
    }

}
