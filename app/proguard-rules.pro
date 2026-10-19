# Room, Compose, DataStore and coroutines ship their own consumer rules.
# The game engines use no reflection, so nothing extra is required here.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}
-keepattributes RuntimeVisibleAnnotations,AnnotationDefault,InnerClasses,EnclosingMethod
