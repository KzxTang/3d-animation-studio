package com.threedstudio.render.core

import android.opengl.GLES30
import android.util.Log

class Shader {
    private var programId: Int = 0
    private val uniformLocations = mutableMapOf<String, Int>()

    companion object {
        private const val TAG = "Shader"
        val VERT_SRC = """
            #version 300 es
            precision highp float;
            uniform mat4 u_mvpMatrix;
            uniform mat4 u_modelMatrix;
            uniform mat4 u_normalMatrix;
            in vec3 a_position;
            in vec3 a_normal;
            in vec2 a_texCoord;
            out vec3 v_worldPosition;
            out vec3 v_worldNormal;
            out vec2 v_texCoord;
            void main() {
                vec4 worldPos = u_modelMatrix * vec4(a_position, 1.0);
                v_worldPosition = worldPos.xyz;
                v_worldNormal = mat3(u_normalMatrix) * a_normal;
                v_texCoord = a_texCoord;
                gl_Position = u_mvpMatrix * vec4(a_position, 1.0);
            }
        """.trimIndent()

        val FRAG_SRC = """
            #version 300 es
            precision highp float;
            struct Light { int type; vec3 position; vec3 direction; vec3 color; float intensity; float angle; float range; };
            uniform vec3 u_cameraPosition;
            uniform vec3 u_ambientColor;
            uniform float u_ambientIntensity;
            uniform vec4 u_materialDiffuse;
            uniform vec3 u_materialSpecular;
            uniform float u_materialShininess;
            uniform sampler2D u_diffuseTexture;
            uniform bool u_hasDiffuseTexture;
            uniform Light u_lights[8];
            uniform int u_lightCount;
            in vec3 v_worldPosition;
            in vec3 v_worldNormal;
            in vec2 v_texCoord;
            out vec4 fragColor;
            
            vec3 calcDir(Light l, vec3 n, vec3 v, vec3 d) {
                vec3 ld = normalize(-l.direction);
                float df = max(dot(n, ld), 0.0);
                vec3 r = reflect(-ld, n);
                float sp = pow(max(dot(v, r), 0.0), u_materialShininess);
                return l.color * l.intensity * (df * d + sp * u_materialSpecular);
            }
            vec3 calcPoint(Light l, vec3 n, vec3 v, vec3 d) {
                vec3 ld = normalize(l.position - v_worldPosition);
                float dist = length(l.position - v_worldPosition);
                float att = 1.0 / (1.0 + 0.09 * dist + 0.032 * dist * dist);
                float df = max(dot(n, ld), 0.0);
                vec3 r = reflect(-ld, n);
                float sp = pow(max(dot(v, r), 0.0), u_materialShininess);
                return l.color * l.intensity * att * (df * d + sp * u_materialSpecular);
            }
            vec3 calcSpot(Light l, vec3 n, vec3 v, vec3 d) {
                vec3 ld = normalize(l.position - v_worldPosition);
                float theta = dot(ld, normalize(-l.direction));
                float eps = 0.91 - 0.82;
                float inten = clamp((theta - 0.82) / eps, 0.0, 1.0);
                float dist = length(l.position - v_worldPosition);
                float att = 1.0 / (1.0 + 0.09 * dist + 0.032 * dist * dist);
                float df = max(dot(n, ld), 0.0);
                vec3 r = reflect(-ld, n);
                float sp = pow(max(dot(v, r), 0.0), u_materialShininess);
                return l.color * l.intensity * att * inten * (df * d + sp * u_materialSpecular);
            }
            void main() {
                vec3 n = normalize(v_worldNormal);
                vec3 v = normalize(u_cameraPosition - v_worldPosition);
                vec3 d = u_hasDiffuseTexture ? texture(u_diffuseTexture, v_texCoord).rgb * u_materialDiffuse.rgb : u_materialDiffuse.rgb;
                vec3 amb = u_ambientColor * u_ambientIntensity * d;
                vec3 total = amb;
                for (int i = 0; i < u_lightCount; i++) {
                    if (u_lights[i].type == 0) total += calcDir(u_lights[i], n, v, d);
                    else if (u_lights[i].type == 1) total += calcPoint(u_lights[i], n, v, d);
                    else if (u_lights[i].type == 2) total += calcSpot(u_lights[i], n, v, d);
                }
                fragColor = vec4(total, u_materialDiffuse.a);
            }
        """.trimIndent()
    }

    fun createDefault(): Boolean = createShader(VERT_SRC, FRAG_SRC)

    fun createShader(vert: String, frag: String): Boolean {
        val vs = compile(GLES30.GL_VERTEX_SHADER, vert); if (vs == 0) return false
        val fs = compile(GLES30.GL_FRAGMENT_SHADER, frag)
        if (fs == 0) { GLES30.glDeleteShader(vs); return false }
        programId = GLES30.glCreateProgram()
        if (programId == 0) { GLES30.glDeleteShader(vs); GLES30.glDeleteShader(fs); return false }
        GLES30.glAttachShader(programId, vs); GLES30.glAttachShader(programId, fs)
        GLES30.glLinkProgram(programId)
        val st = IntArray(1); GLES30.glGetProgramiv(programId, GLES30.GL_LINK_STATUS, st, 0)
        if (st[0] == 0) { Log.e(TAG, "Link: ${GLES30.glGetProgramInfoLog(programId)}"); GLES30.glDeleteProgram(programId); programId = 0; return false }
        GLES30.glDeleteShader(vs); GLES30.glDeleteShader(fs); return true
    }

    private fun compile(type: Int, src: String): Int {
        val id = GLES30.glCreateShader(type); if (id == 0) return 0
        GLES30.glShaderSource(id, src); GLES30.glCompileShader(id)
        val st = IntArray(1); GLES30.glGetShaderiv(id, GLES30.GL_COMPILE_STATUS, st, 0)
        if (st[0] == 0) { Log.e(TAG, "Compile: ${GLES30.glGetShaderInfoLog(id)}"); GLES30.glDeleteShader(id); return 0 }
        return id
    }

    fun use() { GLES30.glUseProgram(programId) }
    fun setUniform(name: String, value: Float) { GLES30.glUniform1f(getUniformLocation(name), value) }
    fun setUniform(name: String, value: Int) { GLES30.glUniform1i(getUniformLocation(name), value) }
    fun setUniform(name: String, value: Boolean) { GLES30.glUniform1i(getUniformLocation(name), if (value) 1 else 0) }
    fun setUniform3f(name: String, x: Float, y: Float, z: Float) { GLES30.glUniform3f(getUniformLocation(name), x, y, z) }
    fun setUniform4f(name: String, x: Float, y: Float, z: Float, w: Float) { GLES30.glUniform4f(getUniformLocation(name), x, y, z, w) }
    fun setUniformMat4(name: String, matrix: Mat4) { GLES30.glUniformMatrix4fv(getUniformLocation(name), 1, false, matrix.arr(), 0) }
    private fun getUniformLocation(name: String): Int = uniformLocations.getOrPut(name) { GLES30.glGetUniformLocation(programId, name) }
    fun delete() { if (programId != 0) { GLES30.glDeleteProgram(programId); programId = 0 }; uniformLocations.clear() }
    fun isReady(): Boolean = programId != 0
}
