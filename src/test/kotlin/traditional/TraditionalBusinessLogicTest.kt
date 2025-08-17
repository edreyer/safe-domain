package traditional

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import java.time.YearMonth

class TraditionalBusinessLogicTest : ShouldSpec({
    val businessLogic = TraditionalBusinessLogic()

    context("processPayment") {
        should("succeed with valid credit card data") {
            val result = businessLogic.processPayment("4532015112830366", 12, 2025, "123")
            result.shouldBeInstanceOf<CreditCard>()
            val creditCard = result as CreditCard
            creditCard.number shouldBe "4532015112830366"
            creditCard.expiryMonth shouldBe 12
            creditCard.expiryYear shouldBe 2025
            creditCard.cvv shouldBe "123"
        }

        should("throw PaymentValidationException for invalid card number") {
            val exception = shouldThrow<PaymentValidationException> {
                businessLogic.processPayment("4532015112830367", 12, 2025, "123")
            }
            exception.errors shouldHaveSize 1
            exception.errors[0].shouldBeInstanceOf<InvalidCardNumberError>()
        }

        should("throw PaymentValidationException for invalid expiry date") {
            val exception = shouldThrow<PaymentValidationException> {
                businessLogic.processPayment("4532015112830366", 13, 2025, "123")
            }
            exception.errors shouldHaveSize 1
            exception.errors[0].shouldBeInstanceOf<InvalidExpiryDateError>()
        }

        should("throw PaymentValidationException for invalid CVV") {
            val exception = shouldThrow<PaymentValidationException> {
                businessLogic.processPayment("4532015112830366", 12, 2025, "12")
            }
            exception.errors shouldHaveSize 1
            exception.errors[0].shouldBeInstanceOf<InvalidCvvError>()
        }

        should("accumulate multiple validation errors") {
            val exception = shouldThrow<PaymentValidationException> {
                businessLogic.processPayment("invalid", 13, 2025, "1")
            }
            exception.errors.size shouldBe 3 // invalid card number, invalid month, invalid CVV
            exception.errors.any { it is InvalidCardNumberError } shouldBe false // Will be general validation error
            exception.errors.any { it is InvalidExpiryDateError } shouldBe true
            exception.errors.any { it is InvalidCvvError } shouldBe true
        }

        should("throw for past expiry date") {
            val exception = shouldThrow<PaymentValidationException> {
                businessLogic.processPayment("4532015112830366", 1, 2020, "123")
            }
            exception.errors shouldHaveSize 1
            exception.errors[0].shouldBeInstanceOf<InvalidExpiryDateError>()
        }

        should("throw for non-digit card number") {
            val exception = shouldThrow<PaymentValidationException> {
                businessLogic.processPayment("4532-0151-1283-0366", 12, 2025, "123")
            }
            exception.errors shouldHaveSize 1
            exception.errors[0].message shouldBe "card number must contain only digits"
        }

        should("throw for non-digit CVV") {
            val exception = shouldThrow<PaymentValidationException> {
                businessLogic.processPayment("4532015112830366", 12, 2025, "12a")
            }
            exception.errors shouldHaveSize 1
            exception.errors[0].message shouldBe "CVV must contain only digits"
        }
    }

    context("processPaymentSafe") {
        should("return success for valid credit card data") {
            val result = businessLogic.processPaymentSafe("4532015112830366", 12, 2025, "123")
            result.isSuccess shouldBe true
            result.getOrNull().shouldBeInstanceOf<CreditCard>()
        }

        should("return failure for invalid data") {
            val result = businessLogic.processPaymentSafe("invalid", 12, 2025, "123")
            result.isFailure shouldBe true
            result.exceptionOrNull().shouldBeInstanceOf<PaymentValidationException>()
        }
    }

    context("createCreditCard") {
        should("create valid credit card") {
            val creditCard = businessLogic.createCreditCard("4532015112830366", 12, 2025, "123")
            creditCard.number shouldBe "4532015112830366"
            creditCard.expiryMonth shouldBe 12
            creditCard.expiryYear shouldBe 2025
            creditCard.cvv shouldBe "123"
        }

        should("throw for invalid data") {
            shouldThrow<PaymentValidationException> {
                businessLogic.createCreditCard("invalid", 12, 2025, "123")
            }
        }

        should("validate all parameters") {
            val exception = shouldThrow<PaymentValidationException> {
                businessLogic.createCreditCard("4532015112830367", 13, 2020, "12")
            }
            exception.errors.size shouldBe 3 // invalid Luhn, invalid month, invalid CVV length + past date
        }
    }

    context("processCard") {
        should("return the same card (placeholder implementation)") {
            val inputCard = CreditCard("4532015112830366", 12, 2025, "123")
            val result = businessLogic.processCard(inputCard)
            result shouldBe inputCard
        }
    }

    context("error message formatting") {
        should("format multiple errors in exception message") {
            val exception = shouldThrow<PaymentValidationException> {
                businessLogic.processPayment("invalid", 13, 2025, "1")
            }
            exception.message?.contains("Payment validation failed:") shouldBe true
        }

        should("include all error messages") {
            val exception = shouldThrow<PaymentValidationException> {
                businessLogic.createCreditCard("123", 0, 2020, "")
            }
            // Should have multiple validation errors
            exception.errors.size shouldBe 4 // card number invalid, month invalid, past date, empty CVV
        }
    }

    context("edge cases") {
        should("handle minimum valid values") {
            // Test with minimum valid CVV length
            val result = businessLogic.processPayment("4532015112830366", 12, 2025, "123")
            result.shouldBeInstanceOf<CreditCard>()
        }

        should("handle maximum valid values") {
            // Test with maximum valid CVV length
            val result = businessLogic.processPayment("4532015112830366", 12, 2025, "1234")
            result.shouldBeInstanceOf<CreditCard>()
        }

        should("reject boundary invalid values") {
            // Month 0
            shouldThrow<PaymentValidationException> {
                businessLogic.processPayment("4532015112830366", 0, 2025, "123")
            }

            // Month 13
            shouldThrow<PaymentValidationException> {
                businessLogic.processPayment("4532015112830366", 13, 2025, "123")
            }

            // CVV too short
            shouldThrow<PaymentValidationException> {
                businessLogic.processPayment("4532015112830366", 12, 2025, "12")
            }

            // CVV too long
            shouldThrow<PaymentValidationException> {
                businessLogic.processPayment("4532015112830366", 12, 2025, "12345")
            }
        }
    }
})
