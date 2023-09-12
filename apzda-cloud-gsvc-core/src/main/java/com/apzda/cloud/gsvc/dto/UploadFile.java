package com.apzda.cloud.gsvc.dto;

import lombok.Builder;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serial;
import java.io.Serializable;

/**
 * @author fengz
 */
@Data
@Builder
@Accessors(chain = true)
public class UploadFile implements Serializable {

    @Serial
    private static final long serialVersionUID = 7646559420677099169L;

    private String file;// 上传后的文件保存在临时目录下，此值为临时文件的全路径名

    private String filename; // 原始文件名

    private String contentType; // 文件类型, 可能为空

    private String ext; // 文件后缀，原始文件无后缀，此值可能为空

    private String name; // 表单中的名称

    private long size; // 文件大小

    private String error;// 错误信息

}
