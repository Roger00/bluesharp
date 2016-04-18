package com.rnfstudio.bluesharp;

/**
 * Created by Roger on 2016/4/16.
 */
public class PitchTrackerFactory {

    interface PitchTracker {
        float getPitchInHertz(short[] data);
        void setWaveView(WaveView wv);
    }

    private static class ACFPitchTracker implements PitchTracker {

        int mType = ACF_TYPE_DEFAULT;
        WaveView mWaveView;

        public ACFPitchTracker(int type) {
            mType = type;
        }

        @Override
        public float getPitchInHertz(short[] data) {
            int[] shiftedInnerProduct = new int[data.length];

            for (int i = 0; i < data.length; i++) {

                // computing shifted inner product at point i
                int sum = 0;
                for (int j = 0; j < data.length; j++) {
                    if ((i + j) > data.length - 1) break;
                    sum += data[j] * data[j + i];
                }

                switch (mType) {
                    case ACF_TYPE_VARIATION_1:
                        shiftedInnerProduct[i] = sum / (data.length - i);
                        break;
                    case ACF_TYPE_DEFAULT:
                    default:
                        shiftedInnerProduct[i] = sum;
                        break;
                }
            }

            // set starting and ending data to prevent unreasonable pitch
            for (int i = 0; i < data.length; i++) {
                if (i < MAX_PITCH_POINT || i > MIN_PITCH_POINT) {
                    shiftedInnerProduct[i] = Integer.MIN_VALUE;
                }
            }

            return ((float) SAMPLE_RATE) / Utilities.getMaxValueIndex(shiftedInnerProduct);
        }

        @Override
        public void setWaveView(WaveView wv) {
            mWaveView = wv;
        }
    }

    private static class AMDFPitchTracker implements PitchTracker {

        WaveView mWaveView;

        @Override
        public float getPitchInHertz(short[] data) {
            int[] shiftedSumOfAbsDiff = new int[data.length];

            for (int i = 0; i < data.length; i++) {

                // computing shifted sum of absolute difference at point i
                int sum = 0;
                for (int j = 0; j < data.length; j++) {
                    if ((i + j) > data.length - 1) break;
                    sum += Math.abs(data[j] - data[j + i]);
                }
                shiftedSumOfAbsDiff[i] = sum;
            }

            // add augmentation terms for latter data; and flip data array upside-down
            int max = Utilities.getMaxValue(shiftedSumOfAbsDiff);
            for (int i = 0; i < data.length; i++) {
                shiftedSumOfAbsDiff[i] += (max * ((float) i / data.length));
                shiftedSumOfAbsDiff[i] *= -1;
            }

            // set starting and ending data to prevent unreasonable pitch
            for (int i = 0; i < data.length; i++) {
                if (i < MAX_PITCH_POINT || i > MIN_PITCH_POINT) {
                    shiftedSumOfAbsDiff[i] = Integer.MIN_VALUE;
                }
            }

            return ((float) SAMPLE_RATE) / Utilities.getMaxValueIndex(shiftedSumOfAbsDiff);
        }

        @Override
        public void setWaveView(WaveView wv) {
            mWaveView = wv;
        }
    }

    private static class ACFAMDFPitchTracker implements PitchTracker {

        WaveView mWaveView;

        @Override
        public float getPitchInHertz(short[] data) {
            int[] shiftedInnerProduct = new int[data.length];
            int[] shiftedSumOfAbsDiff = new int[data.length];
            int[] acfOverAmdfVector = new int[data.length];

            for (int i = 0; i < data.length; i++) {

                // computing shifted inner product at point i
                int innerProduct = 0;
                int absDiff = 0;
                for (int j = 0; j < data.length; j++) {
                    if ((i + j) > data.length - 1) break;
                    innerProduct += data[j] * data[j + i];
                    absDiff += Math.abs(data[j] - data[j + i]);
                }
                shiftedInnerProduct[i] = innerProduct;
                shiftedSumOfAbsDiff[i] = absDiff;
            }

            // add augmentation terms for latter data; and flip data array upside-down
            int max = Utilities.getMaxValue(shiftedSumOfAbsDiff);
            for (int i = 0; i < data.length; i++) {
                shiftedSumOfAbsDiff[i] += (max * ((float) i / data.length));
                shiftedSumOfAbsDiff[i] *= -1;
            }

            for (int i = 0; i < data.length; i++) {
                acfOverAmdfVector[i] = shiftedSumOfAbsDiff[i] == 0 ?
                        Integer.MIN_VALUE : shiftedInnerProduct[i] / shiftedSumOfAbsDiff[i];
            }

            // set starting and ending data to prevent unreasonable pitch
            for (int i = 0; i < data.length; i++) {
                if (i < MAX_PITCH_POINT || i > MIN_PITCH_POINT) {
                    acfOverAmdfVector[i] = Integer.MIN_VALUE;
                }
            }

            return ((float) SAMPLE_RATE) / Utilities.getMaxValueIndex(acfOverAmdfVector);
        }

        @Override
        public void setWaveView(WaveView wv) {
            mWaveView = wv;
        }
    }

    // ------------------------------------------------------------------------
    // STATIC FIELDS
    // ------------------------------------------------------------------------

    public static final int SAMPLE_RATE = Configuration.SAMPLE_RATE;
    public static final int MAX_PITCH_POINT = Configuration.MAX_PITCH_POINT;
    public static final int MIN_PITCH_POINT = Configuration.MIN_PITCH_POINT;

    private static final int ACF_TYPE_DEFAULT = 0;
    private static final int ACF_TYPE_VARIATION_1 = 1;

    // ------------------------------------------------------------------------
    // STATIC INITIALIZERS
    // ------------------------------------------------------------------------

    // ------------------------------------------------------------------------
    // STATIC METHODS
    // ------------------------------------------------------------------------

    // ------------------------------------------------------------------------
    // FIELDS
    // ------------------------------------------------------------------------

    // ------------------------------------------------------------------------
    // INITIALIZERS
    // ------------------------------------------------------------------------

    // ------------------------------------------------------------------------
    // CONSTRUCTORS
    // ------------------------------------------------------------------------

    // ------------------------------------------------------------------------
    // METHODS
    // ------------------------------------------------------------------------

    public static PitchTracker getACFPitchTracker() {
        return new ACFPitchTracker(ACF_TYPE_DEFAULT);
    }

    public static PitchTracker getAMDFPitchTracker() {
        return new AMDFPitchTracker();
    }

    public static PitchTracker getACFAMDFPitchTracker() {
        return new ACFAMDFPitchTracker();
    }

}
