# LiteRT / TensorFlow Lite uses reflection for its native bindings.
-keep class org.tensorflow.lite.** { *; }
-dontwarn org.tensorflow.lite.**
# OpenCV Java bindings call into native code by name.
-keep class org.opencv.** { *; }
# kotlinx.serialization
-keepattributes *Annotation*, InnerClasses
-keepclassmembers class com.camsense.** {
    *** Companion;
    kotlinx.serialization.KSerializer serializer(...);
}
