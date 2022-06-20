# General (make debugging easier etc)
-dontobfuscate
-optimizations code/simplification/arithmetic,code/simplification/cast,field/*,class/merging/*
-keepattributes SourceFile,LineNumberTable

-keep class com.trifork.timandroid.**{
     *;
}
