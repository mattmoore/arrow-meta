package arrow.meta.ide.plugins.patternMatching

import arrow.meta.ide.IdePlugin
import arrow.meta.ide.IdeMetaPlugin
import arrow.meta.ide.dsl.editor.lineMarker.LineMarkerSyntax
import arrow.meta.ide.resources.ArrowIcons
import arrow.meta.ide.invoke
import org.jetbrains.kotlin.idea.caches.resolve.resolveToDescriptorIfAny
import org.jetbrains.kotlin.idea.util.hasInlineModifier
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.utils.addToStdlib.safeAs

val IdeMetaPlugin.patternMatching: IdePlugin // TODO: Add Animation or example picture
  get() = "Pattern Matching" {
    meta(
      addLineMarkerProvider(
        icon = ArrowIcons.ICON1,
        composite = KtClass::class.java,
        message = { ktClass: KtClass ->
          HTML.renderMessage("Teach your users about this feature for inline classes") + "<br/>" +
            ktClass.resolveToDescriptorIfAny()?.let(HTML::render)
        },
        transform = {
          it.safeAs<KtClass>()?.takeIf { ktClass ->
            ktClass.hasInlineModifier()
          }
        }
      )
    )
  }
