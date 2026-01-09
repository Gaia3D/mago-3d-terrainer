package com.gaia3d.release;

import com.gaia3d.command.GlobalOptions;
import com.gaia3d.command.MagoTerrainerMain;

import java.io.File;

public class MagoTestConfig {
    //public static final String OUTPUT_PATH = "E:/data/mago-server/output";
    public static final String OUTPUT_PATH = "H:/workspace/mago-server/output";
    public static final String SSD_INPUT_PATH = "H:/workspace/mago-3d-terrainer";
    public static final String INPUT_PATH = "D:/data/mago-3d-terrainer/release-sample";
    public static final String TEMP_PATH = "D:/data/mago-3d-terrainer/temp-sample";
    public static final String TERRAIN_PATh = "D:/data/mago-3d-terrainer/terrain-sample";

    public static void execute(String[] args) {
        GlobalOptions.recreateInstance();
        MagoTerrainerMain.main(args);
    }

    public static File getTempPath(String path) {
        return new File(MagoTestConfig.TEMP_PATH, path);
    }

    public static File getSsdInputPath(String path) {
        return new File(MagoTestConfig.SSD_INPUT_PATH, path);
    }

    public static File getInputPath(String path) {
        return new File(MagoTestConfig.INPUT_PATH, path);
    }

    public static File getTerrainPath(String path) {
        return new File(MagoTestConfig.TERRAIN_PATh, path);
    }

    public static File getOutputPath(String path) {
        return new File(MagoTestConfig.OUTPUT_PATH, path);
    }

    public static File getLogPath(String path) {
        File logPath = new File(MagoTestConfig.OUTPUT_PATH, path);
        return new File(logPath, "mago-3d-tiler.log");
    }
}
