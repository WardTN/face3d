package com.wardtn.facemodel.model;

import com.wardtn.facemodel.util.io.IOUtils;

import java.nio.IntBuffer;
import java.util.List;

/**
 *  Element 元素
 */
public class Element {

    public static class Builder {

        // polygon
        private String id; //元素唯一标识
        private List<Integer> indices; //顶点索引

        // materials 材质
        private String materialId;

        public Builder() {
        }

        public Builder id(String id) {
            this.id = id;
            return this;
        }

        public String getId() {
            return this.id;
        }


        public Builder indices(List<Integer> indices) {
            this.indices = indices;
            return this;
        }

        public Builder materialId(String materialId) {
            this.materialId = materialId;
            return this;
        }

        public String getMaterialId() {
            return this.materialId;
        }

        public Element build() {
            return new Element(id, indices, materialId);
        }



    }

    // polygon
    private final String id; //元素唯一标识
    private final List<Integer> indicesArray; //顶点索引
    private IntBuffer indexBuffer;

    // material 材质
    private String materialId;
    // 材质对象
    private Material material;

    public Element(String id, List<Integer> indexBuffer, String material) {
        this.id = id;
        this.indicesArray = indexBuffer;
        this.materialId = material;
    }

    public Element(String id, IntBuffer indexBuffer, String material) {
        this.id = id;
        this.indicesArray = null;
        this.indexBuffer = indexBuffer;
        this.materialId = material;
    }

    public String getId() {
        return this.id;
    }

    public List<Integer> getIndices() {
        return this.indicesArray;
    }


    public IntBuffer getIndexBuffer() {
        if (indexBuffer == null) {
            this.indexBuffer = IOUtils.createIntBuffer(indicesArray.size());
            this.indexBuffer.position(0);
            for (int i = 0; i < indicesArray.size(); i++) {
                this.indexBuffer.put(indicesArray.get(i));
            }
        }
        return indexBuffer;
    }

    public String getMaterialId() {
        return materialId;
    }

    public void setMaterial(Material material) {
        this.material = material;
    }

    public Material getMaterial() {
        return material;
    }

    @Override
    public String toString() {
        return "Element{" +
                "id='" + id + '\'' +
                ", indices="+(indicesArray != null? indicesArray.size(): null)+
                ", indexBuffer="+indexBuffer+
                ", material=" + material +
                '}';
    }
}
