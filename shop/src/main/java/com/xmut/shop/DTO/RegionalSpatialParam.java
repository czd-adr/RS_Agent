package com.xmut.shop.DTO;

public class RegionalSpatialParam {
    private String poiName;    // 映射后的标准区域名：青口河、青口渔场、临洪河口
    private String landType;   // 映射后的标准地物名：互花米草、碱蓬、芦苇、水体、其他

    public String getPoiName() { return poiName; }
    public void setPoiName(String poiName) { this.poiName = poiName; }

    public String getLandType() { return landType; }
    public void setLandType(String landType) { this.landType = landType; }
}