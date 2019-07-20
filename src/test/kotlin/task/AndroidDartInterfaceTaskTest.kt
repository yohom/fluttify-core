package task

import me.yohom.fluttify.Jar
import me.yohom.fluttify.common.extensions.file
import me.yohom.fluttify.common.extensions.isObfuscated
import me.yohom.fluttify.common.extensions.iterate
import me.yohom.fluttify.task.AndroidDartInterfaceTask
import me.yohom.fluttify.task.AndroidSystemRefTask
import me.yohom.fluttify.task.ExportTask
import org.junit.Test

class AndroidDartInterfaceTaskTest {

    @Test
    fun process() {
        Jar.Decompiled.rootDirPath.file().iterate("java") {
            if (!it.nameWithoutExtension.isObfuscated()) {
                val file = AndroidDartInterfaceTask(it).process()
//                val formatedFile = DartfmtTask(file).process()
                println(file.readText())
            }
        }
        AndroidSystemRefTask("/Users/yohom/Github/Util/Kotlin/fluttify-core/build/output-project/baidu_map_flutter".file())
            .process()
        ExportTask("/Users/yohom/Github/Util/Kotlin/fluttify-core/build/output-project/baidu_map_flutter".file())
            .process()
//        println("  abc\ndef".replaceParagraph("abc", "g"))
    }
}