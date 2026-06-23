package com.threedstudio.modelio

import com.threedstudio.render.core.Vec3
import com.threedstudio.render.core.Color
import com.threedstudio.render.core.MeshData

/**
 * ModelData - 通用3D模型数据结构
 * 包含模型名称、网格列表、骨骼列表、表情形变、纹理路径等完整信息
 */
data class ModelData(
    val name: String = "Unnamed Model",
    val meshes: List<MeshData> = emptyList(),
    val bones: List<BoneData> = emptyList(),
    val morphTargets: List<MorphTargetData> = emptyList(),
    val textures: List<String> = emptyList(),
    val materialColors: List<Color> = emptyList(),
    val version: String = "1.0"
)

/**
 * BoneData - 骨骼数据
 * 存储PMX骨骼层级结构信息
 */
data class BoneData(
    val name: String = "Bone",
    val parentIndex: Int = -1,
    val position: Vec3 = Vec3.ZERO,
    val ikTarget: String? = null,
    val ikChainLength: Int = 0
)

/**
 * MorphTargetData - 表情形变/BlendShape数据
 * 存储形变名称及对应的顶点偏移量
 */
data class MorphTargetData(
    val name: String = "Morph",
    val baseDeformations: FloatArray = FloatArray(0),
    val targetDeformations: FloatArray = FloatArray(0)
)

/**
 * MaterialData - 材质数据
 * 存储材质属性、贴图路径等渲染参数
 */
data class MaterialData(
    val name: String = "Material",
    val diffuseColor: Color = Color.WHITE,
    val specularColor: Color = Color(0.5f, 0.5f, 0.5f),
    val ambientColor: Color = Color(0.2f, 0.2f, 0.2f),
    val specularPower: Float = 32f,
    val diffuseTexture: String? = null,
    val normalTexture: String? = null,
    val specularTexture: String? = null
)
