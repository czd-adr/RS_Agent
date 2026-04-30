package com.xmut.shop.entity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * <p>
 *
 * </p>
 *
 * @author Bennu
 * @since 2025-08-13
 */
@Data
@EqualsAndHashCode(callSuper = false)
public class LandCover implements Serializable {

    private int index;        // 地物分类值，如 1、2、3
    private double area;      // 该类别的面积（单位：平方米）


}
