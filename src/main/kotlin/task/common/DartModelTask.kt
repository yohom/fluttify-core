package task.common

import OutputProject
import common.*
import common.extensions.*
import common.translator.toDart
import org.antlr.v4.runtime.CharStreams
import org.antlr.v4.runtime.CommonTokenStream
import parser.java.JavaLexer
import parser.java.JavaParser
import parser.java.JavaParser.*
import parser.java.JavaParserBaseListener
import task.Task
import java.io.File

/**
 * Android模型类转换成对应的Dart模型类
 *
 * 输入: Java模型类文件
 * 输出: Dart模型类文件
 * 依赖: [AndroidRecognizeModelTask]
 */
class AndroidDartModelTask(private val javaModelFile: File) : Task<File, File>(javaModelFile) {
    private val results = mutableListOf<StringBuilder>()
    private var currentDepth = -1 // 嵌套类深度

    override fun process(): File {
        val javaContent = javaModelFile.readText()
        val dartModelSource = translate(javaContent)

        File(OutputProject.Dart.androidModelDirPath).run { if (!exists()) mkdirs() }

        return "${OutputProject.Dart.androidModelDirPath}/${javaModelFile.nameWithoutExtension.replace("$", "_").camel2Underscore()}.dart".file()
            .apply { writeText(dartModelSource) }
    }

    private fun translate(source: JAVA_SOURCE): DART_SOURCE {
        val lexer = JavaLexer(CharStreams.fromString(source))
        val parser = JavaParser(CommonTokenStream(lexer))
        val tree = parser.compilationUnit()

        gWalker.walk(object : JavaParserBaseListener() {
            var skip = false

            //region 普通类
            // 生成类名
            override fun enterClassDeclaration(ctx: ClassDeclarationContext?) {
                results.add(StringBuilder())
                ctx?.run { results[++currentDepth].append(Temps.Dart.classDeclaration.placeholder(ctx.IDENTIFIER().text)) }
            }

            // 生成Field类型名
            override fun enterFieldDeclaration(field: FieldDeclarationContext?) {
                field?.run {
                    if (!type().isJavaModelType()) {
                        skip = true
                        return
                    }

                    results[currentDepth].append("  ${if (isStatic()) "static " else ""}${type().toDartType()}")
                }
            }

            // 生成Field变量名和等于号(如果需要的话)
            override fun enterVariableDeclarator(ctx: VariableDeclaratorContext?) {
                ctx?.run {
                    if (skip) return

                    if (ctx.isChildOf(FieldDeclarationContext::class)) {
                        if (variableDeclaratorId().text in IGNORE_FIELD) {
                            skip = true
                            return
                        }

                        results[currentDepth].append(" ${variableDeclaratorId().text}")

                        // 如果是静态变量且值是字面量的话生成值
                        ctx.ancestorOf(FieldDeclarationContext::class)?.run {
                            if (isStatic() && variableInitializer()?.text?.contains(".") == false) {
                                results[currentDepth].append(" = ${variableInitializer().text}")
                            }
                        }
                    }
                }
            }

            // 生成Field的分号
            override fun exitFieldDeclaration(ctx: FieldDeclarationContext?) {
                ctx?.run {
                    if (skip) {
                        skip = false
                        return
                    }
                    results[currentDepth].append(";\n")
                }
            }

            // 生成方法以及方法体
            override fun enterMethodDeclaration(method: MethodDeclarationContext?) {
                method?.run {
                    if (name() in IGNORE_METHOD
                        || isStatic()
                        || isObfuscated()
                        || method.formalParams().any { !it.type.isJavaModelType() }
                        || method.returnType()?.isJavaModelType() != true
                    ) {
                        skip = true
                        return
                    }

                    results[currentDepth].append(
                        Temps.Dart.method.placeholder(
                            method.returnType().toDartType(),
                            method.name(),
                            method.formalParams().joinToString { "${it.type.toDartType()} ${it.name}" },
                            method.methodBody()?.block()?.toDart()
                        )
                    )
                }
            }

            override fun exitMethodDeclaration(ctx: MethodDeclarationContext?) {
                ctx?.run {
                    if (skip) {
                        skip = false
                        return
                    }
                }
            }

            // 生成类结束花括号
            override fun exitClassDeclaration(ctx: ClassDeclarationContext?) {
                ctx?.run { results[currentDepth--].append(Temps.Dart.classEnd) }
            }
            //endregion

            //region Enum类
            // 生成枚举类名
            override fun enterEnumDeclaration(`enum`: EnumDeclarationContext?) {
                results.add(StringBuilder())
                enum?.run { results[++currentDepth].append("\nenum ${enum.IDENTIFIER()} {\n") }
            }

            // 生成枚举值
            override fun enterEnumConstant(ctx: EnumConstantContext?) {
                ctx?.run { results[currentDepth].append("  ${ctx.IDENTIFIER()},\n") }
            }

            // 生成枚举结束花括号
            override fun exitEnumDeclaration(ctx: EnumDeclarationContext?) {
                ctx?.run { results[currentDepth--].append("}\n") }
            }
            //endregion
        }, tree)

        return results.joinToString("\n")
    }

    /**
     * Java语法的语句转为Dart语法的语句
     *
     * 暂时支持赋值语句和return语句
     */
    private fun BlockContext.toDart(): DART_SOURCE {
        val result = StringBuilder("")
        blockStatement()?.forEach {
            it.localVariableDeclaration()?.run { result.append("    ${toDart()}") }
            it.statement()?.run { result.append("    ${toDart()}") }
            it.localTypeDeclaration()?.run { result.append("    ${toDart()}") }
        }
        return result.toString()
    }
}

/**
 * iOS模型类转换成对应的Dart模型类
 *
 * 输入: Objc模型类文件
 * 输出: Dart模型类文件
 * 依赖: [IOSRecognizeModelTask]
 */
class IOSDartModelTask(private val javaModelFile: File) : Task<File, File>(javaModelFile) {
    private val results = mutableListOf<StringBuilder>()
    private var currentDepth = -1 // 嵌套类深度

    override fun process(): File {
        val javaContent = javaModelFile.readText()
        val dartModelSource = translate(javaContent)

        File(OutputProject.Dart.androidModelDirPath).run { if (!exists()) mkdirs() }

        return File("${OutputProject.Dart.androidModelDirPath}/${javaModelFile.nameWithoutExtension.camel2Underscore()}.dart")
            .apply {
                if (!exists()) createNewFile()
                writeText(dartModelSource)
            }
    }

    private fun translate(source: JAVA_SOURCE): DART_SOURCE {
        val lexer = JavaLexer(CharStreams.fromString(source))
        val parser = JavaParser(CommonTokenStream(lexer))
        val tree = parser.compilationUnit()

        gWalker.walk(object : JavaParserBaseListener() {
            //region 普通类
            // 生成类名
            override fun enterClassDeclaration(ctx: ClassDeclarationContext?) {
                results.add(StringBuilder())
                ctx?.run { results[++currentDepth].append(Temps.Dart.classDeclaration.placeholder(ctx.IDENTIFIER().text)) }
            }

            // 生成Field类型名
            override fun enterFieldDeclaration(field: FieldDeclarationContext?) {
                field?.run {
                    if (!type().isJavaModelType()) return

                    results[currentDepth].append("  ${if (isStatic()) "static " else ""}${type().toDartType()}")
                }
            }

            // 生成Field变量名和等于号(如果需要的话)
            override fun enterVariableDeclarator(ctx: VariableDeclaratorContext?) {
                ctx?.run {
                    if (ctx.isChildOf(FieldDeclarationContext::class)) {
                        results[currentDepth].append(" ${variableDeclaratorId().text}")
                    }
                }
            }

            // 生成Field的分号
            override fun exitFieldDeclaration(ctx: FieldDeclarationContext?) {
                ctx?.run {
                    if (!type().isJavaModelType()) return

                    results[currentDepth].append(";\n")
                }
            }

            // 生成类结束花括号
            override fun exitClassDeclaration(ctx: ClassDeclarationContext?) {
                ctx?.run { results[currentDepth--].append(Temps.Dart.classEnd) }
            }
            //endregion

            //region Enum类
            // 生成枚举类名
            override fun enterEnumDeclaration(`enum`: EnumDeclarationContext?) {
                results.add(StringBuilder())
                enum?.run { results[++currentDepth].append("\nenum ${enum.IDENTIFIER()} {\n") }
            }

            // 生成枚举值
            override fun enterEnumConstant(ctx: EnumConstantContext?) {
                ctx?.run { results[currentDepth].append("  ${ctx.IDENTIFIER()},\n") }
            }

            // 生成枚举结束花括号
            override fun exitEnumDeclaration(ctx: EnumDeclarationContext?) {
                ctx?.run { results[currentDepth--].append("}\n") }
            }
            //endregion
        }, tree)

        return results.joinToString("\n")
    }
}