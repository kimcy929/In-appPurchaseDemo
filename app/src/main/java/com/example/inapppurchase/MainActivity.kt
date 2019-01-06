package com.example.inapppurchase

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.android.billingclient.api.SkuDetails
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity(), DonateClient.DonateClientListener {

    private val donateClient by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        DonateClient(this, this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        btnProversion.setOnClickListener {
            donateClient.setupPurchase()
        }
    }

    override fun skuDetailsResult(skuDetailsList: List<SkuDetails>) {
        if (skuDetailsList.isNotEmpty()) {
            if (skuDetailsList.size == 1) {
                donateClient.makePurchase(skuDetailsList[0])
            } else {
                //show dialog list items
                skuDetailsList.sortedBy { it.priceAmountMicros }
                val prices = mutableListOf<String>()
                skuDetailsList.forEach {
                    prices.add(it.price)
                }
                AlertDialog.Builder(this)
                    .setTitle(R.string.donate_me)
                    .setSingleChoiceItems(prices.toTypedArray(), -1) { dialog, which ->
                        donateClient.makePurchase(skuDetailsList[which])
                        dialog.dismiss()
                    }
                    .setNegativeButton(android.R.string.cancel, null)
                    .show()

            }
        }
    }

    override fun markPurchased() {
        // Update your features here as marking the pro version
        Toast.makeText(this, "Bought!", Toast.LENGTH_LONG).show()
        textView.setText(R.string.thank_you)
    }
}
