package com.xmut.shop.entity;

import java.io.Serializable;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.io.WKBReader;
import org.locationtech.jts.io.WKTReader;

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
@EqualsAndHashCode(callSuper = false)
@ApiModel(value="SamplePoints对象", description="")
public class SamplePoints implements Serializable {

    private static final long serialVersionUID = 1L;

    private String geom;

    @ApiModelProperty(value = "1hhmc2jp3lw4river5other")
    private Integer grass;

    private Integer id;

    private Coordinate coord;
    public Coordinate getCoord() {
        if (coord == null && geom != null) {
            try {
                byte[] bytes = hexStringToByteArray(geom);
                WKBReader reader = new WKBReader();
                Geometry geometry = reader.read(bytes);
                coord = geometry.getCoordinate();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return coord;
    }
}
