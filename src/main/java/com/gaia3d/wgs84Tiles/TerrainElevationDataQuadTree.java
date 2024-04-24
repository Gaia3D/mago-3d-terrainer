package com.gaia3d.wgs84Tiles;

import com.gaia3d.basic.structure.GeographicExtension;

import java.util.ArrayList;

public class TerrainElevationDataQuadTree
{
    // A region is represented by multiple small geoTiff files.***
    // Here, we use a quadtree to represent a region.***
    // The quadtree is a tree data structure in which each internal node has exactly four children.***
    TerrainElevationDataQuadTree parent = null;
    GeographicExtension geographicExtension = new GeographicExtension();
    TerrainElevationDataQuadTree[] children = null;
    ArrayList<TerrainElevationData> terrainElevationDataList = new ArrayList<TerrainElevationData>();
    int depth = 0;

    public TerrainElevationDataQuadTree(TerrainElevationDataQuadTree parent)
    {
        this.parent = parent;
        if(parent != null)
        {
            this.depth = parent.depth + 1;
        }
    }

    public void deleteObjects()
    {
        if(terrainElevationDataList != null)
        {
            int terrainElevationDataCount = terrainElevationDataList.size();
            for(int i=0; i<terrainElevationDataCount; i++)
            {
                TerrainElevationData terrainElevationData = terrainElevationDataList.get(i);
                terrainElevationData.deleteObjects();
            }
        }

        if(children != null)
        {
            for(int i=0; i<4; i++)
            {
                children[i].deleteObjects();
            }
        }

        terrainElevationDataList = null;
        children = null;
        parent = null;
        geographicExtension.deleteObjects();
        geographicExtension = null;
    }
    public void addTerrainElevationData(TerrainElevationData terrainElevationData)
    {
        terrainElevationDataList.add(terrainElevationData);
    }

    public void calculateGeographicExtension()
    {
        int terrainElevationDataCount = terrainElevationDataList.size();
        for(int i=0; i<terrainElevationDataCount; i++)
        {
            TerrainElevationData terrainElevationData = terrainElevationDataList.get(i);
            GeographicExtension geographicExtension = terrainElevationData.geographicExtension;

            if(i==0)
            {
                this.geographicExtension.copyFrom(geographicExtension);
            }
            else
            {
                this.geographicExtension.union(geographicExtension);
            }
        }
    }

    private void makeTree(int maxDepth)
    {
        // if my datas count is less than 2, then return.***
        if(terrainElevationDataList.size() < 2)
        {
            return;
        }

        // 1rst create child nodes if no exist.***
        if(children == null)
        {
            children = new TerrainElevationDataQuadTree[4];
            children[0] = new TerrainElevationDataQuadTree(this);
            children[1] = new TerrainElevationDataQuadTree(this);
            children[2] = new TerrainElevationDataQuadTree(this);
            children[3] = new TerrainElevationDataQuadTree(this);

            // set geographicExtension for children.***
            //    +----+----+
            //    | 3  | 2  |
            //    +----+----+
            //    | 0  | 1  |
            //    +----+----+
            GeographicExtension geographicExtension = this.geographicExtension;
            double midLonDeg = geographicExtension.getMidLongitudeDeg();
            double midLatDeg = geographicExtension.getMidLatitudeDeg();

            double minLonDeg = geographicExtension.getMinLongitudeDeg();
            double minLatDeg = geographicExtension.getMinLatitudeDeg();
            double maxLonDeg = geographicExtension.getMaxLongitudeDeg();
            double maxLatDeg = geographicExtension.getMaxLatitudeDeg();

            GeographicExtension geographicExtension0 = children[0].geographicExtension;
            geographicExtension0.setDegrees(minLonDeg, minLatDeg, 0.0, midLonDeg, midLatDeg, 0.0);

            GeographicExtension geographicExtension1 = children[1].geographicExtension;
            geographicExtension1.setDegrees(midLonDeg, minLatDeg, 0.0, maxLonDeg, midLatDeg, 0.0);

            GeographicExtension geographicExtension2 = children[2].geographicExtension;
            geographicExtension2.setDegrees(midLonDeg, midLatDeg, 0.0, maxLonDeg, maxLatDeg, 0.0);

            GeographicExtension geographicExtension3 = children[3].geographicExtension;
            geographicExtension3.setDegrees(minLonDeg, midLatDeg, 0.0, midLonDeg, maxLatDeg, 0.0);
        }

        // 2nd distribute data to children.***
        int terrainElevationDataCount = terrainElevationDataList.size();
        for(int i=0; i<terrainElevationDataCount; i++)
        {
            TerrainElevationData terrainElevationData = terrainElevationDataList.get(i);
            GeographicExtension geographicExtension = terrainElevationData.geographicExtension;

            for(int j=0; j<4; j++)
            {
                if(children[j].geographicExtension.intersects(geographicExtension))
                {
                    children[j].addTerrainElevationData(terrainElevationData);
                }
            }
        }

        // now remove all data from this node.***
        terrainElevationDataList.clear();

        if(this.depth < maxDepth)
        {
            // continue making children.***
            for(int j=0; j<4; j++)
            {
                children[j].makeTree(maxDepth);
            }
        }
    }

    public void deleteCoverage()
    {
        if(terrainElevationDataList != null)
        {
            int terrainElevationDataCount = terrainElevationDataList.size();
            for(int i=0; i<terrainElevationDataCount; i++)
            {
                TerrainElevationData terrainElevationData = terrainElevationDataList.get(i);
                terrainElevationData.deleteCoverage();
            }
        }

        if(children != null)
        {
            for(int i=0; i<4; i++)
            {
                children[i].deleteCoverage();
            }
        }
    }

    public void deleteCoverageIfNotIntersects(GeographicExtension geographicExtension)
    {
        if(terrainElevationDataList != null)
        {
            int terrainElevationDataCount = terrainElevationDataList.size();
            for(int i=0; i<terrainElevationDataCount; i++)
            {
                TerrainElevationData terrainElevationData = terrainElevationDataList.get(i);
                if(geographicExtension.intersects(terrainElevationData.geographicExtension) == false)
                {
                    terrainElevationData.deleteCoverage();
                }
            }
        }

        if(children != null)
        {
            for(int i=0; i<4; i++)
            {
                if(geographicExtension.intersects(children[i].geographicExtension) == false)
                {
                    children[i].deleteCoverage();
                }
                else
                {
                    children[i].deleteCoverageIfNotIntersects(geographicExtension);
                }
            }
        }
    }

    public void makeQuadTree(int maxDepth)
    {
        calculateGeographicExtension();
        makeTree(maxDepth);
    }

    public void getTerrainElevationDatasArray(double lonDeg, double latDeg, ArrayList<TerrainElevationData> resultTerrainElevDataArray)
    {
        int terrainElevationDataCount = terrainElevationDataList.size();
        for(int i=0; i<terrainElevationDataCount; i++)
        {
            TerrainElevationData terrainElevationData = terrainElevationDataList.get(i);
            GeographicExtension geographicExtension = terrainElevationData.geographicExtension;

            if(geographicExtension.intersects(lonDeg, latDeg))
            {
                resultTerrainElevDataArray.add(terrainElevationData);
                //break;
            }
        }

        if(children != null)
        {
            // check children.***
            for(int j=0; j<4; j++)
            {
                if(children[j].geographicExtension.intersects(lonDeg, latDeg))
                {
                    children[j].getTerrainElevationDatasArray(lonDeg, latDeg, resultTerrainElevDataArray);
                    break;
                }
            }
        }

    }


    public TerrainElevationData getTerrainElevationData(double lonDeg, double latDeg)
    {
        // function used in "TileWgs84 class : public boolean mustRefineTriangle(GaiaTriangle triangle) throws TransformException, IOException {"
        // to know the pixelSizeDegree.***
        TerrainElevationData resultTerrainElevationData = null;

        int terrainElevationDataCount = terrainElevationDataList.size();
        for(int i=0; i<terrainElevationDataCount; i++)
        {
            TerrainElevationData terrainElevationData = terrainElevationDataList.get(i);
            GeographicExtension geographicExtension = terrainElevationData.geographicExtension;

            if(geographicExtension.intersects(lonDeg, latDeg))
            {
                resultTerrainElevationData = terrainElevationData;
                break;
            }
        }

        if(resultTerrainElevationData == null)
        {
            if(children != null)
            {
                // check children.***
                for(int j=0; j<4; j++)
                {
                    if(children[j].geographicExtension.intersects(lonDeg, latDeg))
                    {
                        resultTerrainElevationData = children[j].getTerrainElevationData(lonDeg, latDeg);
                        break;
                    }
                }
            }
        }

        return resultTerrainElevationData;
    }
}
