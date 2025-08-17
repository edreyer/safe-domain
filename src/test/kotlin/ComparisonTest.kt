import arrow.core.raise.either
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import safe.*
import traditional.*

/**
 * Comparison tests to demonstrate the differences between safe and traditional approaches.
 * This test class shows how the safe types reduce the number of tests needed compared to traditional validation.
 */
class ComparisonTest : ShouldSpec({

    context("Safe vs Traditional - Credit Card Creation") {
        val safeBusinessLogic = safe.CreditCard
        val traditionalBusinessLogic = TraditionalBusinessLogic()

        should("Safe approach - single test for successful creation") {
            // Safe approach: Type system guarantees validity at compile time
            // Only need to test successful creation path
            val result = either {
                safe.CreditCard.create("4532015112830366", 12, 2025, "123")
            }
            result.isRight() shouldBe true
        }

        should("Traditional approach - requires more tests for same functionality") {
            // Traditional approach: Need to test all validation scenarios explicitly
            // because types don't guarantee validity

            // Test 1: Valid creation
            val validCard = traditionalBusinessLogic.createCreditCard("4532015112830366", 12, 2025, "123")
            validCard.number shouldBe "4532015112830366"

            // Test 2: Invalid card number
            shouldThrow<PaymentValidationException> {
                traditionalBusinessLogic.createCreditCard("invalid", 12, 2025, "123")
            }

            // Test 3: Invalid month
            shouldThrow<PaymentValidationException> {
                traditionalBusinessLogic.createCreditCard("4532015112830366", 13, 2025, "123")
            }

            // Test 4: Invalid CVV
            shouldThrow<PaymentValidationException> {
                traditionalBusinessLogic.createCreditCard("4532015112830366", 12, 2025, "12")
            }

            // Additional tests needed for boundary conditions, null safety, etc.
        }
    }

    context("Safe vs Traditional - ProcessPayment Method") {
        should("Safe approach - leverages type safety") {
            // Safe approach: Once created, CreditCard is guaranteed to be valid
            // processPayment can focus on business logic, not validation
            val result = either {
                safe.CreditCard.create("4532015112830366", 12, 2025, "123")
                    .let { safe.processCard(it) }
            }
            result.isRight() shouldBe true
        }

        should("Traditional approach - must validate at every step") {
            // Traditional approach: Must validate input parameters explicitly
            // More error-prone as validation can be forgotten
            val businessLogic = TraditionalBusinessLogic()
            val result = businessLogic.processPayment("4532015112830366", 12, 2025, "123")
            result.shouldBeInstanceOf<traditional.CreditCard>()
        }
    }

    context("Type Safety Comparison") {
        should("Safe types prevent invalid states at compile time") {
            // This won't compile if we try to create invalid safe types directly:
            // val invalidCard = CreditCard(MOD10String("invalid"), ...) // Compile error

            // Safe types must go through validation
            val cardResult = safe.CreditCard.createValidated("4532015112830366", 12, 2025, "123")
            cardResult.isRight() shouldBe true
        }

        should("Traditional types allow invalid states") {
            // Traditional approach allows creating invalid objects
            val invalidCard = traditional.CreditCard("invalid", 13, 2020, "1")
            // Object is created but invalid - validation must happen elsewhere
            invalidCard.number shouldBe "invalid"
            invalidCard.expiryMonth shouldBe 13

            // Validation only happens when explicitly called
            val validationService = ValidationService()
            val errors = validationService.validateCreditCardNumber(invalidCard.number)
            errors.size shouldBe 1 // Contains validation error
        }
    }

    context("Error Handling Comparison") {
        should("Safe approach - functional error handling") {
            // Safe approach uses Either for explicit error handling
            val result = safe.CreditCard.createValidated("invalid", 12, 2025, "123")
            result.isLeft() shouldBe true
            result.fold(
                { errors -> errors.size shouldBe 1 },
                { _ -> throw AssertionError("Should have failed") }
            )
        }

        should("Traditional approach - exception-based error handling") {
            // Traditional approach uses exceptions
            val businessLogic = TraditionalBusinessLogic()
            val exception = shouldThrow<PaymentValidationException> {
                businessLogic.createCreditCard("invalid", 12, 2025, "123")
            }
            exception.errors.size shouldBe 1
        }
    }

    context("Test Coverage Analysis") {
        should("Safe approach requires fewer edge case tests") {
            // Safe types eliminate entire classes of bugs:
            // - No null pointer exceptions (NonEmptyString prevents empty values)
            // - No invalid state bugs (MOD10String ensures valid card numbers)
            // - No type confusion (strong typing prevents mixing up parameters)

            // Example: Testing CreditCard creation covers all validation in one place
            val validCard = safe.CreditCard.createValidated("4532015112830366", 12, 2025, "123")
            validCard.isRight() shouldBe true

            val invalidCard = safe.CreditCard.createValidated("invalid", 12, 2025, "123")
            invalidCard.isLeft() shouldBe true
        }

        should("Traditional approach requires extensive validation tests") {
            // Traditional approach needs tests for:
            // - All validation scenarios
            // - Null safety
            // - Type safety
            // - Edge cases
            // - Invalid state handling

            val validationService = ValidationService()

            // Test valid input
            validationService.validateCreditCardNumber("4532015112830366").size shouldBe 0

            // Test invalid input
            validationService.validateCreditCardNumber("invalid").size shouldBe 1

            // Test null safety (if applicable)
            // Test edge cases
            validationService.validateCreditCardNumber("").size shouldBe 1

            // Test boundary conditions
            validationService.validateCvv("12").size shouldBe 1
            validationService.validateCvv("123").size shouldBe 0
            validationService.validateCvv("1234").size shouldBe 0
            validationService.validateCvv("12345").size shouldBe 2
        }
    }

    context("Maintainability Comparison") {
        should("Safe approach - validation logic centralized in types") {
            // Validation logic lives with the type definition
            // Changes to validation rules only need to be made in one place
            val cardValidation = safe.CreditCard.createValidated("4532015112830366", 12, 2025, "123")
            cardValidation.isRight() shouldBe true
        }

        should("Traditional approach - validation logic scattered") {
            // Validation logic lives in service classes
            // Must remember to call validation everywhere it's needed
            // Risk of inconsistent validation across different parts of the application
            val validationService = ValidationService()
            val businessLogic = TraditionalBusinessLogic(validationService)

            // Validation can be bypassed by creating objects directly
            val bypassedValidation = traditional.CreditCard("invalid", 13, 2020, "1")
            bypassedValidation.number shouldBe "invalid" // Invalid but allowed
        }
    }

    context("Performance Comparison") {
        should("Safe approach - validation happens once at creation") {
            // Once a safe type is created, no further validation is needed
            val cardResult = safe.CreditCard.createValidated("4532015112830366", 12, 2025, "123")
            cardResult.fold(
                { _ -> throw AssertionError("Should have succeeded") },
                { card ->
                    // card is guaranteed to be valid - no need to re-validate
                    card.number.value shouldBe "4532015112830366"
                }
            )
        }

        should("Traditional approach - validation may happen multiple times") {
            // Traditional approach may require repeated validation
            val validationService = ValidationService()

            // Validation at creation
            val errors1 = validationService.validateCreditCardNumber("4532015112830366")
            errors1.size shouldBe 0

            // May need validation again before processing
            val card = traditional.CreditCard("4532015112830366", 12, 2025, "123")
            val errors2 = validationService.validateCreditCardNumber(card.number)
            errors2.size shouldBe 0

            // Validation logic is duplicated
        }
    }
})
