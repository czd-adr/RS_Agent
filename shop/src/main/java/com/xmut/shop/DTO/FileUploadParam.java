package com.xmut.shop.DTO;

import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

/**
 * <p>
 * 文件上传入参封装
 * </p>
 *
 * @author Bennu
 * @since 2026-10-26
 */
@Data
public class FileUploadParam {

    /** 上传的文件 */
    private MultipartFile file;

    /** 年份，如 2026 */
    private Integer year;

    /** 月份，如 3 */
    private Integer month;

    /** 特征波段，如 NDVI */
    private String feature;
}
