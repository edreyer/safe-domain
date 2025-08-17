package simple.api

import arrow.core.Either.Left
import arrow.core.Either.Right
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.beInstanceOf
import java.math.BigDecimal

class SafePaymentServiceTest : StringSpec({

    val service = SafePaymentService()

    "should process valid card payment" {
        val request = CardPaymentRequest(
            cardNumber = "4532015112830366",
            expiryMonth = 12,
            expiryYear = 2025,
            cvv = "123",
            amount = BigDecimal("99.99")
        )

        val result = service.processCardPayment(request)

        result should beInstanceOf<Right<PaymentResponse>>()
        val response = (result as Right).value

        response.amount shouldBe BigDecimal("99.99")
        response.status shouldBe "PAID"
        response.paymentMethod.type shouldBe "CREDIT_CARD"
        response.paymentMethod.cardLast4 shouldBe "0366"
        response.paidDate shouldNotBe null
    }

    "should return error for invalid card number" {
        val request = CardPaymentRequest(
            cardNumber = "1234",
            expiryMonth = 12,
            expiryYear = 2025,
            cvv = "123",
            amount = BigDecimal("99.99")
        )

        val result = service.processCardPayment(request)

        result should beInstanceOf<Left<String>>()
    }

    "should return error for negative amount" {
        val request = CardPaymentRequest(
            cardNumber = "4532015112830366",
            expiryMonth = 12,
            expiryYear = 2025,
            cvv = "123",
            amount = BigDecimal("-10.00")
        )

        val result = service.processCardPayment(request)

        result should beInstanceOf<Left<String>>()
    }

    "should return error for zero amount" {
        val request = CardPaymentRequest(
            cardNumber = "4532015112830366",
            expiryMonth = 12,
            expiryYear = 2025,
            cvv = "123",
            amount = BigDecimal.ZERO
        )

        val result = service.processCardPayment(request)

        result should beInstanceOf<Left<String>>()
    }

    "should return error for expired card" {
        val request = CardPaymentRequest(
            cardNumber = "4532015112830366",
            expiryMonth = 1,
            expiryYear = 2020,
            cvv = "123",
            amount = BigDecimal("99.99")
        )

        val result = service.processCardPayment(request)

        result should beInstanceOf<Left<String>>()
    }

    "should return error for invalid CVV" {
        val request = CardPaymentRequest(
            cardNumber = "4532015112830366",
            expiryMonth = 12,
            expiryYear = 2025,
            cvv = "12",
            amount = BigDecimal("99.99")
        )

        val result = service.processCardPayment(request)

        result should beInstanceOf<Left<String>>()
    }

    // Safe approach requires significantly fewer tests because:
    // 1. Type system prevents null pointer exceptions - no need to test null scenarios
    // 2. Invalid state transitions impossible at compile time - no need to test invalid transitions
    // 3. Input validation handled by type constructors - fewer edge cases to test
    // 4. Immutability prevents accidental mutations - no need to test mutation scenarios
    // 5. Positive amounts guaranteed by PositiveBigDecimal type - no need for extensive amount validation

    // Tests NOT needed for safe approach (but required for traditional):
    // - Null value handling (impossible with type system)
    // - Invalid state transitions (PendingPayment -> PaidPayment is type-safe)
    // - Most edge cases (handled by smart constructors)
    // - Mutation scenarios (immutable data)
    // - Many boundary conditions (enforced by types)

    // The traditional approach test file has 15 test cases vs 6 here
    // This demonstrates the power of moving runtime invariants into the type system
})

/*
 * COMPARISON SUMMARY:
 *
 * Traditional Approach Tests (15 tests):
 * - Multiple validation error scenarios
 * - Edge cases for all input parameters
 * - Runtime state validation
 * - Null handling scenarios
 * - Boundary condition testing
 * - Exception handling verification
 * - Multiple error accumulation testing
 *
 * Safe Approach Tests (6 tests):
 * - Basic happy path validation
 * - Core business logic verification
 * - Type-level constraint validation
 * - Functional error handling verification
 *
 * Key Benefits Demonstrated:
 * - 60% fewer tests required (6 vs 15)
 * - No need for exception testing (Either handles errors functionally)
 * - No null pointer testing (type system prevents nulls)
 * - No invalid state transition testing (compile-time prevention)
 * - Focus on business logic rather than defensive programming
 */
