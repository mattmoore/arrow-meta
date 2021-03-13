package arrow.meta.plugins.patternMatching.phases.analysis

import arrow.meta.Meta
import arrow.meta.phases.CompilerContext
import arrow.meta.phases.analysis.AnalysisHandler
import arrow.meta.phases.analysis.dfs
import arrow.meta.phases.analysis.traverseFilter
import arrow.meta.plugins.patternMatching.phases.analysis.PatternExpression.Companion.parsePatternExpression
import org.jetbrains.kotlin.container.get
import org.jetbrains.kotlin.descriptors.ConstructorDescriptor
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.getParentOfTypes
import org.jetbrains.kotlin.resolve.BindingTrace
import org.jetbrains.kotlin.resolve.calls.callUtil.getResolvedCall

fun Meta.patternExpressionAnalysis(): AnalysisHandler =
  analysis(
    doAnalysis = { project, module, projectContext, files, bindingTrace, componentProvider ->
      null
    },
    analysisCompleted = { project, module, bindingTrace, files ->
      val context = PatternResolutionContext(this)
//      val caseExpressions = files.flatMap { file ->
//        context.caseExpressions(file) { caseExpr ->
//          val (ktCallExpression, patternExpression) = caseExpressionResolution(caseExpr)
//          patternExpression?.parameters?.forEachIndexed { index, param ->
//            if (param is PatternExpression.Param.Captured) {
//              val nameExpression = patternExpression.paramExpression(index) as KtSimpleNameExpression
//              bindingTrace.record(PATTERN_EXPRESSION_CAPTURED_PARAMS, nameExpression)
//              referPlaceholder(nameExpression, index)
//            }
//          }
//          Pair(ktCallExpression, patternExpression)
//        }
//      }

//      val patternExpressions = files.flatMap { file ->
//        context.resolvePatternExpression(file) { whenExpr ->
//          patternExpressionResolution(whenExpr).map { (entry, expr) ->
//            expr.parameters.forEachIndexed { index, param ->
//              if (param is PatternExpression.Param.Captured) {
//                val nameExpression = expr.paramExpression(index) as KtSimpleNameExpression
//                bindingTrace.record(PATTERN_EXPRESSION_CAPTURED_PARAMS, nameExpression)
//                referPlaceholder(nameExpression, index)
//              }
//            }
//
//            if (expr.parameters.any { it is PatternExpression.Param.Captured && !it.isWildcard }) {
//              val params = fillCapturedParameters(entry, expr)
//              params.forEach {
//                bindingTrace.record(PATTERN_EXPRESSION_BODY_PARAMS, it)
//              }
//            }
//
//            expr
//          }
//        }
//      }
//      println("Resolved pattern expressions $caseExpressions")

      val patternExpressions = files.flatMap { file ->
        file.patternExpressions.map { expression ->
          expression
        }
      }

      null
    }
  )

object PatternExpressionRules {
  val validParents = arrayOf(
    KtWhenCondition::class.java
  )

  val symbols = mapOf(
    "_" to KtNameReferenceExpression::class.java
  )
}

val KtFile.patternExpressions
  get() = dfs { it is KtNameReferenceExpression && PatternExpressionRules.symbols.contains(it.text) }

val KtExpression.isPatternExpression: Boolean
  get() = PatternExpressionRules.symbols.contains(text)
    && null != getParentOfTypes(true, *PatternExpressionRules.validParents)

fun PatternResolutionContext.caseExpressions(file: KtFile, resolution: PatternResolutionContext.(KtCallExpression) -> Pair<KtCallExpression, PatternExpression?>) =
  file
    .dfs { it is KtCallExpression && it.firstChild.textMatches("case") }
    .map { resolution(it as KtCallExpression) }

fun PatternResolutionContext.caseExpressionResolution(expression: KtCallExpression): Pair<KtCallExpression, PatternExpression?> =
  Pair(expression, parsePatternExpression(expression))

fun PatternResolutionContext.resolvePatternExpression(file: KtFile, resolution: PatternResolutionContext.(KtWhenExpression) -> List<PatternExpression>) =
  file.traverseFilter(KtWhenExpression::class.java) { resolution(it) }.flatten()

fun PatternResolutionContext.patternExpressionResolution(expression: KtWhenExpression): List<Pair<KtWhenEntry, PatternExpression>> =
  expression.entries.flatMap { entry ->
    // fixme: for now pattern is supposed to be in the form of `case(Constructor(param1, param2))`
    val matchingConditions = entry.conditions.mapNotNull { patternExpression(it) }
    assert(matchingConditions.size < 2) { "Cannot have multiple pattern expressions in one when entry" }
    matchingConditions.map { entry to it }
  }

private fun PatternResolutionContext.patternExpression(condition: KtWhenCondition): PatternExpression? =
  condition.children.find { callExpr ->
    callExpr is KtCallExpression
      && callExpr.getResolvedCall(bindingTrace.bindingContext)?.candidateDescriptor == caseDescriptor
  }?.let { parsePatternExpression(it as KtCallExpression) }

data class PatternExpression(
  val elementCall: KtCallExpression,
  val constructor: ConstructorDescriptor,
  val parameters: List<Param>
) {
  fun paramExpression(index: Int) =
    elementCall.valueArguments[index].getArgumentExpression()

  sealed class Param {
    data class Captured(val isWildcard: Boolean) : Param()
    object Expression : Param() {
      override fun toString(): String = "Expression"
    }
  }

  override fun toString(): String =
    "PatternExpression(" +
      "elementCall=${elementCall.text}, " +
      "constructor=$constructor, " +
      "wildcards=$parameters, " +
      ")"

  companion object {
    fun PatternResolutionContext.parsePatternExpression(caseCallExpr: KtCallExpression): PatternExpression? {
      val elementCall =
        when (val innerCall = caseCallExpr.valueArguments.first().getArgumentExpression()) {
          is KtCallExpression -> innerCall
          is KtDotQualifiedExpression -> innerCall.selectorExpression as? KtCallExpression
          else -> null
        }
      check(elementCall != null) { "Case should contain a call inside" }

      val parameters = elementCall.valueArguments.map { valueArgument ->
        when (val argExpression = valueArgument.getArgumentExpression()) {
          is KtNameReferenceExpression ->
            when {
              bindingTrace.getType(argExpression) == null -> {
                val isWildcard = argExpression.getReferencedName() == "_"
                Param.Captured(isWildcard)
              }
              else -> Param.Expression
            }
          else -> Param.Expression
        }
      }

      val constructor = elementCall.getResolvedCall(bindingTrace.bindingContext)?.candidateDescriptor as? ConstructorDescriptor
      check(constructor != null) { "Element call should create a class" }
      check(constructor.constructedClass.isData) { "Only data classes are supported" }

      return PatternExpression(
        elementCall = elementCall,
        constructor = constructor,
        parameters = parameters
      )
    }
  }
}

class PatternResolutionContext(
  val compilerContext: CompilerContext
) {
  val module = compilerContext.module!!
  val bindingTrace = compilerContext.componentProvider!!.get<BindingTrace>()

  val caseDescriptor by lazy {
    // similar to findClassAcrossDeps
    // todo: adjust to case function coordinate when it is introduced properly
    val pkg = module.getPackage(FqName.ROOT)
    pkg.fragments.mapNotNull { pkgFragment ->
      pkgFragment.getMemberScope().getContributedFunctions(
        Name.identifier("case"),
        NoLookupLocation.FROM_SYNTHETIC_SCOPE
      ).firstOrNull {
        it.valueParameters.size == 1
          && it.valueParameters.first().type == module.builtIns.nullableAnyType
          && it.returnType == module.builtIns.nullableAnyType
      }
    }.single()
  }
}
