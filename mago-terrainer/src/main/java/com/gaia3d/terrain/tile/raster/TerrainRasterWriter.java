package com.gaia3d.terrain.tile.raster;

import com.gaia3d.command.GlobalOptions;
import com.gaia3d.terrain.util.NoDataUtils;
import org.eclipse.imagen.media.range.NoDataContainer;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.util.CoverageUtilities;

import java.awt.image.Raster;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public final class TerrainRasterWriter {
    private static final int FLOATS_PER_CHUNK = 8192;
    private static final double LONGITUDE_EPSILON = 1.0e-9;

    public void write(Path path, TerrainRasterData data) throws IOException {
        Path parent = path.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }

        try (BufferedOutputStream outputStream = new BufferedOutputStream(Files.newOutputStream(path))) {
            outputStream.write(createHeader(data).array());
            writePayload(outputStream, data);
        }
    }

    public void write(Path path, GridCoverage2D coverage) throws IOException {
        Raster raster = coverage.getRenderedImage().getData();
        int width = raster.getWidth();
        int height = raster.getHeight();
        double sourceNoDataValue = resolveNoDataValue(coverage);
        double configuredNoDataValue = GlobalOptions.getInstance().getNoDataValue();
        float noDataValue = TerrainRasterFormat.NO_DATA_VALUE;
        float[] elevations = new float[Math.multiplyExact(width, height)];
        int index = 0;
        int maxX = raster.getMinX() + width;
        int maxY = raster.getMinY() + height;
        for (int y = raster.getMinY(); y < maxY; y++) {
            for (int x = raster.getMinX(); x < maxX; x++) {
                double sample = raster.getSampleDouble(x, y, 0);
                float elevation = (float) sample;
                elevations[index++] = NoDataUtils.isNoData(sample, sourceNoDataValue, configuredNoDataValue) || !Float.isFinite(elevation)
                        ? noDataValue : elevation;
            }
        }

        var bounds = coverage.getEnvelope();
        writeNormalizedLongitude(path, new TerrainRasterData(width, height,
                bounds.getMinimum(0), bounds.getMinimum(1),
                bounds.getMaximum(0), bounds.getMaximum(1),
                noDataValue, elevations));
    }

    /**
     * Writes the raster with longitudes in -180..180. Rasters in the 0..360 convention (common for planetary data)
     * are shifted by -360, and a raster crossing the antimeridian is split at 180 into two files
     * (the second one with the suffix "-shifted"). The samples are not changed, only the bounds.
     * @return the written files
     */
    public List<Path> writeNormalizedLongitude(Path path, TerrainRasterData data) throws IOException {
        double minLongitude = data.minLongitude();
        double maxLongitude = data.maxLongitude();
        double shift = 0.0;
        if (minLongitude >= 180.0 - LONGITUDE_EPSILON) {
            shift = -360.0;
        } else if (maxLongitude <= -180.0 + LONGITUDE_EPSILON) {
            shift = 360.0;
        }
        minLongitude += shift;
        maxLongitude += shift;

        double splitLongitude;
        double splitShift;
        if (maxLongitude > 180.0 + LONGITUDE_EPSILON) {
            splitLongitude = 180.0;
            splitShift = -360.0;
        } else if (minLongitude < -180.0 - LONGITUDE_EPSILON) {
            splitLongitude = -180.0;
            splitShift = 360.0;
        } else {
            write(path, withLongitudes(data, 0, data.width(), minLongitude, maxLongitude));
            return List.of(path);
        }

        double pixelWidth = (maxLongitude - minLongitude) / data.width();
        int splitColumn = (int) Math.round((splitLongitude - minLongitude) / pixelWidth);
        splitColumn = Math.max(1, Math.min(data.width() - 1, splitColumn));
        double splitBound = minLongitude + splitColumn * pixelWidth;

        // a global raster (e.g. 0..360) is rotated into one -180..180 raster: its two edges meet inside the raster,
        // so the samples there stay continuous (two rasters would only touch there)
        if (maxLongitude - minLongitude >= 360.0 - pixelWidth * 0.5) {
            boolean beyondEast = splitShift < 0.0;
            int firstColumns = beyondEast ? data.width() - splitColumn : splitColumn;
            float[] source = data.elevations();
            float[] rotated = new float[source.length];
            int width = data.width();
            for (int row = 0; row < data.height(); row++) {
                int rowOffset = row * width;
                if (beyondEast) {
                    // the columns beyond 180 become the west part
                    System.arraycopy(source, rowOffset + splitColumn, rotated, rowOffset, firstColumns);
                    System.arraycopy(source, rowOffset, rotated, rowOffset + firstColumns, splitColumn);
                } else {
                    // the columns before -180 become the east part
                    System.arraycopy(source, rowOffset + splitColumn, rotated, rowOffset, width - splitColumn);
                    System.arraycopy(source, rowOffset, rotated, rowOffset + width - splitColumn, splitColumn);
                }
            }
            double rotatedMinLongitude = beyondEast ? splitBound - 360.0 : splitBound;
            write(path, TerrainRasterData.takeOwnership(width, data.height(), width, data.height(),
                    rotatedMinLongitude, data.minLatitude(), rotatedMinLongitude + width * pixelWidth, data.maxLatitude(),
                    data.noDataValue(), rotated));
            return List.of(path);
        }

        Path shiftedPath = path.resolveSibling(withSuffix(path.getFileName().toString(), "-shifted"));
        boolean shiftWest = splitShift > 0.0;
        // the part beyond the antimeridian is moved to the other side
        TerrainRasterData west = withLongitudes(data, 0, splitColumn, minLongitude + (shiftWest ? splitShift : 0.0), splitBound + (shiftWest ? splitShift : 0.0));
        TerrainRasterData east = withLongitudes(data, splitColumn, data.width(), splitBound + (shiftWest ? 0.0 : splitShift), maxLongitude + (shiftWest ? 0.0 : splitShift));
        write(path, west);
        write(shiftedPath, east);
        return List.of(path, shiftedPath);
    }

    private static TerrainRasterData withLongitudes(TerrainRasterData data, int fromColumn, int toColumn, double minLongitude, double maxLongitude) {
        boolean allColumns = fromColumn == 0 && toColumn == data.width();
        if (allColumns && minLongitude == data.minLongitude() && maxLongitude == data.maxLongitude()) {
            return data;
        }
        int width = toColumn - fromColumn;
        int height = data.height();
        float[] source = data.elevations();
        float[] elevations = source;
        if (!allColumns) {
            elevations = new float[Math.multiplyExact(width, height)];
            for (int row = 0; row < height; row++) {
                System.arraycopy(source, row * data.width() + fromColumn, elevations, row * width, width);
            }
        }
        return TerrainRasterData.takeOwnership(width, height, width, height,
                minLongitude, data.minLatitude(), maxLongitude, data.maxLatitude(), data.noDataValue(), elevations);
    }

    private static String withSuffix(String fileName, String suffix) {
        int extensionIndex = fileName.lastIndexOf('.');
        return extensionIndex > 0 ? fileName.substring(0, extensionIndex) + suffix + fileName.substring(extensionIndex) : fileName + suffix;
    }

    private double resolveNoDataValue(GridCoverage2D coverage) {
        NoDataContainer noData = CoverageUtilities.getNoDataProperty(coverage);
        return noData == null ? Double.NaN : noData.getAsSingleValue();
    }

    private ByteBuffer createHeader(TerrainRasterData data) {
        ByteBuffer header = ByteBuffer.allocate(TerrainRasterFormat.HEADER_SIZE_BYTES).order(TerrainRasterFormat.BYTE_ORDER);
        header.put(TerrainRasterFormat.MAGIC);
        header.putInt(TerrainRasterFormat.VERSION);
        header.putInt(TerrainRasterFormat.HEADER_SIZE_BYTES);
        header.putInt(data.width());
        header.putInt(data.height());
        header.putDouble(data.minLongitude());
        header.putDouble(data.minLatitude());
        header.putDouble(data.maxLongitude());
        header.putDouble(data.maxLatitude());
        header.putFloat(data.noDataValue());
        header.putInt(data.originalWidth());
        header.putInt(data.originalHeight());
        return header;
    }

    private void writePayload(BufferedOutputStream outputStream, TerrainRasterData data) throws IOException {
        ByteBuffer payload = ByteBuffer.allocate(FLOATS_PER_CHUNK * TerrainRasterFormat.FLOAT_BYTES).order(TerrainRasterFormat.BYTE_ORDER);

        for (int i = 0; i < data.getSampleCount(); i++) {
            payload.putFloat(data.getElevationAtIndex(i));
            if (!payload.hasRemaining()) {
                outputStream.write(payload.array());
                payload.clear();
            }
        }

        int remainingBytes = payload.position();
        if (remainingBytes > 0) {
            outputStream.write(payload.array(), 0, remainingBytes);
        }
    }
}
