package com.example.inapppurchase

import android.annotation.SuppressLint
import android.app.Activity
import com.android.billingclient.api.*
import com.android.billingclient.api.BillingClient.BillingResponse.*
import com.android.billingclient.api.BillingClient.SkuType.INAPP
import timber.log.Timber

/**
 * https://developer.android.com/google/play/billing/billing_library_overview
 */

class DonateClient(
    private val activity: Activity,
    private val donateClientListener: DonateClientListener
) : PurchasesUpdatedListener {

    interface DonateClientListener {
        fun skuDetailsResult(skuDetailsList: List<SkuDetails>)
        fun markPurchased()
    }

    private lateinit var billingClient: BillingClient

    fun setupPurchase() {
        billingClient = BillingClient.newBuilder(activity).setListener(this).build()

        /*
        // waiting for bug fix https://github.com/googlesamples/android-play-billing/issues/92
        val purchasesResult = billingClient.queryPurchases(INAPP)
        Timber.d("purchasesResult.responseCode -> ${purchasesResult.responseCode}")
        if (purchasesResult.responseCode == OK && purchasesResult.purchasesList != null) {
            purchasesResult.purchasesList.forEach {
                Timber.d("purchased item -> ${it.sku} - ${it.orderId}")
            }
            donateClientListener.markPurchased()
        } else { }*/

        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResponseCode: Int) {
                if (billingResponseCode == OK) {
                    retrieveSkuDetails()
                }
            }

            override fun onBillingServiceDisconnected() {
                // Try to restart the connection on the next request to
                // Google Play by calling the startConnection() method.
                Timber.d("Billing client disconnected from service")
            }
        })

    }

    private fun retrieveSkuDetails() {
        //val skuList = listOf(Constant.SKU_ITEM)
        val skuList = activity.resources.getStringArray(R.array.sku_ids).toList()
        val params = SkuDetailsParams
            .newBuilder()
            .setSkusList(skuList)
            .setType(INAPP)

        billingClient.querySkuDetailsAsync(params.build()) { responseCode, skuDetailsList ->
            if (responseCode == OK && skuDetailsList != null) {
                skuDetailsList.sortBy { it.priceAmountMicros }
                donateClientListener.skuDetailsResult(skuDetailsList)
            }
        }
    }

    fun makePurchase(skuDetails: SkuDetails): Boolean {
        // sale
        val flowParams =
            BillingFlowParams.newBuilder()
                .setSkuDetails(skuDetails)
                .build()
        val responseCode = billingClient.launchBillingFlow(activity, flowParams)
        when (responseCode) {
            ITEM_ALREADY_OWNED -> donateClientListener.markPurchased()

            OK -> Timber.d("makePurchase() successful!")

            else -> Timber.e(DonationException("makePurchase()", responseCode))
        }

        billingClient.endConnection()

        return responseCode == OK
    }

    @SuppressLint("SwitchIntDef")
    override fun onPurchasesUpdated(responseCode: Int, purchases: MutableList<Purchase>?) {
        when (responseCode) {
            OK -> if (purchases != null) {
                donateClientListener.markPurchased()
                purchases.forEach {
                    Timber.d("onPurchasesUpdated() - Success for SKU ${it.sku}")
                }
            }

            USER_CANCELED -> Timber.d("onPurchasesUpdated() user canceled")

            else -> Timber.e(DonationException("onPurchasesUpdated()", responseCode))
        }
    }
}

class DonationException(
    action: String,
    responseCode: Int
) : Exception("$action unsuccessful - responseCode = ${responseCode.billingCodeName()}")

private fun Int.billingCodeName(): String = when (this) {
    FEATURE_NOT_SUPPORTED -> "FEATURE_NOT_SUPPORTED"
    SERVICE_DISCONNECTED -> "SERVICE_DISCONNECTED"
    OK -> "OK"
    USER_CANCELED -> "USER_CANCELED"
    SERVICE_UNAVAILABLE -> "SERVICE_UNAVAILABLE"
    BILLING_UNAVAILABLE -> "BILLING_UNAVAILABLE"
    ITEM_UNAVAILABLE -> "ITEM_UNAVAILABLE"
    DEVELOPER_ERROR -> "DEVELOPER_ERROR"
    ERROR -> "ERROR"
    ITEM_ALREADY_OWNED -> "ITEM_ALREADY_OWNED"
    ITEM_NOT_OWNED -> "ITEM_NOT_OWNED"
    else -> "Not Know??"
}