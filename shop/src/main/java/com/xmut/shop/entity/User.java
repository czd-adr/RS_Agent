package com.xmut.shop.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.io.WKBReader;

import java.io.Serializable;
import java.time.LocalDateTime;

import static com.xmut.shop.common.Util.hexStringToByteArray;

/**
 * <p>
 * 
 * </p>
 *
 * @author Bennu
 * @since 2025-08-13
 */
@Data
@TableName("\"users\"") // 关键点 1：给表名加双引号转义，防止和系统保留字冲突
public class User implements Serializable {

    @TableId(value = "\"id\"", type = IdType.AUTO) // 关键点 2：给字段名加双引号
    private Long id;

    @TableField("\"username\"")
    private String username;

    @TableField("\"password\"")
    private String password;

    @TableField("\"token\"")
    private String token;

    @TableField("\"role\"")
    private String role;

    @TableField("\"create_time\"")
    private LocalDateTime createTime;
}
