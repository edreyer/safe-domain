package traditional

class PaymentValidationException(val errors: List<ValidationError>) : Exception("Payment validation failed: ${errors.joinToString(", ") { it.message }}")

class TraditionalBusinessLogic(private val validationService: ValidationService = ValidationService()) {

    /**
     * Processes a payment using traditional approach with exception-based error handling.
     * Validates all input parameters and throws PaymentValidationException if any validation fails.
     *
     * @param cardNumber Credit card number as String
     * @param month Expiry month as Int
     * @param year Expiry year as Int
     * @param cvv CVV code as String
     * @return PaymentMethod if validation succeeds
     * @throws PaymentValidationException if validation fails
     */
    @Throws(PaymentValidationException::class)
    fun processPayment(cardNumber: String, month: Int, year: Int, cvv: String): PaymentMethod {
        val allErrors = mutableListOf<ValidationError>()

        // Validate card number
        allErrors.addAll(validationService.validateCreditCardNumber(cardNumber))

        // Validate expiry date
        allErrors.addAll(validationService.validateExpiryDate(month, year))

        // Validate CVV
        allErrors.addAll(validationService.validateCvv(cvv))

        // If any validation errors, throw exception
        if (allErrors.isNotEmpty()) {
            throw PaymentValidationException(allErrors)
        }

        // Create credit card and process it
        val creditCard = CreditCard(cardNumber, month, year, cvv)
        return processCard(creditCard)
    }

    /**
     * Alternative processPayment that returns a Result type instead of throwing exceptions
     * This provides a more functional approach while still being traditional
     */
    fun processPaymentSafe(cardNumber: String, month: Int, year: Int, cvv: String): Result<PaymentMethod> {
        return try {
            val paymentMethod = processPayment(cardNumber, month, year, cvv)
            Result.success(paymentMethod)
        } catch (e: PaymentValidationException) {
            Result.failure(e)
        }
    }

    /**
     * Processes a credit card (placeholder implementation matching safe version)
     * In a real implementation, this would handle payment processing logic
     */
    fun processCard(card: CreditCard): CreditCard {
        // TODO: Implement actual payment processing logic
        // For now, just return the card as in the safe version
        return card
    }

    /**
     * Factory method to create a CreditCard with validation
     * This is the traditional equivalent of the safe CreditCard.create method
     */
    @Throws(PaymentValidationException::class)
    fun createCreditCard(number: String, expiryMonth: Int, expiryYear: Int, cvv: String): CreditCard {
        val allErrors = mutableListOf<ValidationError>()

        allErrors.addAll(validationService.validateCreditCardNumber(number))
        allErrors.addAll(validationService.validateExpiryDate(expiryMonth, expiryYear))
        allErrors.addAll(validationService.validateCvv(cvv))

        if (allErrors.isNotEmpty()) {
            throw PaymentValidationException(allErrors)
        }

        return CreditCard(number, expiryMonth, expiryYear, cvv)
    }

}
