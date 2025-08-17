import arrow.core.raise.Raise
import arrow.core.raise.either

data class ValidationError(val msg: String)

data class DomainError(val msg: String)

context(r: Raise<ValidationError>)
fun lowLevel(str: String): String {
  ensure(str.trim().isNotEmpty()) { ValidationError("Empty string") }
  return str
}

context(r: Raise<DomainError>)
fun businessLogic(): String {
  val (a, b) = either {
    val a = lowLevel("a")
    val b = lowLevel("b")
    a to b
  }
    .mapLeft { err -> DomainError(err.msg) }
    .bind()

  return a + b
}
