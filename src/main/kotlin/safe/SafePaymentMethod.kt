package safe

import arrow.core.Either
import arrow.core.raise.Raise
import arrow.core.raise.either
import arrow.core.raise.zipOrAccumulate
import safe.DigitString.Companion.toDigitString
import safe.MOD10String.Companion.toMOD10String
import safe.RoutingString.Companion.toRoutingString

sealed interface PaymentMethod
object Cash : PaymentMethod
data class CreditCard(
  val number: MOD10String,
  val expiryDate: ExpiryDate,
  val cvv: DigitString
) : PaymentMethod {

  companion object {
    // Context-based factory (raises errors immediately)
    context(raise: Raise<ValidationErrors>)
    fun create(
      number: String,
      expiryMonth: Int,
      expiryYear: Int,
      cvv: String
    ): CreditCard {
      return raise.zipOrAccumulate(
        { number.toMOD10String("card number", 19) },
        { ExpiryDate.toExpiryDate("expiry date", expiryMonth, expiryYear) },
        { cvv.toDigitString("CVV", maxLength = 4, minLength = 3) }
      ) { cardNumber, expiry, cvvCode ->
        CreditCard(cardNumber, expiry, cvvCode)
      }
    }

    // Validated factory (returns Either with all errors)
    fun createValidated(
      number: String,
      expiryMonth: Int,
      expiryYear: Int,
      cvv: String
    ): Either<ValidationErrors, CreditCard> {
      return either { create(number, expiryMonth, expiryYear, cvv) }
    }
  }
}


data class Check(val bankRoutingNumber: RoutingString, val accountNumber: DigitString) : PaymentMethod {

  companion object {
    context(raise: Raise<ValidationErrors>)
    fun create(routingNumber: String, accountNumber: String): Check {
      return raise.zipOrAccumulate(
        { routingNumber.toRoutingString("routing number") },
        { accountNumber.toDigitString("account number") }
      ) { routing, account ->
        Check(routing, account)
      }
    }

    fun createValidated(
      routingNumber: String,
      accountNumber: String
    ): Either<ValidationErrors, Check> {
      return either { create(routingNumber, accountNumber) }
    }
  }
}
