package com.gaia3d.image;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.io.File;
import java.io.IOException;

public class TextureUtils {
    public static void getMosaicColumnsAndRows(int textureWidth, int textureHeight, int numSlices, int[] result) {
        // The webgl texture maxSize is 16000.
        float maxTextureSize = 16000.0F;

        float mosaicXCount = (float) Math.floor(maxTextureSize / (float) textureWidth);
        float mosaicYCount = (float) Math.floor(maxTextureSize / (float) textureHeight);

        float totalSubTexCount = mosaicXCount * mosaicYCount;
        if (totalSubTexCount > (float) numSlices) {
            mosaicXCount = (float) Math.ceil(Math.sqrt((float) numSlices));
            mosaicYCount = (float) Math.ceil((float) numSlices / mosaicXCount);

            result[0] = (int) mosaicXCount;
            result[1] = (int) mosaicYCount;
        }
    }

    public static BufferedImage loadBufferedImage(String imageFilePath) throws IOException {
        BufferedImage bufferedImage = ImageIO.read(new File(imageFilePath));
        return bufferedImage;
    }

    public static Texture2D loadTexture(String imageFilePath) throws IOException {
        BufferedImage bufferedImage = ImageIO.read(new File(imageFilePath));
        Texture2D texture = new Texture2D(bufferedImage.getWidth(), bufferedImage.getHeight());
        texture.data = ((DataBufferByte) bufferedImage.getRaster()
                .getDataBuffer()).getData();
        return texture;
    }
}
