package arrow.meta.ide.plugins.fpGuide

import arrow.meta.ide.IdePlugin
import arrow.meta.ide.IdeMetaPlugin
import arrow.meta.ide.dsl.editor.lineMarker.LineMarkerSyntax
import arrow.meta.ide.resources.ArrowIcons
import arrow.meta.ide.invoke
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.utils.addToStdlib.safeAs

/**
 * The following section exemplifies a FP Guide IDE Plugin
 *
 * The FP Guide plugin provides tips for rewriting code in a functional style.
 *
 * ```kotlin
 * val IdeMetaPlugin.fpGuide: IdePlugin
 *    get() = "Functional Programming Guide Plugin" {
 *      meta(
 *        addLineMarkerProvider(
 *          icon = ArrowIcons.ICON1,
 *          composite = KtNamedFunction::class.java,
 *          message = { f: KtNamedFunction -> "Teach your users about this feature in function $f" },
 *          transform = {
 *            it.safeAs<KtNamedFunction>()?.takeIf { f ->
 *              f.name == "helloWorld"
 *            }
 *          }
 *        )
 *      )
 *    }
 * ```
 *
 * For every function with the name `helloWorld`, our IDE plugin will register a lineMarker with our custom icon. And whenever
 * the user hovers over the Icon, it will display the message.
 *
 * Take a look at [`arrow-meta-examples`](https://github.com/arrow-kt/arrow-meta-examples) repository for more details.
 *
 * @see [LineMarkerSyntax]
 */
val IdeMetaPlugin.fpGuide: IdePlugin // TODO: Add Animation or example picture
  get() = "Functional Programming Guide Plugin" {
    meta(
      addLineMarkerProvider(
        icon = ArrowIcons.ICON1,
        composite = KtNamedFunction::class.java,
        message = { f: KtNamedFunction -> "Teach your users about this feature in function ${f.name}" },
        transform = {
          it.safeAs<KtNamedFunction>()?.takeIf { f ->
            f.name == "helloWorld"
          }
        }
      )
    )
  }
