package com.gaia3d.image;


import lombok.extern.slf4j.Slf4j;

import javax.imageio.*;
import javax.imageio.metadata.IIOInvalidTreeException;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.metadata.IIOMetadataNode;
import javax.imageio.stream.FileImageOutputStream;
import java.awt.*;
import java.awt.image.*;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;


@Slf4j
public class Texture2D {
    public int width, height;
    public byte[] data;

    public Texture2D() {
    }

    public Texture2D(int width, int height) {
        this.width = width;
        this.height = height;
        data = new byte[width * height * 4];
    }

    public void setSize(int width, int height) {
        this.width = width;
        this.height = height;
        data = new byte[width * height * 4];
    }

    public void setPixel(int x, int y, byte r, byte g, byte b, byte a) {
        int index = (y * width + x) * 4;
        data[index] = r;
        data[index + 1] = g;
        data[index + 2] = b;
        data[index + 3] = a;
    }

    public int saveAsPNG_test(String filePathName) throws FileNotFoundException {
        // This function saves the texture as PNG file.
        ImageWriter pngWriter = ImageIO.getImageWritersByFormatName("png").next();
        ImageWriteParam pngWriteParam = pngWriter.getDefaultWriteParam();

        return 0;
    }

    // bKGD.
//        IIOMetadataNode bkgdNode = new IIOMetadataNode("bKGD");
//        IIOMetadataNode bKGDRGBnode = new IIOMetadataNode("bKGD_RGB");
//        bKGDRGBnode.setAttribute("red", "255");
//        bKGDRGBnode.setAttribute("green", "255");
//        bKGDRGBnode.setAttribute("blue", "255");
//        bkgdNode.appendChild(bKGDRGBnode);
//        root.appendChild(bkgdNode);


//        // Cambiado a valores de punto flotante
//        IIOMetadataNode whitePointNode = new IIOMetadataNode("whitePoint");
//        whitePointNode.setAttribute("x", "0.3127");
//        whitePointNode.setAttribute("y", "0.3290");
//        srgbNode.appendChild(whitePointNode);
//
//        IIOMetadataNode redPrimaryNode = new IIOMetadataNode("redPrimary");
//        redPrimaryNode.setAttribute("x", "0.6400");
//        redPrimaryNode.setAttribute("y", "0.3300");
//        srgbNode.appendChild(redPrimaryNode);
//
//        IIOMetadataNode greenPrimaryNode = new IIOMetadataNode("greenPrimary");
//        greenPrimaryNode.setAttribute("x", "0.3000");
//        greenPrimaryNode.setAttribute("y", "0.6000");
//        srgbNode.appendChild(greenPrimaryNode);
//
//        IIOMetadataNode bluePrimaryNode = new IIOMetadataNode("bluePrimary");
//        bluePrimaryNode.setAttribute("x", "0.1500");
//        bluePrimaryNode.setAttribute("y", "0.0600");
//        srgbNode.appendChild(bluePrimaryNode);


    // chunk "iCCP" para indicar el perfil ICC de la imagen
//        IIOMetadataNode iccpNode = new IIOMetadataNode("iCCP");
//        iccpNode.setAttribute("profileName", "icc-profile-name");
//        iccpNode.setAttribute("compressionMethod", "deflate");
//        // iccpNode.setUserObject("icc-profile-data");
//        root.appendChild(iccpNode);

    // chunk "tRNS" para indicar el color transparente de la imagen
//        IIOMetadataNode trnsNode = new IIOMetadataNode("tRNS");
//        IIOMetadataNode tRNSRGBnode = new IIOMetadataNode("tRNS_RGB");
//        tRNSRGBnode.setAttribute("red", Integer.toString(0));
//        tRNSRGBnode.setAttribute("green", Integer.toString(0));
//        tRNSRGBnode.setAttribute("blue", Integer.toString(0));
//        trnsNode.appendChild(tRNSRGBnode);
//        root.appendChild(trnsNode);

    // chunk "gAMA" para indicar el valor gamma de la imagen
//        int gAMA_gamma = 45455; // 1.0 / 2.2 * 100000
//        IIOMetadataNode gamaNode = new IIOMetadataNode("gAMA");
//        gamaNode.setAttribute("value",  Integer.toString(gAMA_gamma));
//        root.appendChild(gamaNode);

    // chunk "cHRM" para indicar los valores de los puntos críticos de la imagen
//        IIOMetadataNode chrmNode = new IIOMetadataNode("cHRM");
//        chrmNode.setAttribute("whitePointX", "0.3127");
//        chrmNode.setAttribute("whitePointY", "0.329");
//        chrmNode.setAttribute("redX", "0.64");
//        chrmNode.setAttribute("redY", "0.33");
//        chrmNode.setAttribute("greenX", "0.3");
//        chrmNode.setAttribute("greenY", "0.6");
//        chrmNode.setAttribute("blueX", "0.15");
//        chrmNode.setAttribute("blueY", "0.06");
//        root.appendChild(chrmNode);

    // Configura el nodo "sBIT" para escala de grises
//        IIOMetadataNode grayAlphaNode = new IIOMetadataNode("sBIT_GrayAlpha");
//        grayAlphaNode.setAttribute("gray", Integer.toString(8));
//        grayAlphaNode.setAttribute("alpha", Integer.toString(8));
//        sbitNode.appendChild(grayAlphaNode);

    // chunk "iTXt" para indicar el idioma de la imagen
//        IIOMetadataNode itxtNode = new IIOMetadataNode("iTXt");
//        itxtNode.setAttribute("keyword", "XML:lang");
//        itxtNode.setAttribute("compressionFlag", "0");
//        itxtNode.setAttribute("compressionMethod", "deflate");
//        itxtNode.setAttribute("languageTag", "en");
//        itxtNode.setAttribute("translatedKeyword", "XML:lang");
//        itxtNode.setAttribute("text", "en");
//        root.appendChild(itxtNode);
//

    public int saveAsPNG(String filePathName) throws FileNotFoundException, IIOInvalidTreeException {
        // This function saves the texture as PNG file.
        BufferedImage myImage = new BufferedImage(width, height, BufferedImage.TYPE_4BYTE_ABGR); // Reemplaza con tu imagen
        myImage.getRaster().setDataElements(0, 0, width, height, data);


        ImageWriter pngWriter = ImageIO.getImageWritersByFormatName("PNG").next();
        ImageWriteParam pngWriteParam = pngWriter.getDefaultWriteParam();
        pngWriteParam.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
        //float defaultCompressionQuality = pngWriteParam.getCompressionQuality();
        //String compressionType = pngWriteParam.getCompressionType();
        pngWriteParam.setCompressionQuality(0.5f);
        //pngWriteParam.setController(
//        pngWriteParam.setDestinationType(new ImageTypeSpecifier(myImage));
//        pngWriteParam.setTilingMode(ImageWriteParam.MODE_EXPLICIT);
//        pngWriteParam.setTiling(150, 150, 0, 0);
//        pngWriteParam.setDestinationOffset(new Point(0, 0));
        pngWriteParam.setSourceBands(new int[]{0, 1, 2, 3});
//        pngWriteParam.setSourceRegion(new Rectangle(0, 0, width, height));
//        pngWriteParam.setSourceSubsampling(1, 1, 0, 0);

        IIOMetadata metadata = pngWriter.getDefaultImageMetadata(new ImageTypeSpecifier(myImage), pngWriteParam);

        // chunks.
        // https://www.javatips.net/api/javax.imageio.metadata.iiometadatanode
        String metadataFormat = "javax_imageio_png_1.0";
        IIOMetadataNode root = new IIOMetadataNode(metadataFormat);
        // chunk "pHYs" para indicar la resolución de la imagen
        IIOMetadataNode physNode = new IIOMetadataNode("pHYs");
        physNode.setAttribute("pixelsPerUnitXAxis", "8669");
        physNode.setAttribute("pixelsPerUnitYAxis", "8669");
        physNode.setAttribute("unitSpecifier", "meter");
        root.appendChild(physNode);

        // chunk "sRGB" para indicar que es una imagen sRGB
        IIOMetadataNode srgbNode = new IIOMetadataNode("sRGB");
        srgbNode.setAttribute("renderingIntent", "Perceptual");
        root.appendChild(srgbNode);

        // chunk "tEXt" para indicar el título de la imagen
        IIOMetadataNode textNode = new IIOMetadataNode("tEXt");
        textNode.setAttribute("keyword", "Title");
        textNode.setAttribute("value", "Image title");
        root.appendChild(textNode);

        // chunk "tIME" para indicar la fecha y hora de la imagen
        IIOMetadataNode timeNode = new IIOMetadataNode("tIME");
        timeNode.setAttribute("year", "2019");
        timeNode.setAttribute("month", "1");
        timeNode.setAttribute("day", "1");
        timeNode.setAttribute("hour", "0");
        timeNode.setAttribute("minute", "0");
        timeNode.setAttribute("second", "0");
        root.appendChild(timeNode);

        // chunk "sBIT" para indicar el número de bits de cada componente de color
        IIOMetadataNode sbitNode = new IIOMetadataNode("sBIT");
        // Configura el nodo "sBIT" para RGB
        IIOMetadataNode RGBAnode = new IIOMetadataNode("sBIT_RGBAlpha");
        RGBAnode.setAttribute("red", Integer.toString(8));
        RGBAnode.setAttribute("green", Integer.toString(8));
        RGBAnode.setAttribute("blue", Integer.toString(8));
        RGBAnode.setAttribute("alpha", Integer.toString(8));
        sbitNode.appendChild(RGBAnode);

        root.appendChild(sbitNode);


        //metadata.setFromTree(metadataFormat, root);
        metadata.mergeTree(metadataFormat, root);

        try (FileImageOutputStream output = new FileImageOutputStream(new File(filePathName))) {
            pngWriter.setOutput(output);
            pngWriter.write(metadata, new IIOImage(myImage, null, metadata), pngWriteParam);

        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            pngWriter.dispose();
        }
        /*
        try {
            // Crea un ImageOutputStream para escribir la imagen en el archivo
            File outputFile = new File(filePathName);
            ImageOutputStream ios = ImageIO.createImageOutputStream(outputFile);

            // Establece el destino del ImageWriter como el ImageOutputStream
            pngWriter.setOutput(ios);

            // Escribe el BufferedImage utilizando los parámetros configurados
            BufferedImage myImage = new BufferedImage(width, height, BufferedImage.TYPE_4BYTE_ABGR); // Reemplaza con tu imagen
            myImage.getRaster().setDataElements(0, 0, width, height, data);
            pngWriter.write(null, new javax.imageio.IIOImage(myImage, null, null), pngWriteParam);

            // Cierra el ImageOutputStream y el ImageWriter
            ios.close();
            pngWriter.dispose();

           log.info("Imagen guardada como " + filePathName);
        } catch (IOException e) {
            log.error("", e);
        }

         */

        return 0;
    }

    public int saveAsPNG_testRGBA(String filePathName) throws FileNotFoundException, IIOInvalidTreeException {
        // This function saves the texture as PNG file.
        BufferedImage myImage = new BufferedImage(width, height, BufferedImage.TYPE_4BYTE_ABGR); // Reemplaza con tu imagen
        //myImage.getRaster().setDataElements(0, 0, width, height, data);
        // revert ABGR to RGBA.
        byte[] reverted = new byte[data.length];
        for (int col = 0; col < width; col++) {
            for (int row = 0; row < height; row++) {
                int index = (row * width + col) * 4;

                if (data[index] != 0 || data[index + 1] != 0 || data[index + 2] != 0 || data[index + 3] != 0) {
                    int a = 0;
                }

                reverted[index] = data[index + 3];
                reverted[index + 1] = data[index + 2];
                reverted[index + 2] = data[index + 1];
                reverted[index + 3] = data[index];
            }
        }

        myImage.getRaster().setDataElements(0, 0, width, height, reverted);

        ImageWriter pngWriter = ImageIO.getImageWritersByFormatName("PNG").next();
        ImageWriteParam pngWriteParam = pngWriter.getDefaultWriteParam();
        pngWriteParam.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
        //float defaultCompressionQuality = pngWriteParam.getCompressionQuality();
        //String compressionType = pngWriteParam.getCompressionType();
        pngWriteParam.setCompressionQuality(0.5f);
        //pngWriteParam.setController(
//        pngWriteParam.setDestinationType(new ImageTypeSpecifier(myImage));
//        pngWriteParam.setTilingMode(ImageWriteParam.MODE_EXPLICIT);
//        pngWriteParam.setTiling(150, 150, 0, 0);
//        pngWriteParam.setDestinationOffset(new Point(0, 0));
        pngWriteParam.setSourceBands(new int[]{0, 1, 2, 3});
//        pngWriteParam.setSourceRegion(new Rectangle(0, 0, width, height));
//        pngWriteParam.setSourceSubsampling(1, 1, 0, 0);

        IIOMetadata metadata = pngWriter.getDefaultImageMetadata(new ImageTypeSpecifier(myImage), pngWriteParam);

        // chunks.
        // https://www.javatips.net/api/javax.imageio.metadata.iiometadatanode
        String metadataFormat = "javax_imageio_png_1.0";
        IIOMetadataNode root = new IIOMetadataNode(metadataFormat);
        // chunk "pHYs" para indicar la resolución de la imagen
        IIOMetadataNode physNode = new IIOMetadataNode("pHYs");
        physNode.setAttribute("pixelsPerUnitXAxis", "8669");
        physNode.setAttribute("pixelsPerUnitYAxis", "8669");
        physNode.setAttribute("unitSpecifier", "meter");
        root.appendChild(physNode);

        // chunk "sRGB" para indicar que es una imagen sRGB
        IIOMetadataNode srgbNode = new IIOMetadataNode("sRGB");
        srgbNode.setAttribute("renderingIntent", "Perceptual");
        root.appendChild(srgbNode);

        // chunk "tEXt" para indicar el título de la imagen
        IIOMetadataNode textNode = new IIOMetadataNode("tEXt");
        textNode.setAttribute("keyword", "Title");
        textNode.setAttribute("value", "Image title");
        root.appendChild(textNode);

//        // chunk "tIME" para indicar la fecha y hora de la imagen
//        IIOMetadataNode timeNode = new IIOMetadataNode("tIME");
//        timeNode.setAttribute("year", "2019");
//        timeNode.setAttribute("month", "1");
//        timeNode.setAttribute("day", "1");
//        timeNode.setAttribute("hour", "0");
//        timeNode.setAttribute("minute", "0");
//        timeNode.setAttribute("second", "0");
//        root.appendChild(timeNode);

//        // chunk "sBIT" para indicar el número de bits de cada componente de color
//        IIOMetadataNode sbitNode = new IIOMetadataNode("sBIT");
//        // Configura el nodo "sBIT" para RGB
//        IIOMetadataNode RGBAnode = new IIOMetadataNode("sBIT_RGBAlpha");
//        RGBAnode.setAttribute("red", Integer.toString(8));
//        RGBAnode.setAttribute("green", Integer.toString(8));
//        RGBAnode.setAttribute("blue", Integer.toString(8));
//        RGBAnode.setAttribute("alpha", Integer.toString(8));
//        sbitNode.appendChild(RGBAnode);
//        root.appendChild(sbitNode);

        // chunk "gAMA" para indicar el valor gamma de la imagen
        int gAMA_gamma = 45455; // 1.0 / 2.2 * 100000
        IIOMetadataNode gamaNode = new IIOMetadataNode("gAMA");
        gamaNode.setAttribute("value", Integer.toString(gAMA_gamma));
        root.appendChild(gamaNode);


        //metadata.setFromTree(metadataFormat, root);
        metadata.mergeTree(metadataFormat, root);

        try (FileImageOutputStream output = new FileImageOutputStream(new File(filePathName))) {
            pngWriter.setOutput(output);
            pngWriter.write(metadata, new IIOImage(myImage, null, metadata), pngWriteParam);

        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            pngWriter.dispose();
        }

        return 0;
    }

    public int saveAsPNG_old(String filePathName) {
        // This function saves the texture as PNG file.
        BufferedImage bufferedImage = new BufferedImage(width, height, BufferedImage.TYPE_4BYTE_ABGR);
        bufferedImage.getRaster().setDataElements(0, 0, width, height, data);

        try {
            ImageIO.write(bufferedImage, "png", new File(filePathName));
        } catch (IOException e) {
            log.error("", e);
        }

        return 0;
    }

    public int saveAsPNG_original(String filePathName) {
        // This function saves the texture as PNG file.
        DataBufferByte dataBufferByte = new DataBufferByte(data, data.length);
        WritableRaster raster = Raster.createInterleavedRaster(dataBufferByte, width, height, width * 4, 4, new int[]{0, 1, 2, 3}, null);
        ColorModel colorModel = new ComponentColorModel(ColorModel.getRGBdefault().getColorSpace(), true, false, Transparency.TRANSLUCENT, DataBuffer.TYPE_BYTE);
        boolean isRasterPremultiplied = false;
        BufferedImage bufferedImage = new BufferedImage(colorModel, raster, isRasterPremultiplied, null);

        File file = new File(filePathName);
        try {
            ImageIO.write(bufferedImage, "png", file);

        } catch (Exception e) {
            log.error("", e);
            return -1;
        }

        return 0;
    }
}
