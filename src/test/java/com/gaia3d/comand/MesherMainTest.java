package com.gaia3d.comand;

import org.junit.jupiter.api.Test;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.operation.TransformException;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

class MesherMainTest {

    @Test
    void main() throws FactoryException, TransformException, IOException {
        MesherMain.main(null);
    }
}