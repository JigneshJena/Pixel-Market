package com.pixelmarket.app.data.payment

import android.app.Activity
import android.util.Log
import com.pixelmarket.app.domain.model.Asset
import com.razorpay.Checkout
import com.razorpay.PaymentResultListener
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RazorpayManager @Inject constructor() {

    companion object {
        private const val TAG = "RazorpayManager"
        // Razorpay Test API Key
        private const val RAZORPAY_KEY_ID = "rzp_test_SBLcPxcylYWe12"
    }

    /**
     * Initialize Razorpay checkout
     * Call this during app initialization or before payment
     */
    fun preloadCheckout(activity: Activity) {
        try {
            Checkout.preload(activity.applicationContext)
            Log.d(TAG, "Razorpay checkout preloaded successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error preloading Razorpay checkout", e)
        }
    }

    /**
     * Start payment process
     * @param activity The activity that will handle payment callbacks
     * @param asset The asset being purchased
     * @param userEmail User's email address
     * @param userPhone User's phone number
     * @param userName User's name
     * @param listener Payment result listener
     */
    fun startPayment(
        activity: Activity,
        asset: Asset,
        userEmail: String,
        userPhone: String,
        userName: String,
        listener: PaymentResultListener
    ) {
        try {
            val checkout = Checkout()
            checkout.setKeyID(RAZORPAY_KEY_ID)

            // Convert price to paise (Razorpay uses smallest currency unit)
            val amountInPaise = (asset.price * 100).toInt()

            val options = JSONObject().apply {
                put("name", "PixelMarket")
                put("description", "Purchase: ${asset.title}")
                put("image", asset.thumbnailUrl.ifEmpty { "" })
                put("currency", "INR")
                put("amount", amountInPaise)

                // Prefill user details
                val prefill = JSONObject().apply {
                    put("email", userEmail)
                    put("contact", userPhone)
                    put("name", userName)
                }
                put("prefill", prefill)

                // Premium theme matching PixelMarket brand
                val theme = JSONObject().apply {
                    put("color", "#6366F1")          // PixelMarket primary (indigo)
                    put("backdrop_color", "#0F172A") // Dark glassmorphism backdrop
                    put("hide_topbar", false)
                }
                put("theme", theme)

                // Enable all payment methods
                val config = JSONObject().apply {
                    put("display", JSONObject().apply {
                        put("blocks", JSONObject().apply {
                            put("hdfc", JSONObject().apply {
                                put("name", "Pay using HDFC Bank")
                                put("instruments", org.json.JSONArray().apply {
                                    put(JSONObject().apply { put("method", "card") })
                                    put(JSONObject().apply { put("method", "emi") })
                                })
                            })
                            put("other", JSONObject().apply {
                                put("name", "Other Payment Methods")
                                put("instruments", org.json.JSONArray().apply {
                                    put(JSONObject().apply { put("method", "upi") })
                                    put(JSONObject().apply { put("method", "netbanking") })
                                    put(JSONObject().apply { put("method", "wallet") })
                                })
                            })
                        })
                        put("sequence", org.json.JSONArray().apply {
                            put("block.hdfc")
                            put("block.other")
                        })
                        put("preferences", JSONObject().apply {
                            put("show_default_blocks", true)
                        })
                    })
                }
                put("config", config)

                // UPI method options
                put("method", JSONObject().apply {
                    put("upi", true)
                    put("card", true)
                    put("netbanking", true)
                    put("wallet", true)
                    put("emi", true)
                })

                // Additional options
                put("send_sms_hash", true)
                put("allow_rotation", false)
                put("retry", JSONObject().apply {
                    put("enabled", true)
                    put("max_count", 3)
                })
                put("timeout", 300)
                put("remember_customer", true)

                // Metadata notes
                val notes = JSONObject().apply {
                    put("asset_id", asset.id)
                    put("asset_title", asset.title)
                    put("seller_id", asset.sellerId)
                    put("category", asset.category)
                    put("platform", "android")
                }
                put("notes", notes)

                // Modal confirm on close
                put("modal", JSONObject().apply {
                    put("confirm_close", true)
                    put("animation", true)
                })
            }

            Log.d(TAG, "Starting payment for ${asset.title} - Amount: ₹${asset.price}")
            checkout.open(activity, options)
        } catch (e: Exception) {
            Log.e(TAG, "Error starting payment", e)
            listener.onPaymentError(
                500,
                "Payment initialization failed: ${e.message}"
            )
        }
    }

    /**
     * Start payment for wallet top-up
     */
    fun startWalletPayment(
        activity: Activity,
        amount: Double,
        userEmail: String,
        userPhone: String,
        userName: String,
        listener: PaymentResultListener
    ) {
        try {
            val checkout = Checkout()
            checkout.setKeyID(RAZORPAY_KEY_ID)

            val amountInPaise = (amount * 100).toInt()

            val options = JSONObject().apply {
                put("name", "PixelMarket")
                put("description", "Wallet Top-up – ₹${String.format("%.0f", amount)}")
                put("currency", "INR")
                put("amount", amountInPaise)

                val prefill = JSONObject().apply {
                    put("email", userEmail)
                    put("contact", userPhone)
                    put("name", userName)
                }
                put("prefill", prefill)

                // Match premium theme
                val theme = JSONObject().apply {
                    put("color", "#6366F1")
                    put("backdrop_color", "#0F172A")
                    put("hide_topbar", false)
                }
                put("theme", theme)

                put("method", JSONObject().apply {
                    put("upi", true)
                    put("card", true)
                    put("netbanking", true)
                    put("wallet", true)
                })

                put("notes", JSONObject().apply {
                    put("type", "wallet_topup")
                    put("platform", "android")
                })
                put("remember_customer", true)
                put("timeout", 300)
            }

            checkout.open(activity, options)
        } catch (e: Exception) {
            listener.onPaymentError(500, "Payment failed: ${e.message}")
        }
    }

    /**
     * Create payment payload using PayloadHelper (alternative method)
     * This is useful if you need more control over the payload
     */
    fun createPaymentPayload(
        asset: Asset,
        userEmail: String,
        userPhone: String,
        userName: String,
        orderId: String? = null
    ): JSONObject {
        val amountInPaise = (asset.price * 100).toInt()

        return JSONObject().apply {
            put("name", "PixelMarket")
            put("description", asset.title)
            put("image", asset.thumbnailUrl)
            put("currency", "INR")
            put("amount", amountInPaise)

            // If you have order_id from your backend
            orderId?.let { put("order_id", it) }

            // Prefill user details
            put("prefill", JSONObject().apply {
                put("email", userEmail)
                put("contact", userPhone)
                put("name", userName)
                // Optional: prefill card details (for testing)
                // put("card_number", "4111111111111111")
                // put("card_cvv", "123")
                // put("card_expiry", "12/25")
                // put("method", "card")
            })

            // Theme
            put("theme", JSONObject().apply {
                put("color", "#088395")
                put("backdrop_color", "#EBF4F6")
            })

            // Notes
            put("notes", JSONObject().apply {
                put("asset_id", asset.id)
                put("asset_title", asset.title)
                put("seller_id", asset.sellerId)
            })
        }
    }
}
