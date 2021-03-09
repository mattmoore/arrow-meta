package arrow.meta.plugins.patternMatching.phases.ir

import arrow.meta.Meta
import arrow.meta.phases.CompilerContext
import arrow.meta.phases.codegen.ir.IRGeneration
import arrow.meta.phases.codegen.ir.IrUtils
import arrow.meta.plugins.patternMatching.phases.analysis.PatternResolutionContext
import arrow.meta.plugins.patternMatching.phases.analysis.PlaceholderPropertyDescriptor
import org.jetbrains.kotlin.backend.common.BackendContext
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.declarations.IrSymbolOwner
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.IrCallImpl
import org.jetbrains.kotlin.ir.types.impl.originalKotlinType
import org.jetbrains.kotlin.ir.util.ConstantValueGenerator
import org.jetbrains.kotlin.ir.util.TypeTranslator
import org.jetbrains.kotlin.ir.util.getArgumentsWithIr
import org.jetbrains.kotlin.ir.util.referenceFunction
import org.jetbrains.kotlin.ir.visitors.IrElementTransformer
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.resolve.BindingContext

//fun Meta.irPatternMatching(f: IrUtils.(IrExpression) -> IrExpression?): IRGeneration =
//  IrGeneration { compilerContext, moduleFragment, pluginContext ->
//    moduleFragment.transformChildren(object : IrElementTransformer<Unit> {
//      override fun visitExpression(expression: IrExpression, data: Unit): IrExpression =
//        f(IrUtils(pluginContext, compilerContext), expression) ?: super.visitExpression(expression, data)
//    }, Unit)
//  }

fun irPatternMatching(
  compilerContext: CompilerContext,
  moduleFragment: IrModuleFragment,
  pluginContext: IrPluginContext
): Unit =
  moduleFragment.files.forEach { file ->
    file.accept(
      object : IrElementTransformer<IrSymbolOwner?> {
        private val irUtils = IrUtils(pluginContext, compilerContext)
        override fun visitDeclaration(declaration: IrDeclaration, data: IrSymbolOwner?): IrStatement =
          if (declaration is IrSymbolOwner) {
            super.visitDeclaration(declaration, declaration)
          } else {
            super.visitDeclaration(declaration, data)
          }

        override fun visitWhen(expression: IrWhen, data: IrSymbolOwner?): IrExpression {
          return super.visitWhen(expression, data).also {
            val builder = DeclarationIrBuilder(
              pluginContext,
              data!!.symbol,
              expression.startOffset,
              expression.endOffset
            )
            irUtils.patchIrWhen(expression, builder)
          }
        }
      },
      null
    )
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

fun IrUtils.patchIrWhen(irWhen: IrWhen, irBuilder: DeclarationIrBuilder): IrExpression {
//  val patternContext = PatternResolutionContext(compilerContext)
//
//  irWhen.branches.forEach { branch ->
//    val patternCall = findPatternCall(patternContext, branch.condition) ?: return@forEach
//    val (subject, constructorCall) = patternCall
//
//    // todo replace with plugin context type translator in 1.4-M3
//    val typeTranslator = TypeTranslator(
//      pluginContext.symbols.externalSymbolTable,
//      pluginContext.irBuiltIns.languageVersionSettings,
//      pluginContext.builtIns
//    ).apply {
//      constantValueGenerator = ConstantValueGenerator(
//        pluginContext.moduleDescriptor,
//        pluginContext.symbols.externalSymbolTable
//      )
//    }
//
//    val targetClass = constructorCall.descriptor.constructedClass
//    val targetType = constructorCall.type
//
//    fun componentCall(index: Int, startOffset: Int, endOffset: Int): IrCall {
//      val descriptor = targetClass.findFirstFunction("component${index + 1}") { it.valueParameters.isEmpty() }
//      val irType = typeTranslator.translateType(descriptor.returnType!!)
//      return IrCallImpl(
//        startOffset,
//        endOffset,
//        irType,
//        pluginContext.symbols.externalSymbolTable.referenceFunction(descriptor),
//        descriptor
//      ).also { call ->
//        call.dispatchReceiver = subject
//      }
//    }
//
//    branch.condition = irBuilder.buildStatement(branch.condition.startOffset, branch.condition.endOffset) {
//      val booleanType = context.irBuiltIns.booleanType
//      val typeCheck: IrExpression =
//        typeOperator(
//          booleanType,
//          subject,
//          IrTypeOperator.INSTANCEOF,
//          targetType
//        )
//
//      constructorCall.getArgumentsWithIr().foldIndexed(typeCheck) argLoop@{ index, acc, (param, expression) ->
//        if (expression is IrGetField && expression.descriptor is PlaceholderPropertyDescriptor) return@argLoop acc
//
//        irIfThenElse(
//          booleanType,
//          acc,
//          irEquals(componentCall(index, startOffset, endOffset), expression),
//          irFalse(),
//          origin = IrStatementOrigin.ANDAND
//        )
//      }
//    }
//
//    val resultTransformer = object : IrElementTransformerVoid() {
//      override fun visitGetField(expression: IrGetField): IrExpression {
//        val descriptor = expression.descriptor
//        if (descriptor !is PlaceholderPropertyDescriptor) {
//          return super.visitGetField(expression)
//        }
//
//        return componentCall(descriptor.parameterIndex, expression.startOffset, expression.endOffset)
//      }
//    }
//
//    branch.result = branch.result.transform(resultTransformer, null)
//  }

  return irWhen
}

// todo: replace with IrVisitor
//fun IrUtils.findPatternCall(patternContext: PatternResolutionContext, expr: IrExpression): Pair<IrExpression, IrConstructorCall>? {
//  // fixme: based on the template from tests, must be more complex
//  if (expr !is IrCall) return null
//  if (expr.symbol != backendContext.irBuiltIns.eqeqSymbol) return null
//
//  val subject = expr.getValueArgument(0) ?: return null
//  val caseCall = expr.getValueArgument(1)
//  if (caseCall !is IrCall) return null
//  if (caseCall.descriptor != patternContext.caseDescriptor) return null
//
//  val constructorCall = caseCall.getValueArgument(0)
//  if (constructorCall !is IrConstructorCall) return null
//
//  return Pair(subject, constructorCall)
//}