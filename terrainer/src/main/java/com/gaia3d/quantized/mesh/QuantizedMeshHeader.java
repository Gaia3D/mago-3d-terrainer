package com.gaia3d.quantized.mesh;

import com.gaia3d.io.LittleEndianDataInputStream;
import com.gaia3d.io.LittleEndianDataOutputStream;
import lombok.Getter;
import lombok.Setter;

import java.io.IOException;

@Getter
@Setter
public class QuantizedMeshHeader {
    //https://github.com/CesiumGS/quantized-mesh
    // The center of the tile in Earth-centered Fixed coordinates.
    private double CenterX = 0.0;
    private double CenterY = 0.0;
    private double CenterZ = 0.0;

    // The minimum and maximum heights in the area covered by this tile.
    // The minimum may be lower and the maximum may be higher than
    // the height of any vertex in this tile in the case that the min/max vertex
    // was removed during mesh simplification, but these are the appropriate
    // values to use for analysis or visualization.
    private float MinimumHeight = 0.0f;
    private float MaximumHeight = 0.0f;

    // The tile’s bounding sphere.  The X,Y,Z coordinates are again expressed
    // in Earth-centered Fixed coordinates, and the radius is in meters.
    private double BoundingSphereCenterX = 0.0;
    private double BoundingSphereCenterY = 0.0;
    private double BoundingSphereCenterZ = 0.0;
    private double BoundingSphereRadius = 0.0;

    // The horizon occlusion point, expressed in the ellipsoid-scaled Earth-centered Fixed frame.
    // If this point is below the horizon, the entire tile is below the horizon.
    // See http://cesiumjs.org/2013/04/25/Horizon-culling/ for more information.
    private double HorizonOcclusionPointX = 0.0;
    private double HorizonOcclusionPointY = 0.0;
    private double HorizonOcclusionPointZ = 0.0;

    public void loadDataInputStream(LittleEndianDataInputStream dataInputStream) throws IOException {
        CenterX = dataInputStream.readDouble();
        CenterY = dataInputStream.readDouble();
        CenterZ = dataInputStream.readDouble();

        MinimumHeight = dataInputStream.readFloat();
        MaximumHeight = dataInputStream.readFloat();

        BoundingSphereCenterX = dataInputStream.readDouble();
        BoundingSphereCenterY = dataInputStream.readDouble();
        BoundingSphereCenterZ = dataInputStream.readDouble();
        BoundingSphereRadius = dataInputStream.readDouble();

        HorizonOcclusionPointX = dataInputStream.readDouble();
        HorizonOcclusionPointY = dataInputStream.readDouble();
        HorizonOcclusionPointZ = dataInputStream.readDouble();
    }

    public void saveDataOutputStream(LittleEndianDataOutputStream dataOutputStream) throws IOException {
        dataOutputStream.writeDouble(CenterX);
        dataOutputStream.writeDouble(CenterY);
        dataOutputStream.writeDouble(CenterZ);

        dataOutputStream.writeFloat(MinimumHeight);
        dataOutputStream.writeFloat(MaximumHeight);

        dataOutputStream.writeDouble(BoundingSphereCenterX);
        dataOutputStream.writeDouble(BoundingSphereCenterY);
        dataOutputStream.writeDouble(BoundingSphereCenterZ);
        dataOutputStream.writeDouble(BoundingSphereRadius);

        dataOutputStream.writeDouble(HorizonOcclusionPointX);
        dataOutputStream.writeDouble(HorizonOcclusionPointY);
        dataOutputStream.writeDouble(HorizonOcclusionPointZ);
    }
}
