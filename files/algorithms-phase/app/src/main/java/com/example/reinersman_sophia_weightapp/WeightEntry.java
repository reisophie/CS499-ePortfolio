package com.example.reinersman_sophia_weightapp;

public class WeightEntry {
    public long id;
    public String date;
    public double weight;

    public WeightEntry(long id, String date, double weight) {
        this.id = id;
        this.date = date;
        this.weight = weight;
    }
}