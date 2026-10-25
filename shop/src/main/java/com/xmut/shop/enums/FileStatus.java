package com.xmut.shop.enums;

import lombok.Getter;

/**
 * <p>
 * 文件记录状态枚举（对应 sys_file_record.status 字段）
 * </p>
 *
 * @author Bennu
 * @since 2026-10-23
 */
@Getter
public enum FileStatus {

    /** 正常/未删除 */
    NORMAL((short) 1, "正常"),

    /** 已删除 */
    DELETED((short) 0, "已删除");

    private final short code;
    private final String desc;

    FileStatus(short code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    /**
     * 按状态码校验并返回枚举，未知状态码返回 null
     */
    public static FileStatus of(short code) {
        for (FileStatus s : values()) {
            if (s.code == code) {
                return s;
            }
        }
        return null;
    }

    /**
     * 校验状态码是否合法
     */
    public static boolean isValid(short code) {
        return of(code) != null;
    }
}
