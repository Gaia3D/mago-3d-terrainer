package com.gaia3d.terrain.tile.raster;

import com.gaia3d.command.GlobalOptions;
import org.eclipse.imagen.media.range.NoDataContainer;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.util.CoverageUtilities;

import java.awt.image.Raster;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;

public final class TerrainRasterWriter {
    private static final int FLOATS_PER_CHUNK = 8192;

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
                elevations[index++] = isNoData(sample, sourceNoDataValue, configuredNoDataValue) || !Float.isFinite(elevation)
                        ? noDataValue : elevation;
            }
        }

        var bounds = coverage.getEnvelope();
        write(path, new TerrainRasterData(width, height,
                bounds.getMinimum(0), bounds.getMinimum(1),
                bounds.getMaximum(0), bounds.getMaximum(1),
                noDataValue, elevations));
    }

    private double resolveNoDataValue(GridCoverage2D coverage) {
        NoDataContainer noData = CoverageUtilities.getNoDataProperty(coverage);
        return noData == null ? Double.NaN : noData.getAsSingleValue();
    }

    private boolean isNoData(double sample, double sourceNoDataValue, double configuredNoDataValue) {
        return !Double.isFinite(sample)
                || matchesNoData(sample, sourceNoDataValue)
                || matchesNoData(sample, configuredNoDataValue);
    }

    private boolean matchesNoData(double sample, double noDataValue) {
        return !Double.isNaN(noDataValue) && Double.compare(sample, noDataValue) == 0;
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
