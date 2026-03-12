package com.gaia3d.util;

import com.gaia3d.basic.geometry.octree.GaiaFaceContent;
import com.gaia3d.basic.model.*;
import lombok.extern.slf4j.Slf4j;
import org.joml.Vector3d;

import java.util.List;

@Slf4j
public class GaiaOctreeUtils {

    public static void getFaceDataListOfNode(GaiaScene sceneParent, GaiaNode node, List<GaiaFaceContent> resultFaceDataList) {
        // 1rst, check meshes.
        if (node.getMeshes() != null) {
            for (int i = 0, length = node.getMeshes().size(); i < length; i++) {
                GaiaMesh mesh = node.getMeshes().get(i);
                if (mesh.getPrimitives() != null) {
                    for (int j = 0, primitivesLength = mesh.getPrimitives().size(); j < primitivesLength; j++) {
                        GaiaPrimitive primitive = mesh.getPrimitives().get(j);

                        List<GaiaVertex> primitiveVertices = primitive.getVertices();
                        if (primitive.getSurfaces() != null) {
                            for (int k = 0, surfacesLength = primitive.getSurfaces().size(); k < surfacesLength; k++) {
                                GaiaSurface surface = primitive.getSurfaces().get(k);
                                if (surface.getFaces() != null) {
                                    for (int m = 0, facesLength = surface.getFaces().size(); m < facesLength; m++) {
                                        GaiaFace face = surface.getFaces().get(m);

                                        int indicesCount = face.getIndices().length;
                                        int[] indices = face.getIndices();
                                        int triangleCount = indicesCount / 3;
                                        for (int n = 0; n < triangleCount; n++) {
                                            int index0 = indices[n * 3];
                                            int index1 = indices[n * 3 + 1];
                                            int index2 = indices[n * 3 + 2];

                                            GaiaFaceContent faceContent = new GaiaFaceContent();
                                            GaiaFace face0 = new GaiaFace();
                                            face0.setIndices(new int[]{index0, index1, index2});

                                            // check out of bound.
                                            if (index0 < 0 || index0 >= primitiveVertices.size()) {
                                                log.warn("Invalid index0: {}, primitive vertices size: {}", index0, primitiveVertices.size());
                                                continue;
                                            } else if (index1 < 0 || index1 >= primitiveVertices.size()) {
                                                log.warn("Invalid index1: {}, primitive vertices size: {}", index1, primitiveVertices.size());
                                                continue;
                                            } else if (index2 < 0 || index2 >= primitiveVertices.size()) {
                                                log.warn("Invalid index2: {}, primitive vertices size: {}", index2, primitiveVertices.size());
                                                continue;
                                            }

                                            GaiaVertex vertex0 = primitiveVertices.get(index0);
                                            GaiaVertex vertex1 = primitiveVertices.get(index1);
                                            GaiaVertex vertex2 = primitiveVertices.get(index2);

                                            // check if the positions are valid.
                                            Vector3d position0 = vertex0.getPosition();
                                            Vector3d position1 = vertex1.getPosition();
                                            Vector3d position2 = vertex2.getPosition();
                                            if (Double.isNaN(position0.x) || Double.isNaN(position0.y) || Double.isNaN(position0.z) || Double.isNaN(position1.x) || Double.isNaN(position1.y) || Double.isNaN(position1.z) || Double.isNaN(position2.x) || Double.isNaN(position2.y) || Double.isNaN(position2.z)) {
                                                continue;
                                            }

                                            faceContent.setSceneParent(sceneParent);
                                            faceContent.setPrimitiveParent(primitive);
                                            faceContent.setFace(face0);
                                            resultFaceDataList.add(faceContent);
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // now, check children.
        if (node.getChildren() != null) {
            for (int i = 0, length = node.getChildren().size(); i < length; i++) {
                GaiaNode child = node.getChildren().get(i);
                getFaceDataListOfNode(sceneParent, child, resultFaceDataList);
            }
        }
    }

    public static void getFaceDataListOfScene(GaiaScene gaiaScene, List<GaiaFaceContent> resultFaceDataList) {
        for (GaiaNode node : gaiaScene.getNodes()) {
            getFaceDataListOfNode(gaiaScene, node, resultFaceDataList);
        }
    }
}
