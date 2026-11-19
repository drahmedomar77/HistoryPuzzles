package com.puzzles.history

import android.app.Activity
import android.content.Context
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback

object AdMobManager {
    const val BANNER_AD_UNIT_ID = "ca-app-pub-3940256099942544/6300978111"
    const val INTERSTITIAL_AD_UNIT_ID = "ca-app-pub-3940256099942544/1033173712"
    const val REWARDED_AD_UNIT_ID = "ca-app-pub-3940256099942544/5224354917"

    private var interstitialAd: InterstitialAd? = null
    private var rewardedAd: RewardedAd? = null

    fun loadInterstitial(context: Context) {
        try {
            val adRequest = AdRequest.Builder().build()
            InterstitialAd.load(context, INTERSTITIAL_AD_UNIT_ID, adRequest, object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    interstitialAd = ad
                }
                override fun onAdFailedToLoad(error: LoadAdError) {
                    interstitialAd = null
                }
            })
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun showInterstitial(activity: Activity, onDismissed: () -> Unit = {}) {
        try {
            if (interstitialAd != null) {
                interstitialAd?.show(activity)
                interstitialAd = null
                loadInterstitial(activity)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        onDismissed()
    }

    fun loadRewarded(context: Context) {
        try {
            val adRequest = AdRequest.Builder().build()
            RewardedAd.load(context, REWARDED_AD_UNIT_ID, adRequest, object : RewardedAdLoadCallback() {
                override fun onAdLoaded(ad: RewardedAd) {
                    rewardedAd = ad
                }
                override fun onAdFailedToLoad(error: LoadAdError) {
                    rewardedAd = null
                }
            })
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun showRewarded(activity: Activity, onRewardEarned: (Int) -> Unit, onNotReady: () -> Unit) {
        try {
            val ad = rewardedAd
            if (ad != null) {
                ad.show(activity) { rewardItem ->
                    onRewardEarned(rewardItem.amount)
                }
                rewardedAd = null
                loadRewarded(activity)
            } else {
                loadRewarded(activity)
                onNotReady()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            onNotReady()
        }
    }
}

@Composable
fun BannerAdView(modifier: Modifier = Modifier) {
    AndroidView(
        modifier = modifier.fillMaxWidth(),
        factory = { context ->
            AdView(context).apply {
                setAdSize(AdSize.BANNER)
                adUnitId = AdMobManager.BANNER_AD_UNIT_ID
                try {
                    loadAd(AdRequest.Builder().build())
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    )
}
