package com.threedstudio.render.core

import android.opengl.GLES30
import android.util.Log

class Shader {

    private var programId: Int = 0
    private val uniformLocations = mutableMapOf<String, Int>()

    companion object {
        private const val TAG = "Shader"
        private const val DEFAULT_VERTEX_SRC = """
            #version 300 es
            precision highp float;
            uniform mat4 u_mvpMatrix;
            uniform mat4 u_modelMatrix;
            uniform mat4 u_normalMatrix;
            in vec3 a_position;
            in vec3 a_normal;
            in vec2 a_texCoord;
            in vec4 a_jointIndices;
            in vec4 a_jointWeights;
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
        
        private const val DEFAULT_FRAGMENT_SRC = """
            #version 300 es
            precision highp float;
            struct Light {
                int type;
                vec3 position;
                vec3 direction;
                vec3 color;
                float intensity;
                float angle;
                float range;
            };
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
            
            vec3 calcDirectionalLight(Light light, vec3 normal, vec3 viewDir, vec3 diffuse) {
                vec3 lightDir = normalize(-light.direction);
                float diff = max(dot(normal, lightDir), 0.0);
                vec3 reflectDir = reflect(-lightDir, normal);
                float spec = pow(max(dot(viewDir, reflectDir), 0.0), u_materialShininess);
                return light.color * light.intensity * (diff * diffuse + spec * u_materialSpecular);
            }
            
            vec3 calcPointLight(Light light, vec3 normal, vec3 viewDir, vec3 diffuse) {
                vec3 lightDir = normalize(light.position - v_worldPosition);
                float distance = length(light.position - v_worldPosition);
                float attenuation = 1.0 / (1.0 + 0.09 * distance + 0.032 * distance * distance);
                float diff = max(dot(normal, lightDir), 0.0);
                vec3 reflectDir = reflect(-lightDir, normal);
                float spec = pow(max(dot(viewDir, reflectDir), 0.0), u_materialShininess);
                return light.color * light.intensity * attenuation * (diff * diffuse + spec * u_materialSpecular);
            }
            
            vec3 calcSpotLight(Light light, vec3 normal, vec3 viewDir, vec3 diffuse) {
                vec3 lightDir = normalize(light.position - v_worldPosition);
                float theta = dot(lightDir, normalize(-light.direction));
                float epsilon = 0.91 - 0.82;
                float intensity = clamp((theta - 0.82) / epsilon, 0.0, 1.0);
                float distance = length(light.position - v_worldPosition);
                float attenuation = 1.0 / (1.0 + 0.09 * distance + 0.032 * distance * distance);
                float diff = max(dot(normal, lightDir), 0.0);
                vec3 reflectDir = reflect(-lightDir, normal);
                float spec = pow(max(dot(viewDir, reflectDir), 0.0), u_materialShininess);
                return light.color * light.intensity * attenuation * intensity * (diff * diffuse + spec * u_materialSpecular);
            }
            
            void main() {
                vec3 normal = normalize(v_worldNormal);
                vec3 viewDir = normalize(u_cameraPosition - v_worldPosition);
                vec3 diffuse = u_hasDiffuseTexture ? texture(u_diffuseTexture, v_texCoord).rgb * u_materialDiffuse.rgb : u_materialDiffuse.rgb;
                vec3 ambient = u_ambientColor * u_ambientIntensity * diffuse;
                vec3 totalLight = ambient;
                for (int i = 0; i < u_lightCount; i++) {
                    if (u_lights[i].type == 0) {
                        totalLight += calcDirectionalLight(u_lights[i], normal, viewDir, diffuse);
                    } else if (u_lights[i].type == 1) {
                        totalLight += calcPointLight(u_lights[i], normal, viewDir, diffuse);
                    } else if (u_lights[i].type == 2) {
                        totalLight += calcSpotLight(u_lights[i], normal, viewDir, diffuse);
                    }
                }
                fragColor = vec4(totalLight, u_materialDiffuse.a);
            }
        """.trimIndent()
    }

    fun createDefaultShader(): Boolean {
        return createShader(DEFAULT_VERTEX_SRC, DEFAULT_FRAGMENT_SRC)
    }

    fun createShader(vertexSource: String, fragmentSource: String): Boolean {
        val vertexShader = compileShader(GLES30.GL_VERTEX_SHADER, vertexSource)
        if (vertexShader == 0) return false
        
        val fragmentShader = compileShader(GLES30.GL_FRAGMENT_SHADER, fragmentSource)
        if (fragmentShader == 0) {
            GLES30.glDeleteShader(vertexShader)
            return false
        }
        
        programId = GLES30.glCreateProgram()
        if (programId == 0) {
            GLES30.glDeleteShader(vertexShader)
            GLES30.glDeleteShader(fragmentShader)
            return false
        }
        
        GLES30.glAttachShader(programId, vertexShader)
        GLES30.glAttachShader(programId, fragmentShader)
        GLES30.glLinkProgram(programId)
        
        val linkStatus = IntArray(1)
        GLES30.glGetProgramiv(programId, GLES30.GL_LINK_STATUS, linkStatus, 0)
        if (linkStatus[0] == 0) {
            val log = GLES30.glGetProgramInfoLog(programId)
            Log.e(TAG, "Program link error: $log")
            GLES30.glDeleteProgram(programId)
            programId = 0
            return false
        }
        
        GLES30.glDeleteShader(vertexShader)
        GLES30.glDeleteShader(fragmentShader)
        return true
    }

    private fun compileShader(type: Int, source: String): Int {
        val shaderId = GLES30.glCreateShader(type)
        if (shaderId == 0) return 0
        
        GLES30.glShaderSource(shaderId, source)
        GLES30.glCompileShader(shaderId)
        
        val compileStatus = IntArray(1)
        GLES30.glGetShaderiv(shaderId, GLES30.GL_COMPILE_STATUS, compileStatus, 0)
        if (compileStatus[0] == 0) {
            val log = GLES30.glGetShaderInfoLog(shaderId)
            Log.e(TAG, "Shader compile error: $log")
            GLES30.glDeleteShader(shaderId)
            return 0
        }
        return shaderId
    }

    fun use() {
        GLES30.glUseProgram(programId)
    }

    fun setUniform(name: String, value: Float) {
        val location = getUniformLocation(name)
        GLES30.glUniform1f(location, value)
    }

    fun setUniform(name: String, value: Int) {
        val location = getUniformLocation(name)
        GLES30.glUniform1i(location, value)
    }

    fun setUniform(name: String, value: Boolean) {
        setUniform(name, if (value) 1 else 0)
    }

    fun setUniform3f(name: String, x: Float, y: Float, z: Float) {
        val location = getUniformLocation(name)
        GLES30.glUniform3f(location, x, y, z)
    }

    fun setUniform4f(name: String, x: Float, y: Float, z: Float, w: Float) {
        val location = getUniformLocation(name)
        GLES30.glUniform4f(location, x, y, z, w)
    }

    fun setUniformMat4(name: String, matrix: Mat4) {
        val location = getUniformLocation(name)
        GLES30.glUniformMatrix4fv(location, 1, false, matrix.toFloatArray(), 0)
    }

    fun getAttributeLocation(name: String): Int {
        return GLES30.glGetAttribLocation(programId, name)
    }

    private fun getUniformLocation(name: String): Int {
        return uniformLocations.getOrPut(name) {
            GLES30.glGetUniformLocation(programId, name)
        }
    }

    fun delete() {
        if (programId != 0) {
            GLES30.glDeleteProgram(programId)
            programId = 0
        }
        uniformLocations.clear()
    }

    fun isReady(): Boolean = programId != 0
}
