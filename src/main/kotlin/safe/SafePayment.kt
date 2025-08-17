package safe

import java.util.*

sealed interface SafePayment {
  val amount: PositiveBigDecimal
  val method: PaymentMethod
}

data class PendingPayment(
  override val amount: PositiveBigDecimal,
  override val method: PaymentMethod
) : SafePayment

data class PaidPayment(
  override val amount: PositiveBigDecimal,
  override val method: PaymentMethod,
  val paidDate: Date
) : SafePayment

data class RefundPayment(
  override val amount: PositiveBigDecimal,
  override val method: PaymentMethod,
  val refundDate: Date
) : SafePayment

data class VoidPayment(
  override val amount: PositiveBigDecimal,
  override val method: PaymentMethod,
  val voidDate: Date
) : SafePayment
