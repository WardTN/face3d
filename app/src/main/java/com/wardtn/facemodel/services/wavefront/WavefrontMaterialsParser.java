package com.wardtn.facemodel.services.wavefront;

import android.util.Log;


import com.wardtn.facemodel.model.Material;
import com.wardtn.facemodel.model.Materials;
import com.wardtn.facemodel.util.math.Math3DUtils;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Arrays;

/**
 * 材质文件解析器
 */
public final class WavefrontMaterialsParser {

    /*
     * 逐行解析 MTL 文件并构建 Material 对象，最终将这些对象收集到 Materials 列表中
     */
   public Materials parse(String id, InputStream inputStream) {

        Log.i("WavefrontMaterialsParse", "Parsing materials... ");

        final Materials materials = new Materials(id);
        try {

            final BufferedReader isReader = new BufferedReader(new InputStreamReader(inputStream));

            String line;

            Material currMaterial = new Material(); // current material
            boolean createNewMaterial = false;

            while (((line = isReader.readLine()) != null)) {

                // read next line
                line = line.trim();

                // ignore empty lines
                if (line.length() == 0) continue;

                // 遇到 newmtl 行，表示一个新的材质开始。
                if (line.startsWith("newmtl ")) { // new material
                    // new material next iteration
                    if (createNewMaterial) {
                        //将当前材质 添加到 materials 中
                        materials.add(currMaterial.getName(), currMaterial);
                        // prepare next material
                        currMaterial = new Material();
                    }

                    // create next material next time
                    createNewMaterial = true;

                    // 提取并设置材料名称
                    currMaterial.setName(line.substring(6).trim());

                    // log event
                    Log.d("WavefrontMaterialsParse", "New material found: " + currMaterial.getName());

                // 纹理文件路径
                } else if (line.startsWith("map_Kd ")) { // texture filename

                    // bind texture
                    currMaterial.setTextureFile(line.substring(6).trim());

                    // log event
                    Log.v("WavefrontMaterialsParse", "Texture found: " + currMaterial.getTextureFile());

                // 环境颜色 Ka
                } else if (line.startsWith("Ka ")) {

                    // ambient colour
                    currMaterial.setAmbient(Math3DUtils.parseFloat(" ".split(line.substring(2).trim())));

                    // log event
                    Log.v("WavefrontMaterialsParse", "Ambient color: " + Arrays.toString(currMaterial.getAmbient()));
                // 漫反射颜色 Kd
                } else if (line.startsWith("Kd ")) {

                    // diffuse colour
                    currMaterial.setDiffuse(Math3DUtils.parseFloat(" ".split(line.substring(2).trim())));

                    // log event
                    Log.v("WavefrontMaterialsParse", "Diffuse color: " + Arrays.toString(currMaterial.getDiffuse()));
                // 镜面颜色 Ks
                } else if (line.startsWith("Ks ")) {

                    // specular colour
                    currMaterial.setSpecular(Math3DUtils.parseFloat(line.substring(2).trim().split(" ")));

                    // log event
                    Log.v("WavefrontMaterialsParse", "Specular color: " + Arrays.toString(currMaterial.getSpecular()));
                // 镜面强度 Ns
                } else if (line.startsWith("Ns ")) {

                    // shininess
                    float val = Float.parseFloat(line.substring(3));
                    currMaterial.setShininess(val);

                    // log event
                    Log.v("WavefrontMaterialsParse", "Shininess: " + currMaterial.getShininess());

                // alpha
                } else if (line.charAt(0) == 'd') {

                    // alpha
                    float val = Float.parseFloat(line.substring(2));
                    currMaterial.setAlpha(val);

                    // log event
                    Log.v("WavefrontMaterialsParse", "Alpha: " + currMaterial.getAlpha());
                // 表示透明度（倒置）
                } else if (line.startsWith("Tr ")) {

                    // Transparency (inverted)
                    currMaterial.setAlpha(1 - Float.parseFloat(line.substring(3)));

                    // log event
                    Log.v("WavefrontMaterialsParse", "Transparency (1-Alpha): " + currMaterial.getAlpha());

                 // 光照模型
                } else if (line.startsWith("illum ")) {

                    // illumination model
                    Log.v("WavefrontMaterialsParse", "Ignored line: " + line);

                } else if (line.charAt(0) == '#') { // comment line

                    // log comment
                    Log.v("WavefrontMaterialsParse", line);

                } else {

                    // log event
                    Log.v("WavefrontMaterialsParse", "Ignoring line: " + line);
                }

            }

            // add last processed material
            materials.add(currMaterial.getName(), currMaterial);

        } catch (Exception e) {
            Log.e("WavefrontMaterialsParse", e.getMessage(), e);
        }

        // log event
        Log.i("WavefrontMaterialsParse", "Parsed materials: " + materials);

        return materials;
    }
}
