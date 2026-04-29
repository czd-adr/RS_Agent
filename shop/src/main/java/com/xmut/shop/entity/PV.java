package com.xmut.shop.entity;

import io.swagger.annotations.ApiModel;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.io.WKBReader;

import static com.xmut.shop.common.Util.hexStringToByteArray;

@Data
@EqualsAndHashCode(callSuper = false)
@ApiModel(value="LandCover对象", description="")
public class PV {
    private static final long serialVersionUID = 1L;

    private String geom;

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
