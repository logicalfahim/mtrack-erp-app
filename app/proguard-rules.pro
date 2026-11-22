# Keep Retrofit and Gson data models
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}
-keep class com.mtrack.installer.data.model.** { *; }
