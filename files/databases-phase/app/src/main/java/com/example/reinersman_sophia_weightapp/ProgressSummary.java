package com.example.reinersman_sophia_weightapp;

public class ProgressSummary {
    public final int entryCount;
    public final Double latestWeight;
    public final Double startingWeight;

    public ProgressSummary(int entryCount, Double latestWeight, Double startingWeight) {
        this.entryCount = entryCount;
        this.latestWeight = latestWeight;
        this.startingWeight = startingWeight;
    }

    public Double getChangeFromStart() {
        if (latestWeight == null || startingWeight == null) {
            return null;
        }
        return latestWeight - startingWeight;
    }
}