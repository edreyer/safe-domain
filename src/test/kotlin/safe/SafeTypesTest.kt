package safe

import arrow.core.raise.either
import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import safe.DigitString.Companion.toDigitString
import safe.DigitString.Companion.toDigitStringValidated
import safe.ExpiryDate.Companion.toExpiryDateValidated
import safe.MOD10String.Companion.toMOD10String
import safe.MOD10String.Companion.toMOD10StringValidated
import safe.NonEmptyString.Companion.toNonEmptyString
import safe.NonEmptyString.Companion.toNonEmptyStringValidated
import safe.PositiveNumber.Companion.toPositiveNumber
import safe.PositiveNumber.Companion.toPositiveNumberValidated
import safe.RoutingString.Companion.toRoutingString
import safe.RoutingString.Companion.toRoutingStringValidated
import java.time.LocalDate
import java.time.YearMonth

class SafeTypesTest : ShouldSpec({

    context("NonEmptyString") {
        should("create valid non-empty string") {
            val result = "hello".toNonEmptyStringValidated("test")
            result.isRight() shouldBe true
            result.fold({}, { it.value shouldBe "hello" })
        }

        should("fail for empty string") {
            val result = "".toNonEmptyStringValidated("test")
            result.isLeft() shouldBe true
            result.fold(
                { error -> error.message shouldBe "test must be at least 1 characters long" },
                { }
            )
        }

        should("fail for blank string") {
            val result = "   ".toNonEmptyStringValidated("test")
            result.isLeft() shouldBe true
            result.fold(
                { error -> error.message shouldBe "test must be at least 1 characters long" },
                { }
            )
        }

        should("work with context receiver") {
            val result = either {
                "valid".toNonEmptyString("test")
            }
            result.isRight() shouldBe true
            result.fold({}, { it.value shouldBe "valid" })
        }
    }

    context("PositiveNumber") {
        should("create valid positive integer") {
            val result = 42.toPositiveNumberValidated("test")
            result.isRight() shouldBe true
            result.fold({}, { it.value shouldBe 42 })
        }

        should("create valid positive long") {
            val result = 42L.toPositiveNumberValidated("test")
            result.isRight() shouldBe true
            result.fold({}, { it.value shouldBe 42L })
        }

        should("create valid positive double") {
            val result = 42.5.toPositiveNumberValidated("test")
            result.isRight() shouldBe true
            result.fold({}, { it.value shouldBe 42.5 })
        }

        should("fail for zero") {
            val result = 0.toPositiveNumberValidated("test")
            result.isLeft() shouldBe true
            result.fold(
                { error -> error.message shouldBe "test must be a positive number (> 0)" },
                { }
            )
        }

        should("fail for negative number") {
            val result = (-5).toPositiveNumberValidated("test")
            result.isLeft() shouldBe true
            result.fold(
                { error -> error.message shouldBe "test must be a positive number (> 0)" },
                { }
            )
        }

        should("work with context receiver") {
            val result = either {
                100.toPositiveNumber("test")
            }
            result.isRight() shouldBe true
            result.fold({}, { it.value shouldBe 100 })
        }
    }

    context("DigitString") {
        should("create valid digit string") {
            val result = "12345".toDigitStringValidated("test")
            result.isRight() shouldBe true
            result.fold({}, { it.value shouldBe "12345" })
        }

        should("create valid digit string with max length") {
            val result = "123".toDigitStringValidated("test", 5)
            result.isRight() shouldBe true
            result.fold({}, { it.value shouldBe "123" })
        }

        should("fail for non-digit characters") {
            val result = "123a5".toDigitStringValidated("test")
            result.isLeft() shouldBe true
            result.fold(
                { errors ->
                    errors shouldHaveSize 1
                    errors[0].shouldBeInstanceOf<DigitString.NonDigitError>()
                    errors[0].message shouldBe "test must contain only digits (spaces allowed)"
                },
                { }
            )
        }

        should("fail for empty string") {
            val result = "".toDigitStringValidated("test")
            result.isLeft() shouldBe true
            result.fold(
                { errors ->
                    errors shouldHaveSize 1
                    errors[0].shouldBeInstanceOf<DigitString.NonDigitError>()
                    errors[0].message shouldBe "test must contain only digits (spaces allowed)"
                },
                { }
            )
        }

        should("fail for exceeding max length") {
            val result = "12345".toDigitStringValidated("test", 3)
            result.isLeft() shouldBe true
            result.fold(
                { errors ->
                    errors shouldHaveSize 1
                    errors[0].shouldBeInstanceOf<DigitString.ExceedsMaxLength>()
                    errors[0].message shouldBe "test must be at most 3 digits"
                },
                { }
            )
        }

        should("work with context receiver") {
            val result = either {
                "987654".toDigitString("test", 10)
            }
            result.isRight() shouldBe true
            result.fold({}, { it.value shouldBe "987654" })
        }
    }

    context("MOD10String") {
        should("create valid MOD10 string with valid Luhn checksum") {
            val result = "4532015112830366".toMOD10StringValidated("card number")
            result.isRight() shouldBe true
            result.fold({}, { it.value shouldBe "4532015112830366" })
        }

        should("normalize spaces from input") {
            val result = "4532 0151 1283 0366".toMOD10StringValidated("card number")
            result.isRight() shouldBe true
            result.fold({}, { it.value shouldBe "4532015112830366" })
        }

        should("fail for invalid Luhn checksum") {
            val result = "4532015112830367".toMOD10StringValidated("card number")
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

        should("fail for non-digit characters") {
            val result = "4532-0151-1283-0366".toMOD10StringValidated("card number")
            result.isLeft() shouldBe true
            result.fold(
                { errors ->
                    errors shouldHaveSize 1
                    errors[0].shouldBeInstanceOf<MOD10String.NonDigitError>()
                    errors[0].message shouldBe "card number must contain only digits (spaces allowed)"
                },
                { }
            )
        }

        should("fail for empty string") {
            val result = "".toMOD10StringValidated("card number")
            result.isLeft() shouldBe true
            result.fold(
                { errors ->
                    errors shouldHaveSize 1
                    errors[0].shouldBeInstanceOf<MOD10String.Mod10CheckFailed>()
                },
                { }
            )
        }

        should("fail for exceeding max length") {
            val result = "45320151128303661234".toMOD10StringValidated("card number", 19)
            result.isLeft() shouldBe true
            result.fold(
                { errors ->
                    errors shouldHaveSize 1
                    errors[0].shouldBeInstanceOf<MOD10String.ExceedsMaxLength>()
                    errors[0].message shouldBe "card number exceeds maximum length of 19"
                },
                { }
            )
        }

        should("work with context receiver") {
            val result = either {
                "4532015112830366".toMOD10String("card number", 19)
            }
            result.isRight() shouldBe true
            result.fold({}, { it.value shouldBe "4532015112830366" })
        }
    }

    context("RoutingString") {
        should("create valid routing number with correct ABA checksum") {
            val result = "021000021".toRoutingStringValidated("routing number")
            result.isRight() shouldBe true
            result.fold({}, { it.value shouldBe "021000021" })
        }

        should("normalize spaces from input") {
            val result = "021 000 021".toRoutingStringValidated("routing number")
            result.isRight() shouldBe true
            result.fold({}, { it.value shouldBe "021000021" })
        }

        should("fail for invalid ABA checksum") {
            val result = "021000022".toRoutingStringValidated("routing number")
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

        should("fail for non-digit characters") {
            val result = "02100002a".toRoutingStringValidated("routing number")
            result.isLeft() shouldBe true
            result.fold(
                { errors ->
                    errors shouldHaveSize 1
                    errors[0].shouldBeInstanceOf<RoutingString.NonDigitError>()
                    errors[0].message shouldBe "routing number must contain only digits (spaces allowed)"
                },
                { }
            )
        }

        should("fail for incorrect length") {
            val result = "12345678".toRoutingStringValidated("routing number")
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

        should("accumulate multiple errors") {
            val result = "1234567a".toRoutingStringValidated("routing number")
            result.isLeft() shouldBe true
            result.fold(
                { errors ->
                    errors shouldHaveSize 2 // non-digit and length errors
                },
                { }
            )
        }

        should("work with context receiver") {
            val result = either {
                "021000021".toRoutingString("routing number")
            }
            result.isRight() shouldBe true
            result.fold({}, { it.value shouldBe "021000021" })
        }
    }

    context("ExpiryDate") {
        val now = YearMonth.of(2024, 8)

        should("create valid future expiry date") {
            val result = toExpiryDateValidated("expiry date", 12, 2025, now)
            result.isRight() shouldBe true
            result.fold({}, { it.value shouldBe YearMonth.of(2025, 12) })
        }

        should("create expiry date for same month as current") {
            val result = toExpiryDateValidated("expiry date", 8, 2024, now)
            result.isRight() shouldBe true
            result.fold({}, { it.value shouldBe YearMonth.of(2024, 8) })
        }

        should("fail for invalid month") {
            val result = toExpiryDateValidated("expiry date", 13, 2025, now)
            result.isLeft() shouldBe true
            result.fold(
                { errors ->
                    errors shouldHaveSize 1
                    errors[0].shouldBeInstanceOf<ExpiryDate.InvalidMonthError>()
                    errors[0].message shouldBe "expiry date month must be between 1 and 12 (was 13)"
                },
                { }
            )
        }

        should("fail for month 0") {
            val result = toExpiryDateValidated("expiry date", 0, 2025, now)
            result.isLeft() shouldBe true
            result.fold(
                { errors ->
                    errors shouldHaveSize 1
                    errors[0].shouldBeInstanceOf<ExpiryDate.InvalidMonthError>()
                    errors[0].message shouldBe "expiry date month must be between 1 and 12 (was 0)"
                },
                { }
            )
        }

        should("fail for past expiry date") {
            val result = toExpiryDateValidated("expiry date", 7, 2024, now)
            result.isLeft() shouldBe true
            result.fold(
                { errors ->
                    errors shouldHaveSize 1
                    errors[0].shouldBeInstanceOf<ExpiryDate.PastExpiryError>()
                    errors[0].message shouldBe "expiry date must not be in the past (now is 2024-08)"
                },
                { }
            )
        }

        should("accumulate multiple errors") {
            val result = toExpiryDateValidated("expiry date", 13, 2020, now)
            result.isLeft() shouldBe true
            result.fold(
                { errors ->
                    errors shouldHaveSize 2 // invalid month and past date
                },
                { }
            )
        }

        should("work with context receiver") {
            val result = either {
                ExpiryDate.toExpiryDate("expiry date", 12, 2025, now)
            }
            result.isRight() shouldBe true
            result.fold({}, { it.value shouldBe YearMonth.of(2025, 12) })
        }

        should("work with YearMonth directly") {
            val yearMonth = YearMonth.of(2025, 12)
            val result = yearMonth.toExpiryDateValidated("expiry date", now)
            result.isRight() shouldBe true
            result.fold({}, { it.value shouldBe YearMonth.of(2025, 12) })
        }

        should("fail when YearMonth is in past") {
            val yearMonth = YearMonth.of(2024, 7)
            val result = yearMonth.toExpiryDateValidated("expiry date", now)
            result.isLeft() shouldBe true
            result.fold(
                { error ->
                    error.shouldBeInstanceOf<ExpiryDate.PastExpiryError>()
                    error.message shouldBe "expiry date must not be in the past (now is 2024-08)"
                },
                { }
            )
        }
    }

    context("FutureDate") {
        val today = LocalDate.of(2024, 8, 21)

        should("create valid future date") {
            val futureDate = LocalDate.of(2024, 8, 22)
            val result = with(FutureDate) { futureDate.toFutureDateValidated("test date", today) }
            result.isRight() shouldBe true
            result.fold({}, { it.value shouldBe futureDate })
        }

        should("fail for today's date") {
            val result = with(FutureDate) { today.toFutureDateValidated("test date", today) }
            result.isLeft() shouldBe true
            result.fold(
                { error ->
                    error.shouldBeInstanceOf<FutureDate.FutureDateError>()
                    error.message shouldBe "test date must be after 2024-08-21"
                },
                { }
            )
        }

        should("fail for past date") {
            val pastDate = LocalDate.of(2024, 8, 20)
            val result = with(FutureDate) { pastDate.toFutureDateValidated("test date", today) }
            result.isLeft() shouldBe true
            result.fold(
                { error ->
                    error.shouldBeInstanceOf<FutureDate.FutureDateError>()
                    error.message shouldBe "test date must be after 2024-08-21"
                },
                { }
            )
        }

        should("work with context receiver") {
            val futureDate = LocalDate.of(2025, 1, 1)
            val result = either {
                with(FutureDate) { futureDate.toFutureDate("test date", today) }
            }
            result.isRight() shouldBe true
            result.fold({}, { it.value shouldBe futureDate })
        }
    }
})
