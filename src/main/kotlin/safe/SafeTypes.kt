package safe

import arrow.core.Either
import arrow.core.Nel
import arrow.core.raise.Raise
import arrow.core.raise.either
import arrow.core.raise.ensure
import arrow.core.raise.zipOrAccumulate
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth

/**
 * Base type for all domain errors
 */
sealed interface ValidationError {
  val message: String
}

typealias ValidationErrors = Nel<ValidationError>

@JvmInline
value class NonEmptyString private constructor(val value: String) {

  // Domain errors for NonEmptyString
  data class NonEmptyStringError(val fieldName: String, val minLength: Int) : ValidationError {
    override val message: String = "$fieldName must be at least $minLength characters long"
  }

  companion object {
    // Public factory that returns an Either with accumulated NonEmptyList<ValidationError> on the Left.
    context(raise: Raise<NonEmptyStringError>)
    fun String.toNonEmptyString(fieldName: String, minLength: Int = 1): NonEmptyString {
      val trimmed = this.trim()
      raise.ensure(trimmed.length >= minLength) { NonEmptyStringError(fieldName, minLength) }
      return NonEmptyString(trimmed)
    }

    fun String.toNonEmptyStringValidated(fieldName: String, minLength: Int = 1)
    : Either<NonEmptyStringError, NonEmptyString> {
      val string = this
      return either { string.toNonEmptyString(fieldName, minLength) }
    }
  }
}

@JvmInline
value class PositiveNumber<T : Number> private constructor(val value: T) {

  data class PositiveNumberError(val fieldName: String) : ValidationError {
    override val message: String = "$fieldName must be a positive number (> 0)"
  }

  companion object Companion {
    context(raise: Raise<PositiveNumberError>)
    fun <T : Number> T.toPositiveNumber(fieldName: String): PositiveNumber<T> {
      val num = this
      val dv = num.toDouble()
      raise.ensure(dv > 0.0) { PositiveNumberError(fieldName) }
      return PositiveNumber(num)
    }

    fun <T : Number> T.toPositiveNumberValidated(fieldName: String): Either<PositiveNumberError, PositiveNumber<T>> {
      val num = this
      return either { num.toPositiveNumber(fieldName) }
    }
  }
}

// Non-negative amount for balances (>= 0)
@JvmInline
value class NonNegativeNumber<T : Number> private constructor(val value: T) {

  data class NonNegativeNumberError(val fieldName: String) : ValidationError {
    override val message: String = "$fieldName must be a positive number (>= 0)"
  }

  companion object Companion {
    context(raise: Raise<NonNegativeNumberError>)
    fun <T : Number> T.toNonNegativeNumber(fieldName: String): NonNegativeNumber<T> {
      val num = this
      val dv = num.toDouble()
      raise.ensure(dv >= 0.0) { NonNegativeNumberError(fieldName) }
      return NonNegativeNumber(num)
    }

    fun <T : Number> T.toNonNegativeNumberValidated(fieldName: String): Either<NonNegativeNumberError, NonNegativeNumber<T>> {
      val num = this
      return either { num.toNonNegativeNumber(fieldName) }
    }
  }
}

/**
 * Small helper wrapper for BigDecimal values that must be strictly positive (> 0).
 */
@JvmInline
value class PositiveBigDecimal private constructor(val value: BigDecimal) {
  data class PositiveBigDecimalError(val fieldName: String) : ValidationError {
    override val message: String = "$fieldName must be a positive amount (> 0)"
  }

  companion object {
    context(raise: Raise<PositiveBigDecimalError>)
    fun BigDecimal.toPositiveBigDecimal(fieldName: String): PositiveBigDecimal {
      val bd = this
      raise.ensure(bd > BigDecimal.ZERO) { PositiveBigDecimalError(fieldName) }
      return PositiveBigDecimal(bd)
    }

    fun BigDecimal.toPositiveBigDecimalValidated(fieldName: String): Either<PositiveBigDecimalError, PositiveBigDecimal> {
      val bd = this
      return either { bd.toPositiveBigDecimal(fieldName) }
    }
  }
}

@JvmInline
value class DigitString private constructor(val value: String) {

  sealed interface DigitStringError : ValidationError
  data class NonDigitError(val fieldName: String) : DigitStringError {
    override val message: String = "$fieldName must contain only digits (spaces allowed)"
  }
  data class ExceedsMaxLength(val fieldName: String, val maxLength: Int) : DigitStringError {
    override val message: String = "$fieldName must be at most $maxLength digits"
  }
  data class BelowMinLength(val fieldName: String, val minLength: Int) : DigitStringError {
    override val message: String = "$fieldName must be at least $minLength digits"
  }

  companion object Companion {

    context(raise: Raise<ValidationErrors>)
    fun String.toDigitString(fieldName: String, maxLength: Int? = null, minLength: Int? = null): DigitString {
      val trimmed = this.trim()
      val normalized = trimmed.replace(" ", "")
      val digits = normalized.all { it.isDigit() }
      return raise.zipOrAccumulate(
        { ensure(digits) { NonDigitError(fieldName) } },
        { ensure(normalized.isNotEmpty()) { NonDigitError(fieldName) } },
        { if (maxLength != null) ensure(normalized.length <= maxLength) { ExceedsMaxLength(fieldName, maxLength) } },
        { if (minLength != null) ensure(normalized.length >= minLength) { BelowMinLength(fieldName, minLength) } },
      ) { _, _, _, _ -> DigitString(normalized) }
    }

    // Accumulating variant: collects empty/non-digit/max-length failures
    fun String.toDigitStringValidated(fieldName: String, maxLength: Int? = null, minLength: Int? = null): Either<ValidationErrors, DigitString> {
      val raw = this
      return either { raw.toDigitString(fieldName, maxLength, minLength) }
    }
  }
}

@JvmInline
value class MOD10String private constructor(val value: String) {

  sealed interface MOD10StringError : ValidationError
  data class NonDigitError(val fieldName: String) : MOD10StringError {
    override val message: String = "$fieldName must contain only digits (spaces allowed)"
  }
  data class Mod10CheckFailed(val fieldName: String) : MOD10StringError {
    override val message: String = "$fieldName failed MOD10 (Luhn) checksum"
  }
  data class ExceedsMaxLength(val fieldName: String, val maxLength: Int) : MOD10StringError {
    override val message: String = "$fieldName exceeds maximum length of $maxLength"
  }

  companion object {
    private fun luhnCheck(number: String): Boolean {
      val sum = number
        .reversed()
        .mapIndexed { index, ch ->
          var n = ch - '0'
          if (index % 2 == 1) {
            n *= 2
            if (n > 9) n -= 9
          }
          n
        }
        .sum()
      return sum % 10 == 0
    }

    context(raise: Raise<ValidationErrors>)
    fun String.toMOD10String(fieldName: String, maxLength: Int? = null): MOD10String {
      val trimmed = this.trim()
      val normalized = trimmed.replace(" ", "")
      val Digits = normalized.all { it.isDigit() }
      return raise.zipOrAccumulate(
        { ensure(Digits) { NonDigitError(fieldName) } },
        { ensure(normalized.isNotEmpty()) { Mod10CheckFailed(fieldName) } },
        { if (maxLength != null) ensure(normalized.length <= maxLength) { ExceedsMaxLength(fieldName, maxLength) } },
        { if (Digits && normalized.isNotEmpty() && (maxLength == null || normalized.length <= maxLength)) ensure(luhnCheck(normalized)) { Mod10CheckFailed(fieldName) } }
      ) { _, _, _, _ -> MOD10String(normalized) }
    }

    // Accumulating variant: collects empty/non-digit/luhn failures
    fun String.toMOD10StringValidated(fieldName: String, maxLength: Int? = null): Either<ValidationErrors, MOD10String> {
      val raw = this
      return either { raw.toMOD10String(fieldName, maxLength) }
    }
  }
}

@JvmInline
value class RoutingString private constructor(val value: String) {

  sealed interface RoutingStringError : ValidationError
  data class NonDigitError(val fieldName: String) : RoutingStringError {
    override val message: String = "$fieldName must contain only digits (spaces allowed)"
  }
  data class LengthError(val fieldName: String) : RoutingStringError {
    override val message: String = "$fieldName must be exactly 9 digits long"
  }
  data class ChecksumFailed(val fieldName: String) : RoutingStringError {
    override val message: String = "$fieldName failed ABA routing checksum"
  }

  companion object {
    // ABA routing checksum per weights [3,7,1,3,7,1,3,7] and check digit at position 9
    private fun abaChecksumValid(number9: String): Boolean {
      if (number9.length != 9 || !number9.all { it.isDigit() }) return false
      val digits = number9.map { it - '0' }
      val weights = intArrayOf(3, 7, 1, 3, 7, 1, 3, 7)
      val sum = (0 until 8).sumOf { i -> digits[i] * weights[i] }
      val expectedCheck = (10 - (sum % 10)) % 10
      return digits[8] == expectedCheck
    }

    context(raise: Raise<ValidationErrors>)
    fun String.toRoutingString(fieldName: String): RoutingString {
      val trimmed = this.trim()
      val normalized = trimmed.replace(" ", "")
      val Digits = normalized.all { it.isDigit() }
      val correctLength = normalized.length == 9

      return raise.zipOrAccumulate(
        { ensure(Digits) { NonDigitError(fieldName) } },
        { ensure(correctLength) { LengthError(fieldName) } },
        {
          if (Digits && correctLength) {
            ensure(abaChecksumValid(normalized)) { ChecksumFailed(fieldName) }
          }
        }
      ) { _, _, _ -> RoutingString(normalized) }
    }

    fun String.toRoutingStringValidated(fieldName: String): Either<ValidationErrors, RoutingString> {
      val raw = this
      return either { raw.toRoutingString(fieldName) }
    }
  }
}


@JvmInline
value class FutureDate private constructor(val value: LocalDate) {

  data class FutureDateError(val fieldName: String, val today: LocalDate): ValidationError {
    override val message: String = "$fieldName must be after $today"
  }

  companion object {
    context(raise: Raise<FutureDateError>)
    fun LocalDate.toFutureDate(fieldName: String, after: LocalDate = LocalDate.now()): FutureDate {
      val date = this
      raise.ensure(date.isAfter(after)) { FutureDateError(fieldName, after) }
      return FutureDate(date)
    }

    fun LocalDate.toFutureDateValidated(fieldName: String, today: LocalDate = LocalDate.now()): Either<FutureDateError, FutureDate> {
      val d = this
      return either { d.toFutureDate(fieldName, today) }
    }
  }
}

@JvmInline
value class ExpiryDate private constructor(val value: YearMonth) {

  sealed interface ExpiryDateError : ValidationError
  data class InvalidMonthError(val fieldName: String, val month: Int) : ExpiryDateError {
    override val message: String = "$fieldName month must be between 1 and 12 (was $month)"
  }
  data class PastExpiryError(val fieldName: String, val now: YearMonth) : ExpiryDateError {
    override val message: String = "$fieldName must not be in the past (now is $now)"
  }

  companion object {
    context(raise: Raise<ValidationErrors>)
    fun toExpiryDate(fieldName: String, month: Int, year: Int, now: YearMonth = YearMonth.now()): ExpiryDate {
      return raise.zipOrAccumulate(
        { ensure(month in 1..12) { InvalidMonthError(fieldName, month) } },
        {
          // Check past date even when month is invalid by using a valid month for comparison
          val testMonth = if (month in 1..12) month else 1
          val ym2 = YearMonth.of(year, testMonth)
          ensure(!ym2.isBefore(now)) { PastExpiryError(fieldName, now) }
        }
      ) { _, _ ->
        val ym = YearMonth.of(year, month)
        ExpiryDate(ym)
      }
    }

    fun toExpiryDateValidated(fieldName: String, month: Int, year: Int, now: YearMonth = YearMonth.now()): Either<ValidationErrors, ExpiryDate> =
      either { toExpiryDate(fieldName, month, year, now) }

    context(raise: Raise<ExpiryDateError>)
    fun YearMonth.toExpiryDate(fieldName: String, now: YearMonth = YearMonth.now()): ExpiryDate {
      raise.ensure(!this.isBefore(now)) { PastExpiryError(fieldName, now) }
      return ExpiryDate(this)
    }

    fun YearMonth.toExpiryDateValidated(fieldName: String, now: YearMonth = YearMonth.now()): Either<ExpiryDateError, ExpiryDate> {
      val ym = this
      return either { ym.toExpiryDate(fieldName, now) }
    }
  }
}

@JvmInline
value class StrongPassword private constructor(val value: String) {

  sealed interface StrongPasswordError : ValidationError
  data class MinLengthError(val fieldName: String, val minLength: Int) : StrongPasswordError {
    override val message: String = "$fieldName must be at least $minLength characters long"
  }
  data class MissingUppercaseError(val fieldName: String) : StrongPasswordError {
    override val message: String = "$fieldName must contain at least one uppercase letter"
  }
  data class MissingLowercaseError(val fieldName: String) : StrongPasswordError {
    override val message: String = "$fieldName must contain at least one lowercase letter"
  }
  data class MissingDigitError(val fieldName: String) : StrongPasswordError {
    override val message: String = "$fieldName must contain at least one digit"
  }
  data class MissingSymbolError(val fieldName: String) : StrongPasswordError {
    override val message: String = "$fieldName must contain at least one symbol"
  }

  companion object {
    private fun hasUpper(s: String) = s.any { it.isUpperCase() }
    private fun hasLower(s: String) = s.any { it.isLowerCase() }
    private fun hasDigit(s: String) = s.any { it.isDigit() }
    private fun hasSymbol(s: String) = s.any { !it.isLetterOrDigit() }

    context(raise: Raise<ValidationErrors>)
    fun String.toStrongPassword(
      fieldName: String,
      minLength: Int = 12,
      requireUpper: Boolean = true,
      requireLower: Boolean = true,
      requireDigit: Boolean = true,
      requireSymbol: Boolean = true
    ): StrongPassword {
      val pwd = this
      with(raise) {
        return zipOrAccumulate(
          { ensure(pwd.isNotEmpty()) { MinLengthError(fieldName, minLength) } },
          { ensure(pwd.length >= minLength) { MinLengthError(fieldName, minLength) } },
          { if (requireUpper) ensure(hasUpper(pwd)) { MissingUppercaseError(fieldName) }; Unit },
          { if (requireLower) ensure(hasLower(pwd)) { MissingLowercaseError(fieldName) }; Unit },
          { if (requireDigit) ensure(hasDigit(pwd)) { MissingDigitError(fieldName) }; Unit },
          { if (requireSymbol) ensure(hasSymbol(pwd)) { MissingSymbolError(fieldName) }; Unit },
        ) { _, _, _, _, _, _ -> StrongPassword(pwd) }
      }
    }

    // Accumulating variant: collects all failing rules
    fun String.toStrongPasswordValidated(
      fieldName: String,
      minLength: Int = 12,
      requireUpper: Boolean = true,
      requireLower: Boolean = true,
      requireDigit: Boolean = true,
      requireSymbol: Boolean = true
    ): Either<ValidationErrors, StrongPassword> {
      val pwd = this
      return either { pwd.toStrongPassword(fieldName, minLength, requireUpper, requireLower, requireDigit, requireSymbol) }
    }
  }
}

@JvmInline
value class Email private constructor(val value: String) {
  data class EmailError(val fieldName: String) : ValidationError {
    override val message: String = "$fieldName must be a valid email address"
  }
  companion object {
    context(r: Raise<ValidationError>)
    fun String.toEmail(field: String = "email"): Email {
      val s = this.trim()
      r.ensure(s.isNotEmpty() && "@" in s) { EmailError("$field must contain @ and not be blank") }
      return Email(s)
    }
  }
}

