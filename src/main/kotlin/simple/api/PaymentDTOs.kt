package simple.api

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Positive
import java.math.BigDecimal

data class CardPaymentRequest(
    @field:NotBlank(message = "Card number is required")
    val cardNumber: String,

    @field:NotNull(message = "Expiry month is required")
    @field:Positive(message = "Expiry month must be positive")
    val expiryMonth: Int,

    @field:NotNull(message = "Expiry year is required")
    @field:Positive(message = "Expiry year must be positive")
    val expiryYear: Int,

    @field:NotBlank(message = "CVV is required")
    val cvv: String,

    @field:NotNull(message = "Amount is required")
    @field:Positive(message = "Amount must be positive")
    val amount: BigDecimal
)

data class PaymentResponse(
    val amount: BigDecimal,
    val status: String,
    val paymentMethod: PaymentMethodResponse,
    val paidDate: String?
)

data class PaymentMethodResponse(
    val type: String,
    val cardLast4: String? = null
)

data class ErrorResponse(
    val message: String,
    val errors: List<String> = emptyList()
)
