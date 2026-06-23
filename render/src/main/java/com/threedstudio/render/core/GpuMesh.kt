package com.threedstudio.render.core

import android.opengl.GLES30
import android.opengl.GLUtils
import android.graphics.Bitmap
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * 网格数据CPU端表示
 */
class MeshData(
    val vertices: FloatArray,
    val normals: FloatArray,
    val texCoords: FloatArray,
    val indices: ShortArray,
    val jointIndices: FloatArray? = null,
    val jointWeights: FloatArray? = null
) {
    val vertexCount: Int get() = vertices.size / 3
    val indexCount: Int get() = indices.size
}

/**
 * GPU端网格资源管理 (VAO/VBO/EBO/Texture)
 * 合并了之前冲突的 companion object 重复声明
 */
class GpuMesh {
    private var vao: Int = 0
    private var vboVertices: Int = 0
    private var vboNormals: Int = 0
    private var vboTexCoords: Int = 0
    private var ebo: Int = 0
    private var indexCount: Int = 0
    private var textureId: Int = 0

    companion object {
        fun createQuad(): GpuMesh {
            val vertices = floatArrayOf(
                -1f, -1f, 0f, 1f, -1f, 0f, 1f, 1f, 0f, -1f, 1f, 0f
            )
            val normals = floatArrayOf(
                0f, 0f, 1f, 0f, 0f, 1f, 0f, 0f, 1f, 0f, 0f, 1f
            )
            val texCoords = floatArrayOf(
                0f, 0f, 1f, 0f, 1f, 1f, 0f, 1f
            )
            val indices = shortArrayOf(0, 1, 2, 0, 2, 3)
            val mesh = GpuMesh()
            mesh.upload(MeshData(vertices, normals, texCoords, indices))
            return mesh
        }
    }

    fun upload(meshData: MeshData) {
        val vaoArray = IntArray(1)
        GLES30.glGenVertexArrays(1, vaoArray, 0)
        vao = vaoArray[0]
        GLES30.glBindVertexArray(vao)

        val vboArray = IntArray(3)
        GLES30.glGenBuffers(3, vboArray, 0)
        vboVertices = vboArray[0]
        vboNormals = vboArray[1]
        vboTexCoords = vboArray[2]

        uploadBuffer(vboVertices, meshData.vertices)
        GLES30.glVertexAttribPointer(0, 3, GLES30.GL_FLOAT, false, 0, 0)
        GLES30.glEnableVertexAttribArray(0)

        uploadBuffer(vboNormals, meshData.normals)
        GLES30.glVertexAttribPointer(1, 3, GLES30.GL_FLOAT, false, 0, 0)
        GLES30.glEnableVertexAttribArray(1)

        uploadBuffer(vboTexCoords, meshData.texCoords)
        GLES30.glVertexAttribPointer(2, 2, GLES30.GL_FLOAT, false, 0, 0)
        GLES30.glEnableVertexAttribArray(2)

        val eboArray = IntArray(1)
        GLES30.glGenBuffers(1, eboArray, 0)
        ebo = eboArray[0]
        val indexBuffer = ByteBuffer.allocateDirect(meshData.indices.size * 2)
            .order(ByteOrder.nativeOrder())
            .asShortBuffer()
            .apply {
                put(meshData.indices)
                position(0)
            }
        GLES30.glBindBuffer(GLES30.GL_ELEMENT_ARRAY_BUFFER, ebo)
        GLES30.glBufferData(GLES30.GL_ELEMENT_ARRAY_BUFFER, meshData.indices.size * 2,
            indexBuffer, GLES30.GL_STATIC_DRAW)

        indexCount = meshData.indexCount
        GLES30.glBindVertexArray(0)
    }

    private fun uploadBuffer(bufferId: Int, data: FloatArray) {
        val buffer = ByteBuffer.allocateDirect(data.size * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
            .apply {
                put(data)
                position(0)
            }
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, bufferId)
        GLES30.glBufferData(GLES30.GL_ARRAY_BUFFER, data.size * 4, buffer, GLES30.GL_STATIC_DRAW)
    }

    fun loadTexture(bitmap: Bitmap): Int {
        val textureIds = IntArray(1)
        GLES30.glGenTextures(1, textureIds, 0)
        textureId = textureIds[0]
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, textureId)
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MIN_FILTER, GLES30.GL_LINEAR_MIPMAP_LINEAR)
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MAG_FILTER, GLES30.GL_LINEAR)
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_S, GLES30.GL_REPEAT)
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_T, GLES30.GL_REPEAT)
        GLUtils.texImage2D(GLES30.GL_TEXTURE_2D, 0, bitmap, 0)
        GLES30.glGenerateMipmap(GLES30.GL_TEXTURE_2D)
        return textureId
    }

    fun bind() {
        GLES30.glBindVertexArray(vao)
        if (textureId != 0) {
            GLES30.glActiveTexture(GLES30.GL_TEXTURE0)
            GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, textureId)
        }
    }

    fun unbind() {
        GLES30.glBindVertexArray(0)
    }

    fun draw() {
        GLES30.glDrawElements(GLES30.GL_TRIANGLES, indexCount, GLES30.GL_UNSIGNED_SHORT, 0)
    }

    fun getTextureId(): Int = textureId

    fun delete() {
        val ids = IntArray(1)
        if (vao != 0) { ids[0] = vao; GLES30.glDeleteVertexArrays(1, ids, 0); vao = 0 }
        if (vboVertices != 0) { ids[0] = vboVertices; GLES30.glDeleteBuffers(1, ids, 0); vboVertices = 0 }
        if (vboNormals != 0) { ids[0] = vboNormals; GLES30.glDeleteBuffers(1, ids, 0); vboNormals = 0 }
        if (vboTexCoords != 0) { ids[0] = vboTexCoords; GLES30.glDeleteBuffers(1, ids, 0); vboTexCoords = 0 }
        if (ebo != 0) { ids[0] = ebo; GLES30.glDeleteBuffers(1, ids, 0); ebo = 0 }
        if (textureId != 0) { ids[0] = textureId; GLES30.glDeleteTextures(1, ids, 0); textureId = 0 }
    }
}
