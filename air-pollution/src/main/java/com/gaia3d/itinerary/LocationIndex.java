package com.gaia3d.itinerary;

public class LocationIndex {
    // location indices sample (data is *.csv file type) :
    // INDEX_ID,CentroidX,CentroidY
    // FP162,900323.2743,1899369.915
    // FQ162,900323.2743,1898369.915
    // FQ163,901323.2743,1898369.915
    // FQ164,902323.2743,1898369.915
    // FQ165,903323.2743,1898369.915
    // ...
    public String indexId;
    public double centroidX;
    public double centroidY;

    public double longitudeDeg;
    public double latitudeDeg;

}
