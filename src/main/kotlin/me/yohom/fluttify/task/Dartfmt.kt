package me.yohom.fluttify.task

import me.yohom.fluttify.common.extensions.iterate
import org.gradle.api.DefaultTask
import java.io.BufferedReader
import java.io.InputStreamReader

/**
 * 把输入文件进行dartfmt
 *
 * 输入: 未格式化的dart文件
 * 输出: 格式化后的dart文件
 */
open class Dartfmt : DefaultTask() {
    override fun getGroup() = "fluttify"

    fun process() {
        project.projectDir.iterate("dart") {
            val process = Runtime
                .getRuntime()
                .exec("dartfmt -w ${it.absolutePath}")

            val br = BufferedReader(InputStreamReader(process.inputStream))
            br.lines().forEach(::println)
        }
    }
}