package simple.api

import org.springframework.stereotype.Service
import traditional.CreditCard
import traditional.Payment
import traditional.PaymentStatus
import traditional.TraditionalBusinessLogic
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*

@Service
class TraditionalPaymentService(
    private val businessLogic: TraditionalBusinessLogic = TraditionalBusinessLogic()
) {

    fun processCardPayment(request: CardPaymentRequest): PaymentResponse {
        // Validate and create payment method
        val paymentMethod = businessLogic.createCreditCard(
            request.cardNumber,
            request.expiryMonth,
            request.expiryYear,
            request.cvv
        )

        // Create pending payment
        var payment = Payment(
            amount = request.amount,
            method = paymentMethod,
            status = PaymentStatus.PENDING,
            paidDate = null,
            voidDate = null,
            refundDate = null
        )

        // Transition to paid status
        payment = transitionToPaid(payment)

        return PaymentResponse(
            amount = payment.amount,
            status = payment.status.name,
            paymentMethod = PaymentMethodResponse(
                type = "CREDIT_CARD",
                cardLast4 = if (paymentMethod is CreditCard) paymentMethod.number.takeLast(4) else null
            ),
            paidDate = payment.paidDate?.let {
                LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
            }
        )
    }

    /**
     * Traditional approach: Runtime validation required to ensure valid state transitions
     * Can transition PENDING payments to PAID, but not VOIDED or REFUNDED payments
     */
    private fun transitionToPaid(payment: Payment): Payment {
        return when (payment.status) {
            PaymentStatus.PENDING -> {
                payment.copy(
                    status = PaymentStatus.PAID,
                    paidDate = Date()
                )
            }
            PaymentStatus.VOIDED -> {
                throw IllegalStateException("Cannot mark a voided payment as paid")
            }
            PaymentStatus.REFUNDED -> {
                throw IllegalStateException("Cannot mark a refunded payment as paid")
            }
            PaymentStatus.PAID -> {
                throw IllegalStateException("Payment is already paid")
            }
        }
    }
}
