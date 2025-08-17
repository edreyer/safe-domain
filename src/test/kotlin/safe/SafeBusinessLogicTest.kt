package safe

import arrow.core.raise.either
import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.types.shouldBeInstanceOf
import java.time.YearMonth

class SafeBusinessLogicTest : ShouldSpec({

    context("processCard") {
        should("return the same CreditCard instance") {
            val creditCard = either {
                CreditCard.create("4532015112830366", 12, 2025, "123")
            }.getOrNull()!!

            val result = processCard(creditCard)
            result shouldBe creditCard
        }
    }

    context("processPayment") {
        should("process valid payment successfully") {
            val result = either {
                processPayment("4532015112830366", 12, 2025, "123")
            }

            result.isRight() shouldBe true
            result.fold({}, { paymentMethod ->
                paymentMethod.shouldBeInstanceOf<CreditCard>()
                val creditCard = paymentMethod as CreditCard
                creditCard.number.value shouldBe "4532015112830366"
                creditCard.expiryDate.value shouldBe YearMonth.of(2025, 12)
                creditCard.cvv.value shouldBe "123"
            })
        }

        should("process payment with spaces in card number") {
            val result = either {
                processPayment("4532 0151 1283 0366", 12, 2025, "123")
            }

            result.isRight() shouldBe true
            result.fold({}, { paymentMethod ->
                paymentMethod.shouldBeInstanceOf<CreditCard>()
                val creditCard = paymentMethod as CreditCard
                creditCard.number.value shouldBe "4532015112830366"
            })
        }

        should("fail for invalid card number and return formatted error") {
            val result = either {
                processPayment("4532015112830367", 12, 2025, "123") // invalid Luhn
            }

            result.isLeft() shouldBe true
            result.fold(
                { error ->
                    error.shouldBeInstanceOf<DomainError>()
                    error.err shouldContain "card number failed MOD10 (Luhn) checksum"
                    error.err shouldContain "[\n"
                    error.err shouldContain "]\n"
                },
                { }
            )
        }

        should("fail for invalid expiry month and return formatted error") {
            val result = either {
                processPayment("4532015112830366", 13, 2025, "123") // invalid month
            }

            result.isLeft() shouldBe true
            result.fold(
                { error ->
                    error.shouldBeInstanceOf<DomainError>()
                    error.err shouldContain "expiry date month must be between 1 and 12 (was 13)"
                    error.err shouldContain "[\n"
                    error.err shouldContain "]\n"
                },
                { }
            )
        }

        should("fail for invalid CVV and return formatted error") {
            val result = either {
                processPayment("4532015112830366", 12, 2025, "12a") // invalid CVV
            }

            result.isLeft() shouldBe true
            result.fold(
                { error ->
                    error.shouldBeInstanceOf<DomainError>()
                    error.err shouldContain "CVV must contain only digits (spaces allowed)"
                    error.err shouldContain "[\n"
                    error.err shouldContain "]\n"
                },
                { }
            )
        }

        should("fail for multiple validation errors") {
            val result = either {
                processPayment("4532015112830367", 13, 2025, "12a") // multiple errors
            }

            result.isLeft() shouldBe true
            result.fold(
                { error ->
                    error.shouldBeInstanceOf<DomainError>()
                },
                { }
            )
        }

        should("fail for past expiry date") {
            val result = either {
                processPayment("4532015112830366", 7, 2024, "123") // past expiry
            }

            result.isLeft() shouldBe true
            result.fold(
                { error ->
                    error.shouldBeInstanceOf<DomainError>()
                    error.err shouldContain "expiry date must not be in the past"
                    error.err shouldContain "[\n"
                    error.err shouldContain "]\n"
                },
                { }
            )
        }

        should("fail for empty card number") {
            val result = either {
                processPayment("", 12, 2025, "123") // empty card number
            }

            result.isLeft() shouldBe true
            result.fold(
                { error ->
                    error.shouldBeInstanceOf<DomainError>()
                    error.err shouldContain "card number failed MOD10 (Luhn) checksum"
                    error.err shouldContain "[\n"
                    error.err shouldContain "]\n"
                },
                { }
            )
        }

        should("fail for empty CVV") {
            val result = either {
                processPayment("4532015112830366", 12, 2025, "") // empty CVV
            }

            result.isLeft() shouldBe true
            result.fold(
                { error ->
                    error.shouldBeInstanceOf<DomainError>()
                    error.err shouldContain "CVV must contain only digits (spaces allowed)"
                    error.err shouldContain "[\n"
                    error.err shouldContain "]\n"
                },
                { }
            )
        }
    }

    context("DomainError") {
        should("create domain error with message") {
            val domainError = DomainError("test error message")
            domainError.err shouldBe "test error message"
        }
    }
})
