package com.pixelmarket.app.util

/**
 * Razorpay Test Utils
 * 
 * Helper object for testing Razorpay payment integration
 */
object RazorpayTestUtils {
    
    // Test Card Numbers
    const val CARD_SUCCESS = "4111111111111111"
    const val CARD_FAILURE = "4000000000000002"
    const val CARD_3DS = "5104015555555558"  // 3D Secure test card
    
    // Test CVV
    const val TEST_CVV = "123"
    
    // Test Expiry
    const val TEST_EXPIRY = "12/25"
    
    // Test OTP for 2FA (Test mode)
    const val TEST_OTP = "1234"
    
    // Test User Details
    const val TEST_EMAIL = "test@pixelmarket.com"
    const val TEST_PHONE = "9999999999"
    const val TEST_NAME = "Test User"
    
    /**
     * Razorpay Test Mode Keys (Replace with your actual test keys)
     */
    object TestKeys {
        const val KEY_ID = "rzp_test_your_key_id_here"
        const val KEY_SECRET = "your_key_secret_here"
    }
    
    /**
     * Test scenarios enum
     */
    enum class TestScenario(val cardNumber: String, val description: String) {
        SUCCESS(CARD_SUCCESS, "Payment will succeed"),
        FAILURE(CARD_FAILURE, "Payment will fail due to insufficient funds"),
        THREE_DS(CARD_3DS, "Payment requires 3D Secure authentication"),
    }
    
    /**
     * Get test payment amount in rupees
     */
    fun getTestAmount(): Double = 1.0  // ₹1 for testing
    
    /**
     * Get test payment amount in paise (Razorpay format)
     */
    fun getTestAmountInPaise(): Int = (getTestAmount() * 100).toInt()
    
    /**
     * Log test payment details
     */
    fun logTestPaymentInfo(scenario: TestScenario) {
        println("""
            ═══════════════════════════════════════
            RAZORPAY TEST PAYMENT
            ═══════════════════════════════════════
            Scenario: ${scenario.description}
            Card Number: ${scenario.cardNumber}
            CVV: $TEST_CVV
            Expiry: $TEST_EXPIRY
            OTP (if required): $TEST_OTP
            Amount: ₹${getTestAmount()}
            ═══════════════════════════════════════
        """.trimIndent())
    }
    
    /**
     * International test cards
     */
    object InternationalCards {
        const val VISA_US = "4000000000000101"
        const val MASTERCARD_UK = "5200000000000007"
        const val AMEX = "370000000000002"
    }
    
    /**
     * Error codes reference
     */
    object ErrorCodes {
        const val BAD_REQUEST_ERROR = 0
        const val GATEWAY_ERROR = 1
        const val SERVER_ERROR = 2
        const val NETWORK_ERROR = 3
        const val TLS_ERROR = 4
        const val INVALID_OPTIONS = 5
        const val PAYMENT_CANCELED = 6
    }
    
    /**
     * Get error description
     */
    fun getErrorDescription(errorCode: Int): String {
        return when (errorCode) {
            ErrorCodes.BAD_REQUEST_ERROR -> "Bad Request - Invalid parameters"
            ErrorCodes.GATEWAY_ERROR -> "Gateway Error - Payment gateway issue"
            ErrorCodes.SERVER_ERROR -> "Server Error - Razorpay server issue"
            ErrorCodes.NETWORK_ERROR -> "Network Error - Check internet connection"
            ErrorCodes.TLS_ERROR -> "TLS Error - SSL/Security issue"
            ErrorCodes.INVALID_OPTIONS -> "Invalid Options - Payment configuration error"
            ErrorCodes.PAYMENT_CANCELED -> "Payment Canceled - User cancelled payment"
            else -> "Unknown Error - Error code: $errorCode"
        }
    }
}
