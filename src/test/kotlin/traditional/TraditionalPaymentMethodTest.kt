package traditional

import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf

class TraditionalPaymentMethodTest : ShouldSpec({

    context("PaymentMethod sealed class") {
        should("allow Cash, CreditCard, and Check as implementations") {
            val cash: PaymentMethod = Cash
            val creditCard: PaymentMethod = CreditCard("4532015112830366", 12, 2025, "123")
            val check: PaymentMethod = Check("021000021", "123456789")

            cash.shouldBeInstanceOf<Cash>()
            creditCard.shouldBeInstanceOf<CreditCard>()
            check.shouldBeInstanceOf<Check>()
        }
    }

    context("Cash") {
        should("be a singleton object") {
            val cash1 = Cash
            val cash2 = Cash
            cash1 shouldBe cash2
        }

        should("be instance of PaymentMethod") {
            Cash.shouldBeInstanceOf<PaymentMethod>()
        }
    }

    context("CreditCard") {
        should("create credit card with all properties") {
            val creditCard = CreditCard("4532015112830366", 12, 2025, "123")
            creditCard.number shouldBe "4532015112830366"
            creditCard.expiryMonth shouldBe 12
            creditCard.expiryYear shouldBe 2025
            creditCard.cvv shouldBe "123"
        }

        should("implement equals correctly") {
            val card1 = CreditCard("4532015112830366", 12, 2025, "123")
            val card2 = CreditCard("4532015112830366", 12, 2025, "123")
            val card3 = CreditCard("4532015112830367", 12, 2025, "123")
            val card4 = CreditCard("4532015112830366", 11, 2025, "123")
            val card5 = CreditCard("4532015112830366", 12, 2024, "123")
            val card6 = CreditCard("4532015112830366", 12, 2025, "124")

            card1 shouldBe card2
            card1 shouldNotBe card3
            card1 shouldNotBe card4
            card1 shouldNotBe card5
            card1 shouldNotBe card6
        }

        should("implement equals correctly with same object reference") {
            val card = CreditCard("4532015112830366", 12, 2025, "123")
            card shouldBe card
        }

        should("implement equals correctly with null") {
            val card = CreditCard("4532015112830366", 12, 2025, "123")
            (card == null) shouldBe false
        }

        should("implement equals correctly with different class") {
            val card = CreditCard("4532015112830366", 12, 2025, "123")
            val check = Check("021000021", "123456789")
            card shouldNotBe check
        }

        should("implement hashCode correctly") {
            val card1 = CreditCard("4532015112830366", 12, 2025, "123")
            val card2 = CreditCard("4532015112830366", 12, 2025, "123")
            val card3 = CreditCard("4532015112830367", 12, 2025, "123")

            card1.hashCode() shouldBe card2.hashCode()
            card1.hashCode() shouldNotBe card3.hashCode()
        }

    }

    context("Check") {
        should("create check with all properties") {
            val check = Check("021000021", "123456789")
            check.routingNumber shouldBe "021000021"
            check.accountNumber shouldBe "123456789"
        }

        should("implement equals correctly") {
            val check1 = Check("021000021", "123456789")
            val check2 = Check("021000021", "123456789")
            val check3 = Check("021000022", "123456789")
            val check4 = Check("021000021", "123456790")

            check1 shouldBe check2
            check1 shouldNotBe check3
            check1 shouldNotBe check4
        }

        should("implement equals correctly with same object reference") {
            val check = Check("021000021", "123456789")
            check shouldBe check
        }

        should("implement equals correctly with null") {
            val check = Check("021000021", "123456789")
            (check == null) shouldBe false
        }

        should("implement equals correctly with different class") {
            val check = Check("021000021", "123456789")
            val card = CreditCard("4532015112830366", 12, 2025, "123")
            check shouldNotBe card
        }

        should("implement hashCode correctly") {
            val check1 = Check("021000021", "123456789")
            val check2 = Check("021000021", "123456789")
            val check3 = Check("021000022", "123456789")

            check1.hashCode() shouldBe check2.hashCode()
            check1.hashCode() shouldNotBe check3.hashCode()
        }

    }

    context("ValidationError classes") {
        should("create ValidationError with message") {
            val error = ValidationError("test error message")
            error.message shouldBe "test error message"
        }

        should("create InvalidCardNumberError with message") {
            val error = InvalidCardNumberError("card number is invalid")
            error.message shouldBe "card number is invalid"
            error.shouldBeInstanceOf<ValidationError>()
        }

        should("create InvalidExpiryDateError with message") {
            val error = InvalidExpiryDateError("expiry date is invalid")
            error.message shouldBe "expiry date is invalid"
            error.shouldBeInstanceOf<ValidationError>()
        }

        should("create InvalidCvvError with message") {
            val error = InvalidCvvError("CVV is invalid")
            error.message shouldBe "CVV is invalid"
            error.shouldBeInstanceOf<ValidationError>()
        }

        should("create InvalidRoutingNumberError with message") {
            val error = InvalidRoutingNumberError("routing number is invalid")
            error.message shouldBe "routing number is invalid"
            error.shouldBeInstanceOf<ValidationError>()
        }

        should("create InvalidAccountNumberError with message") {
            val error = InvalidAccountNumberError("account number is invalid")
            error.message shouldBe "account number is invalid"
            error.shouldBeInstanceOf<ValidationError>()
        }
    }

    context("Edge cases and boundary conditions") {
        should("handle empty strings in credit card") {
            val card = CreditCard("", 0, 0, "")
            card.number shouldBe ""
            card.expiryMonth shouldBe 0
            card.expiryYear shouldBe 0
            card.cvv shouldBe ""
        }

        should("handle empty strings in check") {
            val check = Check("", "")
            check.routingNumber shouldBe ""
            check.accountNumber shouldBe ""
        }

        should("handle special characters in credit card fields") {
            val card = CreditCard("4532-0151-1283-0366", 12, 2025, "abc")
            card.number shouldBe "4532-0151-1283-0366"
            card.cvv shouldBe "abc"
        }

        should("handle very long values") {
            val longNumber = "1234567890".repeat(10)
            val card = CreditCard(longNumber, 12, 2025, "123")
            card.number shouldBe longNumber

            val longAccount = "9876543210".repeat(10)
            val check = Check("021000021", longAccount)
            check.accountNumber shouldBe longAccount
        }
    }
})
