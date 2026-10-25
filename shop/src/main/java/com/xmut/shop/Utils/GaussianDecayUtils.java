package com.xmut.shop.Utils;

public class GaussianDecayUtils {

    private GaussianDecayUtils() {
    }

    /**
     * 高斯衰减函数
     *
     * fs = exp(-(d²)/(2σ²))
     */
    public static double gaussianWeight(
            double distance,
            double sigma) {

        return Math.exp(
                -(distance * distance)
                        / (2 * sigma * sigma)
        );
    }

    /**
     * 面积加权
     */
    public static double weightedArea(
            double areaHm,
            double distance,
            double sigma) {

        return areaHm * gaussianWeight(distance, sigma);
    }
}
