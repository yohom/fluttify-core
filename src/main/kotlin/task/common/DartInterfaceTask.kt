package task.common

import Jar
import OutputProject
import common.*
import common.extensions.*
import common.model.Lambda
import common.model.Variable
import parser.java.JavaParser
import parser.java.JavaParserBaseListener
import task.Task

/**
 * 生成Java文件的Dart接口文件
 *
 * 输入: Java文件
 * 输出: 生成接口后的Dart文件
 * 依赖: [DecompileClassTask]
 */
class DartInterfaceTask(private val javaFile: JAVA_FILE) : Task<JAVA_FILE, DART_FILE>(javaFile) {

    private val lambdas: MutableList<Lambda> = mutableListOf()

    override fun process(): DART_FILE {
        val javaSource = javaFile.readText()

        val dartBuilder = StringBuilder()
        val androidViewBuilder = StringBuilder()
        val methodList = mutableListOf<String>()

        javaSource.walkTree(object : JavaParserBaseListener() {

            override fun enterClassDeclaration(ctx: JavaParser.ClassDeclarationContext?) {
                ctx?.run {
                    dartBuilder.append(
                        Temps.Dart.classDeclaration.placeholder(
                            fullName().toDartType(),
                            fullName().toDartType()
                        )
                    )
                    dartBuilder.append(Temps.Dart.methodChannel)

                    if (superClass() in listOf("View", "ViewGroup")) {
                        androidViewBuilder.append(
                            Temps.Dart.AndroidView.androidView.placeholder(
                                fullName().toDartType(),
                                fullName().toDartType(),
                                fullName().toDartType(),
                                fullName(),
                                fullName().toDartType()
                            )
                        )
                    }
                }
            }

            override fun enterMethodDeclaration(ctx: JavaParser.MethodDeclarationContext?) {
                ctx?.run {
                    // 每进入一个方法, 就清空lambda列表
                    lambdas.clear()

                    if (!isPublic()
                        || name() in IGNORE_METHOD
                        || formalParams().any { it.type.isUnknownType() || it.type.isObfuscated() }
                        || returnType().run { isUnknownType() || isObfuscated() }
                    ) return

                    val methodBuilder = StringBuilder()

                    val className = ancestorOf(JavaParser.ClassDeclarationContext::class)?.fullName() ?: ""
                    val params = formalParams().filter { it.type !in PRESERVED_CLASS }.toMutableList()
                    var methodName = name()
                    // 处理java方法重载的情况
                    if (methodName in methodList) {
                        methodName = "${methodName}_${params.joinToString("") { it.type.toDartType().capitalize() }}"
                    }

                    // 0. 方法修饰
                    methodBuilder.append("\n")
                    methodBuilder.append(if (isStatic()) "static " else "")
                    // 1. 返回类型
                    methodBuilder.append("Future<${returnType().toDartType()}>")
                    methodBuilder.append(" ")
                    // 2. 方法名
                    methodBuilder.append(methodName)
                    methodBuilder.append("(")
                    // 3. 形参
                    methodBuilder.append(formalParamsString(params))
                    methodBuilder.append(")")
                    methodBuilder.append(" async {")
                    // 4. 方法体
                    methodBuilder.append(methodBodyString(isStatic(), className, methodName, params))
                    // 5. 返回值
                    methodBuilder.append(returnString(returnType()))
                    methodBuilder.append("}")

                    dartBuilder.append(methodBuilder.toString())
                    methodList.add(name())
                }
            }

            override fun exitClassDeclaration(ctx: JavaParser.ClassDeclarationContext?) {
                ctx?.run {
                    dartBuilder.append(Temps.Dart.classEnd)
                }
            }

            override fun enterEnumDeclaration(ctx: JavaParser.EnumDeclarationContext?) {
                ctx?.run {
                    dartBuilder.append("enum ${fullName().toDartType()} {")
                }
            }

            override fun enterEnumConstant(ctx: JavaParser.EnumConstantContext?) {
                ctx?.run {
                    dartBuilder.append("${IDENTIFIER().text},")
                }
            }

            override fun exitEnumDeclaration(ctx: JavaParser.EnumDeclarationContext?) {
                ctx?.run {
                    dartBuilder.append("}")
                }
            }
        })

        if (androidViewBuilder.toString().isNotBlank()) {
            OutputProject.Dart.androidPlatformViewFilePath.file().writeText(androidViewBuilder.toString())
        }

        return "${OutputProject.Dart.androidDirPath}${javaFile.toRelativeString(Jar.Decompiled.rootDirPath.file()).substringBeforeLast(
            "."
        ).replace("$", "_")}.dart".file()
            .apply { writeText(dartBuilder.toString()) }
    }

    /**
     * 形参列表拼接字符串
     */
    private fun formalParamsString(params: List<Variable>): String {
        return params
            .filter { !it.type.isUnknownType() }
            .joinToString { variable ->
                variable
                    .type
                    .javaTypeInfo()
                    ?.run {
                        // 如果参数是回调类, 那么把类拆成lambda, 并作为参数传入
                        if (isCallback == true) {
                            val result = StringBuilder()

                            path
                                .file()
                                .readText()
                                .walkTree(object : JavaParserBaseListener() {
                                    override fun enterInterfaceDeclaration(ctx: JavaParser.InterfaceDeclarationContext?) {
                                        ctx?.run {
                                            lambdas.addAll(interface2lambdas())
                                            if (lambdas.isNotEmpty()) {
                                                result.append(lambdas.joinToString(prefix = "{", postfix = ",}"))
                                            }
                                        }
                                    }
                                })

                            result.toString()
                        }
                        // 普通Ref类
                        else {
                            "${variable.type.toDartType()} ${variable.name}"
                        }
                    } ?: "${variable.type.toDartType()} ${variable.name}" // jsonable类
            }
    }

    /**
     * 方法体拼接字符串
     */
    private fun methodBodyString(
        isStatic: Boolean,
        className: String,
        methodName: String,
        params: List<Variable>
    ): String {
        val resultBuilder = StringBuilder("")

        val removeCallbackParam = params
            .filter { !it.type.isCallback() }
            .toMutableList()
            .apply { if (!isStatic) add(Variable("int", "refId")) }

        val actualParams = removeCallbackParam.toDartMap {
            when {
                it.type.isEnum() -> "${it.name}.index"
                it.type.isList() -> "${it.name}.map((it) => it.refId).toList()"
                it.type.jsonable() -> it.name
                else -> "${it.name}.refId"
            }
        }

        resultBuilder.append(
            "final result = await _channel.invokeMethod('$className::$methodName', $actualParams);"
        )

        lambdas.forEachIndexed { index, lambda ->
            if (index == 0) {
                resultBuilder.append(
                    """MethodChannel('$className::${methodName}_Callback' + refId.toString())
                        .setMethodCallHandler((methodCall) { 
                            final args = methodCall.arguments as Map; 
                            final refId = args['refId'] as int; 
                            if (refId != this.refId) return;

                            switch (methodCall.method) {"""
                )
            }

            resultBuilder.append(
                """
            case '$className::${methodName}_Callback::${lambda.methodName}':
              if (${lambda.methodName} != null) {
                ${lambda.methodName}(${lambda.formalParams.joinToString {
                    if (it.type.jsonable()) {
                        "args['${it.name}']"
                    } else {
                        "${it.type.toDartType()}.withRefId(args['${it.name}'])"
                    }
                }});
              }
              break;"""
            )

            if (index == lambdas.lastIndex) {
                resultBuilder.append("""default: break; } });""")
            }
        }
        return resultBuilder.toString()
    }

    /**
     * 返回值拼接字符串
     */
    private fun returnString(returnType: String): String {
        val resultBuilder = StringBuilder("")

        if (returnType.jsonable()) {
            if (returnType.isList()) {
                resultBuilder.append(
                    "return (result as List).cast<${returnType.genericType().toDartType()}>();"
                )
            } else {
                resultBuilder.append("return result;")
            }
        } else if (returnType.isEnum()) {
            resultBuilder.append("return ${returnType.toDartType()}.values[result];")
        } else {
            if (returnType.isList()) {
                resultBuilder.append(
                    "return (result as List).map((it) => ${returnType.genericType().toDartType()}.withRefId(it));"
                )
            } else {
                resultBuilder.append("return ${returnType.toDartType()}.withRefId(result);")
            }
        }

        return resultBuilder.toString()
    }
}