package com.urbandroid.util;

import org.apache.commons.math3.stat.descriptive.rank.Percentile;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

public class ScienceUtil {

    public static float min(Float[] data) {
        return min(convertArray(data));
    }

    public static float min(float[] data) {
        float min = Float.POSITIVE_INFINITY;
        for (float v : data) {
            if (v == Float.NaN) {
                continue;
            }
            if (v < min) {
                min = v;
            }
        }
        return min;
    }


    public static double round(double value, int precision) {
        int scale = (int) Math.pow(10, precision);
        return (double) (Math.round(value * scale) / scale);
    }

    public static float stddev(Float[] data) {
        return stddev(convertArray(data));
    }

    public static int findMaxPeakIndex(double[] array) {

        double maxValue = 0;
        int maxIndex = -1;

        for (int i = 1; i < array.length - 1; i++) {

            if (array[i] > 0 && array[i - 1] > 0 && array[i + 1] > 0 && array[i] > maxValue && array[i] > array[i - 1] && array[i + 1] < array[i]) {
                maxValue = array[i];
                maxIndex = i;
            }
        }

        return maxIndex;
    }

    public static int findMaxPeakIndex(float[] array) {

        double maxValue = 0;
        int maxIndex = -1;

        for (int i = 1; i < array.length - 1; i++) {

            if (array[i] > 0 && array[i - 1] > 0 && array[i + 1] > 0 && array[i] > maxValue && array[i] > array[i - 1] && array[i + 1] < array[i]) {
                maxValue = array[i];
                maxIndex = i;
            }
        }

        return maxIndex;
    }

    public static int findMaxIndex(float[] array) {

        float maxValue = Float.NEGATIVE_INFINITY;
        int maxIndex = -1;

        for (int i = 0; i < array.length; i++) {
            float value = array[i];
            if (value > maxValue) {
                maxValue = value;
                maxIndex = i;
            }
        }

        return maxIndex;
    }

    public static int findMaxIndex(double[] array) {
        return findMaxIndex(array, 0);
    }

    public static int findMaxIndex(double[] array, int fromIndex) {

        double maxValue = Double.NEGATIVE_INFINITY;
        int maxIndex = -1;

        for (int i = fromIndex; i < array.length; i++) {
            double value = array[i];
            if (value > maxValue) {
                maxValue = value;
                maxIndex = i;
            }
        }

        return maxIndex;
    }

    public static int findMaxIndex(float[] array, int fromIndex) {

        float maxValue = Float.NEGATIVE_INFINITY;
        int maxIndex = -1;

        for (int i = fromIndex; i < array.length; i++) {
            float value = array[i];
            if (value > maxValue) {
                maxValue = value;
                maxIndex = i;
            }
        }

        return maxIndex;
    }

    public static float stddevLE(float[] data, float center) {
        return (float) Math.sqrt(varLE(data, center));
    }

    public static float stddevGE(float[] data, float center) {
        return (float) Math.sqrt(varGE(data, center));
    }

    public static float stddev(float[] data) {
        return (float) Math.sqrt(var(data));
    }

    public static double stddev(Double[] data) {
        return stddev(convertArray(data));
    }

    public static double stddev(double[] data) {
        return (double) Math.sqrt(var(data));
    }

    public static double stddev(double[] data, int windowStart, int windowEndExcl) {
        return Math.sqrt(var(data, windowStart, windowEndExcl));
    }

    public static float stddev(float[] data, int windowStart, int windowEndExcl) {
        return (float) Math.sqrt(var(data, windowStart, windowEndExcl));
    }

    public static double var(Double[] values) {
        return var(convertArray(values));
    }

    public static double var(double[] values, int windowStart, int windowEndExcl) {
        if (values == null || values.length <= 1) return Double.NaN;

        int windowSize = windowEndExcl - windowStart;
        if (windowSize == 0) {
            return 0;
        }

        double avg = avg(values);
        double sum = 0;
        for(int i = windowStart; i < windowEndExcl; i++) {
            double value = values[i];
            sum += (value - avg) * (value - avg);
        }
        return sum / (values.length - 1);
    }

    public static float var(float[] values, int windowStart, int windowEndExcl) {

        if (values == null || values.length <= 1) return Float.NaN;

        int windowSize = windowEndExcl - windowStart;
        if (windowSize == 0) {
            return 0;
        }

        float avg = avg(values, windowStart, windowEndExcl);
        float sum = 0;
        for(int i = windowStart; i < windowEndExcl; i++) {
            float value = values[i];
            sum += (value - avg) * (value - avg);
        }
        return sum / windowSize;
    }

    public static float varLE(float[] values, float center) {
        if (values == null) {
            return Float.NaN;
        } else {
            return varLE(values, center, 0, values.length);
        }
    }

    /**
     * Analogue of var, but:
     *  - we compute distances from the given center, rather than from the sample mean.
     *  - we include only elements that are lower or equal to the center
     */
    public static float varLE(float[] values, float center, int windowStart, int windowEndExcl) {

        if (values == null || values.length <= 1) return Float.NaN;

        int windowSize = windowEndExcl - windowStart;
        if (windowSize == 0) {
            return 0;
        }

        int cnt = 0;
        float sum = 0;
        for(int i = windowStart; i < windowEndExcl; i++) {
            float value = values[i];
            if (value <= center) {
                cnt++;
                sum += (value - center) * (value - center);
            }
        }

        if (cnt == 0) {
            return 0;
        } else {
            return sum / cnt;
        }
    }

    public static float varGE(float[] values, float center) {
        if (values == null) {
            return Float.NaN;
        } else {
            return varGE(values, center, 0, values.length);
        }
    }

    /**
     * Analogue of var, but:
     *  - we compute distances from the given center, rather than from the sample mean.
     *  - we include only elements that are greater or equal to the center
     */
    public static float varGE(float[] values, float center, int windowStart, int windowEndExcl) {

        if (values == null || values.length <= 1) return Float.NaN;

        int windowSize = windowEndExcl - windowStart;
        if (windowSize == 0) {
            return 0;
        }

        int cnt = 0;
        float sum = 0;
        for(int i = windowStart; i < windowEndExcl; i++) {
            float value = values[i];
            if (value >= center) {
                cnt++;
                sum += (value - center) * (value - center);
            }
        }

        if (cnt == 0) {
            return 0;
        } else {
            return sum / cnt;
        }
    }

    public static double[] movingAverage(double[] array, int winLength) {
        double[] copy = Arrays.copyOf(array, array.length);
        inPlaceMovingAverage(copy, winLength);
        return copy;
    }

    public static float[] movingAverage(float[] array, int winLength) {
        float[] copy = Arrays.copyOf(array, array.length);
        inPlaceMovingAverage(copy, winLength);
        return copy;
    }

    public static float[] movingStdev(float[] array, int winLength) {
        float[] copy = Arrays.copyOf(array, array.length);
        inPlaceMovingStdev(copy, winLength);
        return copy;
    }

    public static double[] movingStdev(double[] array, int winLength) {
        double[] copy = Arrays.copyOf(array, array.length);
        inPlaceMovingStdev(copy, winLength);
        return copy;
    }

    //Moving average can be implemented faster - just one pass through the data,
    //no repeated calculation over entire window:
    //avg[i] = avg[i-1] + input[i]/windowSize - input[i-windowSize]/windowSize
    //But it can not be done in-place.

    public static void inPlaceMovingAverage(double[] array, int winLength) {
        for (int windowStart = 0; windowStart < array.length; windowStart++) {
            int windowEnd = Math.min(windowStart+winLength, array.length);
            array[windowStart] = avg(array, windowStart, windowEnd);
        }
    }

    public static void inPlaceMovingAverage(float[] array, int winLength) {
        for (int windowStart = 0; windowStart < array.length; windowStart++) {
            int windowEnd = Math.min(windowStart+winLength, array.length);
            array[windowStart] = avg(array, windowStart, windowEnd);
        }
    }

    public static float[] middleWindowMovingAverage(float[] array, int winLength) {

        array = Arrays.copyOf(array, array.length);

        int halfWindow = winLength / 2;

        for (int windowStart = -halfWindow; windowStart < array.length; windowStart++) {
            int from = Math.min(Math.max(0, windowStart), array.length - 1);
            int to = Math.max(0, Math.min(windowStart+winLength, array.length));
            array[Math.min(array.length - 1, windowStart + halfWindow)] = avg(array, from, to);
        }

        return array;
    }

    public static void inPlaceMovingStdev(float[] array, int winLength) {
        for (int windowStart = 0; windowStart < array.length; windowStart++) {
            int windowEnd = Math.min(windowStart+winLength, array.length);
            array[windowStart] = stddev(array, windowStart, windowEnd);
        }
    }

    public static void inPlaceMovingStdev(double[] array, int winLength) {
        for (int windowStart = 0; windowStart < array.length; windowStart++) {
            int windowEnd = Math.min(windowStart+winLength, array.length);
            array[windowStart] = stddev(array, windowStart, windowEnd);
        }
    }

    public static float avg(Float[] data) {
        return avg(convertArray(data));
    }

    public static double avg(Double[] data) {
        return avg(convertArray(data));
    }

    public static float avg(float[] data) {
        return avg(data, 0, data.length);
    }

    public static double avg(double[] data) {
        return avg(data, 0, data.length);
    }

    public static double avg(double[] data, int windowStart, int windowEndExcl) {
        int windowSize = windowEndExcl - windowStart;
        if (windowSize == 0) {
            return 0;
        }
        double sum = 0;
        for(int i=windowStart; i<windowEndExcl; i++) {
            double value = data[i];
            if (value == Double.NaN) {
                continue;
            }
            sum = sum + value;
        }
        return sum / windowSize;
    }

    public static float avg(float[] data, int windowStart, int windowEndExcl) {
        int windowSize = windowEndExcl - windowStart;
        if (windowSize == 0) {
            return 0;
        }
        float sum = 0;
        int count = 0;
        for(int i=windowStart; i<windowEndExcl; i++) {
            float value = data[i];
            if (value == Float.NaN) {
                continue;
            }
            sum = sum + value;
            count++;
        }
        return count == 0 ? 0 : sum / count;
    }

    public static double sum(Float[] data) {
        return sum(convertArray(data));
    }

    public static float sum(float[] data) {
        if (data.length == 0) {
            return 0;
        }

        float sum = 0;

        for (float value : data) {
            if (value == Float.NaN) {
                continue;
            }
            sum = sum + value;
        }

        return sum;
    }

    public static double var(Float[] values) {
        return var(convertArray(values));
    }

    public static float var(float[] values) {
        return var(values, 0, values.length);
    }

    public static double var(double[] values) {
        return var(values, 0, values.length);
    }

    public static double[] toDoubleArray(Collection<Double> values) {
        return convertArray(values.toArray(new Double[values.size()]));
    }

    public static float[] toFloatArray(Collection<Float> values) {
        return convertArray(values.toArray(new Float[values.size()]));
    }

    public static double[] convertArray(Double[] values) {
        double[] result = new double[values.length];

        for (int i = 0; i < values.length; i++) {
            result[i] = values[i];
        }

        return result;
    }

    public static float[] convertArray(Float[] values) {
        int length = values.length;
        float[] result = new float[length];
        for (int i = 0; i < length; i++) {
            Float value = values[i];
            result[i] = value == null ? Float.NaN : value;
        }
        return result;
    }

    public static List<Float> convertArrayToList(float[] values) {
        List<Float> result = new ArrayList<>();
        for (float v : values) {
            result.add(v);
        }
        return result;
    }

    /**
     * Linked lists are still used at several places, such as SleepRecord,
     * to store a large sequence of float values. They are highly inefficient
     * and should be converted to ArrayList or just an array.
     * @return
     */
    @Deprecated
    public static LinkedList<Float> convertArrayToLinkedList(float[] values) {
        LinkedList<Float> result = new LinkedList<>();
        for (float v : values) {
            result.add(v);
        }
        return result;
    }

    public static Float[] convertArray(float[] values) {
        Float[] result = new Float[values.length];

        for (int i = 0; i < values.length; i++) {
            result[i] = values[i];
        }

        return result;
    }

    public static float varp(Float[] data) {
        return varp(convertArray(data));
    }

    public static double varp(double[] values) {
        if (values == null || values.length == 0) return Double.NaN;

        double avg = avg(values);
        double sum = 0;
        for (double value : values) {
            sum += (value - avg) * (value - avg);
        }
        return sum / values.length;
    }


    public static float varp(float[] values) {
        if (values == null || values.length == 0) return Float.NaN;

        float avg = avg(values);
        float sum = 0;
        for (float value : values) {
            sum += (value - avg) * (value - avg);
        }
        return sum / values.length;
    }

    public static double stddevp(Double[] values) {
        return stddevp(convertArray(values));
    }

    public static double stddevp(double[] values) {
        return Math.sqrt(varp(values));
    }

    public static double stddevp(Float[] values) {
        return stddevp(convertArray(values));
    }

    public static double stddevp(float[] values) {
        return Math.sqrt(varp(values));
    }

    public static float[] convertArrayToFloat(double[] values) {
        float[] result = new float[values.length];

        for (int i = 0; i < values.length; i++) {
            result[i] = (float) values[i];
        }

        return result;
    }

    public static short[] convertArrayFloatToShort(float[] values) {
        short[] result = new short[values.length];

        for (int i = 0; i < values.length; i++) {
            result[i] = (short) values[i];
        }

        return result;
    }

    public static float[] convertArrayShortToFloat(short[] values) {
        float[] result = new float[values.length];

        for (int i = 0; i < values.length; i++) {
            result[i] = (float) values[i];
        }

        return result;
    }

    public static double[] convertArrayToDouble(float[] values) {
        double[] result = new double[values.length];

        for (int i = 0; i < values.length; i++) {
            result[i] = values[i];
        }

        return result;
    }

    public static double percentile(double[] data, float p) {
        Percentile percentile = new Percentile(p);
        return percentile.evaluate(data);
    }

    public static double percentile(Double[] data, float p) {
        return percentile(convertArray(data), p);
    }

    public static float percentile(float[] data, float p) {
        Percentile percentile = new Percentile(p);
        return (float) percentile.evaluate(convertArrayToDouble(data));
    }

    public static float[] truncateZerosAtTheEnd(float[] data) {
        int i = data.length - 1;
        for (; i >= 0; i--) {
            if (data[i] != 0) {
                break;
            }
        }

        if (i == 0) {
            return new float[0];
        }

        return Arrays.copyOfRange(data, 0, i + 1);
    }


    public static float percentile(Float[] data, float p) {
        return percentile(convertArray(data), p);
    }

    // aggregate X neightbour values with sum
    public static float[] aggSum(Float[] data, int x) {
        return aggSum(convertArray(data), x);
    }

    public static float[] aggSum(float[] data, int x) {

        if (data.length < x) {
            return new float[] {sum(data)};
        }

        float[] result = new float[data.length / (x + 1)];

        for (int i = 0; i < result.length; i++) {
            result[i] = sum(Arrays.copyOfRange(data, i * x, Math.min(i * x + 1, data.length - 1)));
        }

        return result;
    }


    public static int count(Float[] data, Condition condition) {
        return count(convertArray(data), condition);
    }

    public static int count(float[] values, Condition condition) {
        if (values == null || values.length == 0) return 0;

        int count = 0;
        for (float value : values) {
            if (condition.isMet(value)) count++;
        }
        return count;
    }

    public interface Condition {
        boolean isMet(double value);
    }


    public static float[] sleepProbability(Float[] activity) {
        return sleepProbability(convertArray(activity));
    }

    public static float[] sleepProbability(float[] activity) {

        if (activity.length < 5) {
            return null;
        }

        float[] result = new float[activity.length];

        for (int i = 5; i < activity.length - 5; i++) {
            // values in a current epoch and 5 preceding and 5 following epochs
            float[] epoch11 = Arrays.copyOfRange(activity, i - 5, i + 6);

            // values in a current epoch and 5 preceding epochs
            float[] epoch6 = Arrays.copyOfRange(activity, i - 5, i + 1);

            float meanW5min = ScienceUtil.avg(epoch11);
            float sdLast6 = ScienceUtil.stddev(epoch6);
            float nat = ScienceUtil.count(epoch11, new Condition() {
                @Override
                public boolean isMet(double value) {
                    return value >= 50 && value < 100;
                }
            });

            float logAct;
            if (activity[i] > 0) {
                logAct = (float) Math.log(activity[i]) + 1;
            } else {
                logAct = (float) Math.log(0.1d) + 1;
            }

            result[i] = 7.601f - 0.065f * meanW5min - 1.08f * nat - 0.056f * sdLast6 - 0.703f * logAct;
        }

        // compute the Sadeh averages
        float[] averages = new float[activity.length];
        for (int i = 5; i < activity.length - 5; i++) {
            averages[i] = ScienceUtil.avg(Arrays.copyOfRange(result, i - 5, i + 6));
        }

        return averages;
    }

    public static double[] delta(double[] current, double[] last) {

        double[] result = new double[current.length];

        for (int i = 0; i < current.length; i++) {
            result[i] = current[i] - last[i];
        }

        return result;
    }

    public static void deltaInPlace(double[] current, double[] last) {

        for (int i = 0; i < current.length; i++) {
            last[i] = Math.abs(current[i] - last[i]);
        }

    }

    public static double deltaSum(double[] current, double[] last) {

        double sum = 0;

        for (int i = 0; i < current.length; i++) {
            sum += Math.abs(current[i] - last[i]);
        }

        return sum;
    }

    public static float deltaSum(float[] current, float[] last) {

        float sum = 0;

        for (int i = 0; i < current.length; i++) {
            sum += Math.abs(current[i] - last[i]);
        }

        return sum;
    }

    public static void shiftRight(double[] array) {
        for (int i = array.length - 1; i > 0; i--) {
            array[i] = array[i - 1];
        }
        array[0] = 0;
    }

    public static void shiftRight(float[] array) {
        for (int i = array.length - 1; i > 0; i--) {
            array[i] = array[i - 1];
        }
        array[0] = 0;
    }

    public static void rotate(float[] array, int k) {
        if(k > array.length)
            k=k%array.length;

        float[] result = new float[array.length];

        for(int i=0; i < k; i++){
            result[i] = array[array.length-k+i];
        }

        int j=0;
        for(int i=k; i<array.length; i++){
            result[i] = array[j];
            j++;
        }

        System.arraycopy( result, 0, array, 0, array.length );
    }

    public static void rotate(double[] array, int k) {
        if(k > array.length)
            k=k%array.length;

        double[] result = new double[array.length];

        for(int i=0; i < k; i++){
            result[i] = array[array.length-k+i];
        }

        int j=0;
        for(int i=k; i<array.length; i++){
            result[i] = array[j];
            j++;
        }

        System.arraycopy( result, 0, array, 0, array.length );
    }

    public static float[] movingMax(float[] array, int winLength) {
        float[] copy = Arrays.copyOf(array, array.length);
        inPlaceMovingMax(copy, winLength);
        return copy;
    }

    public static void inPlaceMovingMax(float[] array, int winLength) {
        for (int windowStart = 0; windowStart < array.length; windowStart++) {
            int windowEnd = Math.min(windowStart+winLength, array.length);
            array[windowStart] = max(array, windowStart, windowEnd);
        }
    }

    public static float max(float[] data, int windowStart, int windowEndExcl) {
        float max = Float.NEGATIVE_INFINITY;
        for(int i=windowStart; i<windowEndExcl; i++) {
            float f = data[i];
            if (f == Float.NaN) {
                continue;
            }
            if (f > max) {
                max = f;
            }
        }
        return max;
    }

    public static double[] movingMax(double[] array, int winLength) {
        double[] copy = Arrays.copyOf(array, array.length);
        inPlaceMovingMax(copy, winLength);
        return copy;
    }

    public static void inPlaceMovingMax(double[] array, int winLength) {
        for (int windowStart = 0; windowStart < array.length; windowStart++) {
            int windowEnd = Math.min(windowStart+winLength, array.length);
            array[windowStart] = max(array, windowStart, windowEnd);
        }
    }

    public static double max(double[] data, int windowStart, int windowEndExcl) {
        double max = Double.NEGATIVE_INFINITY;
        for(int i=windowStart; i<windowEndExcl; i++) {
            double f = data[i];
            if (f == Double.NaN) {
                continue;
            }
            if (f > max) {
                max = f;
            }
        }
        return max;
    }

    public static double max(Double[] data) {
        return max(convertArray(data));
    }

    public static float max(Float[] data) {
        return max(convertArray(data));
    }

    public static float max(float[] data) {
        return max(data, 0, data.length);
    }

    public static double max(double[] data) {
        return max(data, 0, data.length);
    }

    public static double[] sqrt(double... a) {
        double[] result = new double[a.length];
        for(int i=0; i<a.length; i++) {
            result[i] = Math.sqrt(a[i]);
        }
        return result;
    }

    public static double[] detectPeaks(double[] array) {
        double[] result = new double[array.length];

        for (int i = 1; i < array.length - 1; i++) {
            if (array[i -1] < array[i] && array[i] > array[i + 1]) {
                result[i] = 1;
            } else {
                result[i] = 0;
            }
        }
        return result;
    }

    public static void detectPeaksAdvance(double[] array, int windowSize, List peakIndexes, List peakValues) {
        detectPeaksAdvance(array, windowSize, peakIndexes, peakValues, 0);
    }

    public static void detectPeaksAdvance(double[] array, int windowSize, List<Integer> peakIndexes, List<Double> peakValues, double threshold) {

        int index = findMaxPeakIndex(array);

        if (index > -1 && array[index] > threshold) {
            peakIndexes.add(index);
            peakValues.add(array[index]);
            int fromIndex = Math.max(index - (windowSize / 2), 0);
            int toIndex = Math.min(index + (windowSize / 2), array.length - 1);

            for (int i = fromIndex; i <= toIndex; i++) {
                array[i] = 0;
            }

            detectPeaksAdvance(array, windowSize, peakIndexes, peakValues, threshold);
        }
    }

    public static void detectPeaksAdvance(float[] array, int windowSize, List<Integer> peakIndexes, List<Float> peakValues, float threshold) {

        int index = findMaxPeakIndex(array);

        if (index > -1 && array[index] > threshold) {
            if (peakIndexes != null) peakIndexes.add(index);
            if (peakValues != null) peakValues.add(array[index]);
            int fromIndex = Math.max(index - (windowSize / 2), 0);
            int toIndex = Math.min(index + (windowSize / 2), array.length - 1);

            for (int i = fromIndex; i <= toIndex; i++) {
                array[i] = 0;
            }

            detectPeaksAdvance(array, windowSize, peakIndexes, peakValues, threshold);
        }
    }


    public static void detectPeaksAdvance(double[] array, List<Integer> peakIndexes, List<Double> peakValues) {

        if (peakValues == null) {
            peakValues = new ArrayList<Double>();
        }

        int index = findMaxPeakIndex(array);

        if (index > -1) {

            peakIndexes.add(index);

            double value = array[index];

            peakValues.add(value);
            double avg = avg(peakValues.toArray(new Double[] {}));
            double var = stddev(peakValues.toArray(new Double[] {}));

//            Log.i(Config.TAG, index + " V " + value + " TH " + (avg - (var * 2)));

            if ((peakValues.size() < 2 || value > (avg - var))) {

                for (int i = index; i < array.length - 1; i++) {
                    if (array[i] > array[i+1]) {
                        array[i] = 0;
                    } else {
                        break;
                    }
                }

                for (int i = index - 1; i > 1; i--) {
                    if (array[i] > array[i-1]) {
                        array[i] = 0;
                    } else {
                        break;
                    }
                }

                detectPeaksAdvance(array, peakIndexes, peakValues);
            }
        }
    }

    public static int nextPowerOf2(int a) {
        int b = 1;
        while (b < a) {
            b = b << 1;
        }
        return b;
    }

    public static int prevPowerOf2(int a) {
        int b = 0x10000000;
        while (b > a && b > 1) {
            b = b >> 1;
        }
        return b;
    }

    public static float[] rcHighPassFilter(float[] values, int sampleRate, double cutoffFreq) {
        float[] copy = Arrays.copyOf(values, values.length);
        inPlaceRcHighPassFilter(values, sampleRate, cutoffFreq);
        return copy;
    }

    //see https://en.wikipedia.org/wiki/High-pass_filter#Discrete-time_realization
    public static void inPlaceRcHighPassFilter(float[] values, int sampleRate, double cutoffFreq) {
        double rc = 1.0 / (2*Math.PI*cutoffFreq);
        double dt = 1.0 / sampleRate;
        double alfa = rc / (rc+dt);
        float alfaFloat = (float) alfa;
        int size = values.length;
        float prevX = values[0];
        for(int i=1; i<size; i++) {
            float prevY = values[i - 1];
            float x = values[i];
            values[i] = (prevY + x - prevX) * alfaFloat;
            prevX = x;
        }
    }

    public static float[] rcLowPassFilter(float[] values, int sampleRate, double cutoffFreq) {
        float[] copy = Arrays.copyOf(values, values.length);
        inPlaceRcLowPassFilter(values, sampleRate, cutoffFreq);
        return copy;
    }

    //see https://en.wikipedia.org/wiki/Low-pass_filter#Discrete-time_realization
    public static void inPlaceRcLowPassFilter(float[] values, int sampleRate, double cutoffFreq) {
        double rc = 1.0 / (2*Math.PI*cutoffFreq);
        double dt = 1.0 / sampleRate;
        double alfa = dt / (rc+dt);
        float alfaFloat = (float) alfa;
        int size = values.length;
        for(int i=1; i<size; i++) {
            float prevY = values[i - 1];
            float x = values[i];
            float y = prevY + (x - prevY) * alfaFloat;
            values[i] = y;
        }
    }

    public static float[] aggregate(float[] src, int targetSize) {
        float[] target = new float[targetSize];
        aggregateAndAdd(target, src);
        return target;
    }

    public static void aggregateAndAdd(float[] accumulator, float[] src) {
        int targetSize = accumulator.length;
        int srcLen = src.length;
        if (targetSize > srcLen) {
            throw new IllegalArgumentException(targetSize + " > " + srcLen);
        }
        if (srcLen % targetSize != 0) {
            throw new IllegalArgumentException(srcLen + " not divisible by " + targetSize);
        }
        int aggregateFactor = srcLen / targetSize;
        for (int srcIndex = 0; srcIndex < srcLen; srcIndex++) {
            int targetIndex = srcIndex / aggregateFactor;
            accumulator[targetIndex] += src[srcIndex];
        }
    }

    public static void add(float[] accumulator, float[] fs) {
        int length = accumulator.length;
        if (length != fs.length) {
            throw new IllegalArgumentException(length + " != " + fs.length);
        }
        for(int i=0; i<length; i++) {
            accumulator[i] += fs[i];
        }
    }

    public static void subtract(float[] accumulator, float[] fs) {
        int length = accumulator.length;
        if (length != fs.length) {
            throw new IllegalArgumentException(length + " != " + fs.length);
        }
        for(int i=0; i<length; i++) {
            accumulator[i] -= fs[i];
        }
    }

    public static void divide(float[] fs, float d) {
        int length = fs.length;
        for(int i=0; i<length; i++) {
            fs[i] /= d;
        }
    }

    public static double div0(double f1, double f2) {
        if (f2 == 0) {
            return 0;
        }
        return f1/f2;
    }

    public static int argmax(float[] data) {
        float max = Float.NEGATIVE_INFINITY;
        int argmax = -1;
        int length = data.length;
        for(int i = 0; i< length; i++) {
            float f = data[i];
            if (f >= max) {
                max = f;
                argmax = i;
            }
        }
        return argmax;
    }

    public static int argmin(float[] data) {
        float min = Float.POSITIVE_INFINITY;
        int argmin = -1;
        int length = data.length;
        for(int i = 0; i< length; i++) {
            float f = data[i];
            if (f <= min) {
                min = f;
                argmin = i;
            }
        }
        return argmin;
    }

    public static float[] decimate(float[] data, int decimateFactor) {
        if (decimateFactor == 1) {
            return data;
        }
        float[] newData = new float[data.length/decimateFactor];
        for (int i = 0; i < newData.length; i++) {
            newData[i] = data[i * decimateFactor];
        }
        return newData;
    }

    public static long[] decimate(long[] data, int decimateFactor) {
        if (decimateFactor == 1) {
            return data;
        }
        long[] newData = new long[data.length/decimateFactor];
        for (int i = 0; i < newData.length; i++) {
            newData[i] = data[i * decimateFactor];
        }
        return newData;
    }
}