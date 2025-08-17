package traditional

open class PaymentMethod

object Cash : PaymentMethod()

data class CreditCard(
    val number: String,
    val expiryMonth: Int,
    val expiryYear: Int,
    val cvv: String
) : PaymentMethod()

data class Check(
    val routingNumber: String,
    val accountNumber: String
) : PaymentMethod()

// Traditional validation error classes
open class ValidationError(val message: String)

class InvalidCardNumberError(message: String) : ValidationError(message)
class InvalidExpiryDateError(message: String) : ValidationError(message)
class InvalidCvvError(message: String) : ValidationError(message)
class InvalidRoutingNumberError(message: String) : ValidationError(message)
class InvalidAccountNumberError(message: String) : ValidationError(message)
