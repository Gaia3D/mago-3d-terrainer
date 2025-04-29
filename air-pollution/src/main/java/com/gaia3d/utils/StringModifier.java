package com.gaia3d.utils;

import com.gaia3d.geometry.Triangle;
import com.gaia3d.geometry.Vertex;
import lombok.extern.slf4j.Slf4j;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.StringTokenizer;

@Slf4j
public class StringModifier {
    public static void splitString(String wordToSplit, String delimiter, List<String> resultSplittedStrings, boolean skipEmptyStrings) {
        StringTokenizer tokenizer = new StringTokenizer(wordToSplit, delimiter);
        while (tokenizer.hasMoreTokens()) {
            String word = tokenizer.nextToken();
            if (skipEmptyStrings) {
                if (!word.isEmpty()) {
                    resultSplittedStrings.add(word);
                }
            } else {
                resultSplittedStrings.add(word);
            }
        }
    }

    // discard strings with length zero
    public static void splitStringOld(String wordToSplit, String delimiter, List<String> resultSplittedStrings, boolean skipEmptyStrings) {
        String[] splitStrings = wordToSplit.split(delimiter);
        for (String word : splitStrings) {
            if (skipEmptyStrings) {
                if (!word.isEmpty()) {
                    resultSplittedStrings.add(word);
                }
            } else {
                resultSplittedStrings.add(word);
            }
        }
    }

    public static void deleteFolder(String folderPath) {
        File folder = new File(folderPath);
        if (folder.exists()) {
            File[] files = folder.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isDirectory()) {
                        deleteFolder(file.getAbsolutePath());
                    } else {
                        file.delete();
                    }
                }
            }
            folder.delete();
        }
    }

    public static String getLastNameFromPath(String folderPath) {
        String folderName = "";
        int lastIndexOf = folderPath.lastIndexOf(File.separator);
        if (lastIndexOf >= 0) {
            folderName = folderPath.substring(lastIndexOf + 1);
        }
        return folderName;
    }

    public static Optional<String> getExtensionByStringHandling(String filename) {
        // https://www.baeldung.com/java-file-extension
        return Optional.ofNullable(filename).filter(f -> f.contains(".")).map(f -> f.substring(filename.lastIndexOf(".") + 1));
    }

    public static boolean existFolder(Path folderPath) {
        return Files.exists(folderPath);
    }

    public static void createFolderIfNoExists(Path folderPath) {
        if (!existFolder(folderPath)) {
            new File(folderPath.toString()).mkdirs();
        }
    }

    public static boolean createAllFoldersIfNoExist(String filePath) {
        File file = new File(filePath);
        return file.mkdirs();
    }

    public static boolean checkStringCoincidences(String word, ArrayList<String> vecStrings, boolean bIgnoreCase) {
        int stringsCount = vecStrings.size();
        for (int i = 0; i < stringsCount; i++) {
            if (bIgnoreCase) {
                if (word.equalsIgnoreCase(vecStrings.get(i))) {
                    return true;
                }
            } else {
                if (word.equals(vecStrings.get(i))) {
                    return true;
                }
            }

        }
        return false;
    }

    public static List<String> getFolderNamesInFolder(String folderPath) {
        List<String> vecFolderNames = new ArrayList<>();
        File folder = new File(folderPath);

        // Populates the array with names of files and directories
        File[] listOfFiles = folder.listFiles();
        int filesCount = listOfFiles.length;
        for (int i = 0; i < filesCount; i++) {
            File file = listOfFiles[i];

            // check if is a file or folder.
            if (file.isDirectory()) {
                /* is a folder. */
                String folderName = file.getName();
                vecFolderNames.add(folderName);
            }
        }

        return vecFolderNames;
    }

    public static void getFileNamesInFolder(String folderPath, ArrayList<String> vecFileExtensions, ArrayList<String> vecFileNames) {
        File folder = new File(folderPath);

        // Populates the array with names of files and directories
        File[] listOfFiles = folder.listFiles();
        int filesCount = listOfFiles.length;
        boolean bIgnoreCase = true; // ignore char upperCase & lowerCase.
        for (int i = 0; i < filesCount; i++) {
            File file = listOfFiles[i];

            // check if is a file or folder.
            if (file.isFile()) {
                /* is a file. */
                String fileName = file.getName();
                Optional<String> optExtension = getExtensionByStringHandling(fileName);
                if (optExtension.isPresent()) {
                    // now check if the extension is coincident with wanted extension.
                    String extension = optExtension.get();
                    if (checkStringCoincidences(extension, vecFileExtensions, bIgnoreCase)) {
                        vecFileNames.add(fileName);
                    }
                }
            }
        }

    }

    public static String getRawFileName(String fileName) {
        String rawFileName = fileName.substring(0, fileName.lastIndexOf('.'));
        return rawFileName;
    }

    public static int bigEndianToLittleEndian(int value) {
        int result = 0;
        result = ((value & 0x000000FF) << 24) | ((value & 0x0000FF00) << 8) | ((value & 0x00FF0000) >> 8) | ((value & 0xFF000000) >> 24);
        return result;
    }


    private static byte[] floatToBytesInverse(float value) {
        byte[] bytes = new byte[4];
        ByteBuffer buffer = ByteBuffer.allocate(4);
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        buffer.putFloat(value);
        buffer.flip();
        buffer.get(bytes);
        return bytes;
    }

    private static byte[] doubleToBytesInverse(double value) {
        byte[] bytes = new byte[8];
        ByteBuffer buffer = ByteBuffer.allocate(8);
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        buffer.putDouble(value);
        buffer.flip();
        buffer.get(bytes);
        return bytes;
    }

    public static void saveMeshToFile(String filePath, List<Vertex> vecVertexes, List<Triangle> vecTriangles, double pollutionValueMAX) {
        // save vertexes to file.
        try {
            int vertexesCount = vecVertexes.size();
            int trianglesCount = vecTriangles.size();
            FileOutputStream fileOutputStream = new FileOutputStream(filePath);
            DataOutputStream dataOutputStream = new DataOutputStream(fileOutputStream);

            // save the pollutionValueMax.
            dataOutputStream.write(doubleToBytesInverse(pollutionValueMAX));

            // now, save the vertexCount to file.
            dataOutputStream.writeInt(bigEndianToLittleEndian(vertexesCount));

            // now, save the vertex position (x, y, z) to file.
            for (int i = 0; i < vertexesCount; i++) {
                Vertex vertex = vecVertexes.get(i);
                double x = vertex.point3d.x;
                double y = vertex.point3d.y;
                double z = vertex.point3d.z;

                dataOutputStream.write(doubleToBytesInverse(x));
                dataOutputStream.write(doubleToBytesInverse(y));
                dataOutputStream.write(doubleToBytesInverse(z));

                // save the pollution value.
                dataOutputStream.write(doubleToBytesInverse(vertex.pollutionValue));

            }

            // now, save the trianglesCount to file.
            GeometryUtils.setIdxInList(vecVertexes);
            int indicesCount = trianglesCount * 3;
            dataOutputStream.writeInt(bigEndianToLittleEndian(indicesCount));

            // now, save the triangles to file.
            for (int i = 0; i < trianglesCount; i++) {
                Triangle triangle = vecTriangles.get(i);
                int vertexIndex0 = triangle.vertex_0.idxInList;
                int vertexIndex1 = triangle.vertex_1.idxInList;
                int vertexIndex2 = triangle.vertex_2.idxInList;

                dataOutputStream.writeInt(bigEndianToLittleEndian(vertexIndex0));
                dataOutputStream.writeInt(bigEndianToLittleEndian(vertexIndex1));
                dataOutputStream.writeInt(bigEndianToLittleEndian(vertexIndex2));
            }
        } catch (IOException e) {
            log.error("", e);
        }
    }

    public static void saveVertexesToFile(String filePath, List<Vertex> vecVertexes) {
        // save vertexes to file.
        try {
            int vertexesCount = vecVertexes.size();
            FileOutputStream fileOutputStream = new FileOutputStream(filePath);
            DataOutputStream dataOutputStream = new DataOutputStream(fileOutputStream);

            // now, save the vertexCount to file.
            dataOutputStream.writeInt(bigEndianToLittleEndian(vertexesCount));

            // now, save the vertex position (x, y, z) to file.
            for (int i = 0; i < vertexesCount; i++) {
                Vertex vertex = vecVertexes.get(i);
                double x = vertex.point3d.x;
                double y = vertex.point3d.y;
                double z = vertex.point3d.z;

                dataOutputStream.write(doubleToBytesInverse(x));
                dataOutputStream.write(doubleToBytesInverse(y));
                dataOutputStream.write(doubleToBytesInverse(z));

            }
        } catch (IOException e) {
            log.error("", e);
        }
    }

    public static void deleteFile(String mosaicTextureFilePath) {
        File file = new File(mosaicTextureFilePath);
        if (file.exists()) {
            file.delete();
        }
    }
}
