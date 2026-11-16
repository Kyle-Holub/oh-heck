# Shrink unused code, but don't rename anything: crash logs stay readable.
-dontobfuscate

# libGDX finds parts of its Android backend by reflection, and reaches its
# native code by name: keep it whole. It's small next to what gets removed.
-keep class com.badlogic.gdx.** { *; }
-keepclasseswithmembernames class * { native <methods>; }
-dontwarn com.badlogic.gdx.**

# The game itself (tiny); kept so nothing it looks up by name goes missing.
-keep class ohheck.** { *; }
-keep class rules.** { *; }
