package com.apzda.cloud.gsvc.dto;

import lombok.Builder;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * @author fengz
 */
@Data
@Builder
@Accessors(chain = true)
public class UploadFile {
    private String file;// 上传后的文件保存在临时目录下，此值为临时文件的全路径名
    private String filename; // 原始文件名
    private String contentType; // 文件类型, 可能为空
    private String ext; // 文件后缀，原始文件无后缀，此值可能为空
    private String name; // 表单中的名称
}
