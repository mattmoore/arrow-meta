package arrow.meta.plugins.patternMatching.phases.resolve.diagnostics

import arrow.meta.log.Log
import arrow.meta.log.invoke
import arrow.meta.phases.CompilerContext
import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.diagnostics.DiagnosticWithParameters1
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtNameReferenceExpression
import org.jetbrains.kotlin.psi.psiUtil.getParentOfTypesAndPredicate
import org.jetbrains.kotlin.utils.addToStdlib.safeAs

fun CompilerContext.suppressPatMatUnresolvedReference(diagnostic: Diagnostic): Boolean =
  diagnostic.factory == Errors.UNRESOLVED_REFERENCE &&
    diagnostic.safeAs<DiagnosticWithParameters1<KtNameReferenceExpression, KtNameReferenceExpression>>()?.let { diagnosticWithParameters ->
      Log.Verbose({ "suppressPatMatUnresolvedReference: $this" }) {
        diagnosticWithParameters.psiElement.isCaseCallExpression
      }
    } == true

val KtNameReferenceExpression.isCaseCallExpression: Boolean
  get() = null != this.getParentOfTypesAndPredicate(true, KtCallExpression::class.java) { it ->
    it.firstChild is KtNameReferenceExpression && it.firstChild.textMatches("case")
  }

fun CompilerContext.suppressUnderscoreUsageWithoutBackticks(diagnostic: Diagnostic): Boolean =
  diagnostic.factory == Errors.UNDERSCORE_USAGE_WITHOUT_BACKTICKS

fun CompilerContext.suppressExpressionExpectedPackageFound(diagnostic: Diagnostic): Boolean =
  diagnostic.factory == Errors.EXPRESSION_EXPECTED_PACKAGE_FOUND
