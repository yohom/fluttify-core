package preprocess

import Configs.frameworkPath
import Configs.mainJavaClass
import Configs.mainObjcClass
import Configs.outputOrg
import Configs.outputProjectName
import common.underscore2Camel

object Analyzer {
    /**
     * 主java类的路径
     */
    var mainJavaClassPath: String? = null
    /**
     * 主java类的包名
     */
    var javaPackage: String? = null
    /**
     * 主java类的类名
     */
    var javaClassSimpleName: String? = null
    /**
     * 主objc类的路径
     */
    var mainObjcClassPath: String? = null
    /**
     * method channel的名字
     */
    var methodChannelName: String? = null
    /**
     * 当前项目路径
     */
    val projectPath: String = System.getProperty("user.dir")
    /**
     * 生成工程的主类名
     */
    var pluginClassSimpleName: String? = null
    /**
     * 生成工程的Dart文件路径
     */
    var outputPluginDartPath: String? = null
    /**
     * 生成工程的Android端Dart文件路径
     */
    var outputPluginAndroidDartPath: String? = null
    /**
     * 生成工程的Android端Kotlin文件路径
     */
    var outputPluginAndroidKotlinPath: String? = null
    /**
     * 生成工程的iOS端Dart文件路径
     */
    var outputPluginIOSDartPath: String? = null
    /**
     * 生成工程的iOS端Swift文件路径
     */
    var outputPluginIOSSwiftPath: String? = null

    fun analyze() {
        mainJavaClassPath = "$projectPath/resource/android/decompiled/${mainJavaClass.replace(".", "/")}.java"
        javaPackage = mainJavaClass.substringBeforeLast(".")
        javaClassSimpleName = mainJavaClass.substringAfterLast(".")
        mainObjcClassPath = "$frameworkPath/Headers/$mainObjcClass.h"
        methodChannelName = "$javaPackage/$javaClassSimpleName"
        pluginClassSimpleName = outputProjectName.underscore2Camel()
        outputPluginDartPath = "$projectPath/resource/outputPluginProject/$outputProjectName/lib/$outputProjectName.dart"
        outputPluginAndroidDartPath = "$projectPath/resource/outputPluginProject/$outputProjectName/lib/$outputProjectName.android.dart"
        outputPluginAndroidKotlinPath = "$projectPath/resource/outputPluginProject/$outputProjectName/android/src/main/kotlin/${outputOrg.replace(".", "/")}/$outputProjectName/${pluginClassSimpleName}Plugin.kt"
        outputPluginIOSDartPath = "$projectPath/resource/outputPluginProject/$outputProjectName/lib/$outputProjectName.ios.dart"
        outputPluginIOSSwiftPath = "$projectPath/resource/outputPluginProject/$outputProjectName/ios/Classes/Swift${pluginClassSimpleName}Plugin.swift"
    }
}