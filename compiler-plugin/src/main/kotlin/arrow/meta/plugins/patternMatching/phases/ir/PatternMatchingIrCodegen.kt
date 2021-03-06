package arrow.meta.plugins.patternMatching.phases.ir

import arrow.meta.Meta
import arrow.meta.phases.CompilerContext
import arrow.meta.phases.codegen.ir.IRGeneration
import arrow.meta.phases.codegen.ir.IrUtils
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.types.impl.originalKotlinType
import org.jetbrains.kotlin.ir.visitors.IrElementTransformer

fun Meta.irPatternMatching(f: IrUtils.(IrExpression) -> IrExpression?): IRGeneration =
  IrGeneration { compilerContext, moduleFragment, pluginContext ->
    moduleFragment.transformChildren(object : IrElementTransformer<Unit> {
      override fun visitExpression(expression: IrExpression, data: Unit): IrExpression =
        f(IrUtils(pluginContext, compilerContext), expression) ?: super.visitExpression(expression, data)
    }, Unit)
  }

class PatternMatchingIrCodegen(
  val irUtils: IrUtils
) {
  fun CompilerContext.generatePatternExpressionIr(it: IrExpression): IrExpression? {
    val targetType = it.type.originalKotlinType
    return it
  }

  companion object {
    operator fun<A> invoke(irUtils: IrUtils, f: PatternMatchingIrCodegen.() -> A): A =
      f(PatternMatchingIrCodegen(irUtils))
  }
}
