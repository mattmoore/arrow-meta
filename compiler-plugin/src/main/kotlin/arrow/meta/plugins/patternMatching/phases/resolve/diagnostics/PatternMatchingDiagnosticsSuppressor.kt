package arrow.meta.plugins.patternMatching.phases.resolve.diagnostics

import arrow.meta.log.Log
import arrow.meta.log.invoke
import arrow.meta.phases.CompilerContext
import arrow.meta.plugins.patternMatching.phases.analysis.isPatternExpression
import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.diagnostics.DiagnosticWithParameters1
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.psi.KtNameReferenceExpression
import org.jetbrains.kotlin.psi.KtWhenCondition
import org.jetbrains.kotlin.psi.psiUtil.getParentOfTypes
import org.jetbrains.kotlin.utils.addToStdlib.safeAs

fun CompilerContext.suppressPatMatUnresolvedReference(diagnostic: Diagnostic): Boolean =
  diagnostic.factory == Errors.UNRESOLVED_REFERENCE &&
    diagnostic.safeAs<DiagnosticWithParameters1<KtNameReferenceExpression, KtNameReferenceExpression>>()?.let { diagnosticWithParameters ->
      Log.Verbose({ "suppressPatMatUnresolvedReference: $this" }) {
        diagnosticWithParameters.psiElement.isPatternExpression
      }
    } == true

fun CompilerContext.suppressUnderscoreUsageWithoutBackticks(diagnostic: Diagnostic): Boolean =
  diagnostic.factory == Errors.UNDERSCORE_USAGE_WITHOUT_BACKTICKS

fun CompilerContext.suppressExpressionExpectedPackageFound(diagnostic: Diagnostic): Boolean =
  diagnostic.factory == Errors.EXPRESSION_EXPECTED_PACKAGE_FOUND
