package com.xmut.shop.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * <p>
 * 文件记录表 sys_file_record
 * </p>
 *
 * @author Bennu
 * @since 2026-10-23
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("sys_file_record")
public class SysFileRecord implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键ID */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 存储在系统中的文件名 */
    private String fileName;

    /** 用户上传时的原始文件名 */
    private String originalFilename;

    /** 文件的本地存储路径 */
    private String filePath;

    /** 文件大小（字节） */
    private BigDecimal fileSize;

    /** 文件类型 */
    private String fileType;

    /** 文件上传时间 */
    private LocalDateTime uploadTime;

    /** 状态：1-正常，0-已删除 */
    private Short status;
}
