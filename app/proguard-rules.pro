# Add project specific ProGuard rules here.
-keepattributes Signature, InnerClasses, EnclosingMethod
-keepattributes *Annotation*
-keepattributes RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations

# Room
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.paging.**

# Retrofit
-dontwarn retrofit2.**
-keep class retrofit2.** { *; }
-keepattributes Exceptions

# Gson
-keepattributes SerializedName
-keep class com.aa.ledger.data.remote.** { *; }

# Retrofit 接口（显式保留，含泛型方法签名，否则反序列化报 Class cannot be cast to ParameterizedType）
-keep interface com.aa.ledger.data.remote.CloudApi { *; }

# Kotlin 协程 Continuation（Retrofit suspend 依赖它提取返回类型，不能被混淆）
-keep class kotlin.coroutines.Continuation { *; }

# Gson TypeToken 泛型保留
-keep class com.google.gson.reflect.TypeToken { *; }
-keep class * extends com.google.gson.reflect.TypeToken

# ML Kit
-dontwarn com.google.mlkit.**

# 备份 JSON 序列化（Gson 反射，避免 R8 混淆字段名导致解析失败）
-keep class com.aa.ledger.data.backup.** { *; }
-keep class com.aa.ledger.data.local.entity.** { *; }
-keep class com.aa.ledger.data.repository.AuthRepository$* { *; }
