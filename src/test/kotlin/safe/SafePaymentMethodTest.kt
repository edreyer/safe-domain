package safe

import arrow.core.raise.either
import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import java.time.YearMonth

class SafePaymentMethodTest : ShouldSpec({

    context("Cash") {
        should("be a PaymentMethod") {
            Cash.shouldBeInstanceOf<PaymentMethod>()
        }
    }

    context("CreditCard") {
        should("create valid credit card with valid inputs") {
            val result = CreditCard.createValidated(
                "4532015112830366",
                12,
                2025,
                "123"
            )
            result.isRight() shouldBe true
            result.fold({}, { creditCard ->
                creditCard.number.value shouldBe "4532015112830366"
                creditCard.expiryDate.value shouldBe YearMonth.of(2025, 12)
                creditCard.cvv.value shouldBe "123"
            })
        }

        should("create credit card with spaces in card number") {
            val result = CreditCard.createValidated(
                "4532 0151 1283 0366",
                12,
                2025,
                "123"
            )
            result.isRight() shouldBe true
            result.fold({}, { creditCard ->
                creditCard.number.value shouldBe "4532015112830366"
            })
        }

        should("fail for invalid card number") {
            val result = CreditCard.createValidated(
                "4532015112830367", // invalid Luhn
                12,
                2025,
                "123"
            )
            result.isLeft() shouldBe true
            result.fold(
                { errors ->
                    errors shouldHaveSize 1
                    errors[0].shouldBeInstanceOf<MOD10String.Mod10CheckFailed>()
                    errors[0].message shouldBe "card number failed MOD10 (Luhn) checksum"
                },
                { }
            )
        }

        should("fail for invalid expiry month") {
            val result = CreditCard.createValidated(
                "4532015112830366",
                13, // invalid month
                2025,
                "123"
            )
            result.isLeft() shouldBe true
        }

        should("fail for invalid CVV") {
            val result = CreditCard.createValidated(
                "4532015112830366",
                12,
                2025,
                "12a" // non-digit CVV
            )
            result.isLeft() shouldBe true
        }

        should("fail for CVV exceeding max length") {
            val result = CreditCard.createValidated(
                "4532015112830366",
                12,
                2025,
                "12345" // CVV too long
            )
            result.isLeft() shouldBe true
        }

        should("fail for multiple validation errors") {
            val result = CreditCard.createValidated(
                "4532015112830367", // invalid Luhn
                13, // invalid month
                2025,
                "12a" // invalid CVV
            )
            result.isLeft() shouldBe true
        }

        should("work with context receiver") {
            val result = either {
                CreditCard.create("4532015112830366", 12, 2025, "123")
            }
            result.isRight() shouldBe true
            result.fold({}, { creditCard ->
                creditCard.number.value shouldBe "4532015112830366"
                creditCard.expiryDate.value shouldBe YearMonth.of(2025, 12)
                creditCard.cvv.value shouldBe "123"
            })
        }
    }

    context("Check") {
        should("create valid check with valid inputs") {
            val result = Check.createValidated(
                "021000021", // valid routing number
                "123456789" // valid account number
            )
            result.isRight() shouldBe true
            result.fold({}, { check ->
                check.bankRoutingNumber.value shouldBe "021000021"
                check.accountNumber.value shouldBe "123456789"
            })
        }

        should("create check with spaces in routing number") {
            val result = Check.createValidated(
                "021 000 021",
                "123456789"
            )
            result.isRight() shouldBe true
            result.fold({}, { check ->
                check.bankRoutingNumber.value shouldBe "021000021"
            })
        }

        should("create check with spaces in account number") {
            val result = Check.createValidated(
                "021000021",
                "123 456 789"
            )
            result.isRight() shouldBe true
            result.fold({}, { check ->
                check.accountNumber.value shouldBe "123456789"
            })
        }

        should("fail for invalid routing number checksum") {
            val result = Check.createValidated(
                "021000022", // invalid ABA checksum
                "123456789"
            )
            result.isLeft() shouldBe true
            result.fold(
                { errors ->
                    errors shouldHaveSize 1
                    errors[0].shouldBeInstanceOf<RoutingString.ChecksumFailed>()
                    errors[0].message shouldBe "routing number failed ABA routing checksum"
                },
                { }
            )
        }

        should("fail for routing number with wrong length") {
            val result = Check.createValidated(
                "12345678", // too short
                "123456789"
            )
            result.isLeft() shouldBe true
            result.fold(
                { errors ->
                    errors shouldHaveSize 1
                    errors[0].shouldBeInstanceOf<RoutingString.LengthError>()
                    errors[0].message shouldBe "routing number must be exactly 9 digits long"
                },
                { }
            )
        }

        should("fail for non-digit account number") {
            val result = Check.createValidated(
                "021000021",
                "123a56789" // contains letter
            )
            result.isLeft() shouldBe true
            result.fold(
                { errors ->
                    errors shouldHaveSize 1
                    errors[0].shouldBeInstanceOf<DigitString.NonDigitError>()
                    errors[0].message shouldBe "account number must contain only digits (spaces allowed)"
                },
                { }
            )
        }

        should("fail for empty account number") {
            val result = Check.createValidated(
                "021000021",
                "" // empty account number
            )
            result.isLeft() shouldBe true
            result.fold(
                { errors ->
                    errors shouldHaveSize 1
                    errors[0].shouldBeInstanceOf<DigitString.NonDigitError>()
                    errors[0].message shouldBe "account number must contain only digits (spaces allowed)"
                },
                { }
            )
        }

        should("fail for multiple validation errors") {
            val result = Check.createValidated(
                "12345678", // invalid routing (length)
                "123a56" // invalid account (non-digit)
            )
            result.isLeft() shouldBe true
        }

        should("work with context receiver") {
            val result = either {
                Check.create("021000021", "123456789")
            }
            result.isRight() shouldBe true
            result.fold({}, { check ->
                check.bankRoutingNumber.value shouldBe "021000021"
                check.accountNumber.value shouldBe "123456789"
            })
        }
    }
})
