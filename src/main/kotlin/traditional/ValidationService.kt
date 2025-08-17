package traditional

import java.time.YearMonth

class ValidationService {

    /**
     * Validates that a string is not empty and meets minimum length requirements
     */
    fun validateNonEmptyString(value: String, fieldName: String, minLength: Int = 1): List<ValidationError> {
        val errors = mutableListOf<ValidationError>()
        if (value.length < minLength) {
            errors.add(ValidationError("$fieldName must be at least $minLength characters long"))
        }
        return errors
    }

    /**
     * Validates that a number is positive
     */
    fun validatePositiveNumber(value: Number, fieldName: String): List<ValidationError> {
        val errors = mutableListOf<ValidationError>()
        when (value) {
            is Int -> if (value <= 0) errors.add(ValidationError("$fieldName must be positive"))
            is Long -> if (value <= 0) errors.add(ValidationError("$fieldName must be positive"))
            is Float -> if (value <= 0.0f) errors.add(ValidationError("$fieldName must be positive"))
            is Double -> if (value <= 0.0) errors.add(ValidationError("$fieldName must be positive"))
        }
        return errors
    }

    /**
     * Validates that a string contains only digits
     */
    fun validateDigitString(value: String, fieldName: String, maxLength: Int? = null): List<ValidationError> {
        val errors = mutableListOf<ValidationError>()

        if (value.isEmpty() || !value.all { it.isDigit() }) {
            errors.add(ValidationError("$fieldName must contain only digits"))
        }

        maxLength?.let { max ->
            if (value.length > max) {
                errors.add(ValidationError("$fieldName must not exceed $max characters"))
            }
        }

        return errors
    }

    /**
     * Validates credit card number using MOD10 (Luhn) algorithm
     */
    fun validateCreditCardNumber(number: String, fieldName: String = "card number", maxLength: Int = 19): List<ValidationError> {
        val errors = mutableListOf<ValidationError>()

        // Check if all digits
        errors.addAll(validateDigitString(number, fieldName, maxLength))

        // Check Luhn algorithm
        if (errors.isEmpty() && !isValidLuhn(number)) {
            errors.add(InvalidCardNumberError("$fieldName failed MOD10 check"))
        }

        return errors
    }

    /**
     * Validates CVV code
     */
    fun validateCvv(cvv: String, fieldName: String = "CVV", maxLength: Int = 4): List<ValidationError> {
        val errors = mutableListOf<ValidationError>()

        errors.addAll(validateDigitString(cvv, fieldName, maxLength))

        if (cvv.length < 3 || cvv.length > 4) {
            errors.add(InvalidCvvError("$fieldName must be 3 or 4 digits"))
        }

        return errors
    }

    /**
     * Validates expiry date
     */
    fun validateExpiryDate(month: Int, year: Int, fieldName: String = "expiry date", now: YearMonth = YearMonth.now()): List<ValidationError> {
        val errors = mutableListOf<ValidationError>()

        if (month < 1 || month > 12) {
            errors.add(InvalidExpiryDateError("$fieldName month must be between 1 and 12"))
        }

        if (errors.isEmpty()) {
            val expiryDate = YearMonth.of(year, month)
            if (expiryDate.isBefore(now)) {
                errors.add(InvalidExpiryDateError("$fieldName cannot be in the past"))
            }
        }

        return errors
    }

    /**
     * Validates routing number using ABA checksum
     */
    fun validateRoutingNumber(routingNumber: String, fieldName: String = "routing number"): List<ValidationError> {
        val errors = mutableListOf<ValidationError>()

        // Must be exactly 9 digits
        if (routingNumber.length != 9) {
            errors.add(InvalidRoutingNumberError("$fieldName must be exactly 9 digits"))
        }

        errors.addAll(validateDigitString(routingNumber, fieldName))

        // Check ABA checksum
        if (errors.isEmpty() && !isValidAbaChecksum(routingNumber)) {
            errors.add(InvalidRoutingNumberError("$fieldName failed ABA checksum validation"))
        }

        return errors
    }

    /**
     * Validates account number
     */
    fun validateAccountNumber(accountNumber: String, fieldName: String = "account number"): List<ValidationError> {
        val errors = mutableListOf<ValidationError>()

        if (accountNumber.isEmpty()) {
            errors.addAll(validateNonEmptyString(accountNumber, fieldName))
        } else {
            errors.addAll(validateDigitString(accountNumber, fieldName))
        }

        return errors
    }

    /**
     * Validates strong password
     */
    fun validateStrongPassword(
        password: String,
        fieldName: String = "password",
        minLength: Int = 12,
        requireUpper: Boolean = true,
        requireLower: Boolean = true,
        requireDigit: Boolean = true,
        requireSymbol: Boolean = true
    ): List<ValidationError> {
        val errors = mutableListOf<ValidationError>()

        if (password.length < minLength) {
            errors.add(ValidationError("$fieldName must be at least $minLength characters long"))
        }

        if (requireUpper && !password.any { it.isUpperCase() }) {
            errors.add(ValidationError("$fieldName must contain at least one uppercase letter"))
        }

        if (requireLower && !password.any { it.isLowerCase() }) {
            errors.add(ValidationError("$fieldName must contain at least one lowercase letter"))
        }

        if (requireDigit && !password.any { it.isDigit() }) {
            errors.add(ValidationError("$fieldName must contain at least one digit"))
        }

        if (requireSymbol && !password.any { !it.isLetterOrDigit() }) {
            errors.add(ValidationError("$fieldName must contain at least one symbol"))
        }

        return errors
    }

    /**
     * Validates email format
     */
    fun validateEmail(email: String, fieldName: String = "email"): List<ValidationError> {
        val errors = mutableListOf<ValidationError>()

        val emailRegex = "^[A-Za-z0-9+_.-]+@([A-Za-z0-9.-]+\\.[A-Za-z]{2,})$".toRegex()
        if (!email.matches(emailRegex)) {
            errors.add(ValidationError("$fieldName must be a valid email address"))
        }

        return errors
    }

    // Helper methods for complex validations

    private fun isValidLuhn(number: String): Boolean {
        var sum = 0
        var alternate = false

        for (i in number.length - 1 downTo 0) {
            var n = number[i].digitToInt()

            if (alternate) {
                n *= 2
                if (n > 9) {
                    n = n % 10 + 1
                }
            }

            sum += n
            alternate = !alternate
        }

        return sum % 10 == 0
    }

    private fun isValidAbaChecksum(number9: String): Boolean {
        val weights = intArrayOf(3, 7, 1, 3, 7, 1, 3, 7, 1)
        var sum = 0

        for (i in number9.indices) {
            sum += number9[i].digitToInt() * weights[i]
        }

        return sum % 10 == 0
    }
}
