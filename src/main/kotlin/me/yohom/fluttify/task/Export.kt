package me.yohom.fluttify.task

import me.yohom.fluttify.FluttifyCorePluginExtension
import me.yohom.fluttify.common.extensions.file
import org.apache.commons.io.FileUtils
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction

/**
 * 需要引入的内容为android端的主类和模型类以及ios端的主类和模型类
 *
 * 输入: 目标工程文件夹
 * 输出: 加入export后的目标工程文件夹
 */
open class Export : DefaultTask() {
    override fun getGroup() = "fluttify"

    @TaskAction
    fun process() {
        val dartSrcDir = "${project.projectDir}/output-project/lib/src/".file()
        val exportFile =
            "${project.projectDir}/output-project/lib/${FluttifyCorePluginExtension.outputProjectName}.dart".file()

        val existContent = exportFile.readText()
        val result = StringBuilder()
        FileUtils
            .iterateFiles(dartSrcDir, arrayOf("dart"), true)
            .forEach {
                result.append("export '${it.toRelativeString(exportFile).removePrefix("../")}';\n")
            }
        result.append("\n")
        result.append(existContent)

        exportFile.writeText(result.toString())
    }
}