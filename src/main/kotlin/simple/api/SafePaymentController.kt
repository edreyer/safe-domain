package simple.api

import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/safe/payments")
@Validated
class SafePaymentController(
    private val paymentService: SafePaymentService
) {

    @PostMapping("/card")
    fun processCardPayment(@Valid @RequestBody request: CardPaymentRequest): ResponseEntity<Any> {
        return paymentService.processCardPayment(request).fold(
            ifLeft = { error ->
                ResponseEntity.badRequest().body(
                    ErrorResponse(message = error)
                )
            },
            ifRight = { response ->
                ResponseEntity.ok(response)
            }
        )
    }
}
