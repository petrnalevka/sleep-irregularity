package com.urbandroid.sleep.addon.stats.model.socialjetlag.clustering;

import com.urbandroid.sleep.addon.stats.model.socialjetlag.CyclicDoubleKt;

import org.apache.commons.math3.exception.ConvergenceException;
import org.apache.commons.math3.exception.MathIllegalArgumentException;
import org.apache.commons.math3.exception.NumberIsTooSmallException;
import org.apache.commons.math3.exception.util.LocalizedFormats;
import org.apache.commons.math3.ml.clustering.CentroidCluster;
import org.apache.commons.math3.ml.clustering.Cluster;
import org.apache.commons.math3.ml.clustering.Clusterable;
import org.apache.commons.math3.ml.clustering.Clusterer;
import org.apache.commons.math3.ml.clustering.DoublePoint;
import org.apache.commons.math3.ml.distance.DistanceMeasure;
import org.apache.commons.math3.random.JDKRandomGenerator;
import org.apache.commons.math3.random.RandomGenerator;
import org.apache.commons.math3.stat.descriptive.moment.Variance;
import org.apache.commons.math3.util.FastMath;
import org.apache.commons.math3.util.MathUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * The code is based on org.apache.commons.math3.ml.clustering.KMeansPlusPlusClusterer
 * licensed under http://www.apache.org/licenses/LICENSE-2.0
 *
 * First, it was simplified to the bare minimum - discarded all options that we do not use.
 *
 * Second, more importantly, it is adopted to the circular topology, as implemented in CyclicDouble.
 * - CyclicDouble.center() is used instead of arithmetic mean
 * - CyclicDouble.distance() is used instead of (a-b) for euclidean distance
 */
public class KMeansPlusPlusClusterer<T extends Clusterable> extends Clusterer<T> {

    /** The number of clusters. */
    private final int k;

    /** The maximum number of iterations. */
    private final int maxIterations;

    /** Random generator for choosing initial centers. */
    private final RandomGenerator random;

    private final double[] cycles;

    /** Build a clusterer.
     * @param k the number of clusters to split the data into
     * @param maxIterations the maximum number of iterations to run the algorithm for.
     * @param cycles cycle sizes on individual dimensions, used in the CyclicDouble arithmetic.
     *               Note that if some of the axes are plain linear, cyclic arithmetic still
     *               can be used on them, only the cycle size needs to sufficiently large.
     */
    public KMeansPlusPlusClusterer(final int k, final int maxIterations, double[] cycles) {
        super(new CyclicEuclideanDistance(cycles));
        this.k             = k;
        this.maxIterations = maxIterations;
        this.random        = new JDKRandomGenerator();
        this.cycles        = cycles;
    }

    /**
     * Runs the K-means++ clustering algorithm.
     *
     * @param points the points to cluster
     * @return a list of clusters containing the points
     * @throws MathIllegalArgumentException if the data points are null or the number
     *     of clusters is larger than the number of data points
     */
    @Override
    public List<CentroidCluster<T>> cluster(final Collection<T> points)
            throws MathIllegalArgumentException, ConvergenceException {

        // sanity checks
        MathUtils.checkNotNull(points);

        // number of clusters has to be smaller or equal the number of data points
        if (points.size() < k) {
            throw new NumberIsTooSmallException(points.size(), k, false);
        }

        // create the initial clusters
        List<CentroidCluster<T>> clusters = chooseInitialCenters(points);

        // create an array containing the latest assignment of a point to a cluster
        // no need to initialize the array, as it will be filled with the first assignment
        int[] assignments = new int[points.size()];
        assignPointsToClusters(clusters, points, assignments);

        // iterate through updating the centers until we're done
        final int max = (maxIterations < 0) ? Integer.MAX_VALUE : maxIterations;
        for (int count = 0; count < max; count++) {
            boolean emptyCluster = false;
            List<CentroidCluster<T>> newClusters = new ArrayList<CentroidCluster<T>>();
            for (final CentroidCluster<T> cluster : clusters) {
                final Clusterable newCenter;
                if (cluster.getPoints().isEmpty()) {
                    newCenter = getPointFromLargestVarianceCluster(clusters);
                    emptyCluster = true;
                } else {
                    newCenter = centroidOf(cluster.getPoints());
                }
                newClusters.add(new CentroidCluster<T>(newCenter));
            }
            int changes = assignPointsToClusters(newClusters, points, assignments);
            clusters = newClusters;

            // if there were no more changes in the point-to-cluster assignment
            // and there are no empty clusters left, return the current clusters
            if (changes == 0 && !emptyCluster) {
                return clusters;
            }
        }
        return clusters;
    }

    /**
     * Adds the given points to the closest {@link Cluster}.
     *
     * @param clusters the {@link Cluster}s to add the points to
     * @param points the points to add to the given {@link Cluster}s
     * @param assignments points assignments to clusters
     * @return the number of points assigned to different clusters as the iteration before
     */
    private int assignPointsToClusters(final List<CentroidCluster<T>> clusters,
                                       final Collection<T> points,
                                       final int[] assignments) {
        int assignedDifferently = 0;
        int pointIndex = 0;
        for (final T p : points) {
            int clusterIndex = getNearestCluster(clusters, p);
            if (clusterIndex != assignments[pointIndex]) {
                assignedDifferently++;
            }

            CentroidCluster<T> cluster = clusters.get(clusterIndex);
            cluster.addPoint(p);
            assignments[pointIndex++] = clusterIndex;
        }

        return assignedDifferently;
    }

    /**
     * Use K-means++ to choose the initial centers.
     *
     * @param points the points to choose the initial centers from
     * @return the initial centers
     */
    private List<CentroidCluster<T>> chooseInitialCenters(final Collection<T> points) {

        // Convert to list for indexed access. Make it unmodifiable, since removal of items
        // would screw up the logic of this method.
        final List<T> pointList = Collections.unmodifiableList(new ArrayList<T> (points));

        // The number of points in the list.
        final int numPoints = pointList.size();

        // Set the corresponding element in this array to indicate when
        // elements of pointList are no longer available.
        final boolean[] taken = new boolean[numPoints];

        // The resulting list of initial centers.
        final List<CentroidCluster<T>> resultSet = new ArrayList<CentroidCluster<T>>();

        // Choose one center uniformly at random from among the data points.
        final int firstPointIndex = random.nextInt(numPoints);

        final T firstPoint = pointList.get(firstPointIndex);

        resultSet.add(new CentroidCluster<T>(firstPoint));

        // Must mark it as taken
        taken[firstPointIndex] = true;

        // To keep track of the minimum distance squared of elements of
        // pointList to elements of resultSet.
        final double[] minDistSquared = new double[numPoints];

        // Initialize the elements.  Since the only point in resultSet is firstPoint,
        // this is very easy.
        for (int i = 0; i < numPoints; i++) {
            if (i != firstPointIndex) { // That point isn't considered
                double d = distance(firstPoint, pointList.get(i));
                minDistSquared[i] = d*d;
            }
        }

        while (resultSet.size() < k) {

            // Sum up the squared distances for the points in pointList not
            // already taken.
            double distSqSum = 0.0;

            for (int i = 0; i < numPoints; i++) {
                if (!taken[i]) {
                    distSqSum += minDistSquared[i];
                }
            }

            // Add one new data point as a center. Each point x is chosen with
            // probability proportional to D(x)2
            final double r = random.nextDouble() * distSqSum;

            // The index of the next point to be added to the resultSet.
            int nextPointIndex = -1;

            // Sum through the squared min distances again, stopping when
            // sum >= r.
            double sum = 0.0;
            for (int i = 0; i < numPoints; i++) {
                if (!taken[i]) {
                    sum += minDistSquared[i];
                    if (sum >= r) {
                        nextPointIndex = i;
                        break;
                    }
                }
            }

            // If it's not set to >= 0, the point wasn't found in the previous
            // for loop, probably because distances are extremely small.  Just pick
            // the last available point.
            if (nextPointIndex == -1) {
                for (int i = numPoints - 1; i >= 0; i--) {
                    if (!taken[i]) {
                        nextPointIndex = i;
                        break;
                    }
                }
            }

            // We found one.
            if (nextPointIndex >= 0) {

                final T p = pointList.get(nextPointIndex);

                resultSet.add(new CentroidCluster<T> (p));

                // Mark it as taken.
                taken[nextPointIndex] = true;

                if (resultSet.size() < k) {
                    // Now update elements of minDistSquared.  We only have to compute
                    // the distance to the new center to do this.
                    for (int j = 0; j < numPoints; j++) {
                        // Only have to worry about the points still not taken.
                        if (!taken[j]) {
                            double d = distance(p, pointList.get(j));
                            double d2 = d * d;
                            if (d2 < minDistSquared[j]) {
                                minDistSquared[j] = d2;
                            }
                        }
                    }
                }

            } else {
                // None found --
                // Break from the while loop to prevent
                // an infinite loop.
                break;
            }
        }

        return resultSet;
    }

    /**
     * Get a random point from the {@link Cluster} with the largest distance variance.
     *
     * @param clusters the {@link Cluster}s to search
     * @return a random point from the selected cluster
     * @throws ConvergenceException if clusters are all empty
     */
    private T getPointFromLargestVarianceCluster(final Collection<CentroidCluster<T>> clusters)
            throws ConvergenceException {

        double maxVariance = Double.NEGATIVE_INFINITY;
        Cluster<T> selected = null;
        for (final CentroidCluster<T> cluster : clusters) {
            if (!cluster.getPoints().isEmpty()) {

                // compute the distance variance of the current cluster
                final Clusterable center = cluster.getCenter();
                final Variance stat = new Variance();
                for (final T point : cluster.getPoints()) {
                    stat.increment(distance(point, center));
                }
                final double variance = stat.getResult();

                // select the cluster with the largest variance
                if (variance > maxVariance) {
                    maxVariance = variance;
                    selected = cluster;
                }

            }
        }

        // did we find at least one non-empty cluster ?
        if (selected == null) {
            throw new ConvergenceException(LocalizedFormats.EMPTY_CLUSTER_IN_K_MEANS);
        }

        // extract a random point from the cluster
        final List<T> selectedPoints = selected.getPoints();
        return selectedPoints.remove(random.nextInt(selectedPoints.size()));

    }

    /**
     * Returns the nearest {@link Cluster} to the given point
     *
     * @param clusters the {@link Cluster}s to search
     * @param point the point to find the nearest {@link Cluster} for
     * @return the index of the nearest {@link Cluster} to the given point
     */
    private int getNearestCluster(final Collection<CentroidCluster<T>> clusters, final T point) {
        double minDistance = Double.MAX_VALUE;
        int clusterIndex = 0;
        int minCluster = 0;
        for (final CentroidCluster<T> c : clusters) {
            final double distance = distance(point, c.getCenter());
            if (distance < minDistance) {
                minDistance = distance;
                minCluster = clusterIndex;
            }
            clusterIndex++;
        }
        return minCluster;
    }

    /**
     * Computes the centroid for a set of points.
     *
     * @param points the set of points
     * @return the computed centroid for the set of points
     */
    private Clusterable centroidOf(final Collection<T> points) {
        int dimension = cycles.length;
        final double[] centroid = new double[dimension];
        for(int d = 0; d < dimension; d++) {
            int size = points.size();
            double[] projection = new double[size];
            int i = 0;
            for (final T p : points) {
                projection[i++] = p.getPoint()[d];
            }
            centroid[d] = CyclicDoubleKt.center(projection, cycles[d]);
        }
        return new DoublePoint(centroid);
    }

    public static class CyclicEuclideanDistance implements DistanceMeasure {

        private final double[] cycles;

        public CyclicEuclideanDistance(double[] cycles) {
            this.cycles = cycles;
        }

        public double compute(double[] p1, double[] p2) {
            double sum = 0;
            for (int i = 0; i < p1.length; i++) {
                final double dp = CyclicDoubleKt.distance(p1[i], p2[i], cycles[i]);
                sum += dp * dp;
            }
            return FastMath.sqrt(sum);
        }
    }
}
