package simple.api

import arrow.core.Either
import arrow.core.raise.either
import org.springframework.stereotype.Service
import safe.PaidPayment
import safe.PendingPayment
import safe.PositiveBigDecimal.Companion.toPositiveBigDecimalValidated
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*

@Service
class SafePaymentService {

    fun processCardPayment(request: CardPaymentRequest): Either<String, PaymentResponse> {
        return either {
            // Validate amount using safe types
            val amount = request.amount.toPositiveBigDecimalValidated("amount")
                .mapLeft { "Invalid amount: ${it.message}" }
                .bind()

            // Validate and create payment method
            val paymentMethod = safe.CreditCard.createValidated(
                request.cardNumber,
                request.expiryMonth,
                request.expiryYear,
                request.cvv
            ).mapLeft { errors ->
                "Payment method validation failed: ${errors.joinToString(", ") { it.message }}"
            }.bind()

            // Create pending payment
            val pendingPayment = PendingPayment(
                amount = amount,
                method = paymentMethod
            )

            // Type-safe transition to paid status
            val paidPayment = transitionToPaid(pendingPayment)

            PaymentResponse(
                amount = paidPayment.amount.value,
                status = "PAID",
                paymentMethod = PaymentMethodResponse(
                    type = "CREDIT_CARD",
                    cardLast4 = paymentMethod.number.value.takeLast(4)
                ),
                paidDate = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
            )
        }
    }

    /**
     * Safe approach: Type system prevents invalid state transitions
     * Only accepts PendingPayment and returns PaidPayment
     * Impossible to pass in VoidPayment or RefundPayment at compile time
     */
    private fun transitionToPaid(pendingPayment: PendingPayment): PaidPayment {
        return PaidPayment(
            amount = pendingPayment.amount,
            method = pendingPayment.method,
            paidDate = Date()
        )
    }
}
