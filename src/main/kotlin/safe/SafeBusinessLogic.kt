package safe

import arrow.core.raise.Raise
import arrow.core.raise.either
import bind

data class DomainError(val err: String)

// Context-based usage (composes well)
context(_: Raise<DomainError>)
fun processPayment(cardNumber: String, month: Int, year: Int, cvv: String): PaymentMethod =
  either {
    CreditCard.create(cardNumber, month, year, cvv)
      .let { processCard(it)}
  }
    .mapLeft { err -> err
      .map { it.message }
      .joinToString(",\n", "[\n", "]\n")
      .let { DomainError(it) }
    }
    .bind()

fun processCard(card: CreditCard): CreditCard = card

