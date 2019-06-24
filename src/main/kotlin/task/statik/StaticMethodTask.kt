package task.statik

import Configs.mainObjcClass
import Configs.outputOrg
import Configs.outputProjectName
import Jar
import OutputProject
import common.*
import common.extensions.*
import parser.java.JavaParser
import parser.java.JavaParserBaseListener
import parser.objc.ObjectiveCParser
import parser.objc.ObjectiveCParserBaseListener
import task.Task
import task.common.UnzipJarTask
import java.io.File

/**
 * 生成Android端Dart静态方法的MethodChannel
 *
 * 输入: 主Java类文件
 * 输出: 对应Dart的MethodChannel文件
 * 依赖: [UnzipJarTask]
 */
class AndroidDartStaticMethodTask(private val mainClassFile: JAVA_FILE) : Task<JAVA_FILE, DART_FILE>(mainClassFile) {
    override fun process(): DART_FILE {
        val javaSource = mainClassFile.readText()
        val dartResultBuilder = StringBuilder()

        File(OutputProject.Dart.dartAndroidDirPath).run { if (!exists()) mkdirs() }

        javaSource.walkTree(object : JavaParserBaseListener() {
            override fun enterCompilationUnit(ctx: JavaParser.CompilationUnitContext?) {
                dartResultBuilder.append(Temps.Dart.packageImport)
            }

            override fun enterClassDeclaration(ctx: JavaParser.ClassDeclarationContext?) {
                dartResultBuilder.append(Temps.Dart.classDeclaration.placeholder("${OutputProject.classSimpleName}Android"))
                dartResultBuilder.append(Temps.Dart.methodChannel.placeholder(OutputProject.methodChannel))
            }

            override fun enterMethodDeclaration(method: JavaParser.MethodDeclarationContext?) {
                // 跳过实例,私有,废弃方法
                if (method.run { isInstanceMethod() || isPrivate() || isDeprecated() }) return
                // 跳过含有`非model参数`和`非model返回值`的方法
                if (!method.formalParams().all { it.type.isJavaModelType() } || method?.returnType()?.isJavaModelType() != true) return

                if (method.returnType()?.jsonable() == true) {
                    dartResultBuilder.append(
                        Temps.Dart.invokeMethod.placeholder(
                            method.returnType().toDartType(),
                            method.name(),
                            method.formalParams().joinToString { "${it.type.toDartType()} ${it.name}" },
                            method.name(),
                            if (method.formalParams().isEmpty()) "" else ", ",
                            method.formalParams().toDartMap()
                        )
                    )
                } else if (method.returnType()?.isJavaModelType() == true) {
                    dartResultBuilder.append(
                        Temps.Dart.invokeModelMethod.placeholder(
                            method.returnType().toDartType(),
                            method.name(),
                            method.formalParams().joinToString { "${it.type.toDartType()} ${it.name}" },
                            method.name(),
                            if (method.formalParams().isEmpty()) "" else ", ",
                            method.formalParams().toDartMap(),
                            method.returnType().toDartType()
                        )
                    )
                }
            }

            override fun exitClassDeclaration(ctx: JavaParser.ClassDeclarationContext?) {
                dartResultBuilder.append(Temps.Dart.classEnd)
            }
        })
        return File("${OutputProject.Dart.dartAndroidDirPath}/${mainClassFile.nameWithoutExtension.camel2Underscore()}.dart")
            .apply {
                if (!exists()) createNewFile()
                writeText(dartResultBuilder.toString())
            }
    }
}

/**
 * 生成Android端Kotlin静态方法的MethodChannel
 *
 * 输入: 主Java类文件
 * 输出: 对应Kotlin的MethodChannel文件
 * 依赖: [UnzipJarTask]
 */
class AndroidKotlinStaticMethodTask(private val mainClassFile: JAVA_FILE) :
    Task<JAVA_FILE, KOTLIN_FILE>(mainClassFile) {
    override fun process(): KOTLIN_FILE {
        val javaSource = mainClassFile.readText()
        val kotlinResultBuilder = StringBuilder()

        File(OutputProject.Android.kotlinFilePath).run { if (!exists()) mkdirs() }

        javaSource.walkTree(object : JavaParserBaseListener() {
            override fun enterCompilationUnit(ctx: JavaParser.CompilationUnitContext?) {
                kotlinResultBuilder.append(Temps.Kotlin.packageImport.placeholder("$outputOrg.$outputProjectName"))
            }

            override fun enterClassDeclaration(ctx: JavaParser.ClassDeclarationContext?) {
                kotlinResultBuilder.append(Temps.Kotlin.classDeclaration.placeholder(OutputProject.classSimpleName))
                kotlinResultBuilder.append(
                    Temps.Kotlin.companionObject.placeholder(
                        OutputProject.methodChannel,
                        OutputProject.classSimpleName
                    )
                )
                kotlinResultBuilder.append(Temps.Kotlin.onMethodCall)
            }

            override fun enterMethodDeclaration(method: JavaParser.MethodDeclarationContext?) {
                // 跳过实例, 私有, 废弃方法
                if (method.run { isInstanceMethod() || isPrivate() || isDeprecated() }) return
                // 跳过含有`非model参数`和`非model返回值`的方法
                if (!method.formalParams().all { it.type.isJavaModelType() } || method?.returnType()?.isJavaModelType() != true) return

                kotlinResultBuilder.append(
                    Temps.Kotlin.methodBranch.placeholder(
                        method.name(),
                        method.formalParams().joinToString("") {
                            when {
                                it.type.jsonable() -> {
                                    "\n\t\t\t\tval ${it.name} = args[\"${it.name}\"] as ${it.type}"
                                }
                                Jar.Decompiled.classes[it.type]?.isModel == true -> {
                                    "\n\t\t\t\tval ${it.name} = mapper.readValue(args[\"${it.name}\"] as String, ${it.type}::class.java)"
                                }
                                else -> ""
                            }
                        },
                        Jar.Decompiled.mainClassSimpleName,
                        method.name(),
                        method.formalParams().joinToString { it.name },
                        if (method.returnType().jsonable()) "result" else "mapper.convertValue(result, Map::class.java)"
                    )
                )
            }

            override fun exitClassDeclaration(ctx: JavaParser.ClassDeclarationContext?) {
                kotlinResultBuilder.append(Temps.Kotlin.whenElse)
                kotlinResultBuilder.append(Temps.Kotlin.classEnd)
            }
        })
        return File(OutputProject.Android.kotlinFilePath)
            .apply {
                if (!exists()) createNewFile()
                writeText(kotlinResultBuilder.toString())
            }
    }
}

/**
 * 生成iOS端Dart静态方法的MethodChannel
 *
 * 输入: 主Objc类文件
 * 输出: 对应Dart的MethodChannel文件
 * 依赖: []
 */
class IOSDartStaticMethodTask(private val mainClassFile: OBJC_FILE) : Task<OBJC_FILE, DART_FILE>(mainClassFile) {
    override fun process(): DART_FILE {
        val objcSource = mainClassFile.readText()
        val dartResultBuilder = StringBuilder()

        File(OutputProject.Dart.dartIOSDirPath).run { if (!exists()) mkdirs() }

        objcSource.walkTree(object : ObjectiveCParserBaseListener() {
            override fun enterTranslationUnit(ctx: ObjectiveCParser.TranslationUnitContext?) {
                dartResultBuilder.append(Temps.Dart.packageImport)
            }

            override fun enterClassInterface(ctx: ObjectiveCParser.ClassInterfaceContext?) {
                dartResultBuilder.append(Temps.Dart.classDeclaration.placeholder("${OutputProject.classSimpleName}IOS"))
                dartResultBuilder.append(Temps.Dart.methodChannel.placeholder(OutputProject.methodChannel))
            }

            override fun enterClassMethodDeclaration(method: ObjectiveCParser.ClassMethodDeclarationContext?) {
                val methodDeclaration = method?.methodDeclaration() ?: return
                // 如果参数中有无法直接json序列化的, 就跳过
                if (methodDeclaration.formalParams().any { !it.type.jsonable() }) return
                // 如果返回类型无法直接json序列化的, 就跳过
                if (!methodDeclaration.returnType().jsonable()) return

                dartResultBuilder.append(
                    Temps.Dart.invokeMethod.placeholder(
                        methodDeclaration.returnType(),
                        methodDeclaration.name(),
                        methodDeclaration.formalParams().joinToString { "${it.type} ${it.name}" },
                        methodDeclaration.name(),
                        if (methodDeclaration.formalParams().isEmpty()) "" else ", ",
                        methodDeclaration.formalParams().toDartMap()
                    )
                )
            }

            override fun exitClassInterface(ctx: ObjectiveCParser.ClassInterfaceContext?) {
                dartResultBuilder.append(Temps.Dart.classEnd)
            }
        })
        return File("${OutputProject.Dart.dartIOSDirPath}/${mainClassFile.nameWithoutExtension.camel2Underscore()}.dart")
            .apply {
                if (!exists()) createNewFile()
                writeText(dartResultBuilder.toString())
            }
    }
}

/**
 * 生成iOS端Swift静态方法的MethodChannel
 *
 * 输入: 主Objc类文件
 * 输出: 对应Swift的MethodChannel文件
 * 依赖: []
 */
class IOSSwiftStaticMethodTask(private val mainClassFile: OBJC_FILE) : Task<OBJC_FILE, SWIFT_FILE>(mainClassFile) {
    override fun process(): KOTLIN_FILE {
        val objcSource = mainClassFile.readText()
        val swiftResultBuilder = StringBuilder()

        objcSource.walkTree(object : ObjectiveCParserBaseListener() {
            override fun enterTranslationUnit(ctx: ObjectiveCParser.TranslationUnitContext?) {
                swiftResultBuilder.append(Temps.Swift.packageImport)
            }

            override fun enterClassInterface(ctx: ObjectiveCParser.ClassInterfaceContext?) {
                swiftResultBuilder.append(Temps.Swift.classDeclaration.placeholder(OutputProject.classSimpleName))
                swiftResultBuilder.append(
                    Temps.Swift.register.placeholder(
                        OutputProject.methodChannel,
                        OutputProject.classSimpleName
                    )
                )
                swiftResultBuilder.append(Temps.Swift.onMethodCall)
            }

            override fun enterClassMethodDeclaration(method: ObjectiveCParser.ClassMethodDeclarationContext?) {
                val methodDeclaration = method?.methodDeclaration() ?: return
                // 如果参数中有无法直接json序列化的, 就跳过
                if (methodDeclaration.formalParams().any { !it.type.jsonable() }) return
                // 如果返回类型无法直接json序列化的, 就跳过
                if (!methodDeclaration.returnType().jsonable()) return

                swiftResultBuilder.append(Temps.Swift.invokeResult.placeholder(
                    methodDeclaration.name(),
                    mainObjcClass,
                    methodDeclaration.name(),
                    methodDeclaration.formalParams().joinToString { "args[\"${it.name}\"] as! ${it.type}" }
                ))
            }

            override fun exitClassInterface(ctx: ObjectiveCParser.ClassInterfaceContext?) {
                swiftResultBuilder.append(Temps.Swift.switchDefault)
                swiftResultBuilder.append(Temps.Swift.classEnd)
            }
        })
        return File(OutputProject.iOS.swiftFilePath)
            .apply {
                if (!exists()) createNewFile()
                writeText(swiftResultBuilder.toString())
            }
    }
}