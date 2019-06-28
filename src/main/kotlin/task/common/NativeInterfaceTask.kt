package task.common

import Configs.outputOrg
import Configs.outputProjectName
import OutputProject
import OutputProject.classSimpleName
import OutputProject.methodChannel
import common.*
import common.extensions.*
import common.model.Callback
import parser.java.JavaParser
import parser.java.JavaParserBaseListener
import task.Task
import java.io.File

/**
 * Android端接口生成
 *
 * 输入: java文件
 * 输出: 对应的method channel文件
 * 依赖: [DecompileClassTask]
 */
class AndroidInterfaceTask(private val jarDir: DIR) : Task<DIR, KOTLIN_FILE>(jarDir) {
    override fun process(): KOTLIN_FILE {
        val resultBuilder = StringBuilder("")
        // package
        resultBuilder.appendln(packageString())
        // import
        resultBuilder.appendln(importList().joinToString("\n"))
        // REF_MAP
        resultBuilder.appendln(refMapString())
        // 头部固定代码
        resultBuilder.append(headerString())

        // body
        jarDir.iterate("java") {
            if (!it.nameWithoutExtension.isObfuscated()) {
                resultBuilder.append(generateForFile(it))
            }
        }
        resultBuilder.append(
            """
                    }
                }
        """
        )

        jarDir.iterate("java") {
            if (it.readText().isView()) {
                if (it.readText().isView()) {
                    resultBuilder.appendln(registerPlatformFactory(it))
                    generatePlatformView(it)
                }
            }
        }

        // 尾部
        resultBuilder.append(
            """
        }
    }
}"""
        )

        return OutputProject.Android.kotlinFilePath.file().apply { writeText(resultBuilder.toString()) }
    }

    private fun packageString(): String {
        return "package $outputOrg.$outputProjectName"
    }

    private fun importList(): List<String> {
        return listOf(
            "import io.flutter.plugin.common.MethodChannel",
            "import io.flutter.plugin.common.PluginRegistry.Registrar"
        )
    }

    private fun refMapString(): String {
        return "val REF_MAP = mutableMapOf<Int, Any>()"
    }

    private fun headerString(): String {
        return """
class ${classSimpleName}Plugin {
    companion object {
        @JvmStatic
        fun registerWith(registrar: Registrar) {
            val channel = MethodChannel(registrar.messenger(), "$methodChannel")
            channel.setMethodCallHandler { methodCall, methodResult ->
                val args = methodCall.arguments as? Map<String, *> ?: mapOf()
                when (methodCall.method) {"""
    }

    private fun generateForFile(javaFile: JAVA_FILE): String {
        val resultBuilder = StringBuilder("")
        val methodList = mutableListOf<String>()

        javaFile
            .readText()
            .walkTree(object : JavaParserBaseListener() {
                override fun enterMethodDeclaration(ctx: JavaParser.MethodDeclarationContext?) {
                    ctx?.run {
                        if (!isPublic()
                            || name() in IGNORE_METHOD
                            || formalParams().any { it.isUnknownType() || it.type.isObfuscated() }
                            || returnType().run { isUnknownType() || isObfuscated() }
                        ) return

                        val methodBuilder = StringBuilder("")

                        val className = ancestorOf(JavaParser.ClassDeclarationContext::class)?.fullClassName() ?: ""
                        val params = formalParams().filter { it.type !in PRESERVED_CLASS }.toMutableList()
                        var methodName = name()
                        // 处理java方法重载的情况
                        if (methodName in methodList) {
                            methodName =
                                "${methodName}_${params.joinToString("") { it.type.toDartType().capitalize() }}"
                        }

                        // 解析参数
                        methodBuilder.append(
                            Temps.Kotlin.PlatformView.methodBranchHeader.placeholder(
                                className,
                                methodName,
                                formalParams()
                                    .filter { !it.isCallback() }
                                    .joinToString("") {
                                        when {
                                            it.type.jsonable() -> {
                                                "\n\t\t\t\t\tval ${it.name} = args[\"${it.name}\"] as ${it.type.capitalize()}"
                                            }
                                            it.type in PRESERVED_MODEL -> {
                                                it.convertPreservedModel()
                                            }
                                            else -> {
                                                "\n\t\t\t\t\tval ${it.name} = REF_MAP[args[\"${it.name}\"] as Int] as ${it.type}"
                                            }
                                        }
                                    }
                            )
                        )

                        // 静态方法单独处理
                        if (isStatic()) {
                            // 返回类型是jsonable
                            when {
                                returnType() == "void" -> methodBuilder.append(
                                    Temps.Kotlin.PlatformView.staticReturnVoid.placeholder(
                                        className,
                                        methodName,
                                        formalParams().joinToString {
                                            if (!it.isCallback())
                                                it.name
                                            else {
                                                Callback(className, methodName, it.type).toString()
                                            }
                                        }
                                    )
                                )
                                returnType().jsonable() -> methodBuilder.append(
                                    Temps.Kotlin.PlatformView.staticReturnJsonable.placeholder(
                                        className,
                                        methodName,
                                        formalParams().joinToString {
                                            if (!it.isCallback())
                                                it.name
                                            else {
                                                Callback(className, methodName, it.type).toString()
                                            }
                                        }
                                    )
                                )
                                returnType().isJavaRefType() -> methodBuilder.append(
                                    Temps.Kotlin.PlatformView.staticReturnRef.placeholder(
                                        className,
                                        methodName,
                                        formalParams().joinToString {
                                            if (!it.isCallback())
                                                it.name
                                            else {
                                                Callback(className, methodName, it.type).toString()
                                            }
                                        }
                                    )
                                )
                            }
                            methodBuilder.append("\n\t\t\t}")
                        } else {
                            // 返回类型是jsonable
                            when {
                                // 返回void
                                returnType() == "void" -> methodBuilder.append(
                                    Temps.Kotlin.PlatformView.refReturnVoid.placeholder(
                                        className.replace("_", "."),
                                        methodName,
                                        formalParams().joinToString {
                                            if (!it.isCallback())
                                                it.name
                                            else {
                                                Callback(className, methodName, it.type).toString()
                                            }
                                        }
                                    )
                                )
                                returnType().jsonable() -> methodBuilder.append(
                                    Temps.Kotlin.PlatformView.refReturnJsonable.placeholder(
                                        className.replace("_", "."),
                                        methodName,
                                        formalParams().joinToString {
                                            if (!it.isCallback())
                                                it.name
                                            else {
                                                Callback(className, methodName, it.type).toString()
                                            }
                                        }
                                    )
                                )
                                // 返回类型是ref
                                else -> methodBuilder.append(
                                    Temps.Kotlin.PlatformView.refReturnRef.placeholder(
                                        className.replace("_", "."),
                                        methodName,
                                        formalParams().joinToString {
                                            if (!it.isCallback())
                                                it.name
                                            else {
                                                Callback(className, methodName, it.type).toString()
                                            }
                                        }
                                    )
                                )
                            }
                            methodBuilder.append("\n\t\t\t}")
                        }
                        resultBuilder.append(methodBuilder.toString())
                    }
                }
            })

        return resultBuilder.toString()
    }

    private fun registerPlatformFactory(viewFile: File): String {
        val javaTypeInfo = viewFile.javaTypeInfo()
        return """
            registrar
                    .platformViewRegistry()
                    .registerViewFactory("${javaTypeInfo.name}", ${javaTypeInfo.simpleName}Factory(registrar))
        """
    }

    private fun generatePlatformView(javaFile: JAVA_FILE) {
        val platformViewSource =
            Temps.Kotlin.PlatformView.factory.placeholder(javaFile.nameWithoutExtension, javaFile.nameWithoutExtension)

        "${OutputProject.Android.kotlinDirPath}${javaFile.nameWithoutExtension}Factory.kt".file().run {
            writeText(platformViewSource)
        }
    }
}