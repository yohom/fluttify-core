package me.yohom.fluttify.task

import me.yohom.fluttify.extensions.*
import me.yohom.fluttify.model.SDK
import me.yohom.fluttify.tmpl.java.common.handler.handler_getter.HandlerGetterTmpl
import me.yohom.fluttify.tmpl.java.common.handler.handler_getter_batch.HandlerGetterBatchTmpl
import me.yohom.fluttify.tmpl.java.common.handler.handler_method.HandlerMethodTmpl
import me.yohom.fluttify.tmpl.java.common.handler.handler_method_batch.HandlerMethodBatchTmpl
import me.yohom.fluttify.tmpl.java.common.handler.handler_object_creator.HandlerObjectFactoryTmpl
import me.yohom.fluttify.tmpl.java.common.handler.handler_object_creator_batch.HandlerObjectFactoryBatchTmpl
import me.yohom.fluttify.tmpl.java.common.handler.handler_setter.HandlerSetterTmpl
import me.yohom.fluttify.tmpl.java.common.handler.handler_setter_batch.HandlerSetterBatchTmpl
import me.yohom.fluttify.tmpl.java.plugin.JavaPluginTmpl
import me.yohom.fluttify.tmpl.objc.common.handler.handler_object_creator.handler_object_creator_ref.HandlerObjectFactoryRefTmpl
import me.yohom.fluttify.tmpl.objc.common.handler.handler_object_creator.handler_object_creator_ref_batch.HandlerObjectFactoryRefBatchTmpl
import me.yohom.fluttify.tmpl.objc.common.handler.handler_object_creator.handler_object_creator_struct.HandlerObjectFactoryStructTmpl
import me.yohom.fluttify.tmpl.objc.common.handler.handler_object_creator.handler_object_creator_struct_batch.HandlerObjectFactoryStructBatchTmpl
import me.yohom.fluttify.tmpl.objc.common.handler.handler_type_cast.HandlerTypeCastTmpl
import me.yohom.fluttify.tmpl.objc.common.handler.handler_type_check.HandlerTypeCheckTmpl
import me.yohom.fluttify.tmpl.objc.plugin.ObjcPluginTmpl
import org.gradle.api.tasks.TaskAction
import me.yohom.fluttify.tmpl.java.platform_view_factory.PlatformViewFactoryTmpl as JavaPlatformViewFactory
import me.yohom.fluttify.tmpl.java.plugin.sub_handler.SubHandlerTmpl as JavaSubHandlerTmpl
import me.yohom.fluttify.tmpl.objc.platform_view_factory.PlatformViewFactoryTmpl as ObjcPlatformViewFactory
import me.yohom.fluttify.tmpl.objc.plugin.sub_handler.SubHandlerTmpl as ObjcSubHandlerTmpl

/**
 * Android端接口生成
 *
 * 输入: java文件
 * 输出: 对应的method channel文件
 */
open class AndroidJavaInterface : FluttifyTask() {

    @TaskAction
    fun process() {
        val projectDir = project.projectDir
        val projectName = ext.projectName
        val packageSubDir = ext.org.replace(".", "/")

        val jrFile = "$projectDir/jr/$projectName.android.json".file()
        val packageDir = "$projectDir/output-project/$projectName/android/src/main/java/$packageSubDir/$projectName"
        val pluginOutputFile = "$packageDir/${ext.projectName.underscore2Camel()}Plugin.java"
        val subHandlerOutputDir = "$packageDir/sub_handler"
        val subHandlerOutputFile = "$subHandlerOutputDir/SubHandler#__number__#.java"
        val subHandlerCustomOutputFile = "$subHandlerOutputDir/SubHandlerCustom.java"

        val sdk = jrFile.readText().fromJson<SDK>()

        // TODO 跳过SubHandlerCustom
        // 生成前先删除之前的文件
        if (sdk.directLibs.isNotEmpty()) packageDir.file().deleteRecursively()

        // 生成主plugin文件
        sdk.directLibs.forEach { lib ->
            val getters = lib.types
                .filterType()
                .flatMap { it.fields }
                .filterGetters()
                .map { HandlerGetterTmpl(it) }

            val gettersBatch = lib.types
                .filterType()
                .flatMap { it.fields }
                .filterGetters()
                .map { HandlerGetterBatchTmpl(it) }

            val setters = lib.types
                .filterType()
                .flatMap { it.fields }
                .filterSetters()
                .map { HandlerSetterTmpl(it) }

            val settersBatch = lib.types
                .filterType()
                .flatMap { it.fields }
                .filterSetters(true)
                .map { HandlerSetterBatchTmpl(it) }

            val methods = lib.types
                .filterType()
                .flatMap { it.methods }
                .filterMethod()
                .map { HandlerMethodTmpl(it) }

            val methodsBatch = lib.types
                .filterType()
                .flatMap { it.methods }
                .filterMethod(batch = true)
                .map { HandlerMethodBatchTmpl(it) }

            val objectCreators = lib.types
                .filterConstructable()
                .flatMap { HandlerObjectFactoryTmpl(it) }

            val objectCreatorsBatch = lib.types
                .filterConstructable()
                .flatMap { HandlerObjectFactoryBatchTmpl(it) }

            getters
                .union(gettersBatch)
                .union(setters)
                .union(settersBatch)
                .union(methods)
                .union(methodsBatch)
                .union(objectCreators)
                .union(objectCreatorsBatch)
                .toObservable()
                .buffer(200)
                .blockingIterable()
                .mapIndexed { index, subHandler -> JavaSubHandlerTmpl(index, subHandler) }
                .forEachIndexed { index, content ->
                    subHandlerOutputFile.replace("#__number__#", index.toString()).file().writeText(content)
                }

            val subHandlerCustomTmpl = getResource("/tmpl/java/sub_handler_custom.java.tmpl").readText()
            subHandlerCustomTmpl
                .replace("#__package_name__#", "${ext.org}.${ext.projectName}")
                .replace("#__plugin_name__#", ext.projectName.underscore2Camel(true))
                .run { subHandlerCustomOutputFile.file().writeText(this) }

            JavaPluginTmpl(lib, subHandlerOutputDir)
                .run {
                    pluginOutputFile.file().writeText(this)
                }
        }

        // 生成PlatformViewFactory文件
        sdk.directLibs
            .forEach { lib ->
                lib.types
                    .filter { it.isView() && !it.isObfuscated() }
                    .forEach {
                        val factoryOutputFile = "$packageDir/${it.name.simpleName()}Factory.java".file()

                        JavaPlatformViewFactory(it)
                            .run {
                                factoryOutputFile.writeText(this)
                            }
                    }
            }
    }
}

/**
 * iOS端接口生成
 *
 * 输入: framework文件夹
 * 输出: 对应的method channel文件
 */
open class IOSObjcInterface : FluttifyTask() {

    @TaskAction
    fun process() {
        val jrFile = "${project.projectDir}/jr/${ext.projectName}.ios.json".file()
        val projectRootDir = "${project.projectDir}/output-project/${ext.projectName}/ios/Classes/"
        val pluginHFile = "$projectRootDir/${ext.projectName.underscore2Camel()}Plugin.h"
        val pluginMFile = "$projectRootDir/${ext.projectName.underscore2Camel()}Plugin.m"
        val subHandlerOutputDir = "$projectRootDir/SubHandler/"
        val subHandlerOutputHFile = "$subHandlerOutputDir/SubHandler#__number__#.h"
        val subHandlerOutputMFile = "$subHandlerOutputDir/SubHandler#__number__#.m"

        val subHandlerCustomOutputHFile = "$subHandlerOutputDir/SubHandlerCustom.h"
        val subHandlerCustomOutputMFile = "$subHandlerOutputDir/SubHandlerCustom.m"

        val sdk = jrFile.readText().fromJson<SDK>()

        // TODO 跳过SubHandlerCustom
        // 生成前先删除之前的文件
        if (sdk.directLibs.isNotEmpty()) projectRootDir.file().deleteRecursively()

        sdk.directLibs.forEach { lib ->
            val getters = lib.types
                .filterType()
                .flatMap { it.fields }
                .filterGetters()
                .map { me.yohom.fluttify.tmpl.objc.common.handler.handler_getter.HandlerGetterTmpl(it) }

            val gettersBatch = lib.types
                .filterType()
                .flatMap { it.fields }
                .filterGetters()
                .map { me.yohom.fluttify.tmpl.objc.common.handler.handler_getter_batch.HandlerGetterBatchTmpl(it) }

            val setters = lib.types
                .filterType()
                .flatMap { it.fields }
                .filterSetters()
                .map { me.yohom.fluttify.tmpl.objc.common.handler.handler_setter.HandlerSetterTmpl(it) }

            val settersBatch = lib.types
                .filterType()
                .flatMap { it.fields }
                .filterSetters(true)
                .map { me.yohom.fluttify.tmpl.objc.common.handler.handler_setter_batch.HandlerSetterBatchTmpl(it) }

            val functions = lib.types
                // 暂时先不处理含有lambda的函数
                .filter { it.isKnownFunction() && it.formalParams.all { !it.variable.isLambda() } }
                .map { it.asMethod() }
                .map { me.yohom.fluttify.tmpl.objc.common.handler.handler_method.HandlerMethodTmpl(it) }

            val methods = lib.types
                .filterType()
                .flatMap { it.methods }
                .filterMethod()
                .map { me.yohom.fluttify.tmpl.objc.common.handler.handler_method.HandlerMethodTmpl(it) }

            val methodsBatch = lib.types
                .filterType()
                .flatMap { it.methods }
                .filterMethod(batch = true)
                .map { me.yohom.fluttify.tmpl.objc.common.handler.handler_method_batch.HandlerMethodBatchTmpl(it) }

            val typeCasts = lib.types
                .filterType()
                .asSequence()
                .filterNot { it.isLambda() }
                .filterNot { it.isFunction() }
                .filterNot { it.isAlias() }
                .distinctBy { it.name }
                .filter { !it.isInterface() && !it.isEnum() && !it.isStruct() }
                .map { HandlerTypeCastTmpl(it) }
                .toList()

            val typeChecks = lib.types
                .filterType()
                .asSequence()
                .filterNot { it.isLambda() }
                .filterNot { it.isFunction() }
                .filterNot { it.isAlias() }
                .distinctBy { it.name }
                .filter { !it.isInterface() && !it.isEnum() && !it.isStruct() }
                .map { HandlerTypeCheckTmpl(it) }
                .toList()

            val objectCreators = lib.types
                .filterConstructable()
                .distinctBy { it.name }
                .map {
                    if (it.isStruct()) {
                        HandlerObjectFactoryStructTmpl(it)
                    } else {
                        HandlerObjectFactoryRefTmpl(it)
                    }
                }

            val objectCreatorsBatch = lib.types
                .filterConstructable()
                .distinctBy { it.name }
                .map {
                    if (it.isStruct()) {
                        HandlerObjectFactoryStructBatchTmpl(it)
                    } else {
                        HandlerObjectFactoryRefBatchTmpl(it)
                    }
                }

            methods
                .union(methodsBatch)
                .union(getters)
                .union(gettersBatch)
                .union(setters)
                .union(settersBatch)
                .union(typeChecks)
                .union(typeCasts)
                .union(objectCreators)
                .union(objectCreatorsBatch)
                .union(functions)
                .toObservable()
                .buffer(200)
                .blockingIterable()
                .mapIndexed { index, subHandler -> ObjcSubHandlerTmpl(index, subHandler) }
                .forEachIndexed { index, content ->
                    subHandlerOutputHFile.replace("#__number__#", index.toString()).file().writeText(content[0])
                    subHandlerOutputMFile.replace("#__number__#", index.toString()).file().writeText(content[1])
                }

            val subHandlerCustomHTmpl = getResource("/tmpl/objc/sub_handler_custom.h.tmpl").readText()
            val subHandlerCustomMTmpl = getResource("/tmpl/objc/sub_handler_custom.m.tmpl").readText()
            subHandlerCustomHTmpl
                .replace("#__plugin_name__#", ext.projectName.underscore2Camel(true))
                .run { subHandlerCustomOutputHFile.file().writeText(this) }
            subHandlerCustomMTmpl
                .replace("#__plugin_name__#", ext.projectName.underscore2Camel(true))
                .run { subHandlerCustomOutputMFile.file().writeText(this) }

            // 生成主plugin文件
            ObjcPluginTmpl(sdk.directLibs, subHandlerOutputDir)
                .run {
                    pluginHFile.file().writeText(this[0])
                    pluginMFile.file().writeText(this[1])
                }
        }

        // 生成PlatformViewFactory文件
        sdk.directLibs
            .forEach { lib ->
                lib.types
                    .filter { it.isView() && !it.isObfuscated() }
                    .forEach {
                        val factoryHFile = "$projectRootDir/${it.name.simpleName()}Factory.h".file()
                        val factoryMFile = "$projectRootDir/${it.name.simpleName()}Factory.m".file()

                        ObjcPlatformViewFactory(it, lib)
                            .run {
                                factoryHFile.writeText(this[0])
                                factoryMFile.writeText(this[1])
                            }
                    }
            }
    }
}