package com.xmut.shop.entity;

import java.io.Serializable;
import lombok.Data;
import lombok.EqualsAndHashCode;

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
public class RfArea implements Serializable {

    private static final long serialVersionUID = 1L;

    private String geom;


    private float area;

    private Integer id;

    private Integer count;

    private Integer className;//class
}
