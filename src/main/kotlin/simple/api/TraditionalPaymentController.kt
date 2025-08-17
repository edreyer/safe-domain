package simple.api

import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import traditional.PaymentValidationException

@RestController
@RequestMapping("/api/traditional/payments")
@Validated
class TraditionalPaymentController(
    private val paymentService: TraditionalPaymentService
) {

    @PostMapping("/card")
    fun processCardPayment(@Valid @RequestBody request: CardPaymentRequest): ResponseEntity<Any> {
        return try {
            val response = paymentService.processCardPayment(request)
            ResponseEntity.ok(response)
        } catch (e: PaymentValidationException) {
            ResponseEntity.badRequest().body(
                ErrorResponse(
                    message = "Payment validation failed",
                    errors = e.errors.map { it.message }
                )
            )
        } catch (e: IllegalStateException) {
            ResponseEntity.badRequest().body(
                ErrorResponse(message = e.message ?: "Invalid payment state")
            )
        } catch (e: Exception) {
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ErrorResponse(message = "Internal server error")
            )
        }
    }
}
