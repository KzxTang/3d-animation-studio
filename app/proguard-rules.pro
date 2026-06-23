# ProGuard 混淆规则 - 3D Animation Studio
# 保持OpenGL ES相关类
-keep class com.threedstudio.render.** { *; }
-keep class com.threedstudio.render.core.** { *; }

# 保持模型数据类（反射使用）
-keep class com.threedstudio.modelio.** { *; }
-keep class com.threedstudio.animation.** { *; }

# 保持JNI/NDK相关
-keepclasseswithmembernames class * {
    native <methods>;
}

# Gson序列化保持
-keepattributes Signature
-keepattributes *Annotation*
-keep class com.google.gson.** { *; }
-keep class * extends com.google.gson.TypeAdapter
-keep class * implements com.google.gson.TypeAdapterFactory

# Kotlin协程保持
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}

# AndroidX保持
-keep class androidx.compose.** { *; }
-dontwarn androidx.compose.**
