// WavefrontLoader.java
// Andrew Davison, February 2007, ad@fivedots.coe.psu.ac.th

/* Load the OBJ model from MODEL_DIR, centering and scaling it.
 The scale comes from the sz argument in the constructor, and
 is implemented by changing the vertices of the loaded model.

 The model can have vertices, normals and tex coordinates, and
 refer to materials in a MTL file.

 The OpenGL commands for rendering the model are stored in 
 a display list (modelDispList), which is drawn by calls to
 draw().

 Information about the model is printed to stdout.

 Based on techniques used in the OBJ loading code in the
 JautOGL multiplayer racing game by Evangelos Pournaras 
 (http://today.java.net/pub/a/today/2006/10/10/
 development-of-3d-multiplayer-racing-game.html 
 and https://jautogl.dev.java.net/), and the 
 Asteroids tutorial by Kevin Glass 
 (http://www.cokeandcode.com/asteroidstutorial)

 CHANGES (Feb 2007)
 - a global flipTexCoords boolean
 - drawToList() sets and uses flipTexCoords
 */

package com.wardtn.modellibrary.services.wavefront;

import static com.wardtn.modellibrary.util.ModelFileUtilKt.isFileExists;

import android.net.Uri;
import android.opengl.GLES20;

import androidx.annotation.Nullable;

import android.util.Log;


import com.wardtn.modellibrary.model.Element;
import com.wardtn.modellibrary.model.Material;
import com.wardtn.modellibrary.model.Materials;
import com.wardtn.modellibrary.model.Object3DData;
import com.wardtn.modellibrary.services.LoadListener;
import com.wardtn.modellibrary.services.collada.entities.MeshData;
import com.wardtn.modellibrary.services.collada.entities.Vertex;
import com.wardtn.modellibrary.util.android.ContentUtils;
import com.wardtn.modellibrary.util.io.IOUtils;
import com.wardtn.modellibrary.util.AdjustUtil;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class WavefrontLoader {

    private final int triangulationMode;
    private final LoadListener callback;

    private String folder = "";
    private String overLayoutPath = "";

    public WavefrontLoader(int triangulationMode, LoadListener callback) {
        this.triangulationMode = triangulationMode;
        this.callback = callback;
    }

    @Nullable
    public static String getMaterialLib(Uri uri) {
        return getParameter(uri, "mtllib ");
    }

    @Nullable
    public static String getTextureFile(Uri uri) {
        return getParameter(uri, "map_Kd ");
    }

    @Nullable
    private static String getParameter(Uri uri, String parameter) {
        try (BufferedReader br = new BufferedReader(new InputStreamReader(ContentUtils.getInputStream(uri)))) {
            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.startsWith(parameter)) {
                    return line.substring(parameter.length()).trim();
                }
            }
        } catch (IOException e) {
            Log.e("WavefrontLoader", "Problem reading file '" + uri + "': " + e.getMessage(), e);
            throw new RuntimeException(e);
        }
        return null;
    }

    /**
     * 加载模型
     *
     * @param objPath 路径
     * @return
     */
    public synchronized List<Object3DData> load(String objPath,String overLayoutname) {
        try {
//            InputStream is = r.getAssets().open(fname);

            // get Folder
            folder = objPath.substring(0, objPath.lastIndexOf("/"));
            this.overLayoutPath = overLayoutname;

            //更换成路径传输
            InputStream is = new FileInputStream(objPath);
            // log event
//            Log.i("WavefrontLoader", "Loading model... " + modelURI.toString());
            // log event
            Log.i("WavefrontLoader", "--------------------------------------------------");
            Log.i("WavefrontLoader", "Parsing geometries... ");
            Log.i("WavefrontLoader", "--------------------------------------------------");
            // open stream, parse model, then close stream
//            final InputStream is = modelURI.toURL().openStream();

            // load model
            final List<MeshData> meshes = loadModel(objPath, is);

            is.close();
            // 3D meshes
            final List<Object3DData> ret = new ArrayList<>();
            // log event
            Log.i("WavefrontLoader", "Processing geometries... ");

            for (MeshData meshData : meshes) {
                // fix missing or wrong normals
                meshData.fixNormals();
                meshData.validate();
                // create 3D object
                Object3DData data3D = new Object3DData(meshData.getVertexBuffer());
                data3D.setMeshData(meshData);
                data3D.setId(meshData.getId());
                data3D.setName(meshData.getName());
                data3D.setNormalsBuffer(meshData.getNormalsBuffer());
                data3D.setTextureBuffer(meshData.getTextureBuffer());
                data3D.setElements(meshData.getElements());
                data3D.setId(objPath);
                data3D.setDrawUsingArrays(false);
                data3D.setDrawMode(GLES20.GL_TRIANGLES);
                // add model to scene
                callback.onLoad(data3D);
                // notify listener
                String jpgPath = loadMaterials(meshData);
                data3D.setTetxurePath(jpgPath);
            }
            // log event
            Log.i("WavefrontLoader", "Loaded geometries: " + ret.size());

            return ret;
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    /**
     * 加载 材质 mtl
     *
     * @param meshData
     */
    private String loadMaterials(MeshData meshData) {
        // process materials

        String jpgName = "";
        if (meshData.getMaterialFile() == null) return jpgName;

        // log event
        Log.i("WavefrontLoader", "--------------------------------------------------");
        Log.i("WavefrontLoader", "Parsing materials... ");
        Log.i("WavefrontLoader", "--------------------------------------------------");

        try {
            // 执行 MTL
//            String mtlPath = ContentUtils.getMtlPath();

            String mtlPath = folder + "/" + meshData.getMaterialFile();

//            String mtlPath =  get3DDownloadPath() + "/" +  meshData.mtlPath;

            final InputStream inputStream = new FileInputStream(mtlPath);

            // parse materials
            final WavefrontMaterialsParser materialsParser = new WavefrontMaterialsParser();
            final Materials materials = materialsParser.parse(meshData.getMaterialFile(), inputStream);

            // check if there is any material
            if (materials.size() > 0) {
                // bind materials
                for (int e = 0; e < meshData.getElements().size(); e++) {
                    // get element
                    final Element element = meshData.getElements().get(e);
                    // log event
                    Log.i("WavefrontLoader", " Wave Processing element... " + element.getId());
                    // get material id
                    final String elementMaterialId = element.getMaterialId();
                    // check if element has material
                    if (elementMaterialId != null && materials.contains(elementMaterialId)) {
                        // get material for element
                        final Material elementMaterial = materials.get(elementMaterialId);
                        // bind material
                        element.setMaterial(elementMaterial);
                        // check if element has texture mapped
                        if (elementMaterial.getTextureFile() != null) {
                            // log event
                            Log.i("WavefrontLoader", "Reading texture file... " + elementMaterial.getTextureFile());

                            //执行JPG
                            jpgName = folder + "/" + elementMaterial.getTextureFile();
                            try (InputStream stream = new FileInputStream(jpgName)) {
                                // read data
                                elementMaterial.setTextureData(IOUtils.read(stream));
                                // log event
                                Log.i("WavefrontLoader", "Texture linked... " + element.getMaterial().getTextureFile());
                            } catch (Exception ex) {
                                Log.e("WavefrontLoader", String.format("Error reading texture file: %s", ex.getMessage()));
                            }


                            //执行 纹理叠加
                            if (isFileExists(overLayoutPath)){
                                try (InputStream stream = new FileInputStream(overLayoutPath)) {
                                    // read data
                                    elementMaterial.setOverlayData(IOUtils.read(stream));
                                    Log.i("WavefrontLoader", "叠加纹理" + overLayoutPath);
                                } catch (Exception ex) {
                                    Log.e("WavefrontLoader", String.format("Error reading texture file: %s", ex.getMessage()));
                                }
                            }
                        }
                    }
                }
            }
        } catch (IOException ex) {
            Log.e("WavefrontLoader", "Error loading materials... " + meshData.getMaterialFile() + " " + ex.getMessage());
        }
        return jpgName;
    }

    /**
     * 加载Obj Path
     * @param objPath
     * @param is
     * @return
     */
    private List<MeshData> loadModel(String objPath, InputStream is) {

        // log event
        Log.i("WavefrontLoader", "Loading model... " + objPath);

        // String fnm = MODEL_DIR + modelNm + ".obj";
        BufferedReader br = null;
        try {
            br = new BufferedReader(new InputStreamReader(is));

            // debug model purposes
            int lineNum = 0;
            String line = null;

            // primitive data
            final List<float[]> vertexList = new ArrayList<>(); // 顶点数据
            final List<float[]> normalsList = new ArrayList<>(); // 法向量数据
            final List<float[]> textureList = new ArrayList<>(); // 纹理数据

            // mesh data
            final List<MeshData> meshes = new ArrayList<>();
            final List<Vertex> verticesAttributes = new ArrayList<>();

            // material file
            String mtllib = null;

            // smoothing groups
            final Map<String, List<Vertex>> smoothingGroups = new HashMap<>();
            List<Vertex> currentSmoothingList = null;

            // mesh current
            MeshData.Builder meshCurrent = new MeshData.Builder().id(objPath);
            Element.Builder elementCurrent = new Element.Builder().id("default");
            List<Integer> indicesCurrent = new ArrayList<>();

            boolean buildNewMesh = false;
            boolean buildNewElement = false;

            try {
                while (((line = br.readLine()) != null)) {
                    lineNum++;
                    line = line.trim();
                    if (line.isEmpty()) continue;
                    if (line.startsWith("v ")) { // vertex 顶点
                        parseVector(vertexList, line.substring(2).trim());
                    } else if (line.startsWith("vn")) { // normal 法线
                        parseVector(normalsList, line.substring(3).trim());
                    } else if (line.startsWith("vt")) { // tex coord 纹理坐标
                        parseVariableVector(textureList, line.substring(3).trim());
                    } else if (line.charAt(0) == 'o') { // object group 对象

                        if (buildNewMesh) {
                            // build mesh  构建当前网格并添加到网格列表中
                            // 创建一个Element 元素
                            meshCurrent
                                    .vertices(vertexList)
                                    .normals(normalsList)
                                    .textures(textureList)
                                    .vertexAttributes(verticesAttributes)
                                    .materialFile(mtllib)
                                    .addElement(elementCurrent.indices(indicesCurrent).build());

                            // add current mesh
                            final MeshData build = meshCurrent.build();
                            meshes.add(build);

                            // log event
                            Log.d("WavefrontLoader", "Loaded mesh. id:" + build.getId() + ", indices: " + indicesCurrent.size()
                                    + ", vertices:" + vertexList.size()
                                    + ", normals: " + normalsList.size()
                                    + ", textures:" + textureList.size()
                                    + ", elements: " + build.getElements());

                            // next mesh
                            meshCurrent = new MeshData.Builder().id(line.substring(1).trim());

                            // next element
                            elementCurrent = new Element.Builder();
                            indicesCurrent = new ArrayList<>();
                        } else {
                            meshCurrent.id(line.substring(1).trim());
                            buildNewMesh = true;
                        }
                    } else if (line.charAt(0) == 'g') { // group name
                        if (buildNewElement && !indicesCurrent.isEmpty()) {

                            // add current element
                            elementCurrent.indices(indicesCurrent);
                            meshCurrent.addElement(elementCurrent.build());

                            // log event
                            Log.d("WavefrontLoader", "New element. indices: " + indicesCurrent.size());

                            // prepare next element
                            indicesCurrent = new ArrayList<>();
                            elementCurrent = new Element.Builder().id(line.substring(1).trim());
                        } else {
                            elementCurrent.id(line.substring(1).trim());
                            buildNewElement = true;
                        }
                    } else if (line.startsWith("f ")) { // face 面
                        parseFace(verticesAttributes, indicesCurrent, vertexList, normalsList, textureList, line.substring(2), currentSmoothingList);

                    } else if (line.startsWith("mtllib ")) {// build material 材质文件
                        mtllib = line.substring(7);

                    } else if (line.startsWith("usemtl ")) {// use material  使用材质
                        if (elementCurrent.getMaterialId() != null) {

                            // change element since we are dealing with different material
                            elementCurrent.indices(indicesCurrent);
                            meshCurrent.addElement(elementCurrent.build());

                            // log event
                            Log.v("WavefrontLoader", "New material: " + line);

                            // prepare next element
                            indicesCurrent = new ArrayList<>();
                            elementCurrent = new Element.Builder().id(elementCurrent.getId());
                        }

                        elementCurrent.materialId(line.substring(7));
                    } else if (line.charAt(0) == 's') { // smoothing group 光滑组
                        final String smoothingGroupId = line.substring(1).trim();
                        if ("0".equals(smoothingGroupId) || "off".equals(smoothingGroupId)) {
                            currentSmoothingList = null;
                        } else {
                            currentSmoothingList = new ArrayList<>();
                            smoothingGroups.put(smoothingGroupId, currentSmoothingList);
                        }
                    } else if (line.charAt(0) == '#') { // comment line 忽略注释行
                        Log.v("WavefrontLoader", line);
                    } else
                        Log.w("WavefrontLoader", "Ignoring line " + lineNum + " : " + line);

                }

                // build mesh
                final Element element = elementCurrent.indices(indicesCurrent).build();
                Log.e("CHEN", "当前 法向量大小 " + normalsList.size());
                final MeshData meshData = meshCurrent.vertices(vertexList).normals(normalsList).textures(textureList)
                        .vertexAttributes(verticesAttributes).materialFile(mtllib)
                        .addElement(element).smoothingGroups(smoothingGroups).build();

                Log.i("WavefrontLoader", "Loaded mesh. id:" + meshData.getId() + ", indices: " + indicesCurrent.size()
                        + ", vertices:" + vertexList.size()
                        + ", normals: " + normalsList.size()
                        + ", textures:" + textureList.size()
                        + ", elements: " + meshData.getElements());

                // add mesh
                meshes.add(meshData);

                // return all meshes
                return meshes;

            } catch (Exception e) {
                Log.e("WavefrontLoader", "Error reading line: " + lineNum + ":" + line, e);
                Log.e("WavefrontLoader", e.getMessage(), e);
                throw new RuntimeException(e);
            }
        } finally {
            if (br != null) {
                try {
                    br.close();
                } catch (IOException e) {
                    Log.e("WavefrontLoader", e.getMessage(), e);
                }
            }
        }
    }

    /**
     * List of texture coordinates, in (u, [,v ,w]) coordinates, these will vary between 0 and 1. v, w are optional and default to 0.
     * 纹理坐标列表，坐标格式为 (u, [,v ,w])，这些值将在 0 和 1 之间变化。v 和 w 是可选的，默认为 0。
     * There may only be 1 tex coords  on the line, which is determined by looking at the first tex coord line.
     * 每行可能只有一个纹理坐标，这可以通过查看第一行纹理坐标来确定。
     */
    private void parseVector(List<float[]> vectorList, String line) {
        try {
            final String[] tokens = line.split(" +");
            final float[] vector = new float[3];
            vector[0] = Float.parseFloat(tokens[0]);
            vector[1] = Float.parseFloat(tokens[1]);
            vector[2] = Float.parseFloat(tokens[2]);
            AdjustUtil.adjustMaxMin(vector[0], vector[1], vector[2]);
            vectorList.add(vector);
        } catch (Exception ex) {
            Log.e("WavefrontLoader", "Error parsing vector '" + line + "': " + ex.getMessage());
//            vectorList.add(new float[3]);
        }
    }

    /**
     * List of texture coordinates, in (u, [,v ,w]) coordinates, these will vary between 0 and 1. v, w are optional and default to 0.
     * There may only be 1 tex coords  on the line, which is determined by looking at the first tex coord line.
     */
    private void parseVariableVector(List<float[]> textureList, String line) {
        try {
            final String[] tokens = line.split(" +");
            final float[] vector = new float[2];
            vector[0] = Float.parseFloat(tokens[0]);
            if (tokens.length > 1) {
                vector[1] = Float.parseFloat(tokens[1]);
                // ignore 3d coordinate
				/*if (tokens.length > 2) {
					vector[2] = Float.parseFloat(tokens[2]);
				}*/
            }
            textureList.add(vector);
        } catch (Exception ex) {
            Log.e("WavefrontLoader", ex.getMessage());
            textureList.add(new float[2]);
        }

    }

    /**
     * get this face's indicies from line "f v/vt/vn ..." with vt or vn index values perhaps being absent.
     * 从行 "f v/vt/vn ..." 中获取这个面的索引，其中 vt 或 vn 的索引值可能不存在。
     */
    private void parseFace(List<Vertex> vertexAttributes, List<Integer> indices,
                           List<float[]> vertexList, List<float[]> normalsList, List<float[]> texturesList,
                           String line, List<Vertex> currentSmoothingList) {
        try {

            // 判断 line 是否包含双空格，选择合适的分割方式
            final String[] tokens;
            if (line.contains("  ")) {
                tokens = line.split(" +");
            } else {
                tokens = line.split(" ");
            }
            // 将一行面的数据按顶点分隔为数组（tokens），例如 1/2/3

            // number of v/vt/vn tokens
            final int numTokens = tokens.length;

            for (int i = 0, faceIndex = 0; i < numTokens; i++, faceIndex++) {

                // convert to triangles all polygons 如果一个面有超过 3 个顶点，将其分解为多个三角形
                if (faceIndex > 2) {
                    // Converting polygon to triangle
                    faceIndex = 0;

                    i -= 2;
                }

                // triangulate polygon
                final String faceToken;
                // 支持不同的三角化模式
                // GL_TRIANGLE_FAN：以第一个顶点为公共点创建三角形。
                // GL_TRIANGLES 和 GL_TRIANGLE_STRIP：逐顶点解析。

                if (this.triangulationMode == GLES20.GL_TRIANGLE_FAN) {
                    // In FAN mode all meshObject shares the initial vertex
                    if (faceIndex == 0) {
                        faceToken = tokens[0];// get a v/vt/vn
                    } else {
                        faceToken = tokens[i]; // get a v/vt/vn
                    }
                } else {
                    // GL.GL_TRIANGLES | GL.GL_TRIANGLE_STRIP
                    faceToken = tokens[i]; // get a v/vt/vn
                }

                // parse index tokens
                // how many '/'s are there in the token
                final String[] faceTokens = faceToken.split("/");
                final int numSeps = faceTokens.length;

                // 解析顶点索引
                int vertIdx = Integer.parseInt(faceTokens[0]);
                // A valid vertex index matches the corresponding vertex elements of a previously defined vertex list.
                // If an index is positive then it refers to the offset in that vertex list, starting at 1.
                // If an index is negative then it relatively refers to the end of the vertex list,
                // -1 referring to the last element.

                // 将顶点索引从字符串解析为整数
                // 如果索引为负数，则从末尾计算顶点位置
                // 如果为正数，则减 1（OBJ 索引从 1 开始）。
                if (vertIdx < 0) {
                    vertIdx = vertexList.size() + vertIdx;
                } else {
                    vertIdx--;
                }

                // 解析纹理和法线索引

                int textureIdx = -1;
                if (numSeps > 1 && faceTokens[1].length() > 0) {
                    textureIdx = Integer.parseInt(faceTokens[1]);
                    if (textureIdx < 0) {
                        textureIdx = texturesList.size() + textureIdx;
                    } else {
                        textureIdx--;
                    }
                }
                int normalIdx = -1;
                if (numSeps > 2 && faceTokens[2].length() > 0) {
                    normalIdx = Integer.parseInt(faceTokens[2]);
                    if (normalIdx < 0) {
                        normalIdx = normalsList.size() + normalIdx;
                    } else {
                        normalIdx--;
                    }
                }

                // create VertexAttribute
                // 创建并存储顶点属性
                final Vertex vertexAttribute = new Vertex(vertIdx);
                vertexAttribute.setNormalIndex(normalIdx);
                vertexAttribute.setTextureIndex(textureIdx);

                // add VertexAtribute
                final int idx = vertexAttributes.size();
                vertexAttributes.add(idx, vertexAttribute);

                // store the indices for this face
                indices.add(idx);

                // smoothing
                if (currentSmoothingList != null) {
                    currentSmoothingList.add(vertexAttribute);
                }
            }
        } catch (NumberFormatException e) {
            Log.e("WavefrontLoader", e.getMessage(), e);
        }
    }

}
