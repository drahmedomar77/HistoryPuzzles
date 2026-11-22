package com.puzzles.history

import android.app.Activity
import android.content.Context
import android.content.pm.ApplicationInfo
import android.util.Log
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback

object AdMobManager {
    // معرّفات الوحدات الإعلانية الحقيقية الخاصة بتطبيق ألغاز التاريخ (تُستخدم في نسخة الإصدار فقط)
    private const val BANNER_AD_UNIT_ID = "ca-app-pub-1213862400889542/3780447389"
    private const val INTERSTITIAL_AD_UNIT_ID = "ca-app-pub-1213862400889542/4718585598"
    private const val REWARDED_AD_UNIT_ID = "ca-app-pub-1213862400889542/4325735504"

    // معرّفات الاختبار الرسمية من Google: تُستخدم تلقائياً في نسخ التطوير (debug)
    // حتى لا تضغط على إعلانات حقيقية بالخطأ أثناء التجربة، فذلك قد يعرّض حساب AdMob للحظر.
    private const val TEST_BANNER_AD_UNIT_ID = "ca-app-pub-3940256099942544/6300978111"
    private const val TEST_INTERSTITIAL_AD_UNIT_ID = "ca-app-pub-3940256099942544/1033173712"
    private const val TEST_REWARDED_AD_UNIT_ID = "ca-app-pub-3940256099942544/5224354917"

    // ⚠️ مهم: اجعلها true أثناء التجربة لترى إعلانات تجريبية فوراً،
    // و false قبل رفع النسخة النهائية إلى Google Play لتظهر إعلاناتك الحقيقية.
    private const val USE_TEST_ADS = true

    private const val TAG = "AdMob"

    private var interstitialAd: InterstitialAd? = null
    private var rewardedAd: RewardedAd? = null
    private var isLoadingInterstitial = false
    private var isLoadingRewarded = false

    private fun useTestAds(context: Context): Boolean =
        USE_TEST_ADS || (context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0

    fun bannerAdUnitId(context: Context): String =
        if (useTestAds(context)) TEST_BANNER_AD_UNIT_ID else BANNER_AD_UNIT_ID

    private fun interstitialAdUnitId(context: Context): String =
        if (useTestAds(context)) TEST_INTERSTITIAL_AD_UNIT_ID else INTERSTITIAL_AD_UNIT_ID

    private fun rewardedAdUnitId(context: Context): String =
        if (useTestAds(context)) TEST_REWARDED_AD_UNIT_ID else REWARDED_AD_UNIT_ID

    fun loadInterstitial(context: Context) {
        if (isLoadingInterstitial || interstitialAd != null) return
        isLoadingInterstitial = true
        val appContext = context.applicationContext
        InterstitialAd.load(
            appContext,
            interstitialAdUnitId(appContext),
            AdRequest.Builder().build(),
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    interstitialAd = ad
                    isLoadingInterstitial = false
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    Log.w(TAG, "Interstitial failed: code=${error.code} msg=${error.message}")
                    interstitialAd = null
                    isLoadingInterstitial = false
                }
            }
        )
    }

    fun showInterstitial(activity: Activity) {
        val ad = interstitialAd
        if (ad == null) {
            loadInterstitial(activity)
            return
        }
        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                interstitialAd = null
                loadInterstitial(activity)
            }

            override fun onAdFailedToShowFullScreenContent(error: AdError) {
                interstitialAd = null
                loadInterstitial(activity)
            }
        }
        ad.show(activity)
    }

    fun loadRewarded(context: Context) {
        if (isLoadingRewarded || rewardedAd != null) return
        isLoadingRewarded = true
        val appContext = context.applicationContext
        RewardedAd.load(
            appContext,
            rewardedAdUnitId(appContext),
            AdRequest.Builder().build(),
            object : RewardedAdLoadCallback() {
                override fun onAdLoaded(ad: RewardedAd) {
                    rewardedAd = ad
                    isLoadingRewarded = false
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    Log.w(TAG, "Rewarded failed: code=${error.code} msg=${error.message}")
                    rewardedAd = null
                    isLoadingRewarded = false
                }
            }
        )
    }

    fun showRewarded(
        activity: Activity,
        onRewardEarned: () -> Unit,
        onNotReady: () -> Unit
    ) {
        val ad = rewardedAd
        if (ad == null) {
            loadRewarded(activity)
            onNotReady()
            return
        }
        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                rewardedAd = null
                loadRewarded(activity)
            }

            override fun onAdFailedToShowFullScreenContent(error: AdError) {
                rewardedAd = null
                loadRewarded(activity)
                onNotReady()
            }
        }
        ad.show(activity) {
            onRewardEarned()
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
                adUnitId = AdMobManager.bannerAdUnitId(context)
                val adView = this
                adListener = object : AdListener() {
                    override fun onAdFailedToLoad(error: LoadAdError) {
                        Log.w("AdMob", "Banner failed: code=${error.code} msg=${error.message}")
                        // إعادة المحاولة بعد دقيقة (مفيد عند عدم توفر إعلان لحظياً)
                        adView.postDelayed({ adView.loadAd(AdRequest.Builder().build()) }, 60_000L)
                    }
                }
                loadAd(AdRequest.Builder().build())
            }
        }
    )
}
