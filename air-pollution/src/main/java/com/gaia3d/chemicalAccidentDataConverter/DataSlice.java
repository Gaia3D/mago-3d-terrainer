package com.gaia3d.chemicalAccidentDataConverter;

public class DataSlice
{
    public int columnsCount = 0;
    public int rowsCount = 0;

    public String fileName = "";
    public String imagefileName = ""; // used only in chemical accident 2D.***
    public double minValue = 0.0;
    public double maxValue = 0.0;

    public double minAltitude = 0.0;
    public double maxAltitude = 0.0;

    public double[] values = null;

    public DataSlice()
    {

    }

    public double getValue(int col, int row)
    {
        return values[row * columnsCount + col];
    }
    public void makeFAKEData_FORTEST(double radiusFactor, double value)
    {
        // this is a test function.***
        int centerCol = columnsCount / 2;
        int centerRow = rowsCount / 2;
        double radius = (Math.min(columnsCount, rowsCount) / 2)*radiusFactor;
        double radiusSquared = radius * radius;
        int insideCount = 0;
        int outsideCount = 0;
        for(int row = 0; row < rowsCount; row++)
        {
            for(int col = 0; col < columnsCount; col++)
            {
                double colDist = col - centerCol;
                double rowDist = row - centerRow;
                double distanceSquared = colDist * colDist + rowDist * rowDist;

                if(distanceSquared < radiusSquared) {
                    values[row * columnsCount + col] = value;
                    insideCount++;
                }
                else {
                    values[row * columnsCount + col] = 0.0;
                    outsideCount++;
                }

            }
        }

        int hola = 0;
    }

    public void makeFAKEData_FORTEST_2(double radiusFactor, double value)
    {
        // this is a test function.***
        int centerCol = 0;
        int centerRow = rowsCount - 1;
        double radius = (Math.min(columnsCount, rowsCount) / 2)*radiusFactor;
        double radiusSquared = radius * radius;
        int insideCount = 0;
        int outsideCount = 0;
        for(int row = 0; row < rowsCount; row++)
        {
            for(int col = 0; col < columnsCount; col++)
            {
                double colDist = col - centerCol;
                double rowDist = row - centerRow;
                double distanceSquared = colDist * colDist + rowDist * rowDist;

                if(distanceSquared < radiusSquared) {
                    values[row * columnsCount + col] = value;
                    insideCount++;
                }
                else {
                    values[row * columnsCount + col] = 0.0;
                    outsideCount++;
                }

            }
        }

        int hola = 0;
    }
}
