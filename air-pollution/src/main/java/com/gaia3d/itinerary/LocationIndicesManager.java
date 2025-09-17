package com.gaia3d.itinerary;

import com.gaia3d.utils.StringModifier;
import lombok.NoArgsConstructor;
import org.locationtech.proj4j.BasicCoordinateTransform;
import org.locationtech.proj4j.CRSFactory;
import org.locationtech.proj4j.CoordinateReferenceSystem;
import org.locationtech.proj4j.ProjCoordinate;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.operation.TransformException;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Vector;

@NoArgsConstructor
public class LocationIndicesManager {
    // location indices sample (data is *.csv file type) :
    // INDEX_ID,CentroidX,CentroidY
    // FP162,900323.2743,1899369.915
    // FQ162,900323.2743,1898369.915
    // FQ163,901323.2743,1898369.915
    // FQ164,902323.2743,1898369.915
    // FQ165,903323.2743,1898369.915
    // ...
    public HashMap<String, LocationIndex> locationIndices = new HashMap<>();

    public boolean loadLocationIndicesFile(String locationIndicesFilePath) throws IOException {
        Charset charset = StandardCharsets.UTF_8;
        File file = new File(locationIndicesFilePath);    //creates a new file instance
        FileReader fr = new FileReader(file, charset);   //reads the file
        BufferedReader br = new BufferedReader(fr);  //creates a buffering character input stream

        String line;
        Boolean finished = false;
        Integer counter = 0;
        String delimiter = ",";

        int columnsCount = 0;
        int rowsCount = 0;

        // read lines
        // CSV file sample********************************************************************************
        // INDEX_ID,CentroidX,CentroidY
        // FP162,900323.2743,1899369.915
        // FQ162,900323.2743,1898369.915
        // ...
        //----------------------------------------------------------------------------------------------------
        int valuesLinesCount = 0;
        ArrayList<String> vecTitles = new ArrayList<>();
        HashMap<String, ArrayList<String>> mapTitle_vecStrings = new HashMap<String, ArrayList<String>>();
        Vector<String> vecStrings = new Vector<>();
        boolean skipEmptyStrings = false;
        while (!finished) {
            line = br.readLine();
            if (line == null) {
                finished = true;
                break;
            }

            counter += 1;
            vecStrings.clear();
            StringModifier.splitString(line, delimiter, vecStrings, skipEmptyStrings);

            if (counter == 1) {
                // this is the 1rst line
                vecTitles.addAll(vecStrings);
            } else {
                valuesLinesCount += 1;
                int stringsCount = vecStrings.size();  // always is 3
                String indexId = vecStrings.get(0);
                double centroidX = Double.parseDouble(vecStrings.get(1));
                double centroidY = Double.parseDouble(vecStrings.get(2));

                LocationIndex locationIndex = new LocationIndex();
                locationIndex.indexId = indexId;
                locationIndex.centroidX = centroidX;
                locationIndex.centroidY = centroidY;
                locationIndices.put(indexId, locationIndex);
            }
        }
        br.close();
        fr.close();
        return true;
    }

    private ProjCoordinate transformToWGS84(CoordinateReferenceSystem source, ProjCoordinate coordinate) {
        CRSFactory factory = new CRSFactory();
        CoordinateReferenceSystem wgs84 = factory.createFromParameters("WGS84", "+proj=longlat +datum=WGS84 +no_defs");
        BasicCoordinateTransform transformer = new BasicCoordinateTransform(source, wgs84);
        ProjCoordinate result = new ProjCoordinate();
        transformer.transform(coordinate, result);
        return result;
    }

    public void convertLocationIndicesTo4326() throws FactoryException, TransformException {
        // convert location indices to 4326
        // https://www.osgeo.kr/17
        CRSFactory factory = new CRSFactory();
        CoordinateReferenceSystem inputCrs = null;
        String proj = "+proj=tmerc +lat_0=38 +lon_0=127.5 +k=0.9996 +x_0=1000000 +y_0=2000000 +ellps=bessel +units=m +no_defs +towgs84=-115.80,474.99,674.11,1.16,-2.31,-1.63,6.43";
        if (proj != null && !proj.isEmpty()) {
            inputCrs = factory.createFromParameters("CUSTOM", proj);
        }

        //CoordinateReferenceSystem crs4326 = null;
        //CoordinateReferenceSystem inputCrs = CRS.decode("+proj=tmerc +lat_0=38 +lon_0=127.5 +k=0.9996 +x_0=1000000 +y_0=2000000 +ellps=bessel +units=m +no_defs +towgs84=-115.80,474.99,674.11,1.16,-2.31,-1.63,6.43"); // korea 2000
        //CoordinateReferenceSystem inputCrs = CRS.decode("EPSG:5179"); // UTMK
        //CoordinateReferenceSystem crs4326 = CRS.decode("EPSG:4326");
        //MathTransform transform = CRS.findMathTransform(inputCrs, crs4326);

        double[] srcPts = new double[2];
        //double[] dstPts = new double[2];
        for (LocationIndex locationIndex : locationIndices.values()) {
            srcPts[0] = locationIndex.centroidX;
            srcPts[1] = locationIndex.centroidY;

            //transform.transform(srcPts, 0, dstPts, 0, 1);
            ProjCoordinate projCoordinate = new ProjCoordinate(srcPts[0], srcPts[1], 0.0);
            ProjCoordinate result = transformToWGS84(inputCrs, projCoordinate);
            locationIndex.latitudeDeg = result.y;
            locationIndex.longitudeDeg = result.x;

            int hola = 0;
        }

        int hola = 0;
    }
}
