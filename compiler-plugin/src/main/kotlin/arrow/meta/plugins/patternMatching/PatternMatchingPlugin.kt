package arrow.meta.plugins.patternMatching

import arrow.meta.CliPlugin
import arrow.meta.Meta
import arrow.meta.invoke
import arrow.meta.plugins.patternMatching.phases.analysis.patternExpressionAnalysis
import arrow.meta.plugins.patternMatching.phases.ir.PatternMatchingIrCodegen
import arrow.meta.plugins.patternMatching.phases.ir.irPatternMatching
import arrow.meta.plugins.patternMatching.phases.resolve.diagnostics.suppressPatMatUnresolvedReference

val Meta.patternMatching: CliPlugin
  get() =
    "Pattern Matching CLI" {
      meta(
        enableIr(),
        suppressDiagnostic { ctx.suppressPatMatUnresolvedReference(it) },
        patternExpressionAnalysis(),
        irPatternMatching { PatternMatchingIrCodegen(this) { generatePatternExpressionIr(it) } },
        irDump()
      )
    }
