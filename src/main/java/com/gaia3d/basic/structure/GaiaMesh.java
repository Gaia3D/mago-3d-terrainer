package com.gaia3d.basic.structure;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;

public class GaiaMesh {
    public ArrayList<GaiaVertex> vertices = null;
    public ArrayList<GaiaTriangle> triangles = null;

    public ArrayList<GaiaHalfEdge> halfEdges = null;

    public long id = -1L;

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
        dataOutputStream.writeLong(id);

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
        this.id = dataInputStream.readLong();

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


    }

}
