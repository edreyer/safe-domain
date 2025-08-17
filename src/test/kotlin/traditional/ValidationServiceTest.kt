package traditional

import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import java.time.YearMonth

class ValidationServiceTest : ShouldSpec({
    val validationService = ValidationService()

    context("validateNonEmptyString") {
        should("pass for valid non-empty string") {
            val errors = validationService.validateNonEmptyString("hello", "test field")
            errors.shouldBeEmpty()
        }

        should("fail for empty string") {
            val errors = validationService.validateNonEmptyString("", "test field")
            errors shouldHaveSize 1
            errors[0].message shouldBe "test field must be at least 1 characters long"
        }

        should("fail for string shorter than minimum length") {
            val errors = validationService.validateNonEmptyString("ab", "test field", 3)
            errors shouldHaveSize 1
            errors[0].message shouldBe "test field must be at least 3 characters long"
        }
    }

    context("validatePositiveNumber") {
        should("pass for positive integer") {
            val errors = validationService.validatePositiveNumber(5, "test number")
            errors.shouldBeEmpty()
        }

        should("fail for zero") {
            val errors = validationService.validatePositiveNumber(0, "test number")
            errors shouldHaveSize 1
            errors[0].message shouldBe "test number must be positive"
        }

        should("fail for negative number") {
            val errors = validationService.validatePositiveNumber(-5, "test number")
            errors shouldHaveSize 1
            errors[0].message shouldBe "test number must be positive"
        }

        should("work with different number types") {
            validationService.validatePositiveNumber(5L, "long").shouldBeEmpty()
            validationService.validatePositiveNumber(5.5f, "float").shouldBeEmpty()
            validationService.validatePositiveNumber(5.5, "double").shouldBeEmpty()

            validationService.validatePositiveNumber(-5L, "long") shouldHaveSize 1
            validationService.validatePositiveNumber(-5.5f, "float") shouldHaveSize 1
            validationService.validatePositiveNumber(-5.5, "double") shouldHaveSize 1
        }
    }

    context("validateDigitString") {
        should("pass for valid digit string") {
            val errors = validationService.validateDigitString("12345", "test digits")
            errors.shouldBeEmpty()
        }

        should("fail for string with non-digits") {
            val errors = validationService.validateDigitString("123a5", "test digits")
            errors shouldHaveSize 1
            errors[0].message shouldBe "test digits must contain only digits"
        }

        should("fail for string exceeding max length") {
            val errors = validationService.validateDigitString("12345", "test digits", 3)
            errors shouldHaveSize 1
            errors[0].message shouldBe "test digits must not exceed 3 characters"
        }

        should("pass multiple validations when length is within limit") {
            val errors = validationService.validateDigitString("123", "test digits", 5)
            errors.shouldBeEmpty()
        }
    }

    context("validateCreditCardNumber") {
        should("pass for valid credit card number") {
            // Valid Luhn number
            val errors = validationService.validateCreditCardNumber("4532015112830366", "card number")
            errors.shouldBeEmpty()
        }

        should("fail for invalid Luhn number") {
            val errors = validationService.validateCreditCardNumber("4532015112830367", "card number")
            errors shouldHaveSize 1
            errors[0].shouldBeInstanceOf<InvalidCardNumberError>()
            errors[0].message shouldBe "card number failed MOD10 check"
        }

        should("fail for non-digit characters") {
            val errors = validationService.validateCreditCardNumber("4532-0151-1283-0366", "card number")
            errors shouldHaveSize 1
            errors[0].message shouldBe "card number must contain only digits"
        }

        should("fail for too long number") {
            val errors = validationService.validateCreditCardNumber("45320151128303661234", "card number", 19)
            errors shouldHaveSize 1
            errors[0].message shouldBe "card number must not exceed 19 characters"
        }
    }

    context("validateCvv") {
        should("pass for valid 3-digit CVV") {
            val errors = validationService.validateCvv("123", "CVV")
            errors.shouldBeEmpty()
        }

        should("pass for valid 4-digit CVV") {
            val errors = validationService.validateCvv("1234", "CVV")
            errors.shouldBeEmpty()
        }

        should("fail for 2-digit CVV") {
            val errors = validationService.validateCvv("12", "CVV")
            errors shouldHaveSize 1
            errors[0].shouldBeInstanceOf<InvalidCvvError>()
            errors[0].message shouldBe "CVV must be 3 or 4 digits"
        }

        should("fail for 5-digit CVV") {
            val errors = validationService.validateCvv("12345", "CVV")
            errors shouldHaveSize 2  // Both length validation and digit validation
        }

        should("fail for non-digit CVV") {
            val errors = validationService.validateCvv("12a", "CVV")
            errors shouldHaveSize 1
            errors[0].message shouldBe "CVV must contain only digits"
        }
    }

    context("validateExpiryDate") {
        val now = YearMonth.of(2024, 8)

        should("pass for valid future date") {
            val errors = validationService.validateExpiryDate(12, 2025, "expiry date", now)
            errors.shouldBeEmpty()
        }

        should("fail for past date") {
            val errors = validationService.validateExpiryDate(7, 2024, "expiry date", now)
            errors shouldHaveSize 1
            errors[0].shouldBeInstanceOf<InvalidExpiryDateError>()
            errors[0].message shouldBe "expiry date cannot be in the past"
        }

        should("fail for invalid month") {
            val errors = validationService.validateExpiryDate(13, 2025, "expiry date", now)
            errors shouldHaveSize 1
            errors[0].shouldBeInstanceOf<InvalidExpiryDateError>()
            errors[0].message shouldBe "expiry date month must be between 1 and 12"
        }

        should("fail for month 0") {
            val errors = validationService.validateExpiryDate(0, 2025, "expiry date", now)
            errors shouldHaveSize 1
            errors[0].shouldBeInstanceOf<InvalidExpiryDateError>()
            errors[0].message shouldBe "expiry date month must be between 1 and 12"
        }
    }

    context("validateRoutingNumber") {
        should("pass for valid routing number") {
            // Valid ABA routing number
            val errors = validationService.validateRoutingNumber("021000021", "routing number")
            errors.shouldBeEmpty()
        }

        should("fail for invalid length") {
            val errors = validationService.validateRoutingNumber("12345678", "routing number")
            errors shouldHaveSize 1
            errors[0].shouldBeInstanceOf<InvalidRoutingNumberError>()
            errors[0].message shouldBe "routing number must be exactly 9 digits"
        }

        should("fail for non-digits") {
            val errors = validationService.validateRoutingNumber("02100002a", "routing number")
            errors shouldHaveSize 1
            errors[0].message shouldBe "routing number must contain only digits"
        }

        should("fail for invalid ABA checksum") {
            val errors = validationService.validateRoutingNumber("021000022", "routing number")
            errors shouldHaveSize 1
            errors[0].shouldBeInstanceOf<InvalidRoutingNumberError>()
            errors[0].message shouldBe "routing number failed ABA checksum validation"
        }
    }

    context("validateAccountNumber") {
        should("pass for valid account number") {
            val errors = validationService.validateAccountNumber("12345678", "account number")
            errors.shouldBeEmpty()
        }

        should("fail for empty account number") {
            val errors = validationService.validateAccountNumber("", "account number")
            errors shouldHaveSize 1
            errors[0].message shouldBe "account number must be at least 1 characters long"
        }

        should("fail for non-digit account number") {
            val errors = validationService.validateAccountNumber("1234abc", "account number")
            errors shouldHaveSize 1
            errors[0].message shouldBe "account number must contain only digits"
        }
    }

    context("validateStrongPassword") {
        should("pass for strong password") {
            val errors = validationService.validateStrongPassword("MyStr0ngP@ssw0rd!", "password")
            errors.shouldBeEmpty()
        }

        should("fail for too short password") {
            val errors = validationService.validateStrongPassword("Short1!", "password", 12)
            errors shouldHaveSize 1
            errors[0].message shouldBe "password must be at least 12 characters long"
        }

        should("fail for missing uppercase") {
            val errors = validationService.validateStrongPassword("mystr0ngp@ssw0rd!", "password")
            errors shouldHaveSize 1
            errors[0].message shouldBe "password must contain at least one uppercase letter"
        }

        should("fail for missing lowercase") {
            val errors = validationService.validateStrongPassword("MYSTR0NGP@SSW0RD!", "password")
            errors shouldHaveSize 1
            errors[0].message shouldBe "password must contain at least one lowercase letter"
        }

        should("fail for missing digit") {
            val errors = validationService.validateStrongPassword("MyStrongP@ssword!", "password")
            errors shouldHaveSize 1
            errors[0].message shouldBe "password must contain at least one digit"
        }

        should("fail for missing symbol") {
            val errors = validationService.validateStrongPassword("MyStr0ngPassw0rd", "password")
            errors shouldHaveSize 1
            errors[0].message shouldBe "password must contain at least one symbol"
        }

        should("accumulate multiple validation errors") {
            val errors = validationService.validateStrongPassword("weak", "password")
            errors shouldHaveSize 4 // too short, missing upper, missing digit, missing symbol
        }

        should("respect individual requirements flags") {
            val errors = validationService.validateStrongPassword(
                "weakpassword", "password", 12,
                requireUpper = false,
                requireLower = true,
                requireDigit = false,
                requireSymbol = false
            )
            errors.shouldBeEmpty()
        }
    }

    context("validateEmail") {
        should("pass for valid email") {
            val errors = validationService.validateEmail("test@example.com", "email")
            errors.shouldBeEmpty()
        }

        should("pass for email with plus sign") {
            val errors = validationService.validateEmail("test+tag@example.com", "email")
            errors.shouldBeEmpty()
        }

        should("fail for invalid email format") {
            val errors = validationService.validateEmail("invalid-email", "email")
            errors shouldHaveSize 1
            errors[0].message shouldBe "email must be a valid email address"
        }

        should("fail for email without domain") {
            val errors = validationService.validateEmail("test@", "email")
            errors shouldHaveSize 1
            errors[0].message shouldBe "email must be a valid email address"
        }
    }
})
