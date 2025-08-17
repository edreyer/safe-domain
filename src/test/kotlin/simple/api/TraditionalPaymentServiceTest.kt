package simple.api

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import traditional.PaymentValidationException
import java.math.BigDecimal

class TraditionalPaymentServiceTest : StringSpec({

    val service = TraditionalPaymentService()

    "should process valid card payment" {
        val request = CardPaymentRequest(
            cardNumber = "4532015112830366",
            expiryMonth = 12,
            expiryYear = 2025,
            cvv = "123",
            amount = BigDecimal("99.99")
        )

        val response = service.processCardPayment(request)

        response.amount shouldBe BigDecimal("99.99")
        response.status shouldBe "PAID"
        response.paymentMethod.type shouldBe "CREDIT_CARD"
        response.paymentMethod.cardLast4 shouldBe "0366"
        response.paidDate shouldNotBe null
    }

    "should throw exception for invalid card number" {
        val request = CardPaymentRequest(
            cardNumber = "1234",  // Invalid card number
            expiryMonth = 12,
            expiryYear = 2025,
            cvv = "123",
            amount = BigDecimal("99.99")
        )

        shouldThrow<PaymentValidationException> {
            service.processCardPayment(request)
        }
    }

    "should throw exception for expired card" {
        val request = CardPaymentRequest(
            cardNumber = "4532015112830366",
            expiryMonth = 1,
            expiryYear = 2020,  // Expired
            cvv = "123",
            amount = BigDecimal("99.99")
        )

        shouldThrow<PaymentValidationException> {
            service.processCardPayment(request)
        }
    }

    "should throw exception for invalid CVV" {
        val request = CardPaymentRequest(
            cardNumber = "4532015112830366",
            expiryMonth = 12,
            expiryYear = 2025,
            cvv = "12",  // Invalid CVV length
            amount = BigDecimal("99.99")
        )

        shouldThrow<PaymentValidationException> {
            service.processCardPayment(request)
        }
    }

    "should throw exception for invalid CVV format" {
        val request = CardPaymentRequest(
            cardNumber = "4532015112830366",
            expiryMonth = 12,
            expiryYear = 2025,
            cvv = "abc",  // Non-numeric CVV
            amount = BigDecimal("99.99")
        )

        shouldThrow<PaymentValidationException> {
            service.processCardPayment(request)
        }
    }

    "should handle edge case of minimum valid expiry date" {
        val request = CardPaymentRequest(
            cardNumber = "4532015112830366",
            expiryMonth = 1,
            expiryYear = 2026,
            cvv = "123",
            amount = BigDecimal("0.01")
        )

        val response = service.processCardPayment(request)
        response.status shouldBe "PAID"
    }

    // Traditional approach requires extensive testing for runtime validation
    "should validate card number length" {
        val request = CardPaymentRequest(
            cardNumber = "45320151128303661234567",  // Too long
            expiryMonth = 12,
            expiryYear = 2025,
            cvv = "123",
            amount = BigDecimal("99.99")
        )

        shouldThrow<PaymentValidationException> {
            service.processCardPayment(request)
        }
    }

    "should validate month range" {
        val request = CardPaymentRequest(
            cardNumber = "4532015112830366",
            expiryMonth = 13,  // Invalid month
            expiryYear = 2025,
            cvv = "123",
            amount = BigDecimal("99.99")
        )

        shouldThrow<PaymentValidationException> {
            service.processCardPayment(request)
        }
    }

    "should validate negative month" {
        val request = CardPaymentRequest(
            cardNumber = "4532015112830366",
            expiryMonth = -1,  // Invalid month
            expiryYear = 2025,
            cvv = "123",
            amount = BigDecimal("99.99")
        )

        shouldThrow<PaymentValidationException> {
            service.processCardPayment(request)
        }
    }

    "should handle multiple validation errors" {
        val request = CardPaymentRequest(
            cardNumber = "1234",  // Invalid
            expiryMonth = 13,     // Invalid
            expiryYear = 2020,    // Expired
            cvv = "12",           // Invalid
            amount = BigDecimal("99.99")
        )

        val exception = shouldThrow<PaymentValidationException> {
            service.processCardPayment(request)
        }

        // Traditional approach should accumulate multiple errors
        exception.errors.size shouldBe 3
    }

    // Additional tests needed for traditional approach due to runtime validation
    "should handle zero amount edge case" {
        val request = CardPaymentRequest(
            cardNumber = "4532015112830366",
            expiryMonth = 12,
            expiryYear = 2025,
            cvv = "123",
            amount = BigDecimal.ZERO
        )

        // Traditional approach allows zero amounts - needs runtime validation
        val response = service.processCardPayment(request)
        response.amount shouldBe BigDecimal.ZERO
    }

    "should handle very large amounts" {
        val request = CardPaymentRequest(
            cardNumber = "4532015112830366",
            expiryMonth = 12,
            expiryYear = 2025,
            cvv = "123",
            amount = BigDecimal("999999.99")
        )

        val response = service.processCardPayment(request)
        response.amount shouldBe BigDecimal("999999.99")
    }

    "should handle precision in amounts" {
        val request = CardPaymentRequest(
            cardNumber = "4532015112830366",
            expiryMonth = 12,
            expiryYear = 2025,
            cvv = "123",
            amount = BigDecimal("99.999")  // More than 2 decimal places
        )

        val response = service.processCardPayment(request)
        response.amount shouldBe BigDecimal("99.999")
    }
})
