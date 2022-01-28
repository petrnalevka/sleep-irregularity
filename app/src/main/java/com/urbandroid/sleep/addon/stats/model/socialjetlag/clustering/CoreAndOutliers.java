package com.urbandroid.sleep.addon.stats.model.socialjetlag.clustering;

import com.urbandroid.util.ScienceUtil;

import org.apache.commons.math3.stat.descriptive.rank.Percentile;
import org.apache.commons.math3.util.Pair;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class CoreAndOutliers<T> {

    /**
     * Multi-dimensional quantile-based outlier detection.
     * It detects outliers for several given projections separately,
     * and the resulting outliers are the union of the outliers in the individual projections.
     * @param quantile 0..1
     * @param multiplier >0
     */
    @SafeVarargs
    public static <T> CoreAndOutliers<T> compoundQuantileDistance(Collection<T> col,
                                                                  double quantile, double multiplier,
                                                                  DoubleFunction<T>... projections) {
        CoreAndOutliers<T> result = new CoreAndOutliers(col);
        for(DoubleFunction<T> projection : projections) {
            result = result.markOutliers(getQuantileDistanceOutliers(col, quantile, multiplier, projection));
        }
        return result;
    }

    /**
     * Multi-dimensional quantile-based outlier detection for 2 dimensional data with separate parameters
     * It detects outliers for several given projections separately,
     * and the resulting outliers are the union of the outliers in the individual projections.
     * @param quantileX 0..1
     * @param multiplierX >0
     * @param quantileY 0..1
     * @param multiplierY >0
     */
    public static <T> CoreAndOutliers<T> compoundQuantileDistanceXY(Collection<T> col,
                                                                    double quantileX, double multiplierX, DoubleFunction<T> projectionX,
                                                                    double quantileY, double multiplierY, DoubleFunction<T> projectionY) {
        CoreAndOutliers<T> result = new CoreAndOutliers(col);
        result = result.markOutliers(getQuantileDistanceOutliers(col, quantileX, multiplierX, projectionX));
        result = result.markOutliers(getQuantileDistanceOutliers(col, quantileY, multiplierY, projectionY));
        return result;
    }
    /**
     * Cutting-edge (literally) algorithm for outliers detection.
     *
     * First, it projects the elements into a double interval, using the given projection function.
     * Then it sorts the elements, and cuts off the smallest and largest elements based in the given quantile.
     * The remaining core is supposed to be nice, outlier-free data.
     * Then it calculates the distanceThreshold by multiplying the 80-percentile distance between these
     * core elements by the given threshold.
     * Then it goes through the smallest and the largest elements in the extreme quantiles,
     * and if the distance from an element to the next element is smaller than the threshold,
     * it adds the element to the core.
     *
     * So, the resulting outliers are elements that have too big gap (based on the median distance)
     * between them and the neighbour in the direction to the center.
     *
     * @param quantile 0..1
     * @param threshold >0
     */
    private static <T> Set<T> getQuantileDistanceOutliers(Collection<T> col,
                                                          double quantile, double threshold,
                                                          DoubleFunction<T> projection) {
        if (quantile <= 0 || quantile >= 1) {
            throw new IllegalArgumentException("quantile: "+quantile);
        }
        if (threshold < 0) {
            throw new IllegalArgumentException("threshold: "+threshold);
        }

        List<Pair<Double,T>> elements = new ArrayList<>();
        for(T element : col) {
            elements.add(Pair.create(projection.apply(element), element));
        }
        Collections.sort(elements, new Comparator<Pair<Double, T>>() {
            @Override
            public int compare(Pair<Double, T> o1, Pair<Double, T> o2) {
                return Double.compare(o1.getFirst(), o2.getFirst());
            }
        });

        int size = elements.size();

        int intQuantile = new Double(Math.ceil(quantile*((double)size))).intValue();
        if (intQuantile >= size) {
            intQuantile = size - 1;
        }

        int coreStart = intQuantile;
        int coreEnd = size - intQuantile - 1;
        if (coreEnd-coreStart < 3) {
            return Collections.emptySet();
        }

        List<Double> coreDistances = new ArrayList<>();
        for(int i = coreStart; i<coreEnd; i++) {
            coreDistances.add(elements.get(i+1).getFirst() - elements.get(i).getFirst());
        }

        double referenceCoreDistance = new Percentile().evaluate(ScienceUtil.toDoubleArray(coreDistances), 80);
        double distanceThreshold = referenceCoreDistance*threshold;
        for(;
            coreStart>0 && elements.get(coreStart).getFirst() - elements.get(coreStart-1).getFirst() <= distanceThreshold;
            coreStart--);
        for(;
            coreEnd < size-1 && elements.get(coreEnd+1).getFirst() - elements.get(coreEnd).getFirst() <= distanceThreshold;
            coreEnd++);

        Set<T> outliers  = new HashSet<>();
        for(int i=0; i<coreStart; i++) {
            outliers.add(elements.get(i).getSecond());
        }
        for(int i=coreEnd+1; i<size; i++) {
            outliers.add(elements.get(i).getSecond());
        }

        return outliers;
    }

    private final List<T> core;
    private final List<T> outliers;

    public CoreAndOutliers(Collection<T> core) {
        this.core = new ArrayList<>(core);
        this.outliers = new ArrayList<>();
    }

    public CoreAndOutliers<T> markOutliers(Collection<T> newOutliers) {
        Set<T> newOutliersSet = new HashSet<>(newOutliers);
        CoreAndOutliers<T> result = new CoreAndOutliers<>(Collections.<T>emptyList());
        result.outliers.addAll(this.outliers);
        for(T t : this.core) {
            if (newOutliersSet.contains(t)) {
                result.outliers.add(t);
            } else {
                result.core.add(t);
            }
        }
        return result;
    }

    public List<T> getCore() {
        return core;
    }

    public List<T> getOutliers() {
        return outliers;
    }
}
