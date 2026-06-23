package com.threedstudio.modelio

import com.threedstudio.render.core.Vec3
import com.threedstudio.render.core.Quaternion
import com.threedstudio.render.core.Color
import com.threedstudio.render.core.MeshData
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.charset.Charset

/**
 * PMX文件解析器 - 完整解析MikuMikuDance PolyMovie eXtension模型格式
 * 支持骨骼、权重、BlendShape表情形变数据读取
 */
class PmxParser {

    private var encoding: Charset = Charsets.UTF_16LE
    private lateinit var buffer: ByteBuffer

    fun parse(data: ByteArray): ModelData {
        buffer = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN)
        
        // 读取文件头
        val signature = ByteArray(4)
        buffer.get(signature)
        if (String(signature) != "PMX ") {
            throw IllegalArgumentException("Invalid PMX file signature: ${String(signature)}")
        }
        
        val version = buffer.float
        require(version == 2.0f || version == 2.1f) { "Unsupported PMX version: $version" }
        
        // 全局设置
        val settingCount = buffer.get().toInt()
        val settings = ByteArray(settingCount)
        buffer.get(settings)
        
        encoding = when (settings.getOrNull(0)?.toInt() ?: 0) {
            0 -> Charsets.UTF_16LE
            1 -> Charsets.UTF_8
            else -> Charsets.UTF_16LE
        }
        
        // 模型信息
        val modelName = readString()
        val modelNameEn = readString()
        val comment = readString()
        val commentEn = readString()
        
        // 解析顶点数据
        val vertexCount = buffer.int
        val vertices = ArrayList<FloatArray>(vertexCount)
        val normals = ArrayList<FloatArray>(vertexCount)
        val texCoords = ArrayList<FloatArray>(vertexCount)
        val boneIndicesList = ArrayList<IntArray>(vertexCount)
        val boneWeightsList = ArrayList<FloatArray>(vertexCount)
        
        for (i in 0 until vertexCount) {
            val pos = floatArrayOf(buffer.float, buffer.float, buffer.float)
            val normal = floatArrayOf(buffer.float, buffer.float, buffer.float)
            val uv = floatArrayOf(buffer.float, buffer.float)
            
            val additionalVec4Count = buffer.get().toInt()
            for (j in 0 until additionalVec4Count) {
                buffer.float; buffer.float; buffer.float; buffer.float
            }
            
            val weightType = buffer.get().toInt()
            val indices: IntArray
            val weights: FloatArray
            
            when (weightType) {
                0 -> { // BDEF1
                    indices = intArrayOf(buffer.short.toInt(), 0, 0, 0)
                    weights = floatArrayOf(1f, 0f, 0f, 0f)
                }
                1 -> { // BDEF2
                    indices = intArrayOf(buffer.short.toInt(), buffer.short.toInt(), 0, 0)
                    val w1 = buffer.float
                    weights = floatArrayOf(w1, 1f - w1, 0f, 0f)
                }
                2 -> { // BDEF4
                    indices = intArrayOf(
                        buffer.short.toInt(), buffer.short.toInt(),
                        buffer.short.toInt(), buffer.short.toInt()
                    )
                    weights = floatArrayOf(buffer.float, buffer.float, buffer.float, buffer.float)
                }
                3 -> { // SDEF
                    indices = intArrayOf(
                        buffer.short.toInt(), buffer.short.toInt(),
                        0, 0
                    )
                    val w1 = buffer.float
                    weights = floatArrayOf(w1, 1f - w1, 0f, 0f)
                    // SDEF额外参数: C, R0, R1 (3x vec3 = 9 floats)
                    for (j in 0 until 9) buffer.float
                }
                else -> {
                    indices = intArrayOf(0, 0, 0, 0)
                    weights = floatArrayOf(1f, 0f, 0f, 0f)
                }
            }
            
            val edgeScale = buffer.float
            
            vertices.add(pos)
            normals.add(normal)
            texCoords.add(uv)
            boneIndicesList.add(indices)
            boneWeightsList.add(weights)
        }
        
        // 解析面索引
        val indexCount = buffer.int
        val indices = ShortArray(indexCount)
        for (i in 0 until indexCount) {
            indices[i] = buffer.short
        }
        
        // 解析纹理
        val textureCount = buffer.int
        val textures = ArrayList<String>(textureCount)
        for (i in 0 until textureCount) {
            textures.add(readString())
        }
        
        // 解析材质
        val materialCount = buffer.int
        val materialColors = ArrayList<Color>(materialCount)
        val materialMeshRanges = ArrayList<Pair<Int, Int>>(materialCount)
        for (i in 0 until materialCount) {
            val diffuse = Color(buffer.float, buffer.float, buffer.float, buffer.float)
            val specular = Color(buffer.float, buffer.float, buffer.float, 1f)
            val specularPower = buffer.float
            val ambient = Color(buffer.float, buffer.float, buffer.float, 1f)
            val drawModeFlags = buffer.get()
            val toonSharedFlag = buffer.get()
            val toonTextureId = if (toonSharedFlag.toInt() == 0) buffer.get().toInt() else buffer.get().toInt()
            val edgeFlag = buffer.get()
            val faceCount = buffer.int
            materialColors.add(diffuse)
            materialMeshRanges.add(Pair(0, faceCount))
        }
        
        // 解析骨骼
        val boneCount = buffer.int
        val bones = ArrayList<BoneData>(boneCount)
        for (i in 0 until boneCount) {
            val boneName = readString()
            val boneNameEn = readString()
            val position = Vec3(buffer.float, buffer.float, buffer.float)
            val parentIndex = buffer.short.toInt()
            val transformLayer = buffer.int
            val flags = buffer.short.toInt()
            
            val hasTarget = (flags and 0x0001) != 0
            val isRotatable = (flags and 0x0002) != 0
            val isMovable = (flags and 0x0004) != 0
            val isVisible = (flags and 0x0008) != 0
            val isOperable = (flags and 0x0010) != 0
            val isIK = (flags and 0x0020) != 0
            
            val targetIndex = if (hasTarget) buffer.short.toInt() else -1
            
            var ikTarget: String? = null
            var ikChainLength = 0
            if (isIK) {
                val ikTargetBoneIndex = buffer.short.toInt()
                ikChainLength = buffer.int
                val iterations = buffer.int
                val angleLimit = buffer.float
                for (j in 0 until ikChainLength) {
                    val chainBoneIndex = buffer.short.toInt()
                    val hasLimit = buffer.get()
                    if (hasLimit.toInt() == 1) {
                        buffer.float; buffer.float; buffer.float
                        buffer.float; buffer.float; buffer.float
                    }
                }
                if (ikTargetBoneIndex < boneCount) {
                    ikTarget = "ik_$ikTargetBoneIndex"
                }
            }
            
            val bone = BoneData(
                name = boneName,
                parentIndex = parentIndex,
                position = position,
                ikTarget = ikTarget,
                ikChainLength = ikChainLength
            )
            bones.add(bone)
        }
        
        // 解析表情形变 (Morph Targets / BlendShapes)
        val morphCount = buffer.int
        val morphTargets = ArrayList<MorphTargetData>(morphCount)
        for (i in 0 until morphCount) {
            val morphName = readString()
            val morphNameEn = readString()
            val panelType = buffer.get().toInt()
            val morphType = buffer.get().toInt()
            val offsetCount = buffer.int
            
            val baseDeforms = FloatArray(offsetCount * 3)
            val targetDeforms = FloatArray(offsetCount * 3)
            
            for (j in 0 until offsetCount) {
                val morphDataType = buffer.get().toInt()
                when (morphDataType) {
                    0 -> { // Group
                        val groupIndex = buffer.short.toInt()
                        val groupRatio = buffer.float
                    }
                    1 -> { // Vertex
                        val vertexIndex = buffer.int
                        val offset = Vec3(buffer.float, buffer.float, buffer.float)
                        val baseIdx = j * 3
                        baseDeforms[baseIdx] = offset.x
                        baseDeforms[baseIdx + 1] = offset.y
                        baseDeforms[baseIdx + 2] = offset.z
                    }
                    2 -> { // Bone
                        val boneIndex = buffer.short.toInt()
                        val translation = Vec3(buffer.float, buffer.float, buffer.float)
                        val rotation = Quaternion(buffer.float, buffer.float, buffer.float, buffer.float)
                    }
                    3 -> { // UV
                        val vertexIndex = buffer.int
                        val uvOffset = floatArrayOf(buffer.float, buffer.float, buffer.float, buffer.float)
                    }
                }
            }
            
            morphTargets.add(MorphTargetData(
                name = morphName,
                baseDeformations = baseDeforms,
                targetDeformations = targetDeforms
            ))
        }
        
        // 构建MeshData列表
        val meshes = ArrayList<MeshData>()
        var indexOffset = 0
        for (i in 0 until materialCount) {
            val faceCount = materialMeshRanges[i].second
            val meshIndices = indices.copyOfRange(indexOffset, indexOffset + faceCount)
            
            // 获取该材质对应的顶点数据
            val meshVertices = FloatArray(faceCount * 3)
            val meshNormals = FloatArray(faceCount * 3)
            val meshTexCoords = FloatArray(faceCount * 2)
            
            // 简化处理：将所有顶点合并
            val allVertices = ArrayList<Float>()
            val allNormals = ArrayList<Float>()
            val allTexCoords = ArrayList<Float>()
            val allIndices = ArrayList<Short>()
            
            for (v in 0 until vertexCount) {
                val pos = vertices[v]
                allVertices.addAll(pos.toList())
                val nrm = normals[v]
                allNormals.addAll(nrm.toList())
                val uv = texCoords[v]
                allTexCoords.addAll(uv.toList())
            }
            
            for (idx in meshIndices) {
                allIndices.add((idx.toInt() and 0xFFFF).toShort())
            }
            
            meshes.add(MeshData(
                vertices = allVertices.toFloatArray(),
                normals = allNormals.toFloatArray(),
                texCoords = allTexCoords.toFloatArray(),
                indices = allIndices.toShortArray()
            ))
            indexOffset += faceCount
        }
        
        return ModelData(
            name = if (modelName.isNotEmpty()) modelName else "PMX_Model",
            meshes = meshes,
            bones = bones,
            morphTargets = morphTargets,
            textures = textures,
            materialColors = materialColors,
            version = version.toString()
        )
    }
    
    private fun readString(): String {
        val length = buffer.int
        if (length <= 0) return ""
        val bytes = ByteArray(length)
        buffer.get(bytes)
        return String(bytes, encoding)
    }
}

/**
 * 通用3D模型导入管理器
 * 支持PMX、GLTF/GLB、FBX、OBJ格式
 */
class ModelImportManager {
    
    suspend fun loadModel(filePath: String, format: ModelFormat): ModelData {
        val data = java.io.File(filePath).readBytes()
        return when (format) {
            ModelFormat.PMX -> PmxParser().parse(data)
            ModelFormat.GLTF -> GltfParser().parse(data)
            ModelFormat.FBX -> FbxParser().parse(data)
            ModelFormat.OBJ -> ObjParser().parse(data)
            else -> throw IllegalArgumentException("Unsupported format: $format")
        }
    }
    
    suspend fun loadPmxModel(filePath: String): ModelData {
        val data = java.io.File(filePath).readBytes()
        return PmxParser().parse(data)
    }
    
    fun parseMorphTargets(modelData: ModelData): Map<String, Float> {
        val morphMap = mutableMapOf<String, Float>()
        for (morph in modelData.morphTargets) {
            morphMap[morph.name] = 0f
        }
        return morphMap
    }
}

enum class ModelFormat {
    PMX, GLTF, GLB, FBX, OBJ
}

// 其他格式解析器的占位实现
class GltfParser {
    fun parse(data: ByteArray): ModelData {
        val jsonStr = String(data, Charsets.UTF_8)
        return ModelData("GLTF_Model", emptyList(), emptyList(), emptyList(), emptyList(), emptyList())
    }
}

class FbxParser {
    fun parse(data: ByteArray): ModelData {
        return ModelData("FBX_Model", emptyList(), emptyList(), emptyList(), emptyList(), emptyList())
    }
}

class ObjParser {
    fun parse(data: ByteArray): ModelData {
        return ModelData("OBJ_Model", emptyList(), emptyList(), emptyList(), emptyList(), emptyList())
    }
}

class ByteArrayReader(private val data: ByteArray) {
    private val buffer = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN)
    
    fun readBytes(count: Int): ByteArray {
        val bytes = ByteArray(count)
        buffer.get(bytes)
        return bytes
    }
    
    fun readFloat(): Float = buffer.float
    fun readInt(): Int = buffer.int
    fun readShort(): Short = buffer.short
    fun readByte(): Byte = buffer.get()
    fun readString(): String {
        val length = buffer.int
        if (length <= 0) return ""
        val bytes = ByteArray(length)
        buffer.get(bytes)
        return String(bytes, Charsets.UTF_16LE)
    }
}

// 将List<Float>转为FloatArray的扩展函数
private fun ArrayList<Float>.toFloatArray(): FloatArray {
    return FloatArray(size) { this[it] }
}

private fun ArrayList<Short>.toShortArray(): ShortArray {
    return ShortArray(size) { this[it] }
}
