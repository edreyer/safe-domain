package traditional

import java.math.BigDecimal
import java.util.*

data class Payment(
  val amount: BigDecimal,
  val method: PaymentMethod,
  val status: PaymentStatus,
  val paidDate: Date?,
  val voidDate: Date?,
  val refundDate: Date?,
)

enum class PaymentStatus {
  PENDING,
  PAID,
  VOIDED,
  REFUNDED
}
