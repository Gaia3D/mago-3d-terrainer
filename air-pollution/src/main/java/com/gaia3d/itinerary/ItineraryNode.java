package com.gaia3d.itinerary;

public class ItineraryNode {
    public int year;
    public int month;
    public int day;
    public int hour;
    public int minute;

    public double longitudeDeg;
    public double latitudeDeg;

    public String indexId;

    public ItineraryNode() {
        year = 2023;
        month = 1;
        day = 1;
        hour = 0;
        minute = 0;
        longitudeDeg = 0.0;
        latitudeDeg = 0.0;
        indexId = "";
    }
}
