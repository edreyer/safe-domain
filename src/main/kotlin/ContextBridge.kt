@file:OptIn(ExperimentalTypeInference::class, ExperimentalContracts::class)

import arrow.core.Either
import arrow.core.raise.Raise
import arrow.core.raise.RaiseDSL
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract
import kotlin.experimental.ExperimentalTypeInference
import arrow.core.raise.ensure as ensureExt
import arrow.core.raise.ensureNotNull as ensureNotNullExt

/**
 * THIS IS A TEMP FILE UNTIL THIS GETS RELEASED:
 * https://github.com/arrow-kt/arrow/pull/3646
 */

context(raise: Raise<Error>) @RaiseDSL
fun <Error> raise(e: Error): Nothing =
  raise.raise(e)

context(raise: Raise<Error>)
@RaiseDSL
inline fun <Error> ensure(condition: Boolean, otherwise: () -> Error) {
  contract { returns() implies condition }
  raise.ensureExt(condition, otherwise)
}

context(raise: Raise<Error>)
@RaiseDSL
inline fun <Error, B : Any> ensureNotNull(value: B?, otherwise: () -> Error): B {
  contract { returns() implies (value != null) }
  return raise.ensureNotNullExt(value, otherwise)
}

@Suppress("WRONG_IMPLIES_CONDITION")
context(raise: Raise<Error>) @RaiseDSL
fun <Error, A> Either<Error, A>.bind(): A {
  contract { returns() implies (this@bind is Either.Right) }
  return with(raise) { this@bind.bind() }
}
