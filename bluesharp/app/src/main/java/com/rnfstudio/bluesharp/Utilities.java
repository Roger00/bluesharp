package com.rnfstudio.bluesharp;

import java.util.Arrays;

/**
 * Created by Roger on 2016/4/18.
 */
public class Utilities {

    public static int getSimpleVolume(short[] data) {
        short median = (short) getMedianFromArray(data);

        int sum = 0;
        for (short aData : data) {
            sum += Math.abs(aData - median);
        }

        return sum;
    }

    public static float getDecibelVolume(short[] data) {
        float mean = getMeanFromArray(data);

        float sum = 0;
        for (short d : data) {
            float v = ((float) d) - mean;
            sum += v * v;
        }

        return (float) (10 * Math.log10(sum) - 70);
    }

    public static float getMaxDecibelVolume() {
        float max = Short.MAX_VALUE;
        int cPoints = Configuration.BUFFER_ELEMENT_COUNT;
        float sum = max * max * cPoints;

        return (float) (10 * Math.log10(sum) - 70);
    }

    public static float getMeanFromArray(short[] data) {
        int sum = 0;
        for (short d : data) {
            sum += d;
        }
        return ((float) sum) / data.length;
    }

    /**
     * See <a href="http://goo.gl/N8FgT1">How to calculate the median of an array?</a>
     */
    public static float getMedianFromArray(short[] data) {
        // sort the copy of data array
        short[] dataCopy = Arrays.copyOf(data, data.length);
        Arrays.sort(dataCopy);

        // get median
        int centerIndex = dataCopy.length / 2;
        if (dataCopy.length % 2 == 0)
            return (dataCopy[centerIndex] + dataCopy[centerIndex - 1]) / 2f;
        else
            return dataCopy[centerIndex];
    }

    public static int getMaxValue(int[] data) {
        if (data.length == 0) {
            return Integer.MIN_VALUE;
        }

        int max = data[0];
        for (int i = 1; i < data.length; i++) {
            if (data[i] > max) {
                max = data[i];
            }
        }
        return max;
    }

    public static int getMaxValueIndex(int[] data) {
        if (data.length == 0) {
            return -1;
        }

        int maxIndex = 0;
        int max = data[maxIndex];
        for (int i = 1; i < data.length; i++) {
            if (data[i] > max) {
                max = data[i];
                maxIndex = i;
            }
        }
        return maxIndex;
    }

    public static float pitch2Semitone(float pitch) {
        return (float) (69f + 12 * Math.log(pitch /
                Configuration.PITCH_BASE_IN_HERTZ) / Math.log(2));
    }
}
