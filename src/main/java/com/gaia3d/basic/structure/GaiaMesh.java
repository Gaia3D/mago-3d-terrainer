package com.gaia3d.basic.structure;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

public class GaiaMesh {
    public ArrayList<GaiaVertex> vertices = null;
    public ArrayList<GaiaTriangle> triangles = null;

    public ArrayList<GaiaHalfEdge> halfEdges = null;

    public int id = -1;

    public GaiaMesh() {
        vertices = new ArrayList<GaiaVertex>();
        triangles = new ArrayList<GaiaTriangle>();
        halfEdges = new ArrayList<GaiaHalfEdge>();
    }

    public GaiaVertex newVertex() {
        GaiaVertex vertex = new GaiaVertex();
        vertices.add(vertex);
        return vertex;
    }

    public GaiaTriangle newTriangle() {
        GaiaTriangle triangle = new GaiaTriangle();
        triangles.add(triangle);
        return triangle;
    }

    public GaiaHalfEdge newHalfEdge() {
        GaiaHalfEdge halfEdge = new GaiaHalfEdge();
        halfEdges.add(halfEdge);
        return halfEdge;
    }

    public void setVertexIdInList()
    {
        int verticesCount = vertices.size();
        for(int i=0; i<verticesCount; i++) {
            GaiaVertex vertex = vertices.get(i);
            vertex.id = i;
        }
    }

    public void setTriangleIdInList()
    {
        int trianglesCount = triangles.size();
        for(int i=0; i<trianglesCount; i++) {
            GaiaTriangle triangle = triangles.get(i);
            triangle.id = i;
        }
    }

    public void setHalfEdgeIdInList()
    {
        int halfEdgesCount = halfEdges.size();
        for(int i=0; i<halfEdgesCount; i++) {
            GaiaHalfEdge halfEdge = halfEdges.get(i);
            halfEdge.id = i;
        }
    }

    public void setObjectsIdInList()
    {
        setVertexIdInList();
        setTriangleIdInList();
        setHalfEdgeIdInList();
    }

    public HashMap<Integer, GaiaVertex> getVerticesMap()
    {
        HashMap<Integer, GaiaVertex> verticesMap = new HashMap<Integer, GaiaVertex>();
        int verticesCount = vertices.size();
        for(int i=0; i<verticesCount; i++) {
            GaiaVertex vertex = vertices.get(i);
            verticesMap.put(vertex.id, vertex);
        }
        return verticesMap;
    }

    public HashMap<Integer, GaiaTriangle> getTrianglesMap()
    {
        HashMap<Integer, GaiaTriangle> trianglesMap = new HashMap<Integer, GaiaTriangle>();
        int trianglesCount = triangles.size();
        for(int i=0; i<trianglesCount; i++) {
            GaiaTriangle triangle = triangles.get(i);
            trianglesMap.put(triangle.id, triangle);
        }
        return trianglesMap;
    }

    public HashMap<Integer, GaiaHalfEdge> getHalfEdgesMap()
    {
        HashMap<Integer, GaiaHalfEdge> halfEdgesMap = new HashMap<Integer, GaiaHalfEdge>();
        int halfEdgesCount = halfEdges.size();
        for(int i=0; i<halfEdgesCount; i++) {
            GaiaHalfEdge halfEdge = halfEdges.get(i);
            halfEdgesMap.put(halfEdge.id, halfEdge);
        }
        return halfEdgesMap;
    }

    public void addMesh(GaiaMesh mesh)
    {
        // 1rst, add vertices.***
        int verticesCount = mesh.vertices.size();
        for(int i=0; i<verticesCount; i++) {
            GaiaVertex vertex = mesh.vertices.get(i);
            vertices.add(vertex);
        }

        // 2nd, add triangles.***
        int trianglesCount = mesh.triangles.size();
        for(int i=0; i<trianglesCount; i++) {
            GaiaTriangle triangle = mesh.triangles.get(i);
            triangles.add(triangle);
        }

        // 3rd, add halfEdges.***
        int halfEdgesCount = mesh.halfEdges.size();
        for(int i=0; i<halfEdgesCount; i++) {
            GaiaHalfEdge halfEdge = mesh.halfEdges.get(i);
            halfEdges.add(halfEdge);
        }


    }

    public void saveDataOutputStream(DataOutputStream dataOutputStream) throws IOException {
        // save id.***
        dataOutputStream.writeInt(id);

        // save vertices.***
        int verticesCount = vertices.size();
        // save vertices count.***
        dataOutputStream.writeInt(verticesCount);

        for(int i=0; i<verticesCount; i++) {
            GaiaVertex vertex = vertices.get(i);
            vertex.saveDataOutputStream(dataOutputStream);
        }

        // save triangles.***
        int trianglesCount = triangles.size();
        // save triangles count.***
        dataOutputStream.writeInt(trianglesCount);

        for(int i=0; i<trianglesCount; i++) {
            GaiaTriangle triangle = triangles.get(i);
            triangle.saveDataOutputStream(dataOutputStream);
        }

        // save halfEdges.***
        int halfEdgesCount = halfEdges.size();
        // save halfEdges count.***
        dataOutputStream.writeInt(halfEdgesCount);

        for(int i=0; i<halfEdgesCount; i++) {
            GaiaHalfEdge halfEdge = halfEdges.get(i);
            halfEdge.saveDataOutputStream(dataOutputStream);
        }

    }

    public void loadDataInputStream(DataInputStream dataInputStream) throws IOException
    {
        this.id = dataInputStream.readInt();

        // load vertices.***
        int verticesCount = dataInputStream.readInt();
        for(int i=0; i<verticesCount; i++) {
            GaiaVertex vertex = newVertex();
            vertex.loadDataInputStream(dataInputStream);
        }

        // load triangles.***
        int trianglesCount = dataInputStream.readInt();
        for(int i=0; i<trianglesCount; i++) {
            GaiaTriangle triangle = newTriangle();
            triangle.loadDataInputStream(dataInputStream);
        }

        // load halfEdges.***
        int halfEdgesCount = dataInputStream.readInt();
        for(int i=0; i<halfEdgesCount; i++) {
            GaiaHalfEdge halfEdge = newHalfEdge();
            halfEdge.loadDataInputStream(dataInputStream);
        }

        // now, for each object, find pointing objects.***
        HashMap<Integer, GaiaVertex> verticesMap = getVerticesMap();
        HashMap<Integer, GaiaTriangle> trianglesMap = getTrianglesMap();
        HashMap<Integer, GaiaHalfEdge> halfEdgesMap = getHalfEdgesMap();

        // now, find pointing objects.***
        for(int i=0; i<verticesCount; i++) {
            GaiaVertex vertex = vertices.get(i);

            // vertex points outingHalfEdge.***
            int outingHalfEdgeId = vertex.outingHEdgeId;
            if(outingHalfEdgeId != -1) {
                GaiaHalfEdge outingHalfEdge = halfEdgesMap.get(outingHalfEdgeId);
                vertex.outingHEdge = outingHalfEdge;
            }
        }

        for(int i=0; i<trianglesCount; i++) {
            GaiaTriangle triangle = triangles.get(i);

            // triangle points halfEdge.***
            int halfEdgeId = triangle.halfEdgeId;
            if(halfEdgeId != -1) {
                GaiaHalfEdge halfEdge = halfEdgesMap.get(halfEdgeId);
                triangle.halfEdge = halfEdge;
            }
        }

        for(int i=0; i<halfEdgesCount; i++) {
            GaiaHalfEdge halfEdge = halfEdges.get(i);

            // halfEdge points vertex.***
            int vertexId = halfEdge.startVertexId;
            if(vertexId != -1) {
                GaiaVertex vertex = verticesMap.get(vertexId);
                halfEdge.startVertex = vertex;
            }

            // halfEdge points triangle.***
            int triangleId = halfEdge.triangleId;
            if(triangleId != -1) {
                GaiaTriangle triangle = trianglesMap.get(triangleId);
                halfEdge.triangle = triangle;
            }

            // halfEdge points next.***
            int nextId = halfEdge.nextId;
            if(nextId != -1) {
                GaiaHalfEdge next = halfEdgesMap.get(nextId);
                halfEdge.next = next;
            }

            // halfEdge points twin.***
            int twinId = halfEdge.twinId;
            if(twinId != -1) {
                GaiaHalfEdge twin = halfEdgesMap.get(twinId);
                halfEdge.twin = twin;
            }
        }

    }

}
