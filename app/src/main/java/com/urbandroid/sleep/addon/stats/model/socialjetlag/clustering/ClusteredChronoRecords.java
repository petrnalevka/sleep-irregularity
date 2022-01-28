package com.urbandroid.sleep.addon.stats.model.socialjetlag.clustering;

import com.urbandroid.common.logging.Logger;
import com.urbandroid.sleep.addon.stats.model.socialjetlag.ChronoRecord;
import com.urbandroid.sleep.addon.stats.model.socialjetlag.ChronoRecords;
import com.urbandroid.sleep.addon.stats.model.socialjetlag.CyclicDoubleKt;
import com.urbandroid.sleep.addon.stats.model.socialjetlag.CyclicFloatKt;

import org.apache.commons.math3.ml.clustering.CentroidCluster;
import org.apache.commons.math3.ml.distance.DistanceMeasure;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ClusteredChronoRecords {

    //The first coordinate is wakeup time - cycle 24 (hours per day).
    //The second is sleep length - it is not a cyclic variable,
    //so I set the cycle length artificially long enough - 1000.
    private static final double[] cycles = new double[]{24.0, 1000.0};

    private static final int outliersCountPerDimensionAndDirection = 2;
    public  static final int MIN_RECORDS = 30;

    private final double clusteringStrength;
    private final Map<ChronoRecord,SleepLabel> labels = new HashMap<>();

    public ClusteredChronoRecords(ChronoRecords records) {

        long t0 = System.currentTimeMillis();

        if (records.size() < MIN_RECORDS) {
            clusteringStrength = 0.0;
        }
        else {
            List<ChronoRecord> allRecords = new ArrayList<>(records.getRecords().values());
            CoreAndOutliers<ChronoRecord> coreAndOutliers = findOutliers(allRecords);

            double oneClusterInertia = getAverageSquareDistance(cluster(coreAndOutliers.getCore(), 1));
            List<CentroidCluster<ChronoRecordClusterable>> twoClusters = cluster(coreAndOutliers.getCore(), 2);
            double twoClustersInertia = getAverageSquareDistance(twoClusters);
            this.clusteringStrength = oneClusterInertia / twoClustersInertia;

            assignLabels(twoClusters, coreAndOutliers.getOutliers());
        }

        long t1 = System.currentTimeMillis();
        Logger.logDebug("ClusteredChronoRecords elapsedTime: "+(t1-t0)+" strength: "+clusteringStrength);
    }

    private void assignLabels(List<CentroidCluster<ChronoRecordClusterable>> clusters,
                              Collection<ChronoRecord> outliers)
    {
        CentroidCluster<ChronoRecordClusterable> freeDays, busyDays;
        if (clusters.get(0).getPoints().size() < clusters.get(1).getPoints().size()) {
            freeDays = clusters.get(0);
            busyDays = clusters.get(1);
        } else {
            freeDays = clusters.get(1);
            busyDays = clusters.get(0);
        }

        for(ChronoRecordClusterable r : freeDays.getPoints()) {
            labels.put(r.getRecord(), SleepLabel.FREE_DAY);
        }

        for(ChronoRecordClusterable r : busyDays.getPoints()) {
            labels.put(r.getRecord(), SleepLabel.BUSY_DAY);
        }

        for(ChronoRecord r : outliers) {
            labels.put(r, SleepLabel.OUTLIER);
        }
    }

    private List<CentroidCluster<ChronoRecordClusterable>> cluster(Collection<ChronoRecord> elements, int noOfClusters) {

        KMeansPlusPlusClusterer<ChronoRecordClusterable> clusterer =
                new KMeansPlusPlusClusterer<>(noOfClusters, 300, cycles);

        List<ChronoRecordClusterable> elements2 = new ArrayList<>();
        for(ChronoRecord element : elements) {
            elements2.add(new ChronoRecordClusterable(element));
        }
        return clusterer.cluster(elements2);
    }

    private double getAverageSquareDistance(List<CentroidCluster<ChronoRecordClusterable>> clusters) {
        DistanceMeasure distanceMeasure = new KMeansPlusPlusClusterer.CyclicEuclideanDistance(cycles);
        double totalDistance = 0;
        int totalCount = 0;
        for(CentroidCluster<ChronoRecordClusterable> cluster : clusters) {
            for(ChronoRecordClusterable element : cluster.getPoints()) {
                double distance = distanceMeasure.compute(cluster.getCenter().getPoint(), element.getPoint());
                totalDistance += distance*distance;
                totalCount++;
            }
        }
        return totalDistance / totalCount;
    }

    private CoreAndOutliers<ChronoRecord> findOutliers(final List<ChronoRecord> records) {

        CoreAndOutliers<ChronoRecord> co = CoreAndOutliers.compoundQuantileDistance(

                records, 0.025, 5,

                new DoubleFunction<ChronoRecord>() {
                    @Override
                    public double apply(ChronoRecord record) {
                        return record.getLength();
                    }
                },

                new DoubleFunction<ChronoRecord>() {

                    final float meanToHour;

                    {
                        int size = records.size();
                        float[] toHours = new float[size];
                        for(int i=0; i<size; i++) {
                            toHours[i] = getToHour(records.get(i));
                        }
                        meanToHour = CyclicFloatKt.center(toHours, 24);
                    }

                    @Override
                    public double apply(ChronoRecord record) {
                        return CyclicDoubleKt.signedDistance(getToHour(record), meanToHour, 24);
                    }

                    private float getToHour(ChronoRecord record) {
                        return record.getToHour();
                    }
                }
        );

        return co;
    }

    public SleepLabel getLabel(ChronoRecord record) {
        if (labels.containsKey(record)) {
            return labels.get(record);
        } else {
            return SleepLabel.OUTLIER;
        }
    }

    public ChronoRecords getLabeledRecords(SleepLabel label) {
        Set<ChronoRecord> result = new HashSet<>();
        for(Map.Entry<ChronoRecord, SleepLabel> entry : labels.entrySet()) {
            if (entry.getValue() == label) {
                result.add(entry.getKey());
            }
        }
        return new ChronoRecords(result);
    }

    public double getClusteringStrength() {
        return clusteringStrength;
    }
}
