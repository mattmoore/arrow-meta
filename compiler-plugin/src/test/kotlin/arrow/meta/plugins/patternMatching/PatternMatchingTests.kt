package arrow.meta.plugins.patternMatching

import arrow.meta.CliPlugin
import arrow.meta.Meta
import arrow.meta.phases.CompilerContext
import arrow.meta.plugin.testing.Assert
import arrow.meta.plugin.testing.CompilerTest
import arrow.meta.plugin.testing.CompilerTest.Companion.allOf
import arrow.meta.plugin.testing.CompilerTest.Companion.evalsTo
import arrow.meta.plugin.testing.CompilerTest.Companion.failsWith
import arrow.meta.plugin.testing.CompilerTest.Companion.source
import arrow.meta.plugin.testing.assertThis
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test

open class PatternMatchingPlugin : Meta {
  override fun intercept(ctx: CompilerContext): List<CliPlugin> = listOf(
    patternMatching
  )
}

class PatternMatchingTests {
  private infix fun String.verify(assertion: (CompilerTest.Companion) -> Assert) = also {
    assertThis(CompilerTest(
      config = { listOf(CompilerTest.addMetaPlugins(PatternMatchingPlugin())) },
      code = { it.source }, assert = assertion
    ))
  }

  private val prelude = """
    fun case(arg: Any?): Any? = arg
    """

  private val person = """
    data class Person(val firstName: String, val lastName: String)
    val person = Person("Matt", "Moore")
    """

  private val number = """
    sealed class Number {
      data class One(val reallyFirst: Boolean) : Number()
      data class Other(val value: Int) : Number()
    }
  """.trimIndent()

  @Test
  fun `with case expression and all wildcards`() {
    val code =
      """$prelude
         $person

         val result = when (person) {
           Person(_, _) -> "Matched"
           else -> "Not matched"
         }
       """

    code verify {
      allOf(
        "result".source.evalsTo("Matched")
      )
    }
  }

  @Test
  fun `match expression with const params`() {
    val code =
      """$prelude
         $person

         val result = when (person) {
           Person("Moore", "Matt") -> "Wrong match"
           Person("Matt", "Moore") -> "Matched"
           else -> "Not matched"
         }
         """

    code verify {
      allOf(
        "result".source.evalsTo("Matched")
      )
    }
  }

  @Test
  fun `match expression with placeholder`() {
    val code =
      """$prelude
         $person

         val result = when (person) {
           Person(_, "Moore") -> "Matched"
           else -> "Not matched"
         }
         """

    code verify {
      allOf(
        "result".source.evalsTo("Matched")
      )
    }
  }

  @Disabled
  @Test
  fun `match expression with second param placeholder`() {
    val code =
      """$prelude
         $person

         val result = when (person) {
           Person("Matt", _) -> "Matched"
           else -> "Not matched"
         }
         """

    code verify {
      allOf(
        "result".source.evalsTo("Matched")
      )
    }
  }

  @Disabled
  @Test
  fun `captured param results in value`() {
    val code =
      """$prelude
         $person

         val result = when (person) {
           Person(capturedFirstName, _) -> capturedFirstName
           else -> "Not matched"
         }
         """

    code verify {
      allOf(
        "result".source.evalsTo("Matt")
      )
    }
  }

  @Disabled
  @Test
  fun `captured second param results in value`() {
    val code =
      """$prelude
         $person

         val result = when (person) {
           Person(_, capturedSecondName) -> capturedSecondName
           else -> "Not matched"
         }
         """

    code verify {
      allOf(
        "result".source.evalsTo("Moore")
      )
    }
  }

  @Test
  fun `both captured params result in value`() {
    val code =
      """$prelude
         $person

         val result = when (person) {
           Person(capturedFirstName, capturedSecondName) -> capturedFirstName + capturedSecondName
           else -> "Not matched"
         }
         """

    code verify {
      allOf(
        "result".source.evalsTo("MattMoore")
      )
    }
  }

  @Disabled
  @Test
  fun `both captured params can be used in a call`() {
    val code =
      """$prelude
         $person

         val result = when (person) {
           Person(capturedFirstName, capturedSecondName) -> {
             listOf(capturedFirstName, capturedSecondName)
           }
           else -> listOf("Not matched")
         }
         """

    code verify {
      allOf(
        "result".source.evalsTo(listOf("Matt", "Moore"))
      )
    }
  }

  @Disabled
  @Test
  fun `both captured params inside a function result in value`() {
    val code =
      """$prelude
         $person

         fun resolve(person: Person) =
           when (person) {
             Person(capturedFirstName, capturedSecondName) -> capturedFirstName + capturedSecondName
             else -> "Not matched"
           }

         val result = resolve(person)
         """

    code verify {
      allOf(
        "result".source.evalsTo("MattMoore")
      )
    }
  }

  @Disabled
  @Test
  fun `placeholder cannot be used in body`() {
    val code =
      """$prelude
         $person

         fun resolve(person: Person) =
           when (person) {
             Person(_, capturedSecondName) -> _
             else -> "Not matched"
           }

         val result = resolve(person)
         """

    code verify {
      allOf(
        failsWith { it.contains("Unresolved reference: _") }
      )
    }
  }

  @Disabled
  @Test
  fun `other captured params can be used in body`() {
    val code =
      """$prelude
         $person
         fun resolve(person: Person) =
           when (person) {
             Person(_, param123) -> param123
             else -> "Not matched"
           }

         val result = resolve(person)
         """

    code verify {
      allOf(
        "result".source.evalsTo("Moore")
      )
    }
  }

  @Disabled
  @Test
  fun `sealed class is matched`() {
    val code =
      """$prelude
         $number

         fun resolve(number: Number): String {
           return when (number) {
             Number.One(_) -> "Matched"
             Number.Other(value) -> value.toString()
             else -> "Not matched"
           }
         }

         val result = resolve(Number.Other(42))
         """

    code verify {
      allOf(
        "result".source.evalsTo("42")
      )
    }
  }
}
