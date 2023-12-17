package org.astrotools;

import mil.nga.tiff.Rasters;

import java.util.Arrays;

public class Stretcher {
    private static final double shadows_clip=-1.25;
    private static final double target_bkg=0.25;

    private MedianDev rastersInfo(Rasters rasters, int sample) {
        double[] arr = new double[rasters.getHeight() * rasters.getWidth()];

        for (int y = 0; y < rasters.getHeight(); y++) {
            for (int x = 0; x < rasters.getWidth(); x++) {
                arr[x + y*rasters.getWidth()] = rasters.getPixelSample(sample, x, y).doubleValue();
            }
        }

        Arrays.sort(arr);

        double median;
        if (arr.length % 2 == 0) {
            median = (arr[arr.length/2] + arr[arr.length/2 - 1])/2;
        } else {
            median = arr[arr.length/2];
        }

        double sum = 0;
        for (double d: arr) {
            sum += Math.abs(d - median);
        }

        return new MedianDev(median, sum/arr.length);
    }

    private double clip(double a, double min, double max) {
        return Math.min(max, Math.max(a, min));
    }

    /**
        Mid Tone Transfer Function

        MTF(m, x) = {
            0                for x == 0,
            1/2              for x == m,
            1                for x == 1,

            (m - 1)x
            --------------   otherwise.
            (2m - 1)x - m
        }

     **/
    private double mtf(double m, double x) {
        return (m-1) * x / ((2*m - 1) * x - m);
    }

    private StretchParams[] getParams(Rasters rasters) {
        StretchParams[] res = new StretchParams[rasters.getSamplesPerPixel()];

        for (int sample = 0; sample < res.length; sample++) {
            MedianDev median = rastersInfo(rasters, sample);

            double c0 = clip(median.median + shadows_clip * median.avgDev, 0, 1);

            double m = mtf(target_bkg, median.median - c0);

            res[sample] = new StretchParams(m, c0);
        }

        return res;
    }

    public Rasters stretch(Rasters original) {
        Rasters rasters = new Rasters(original.getWidth(), original.getHeight(), original.getSamplesPerPixel(), original.getFieldTypes()[0]);
        StretchParams[] stretchParams = getParams(original);


        for (int y = 0; y < original.getHeight(); y++) {
            for (int x = 0; x < original.getWidth(); x++) {
                for (int s = 0; s < original.getSamplesPerPixel(); s++) {
                    double val = original.getPixelSample(s, x, y).doubleValue();
                    if (val < stretchParams[s].c0) {
                        val = 0;
                    } else {
                        val = mtf(stretchParams[s].m, (val - stretchParams[s].c0)/(1 - stretchParams[s].c0));
                    }

                    rasters.setPixelSample(s, x, y, val);
                }
            }
        }

        return rasters;
    }
}

class StretchParams {
    public double m;
    public double c0;

    public StretchParams(double m, double c0) {
        this.m = m;
        this.c0 = c0;
    }
}


class MedianDev {
    public double median;
    public double avgDev;

    public MedianDev(double median, double avgDev) {
        this.median = median;
        this.avgDev = avgDev;
    }
}
