package com.wardtn.facemodel.model;

import java.util.HashMap;
import java.util.Map;

/**
 * 材质列表
 */
public class Materials {

    final String id;

    final Map<String, Material> materials = new HashMap<>();

    public Materials(String id) {
        this.id = id;
    }

    public void add(String name, Material material) {
        materials.put(name, material);
    }

    public Material get(String name) {
        return materials.get(name);
    }

    public boolean contains(String elementMaterial) {
        return materials.containsKey(elementMaterial);
    }

    public int size() {
        return materials.size();
    }

    @Override
    public String toString() {
        return "Materials{" +
                "id='" + id + '\'' +
                ", materials=" + materials +
                '}';
    }
}
