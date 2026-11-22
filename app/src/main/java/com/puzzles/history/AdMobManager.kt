package com.puzzles.history

import android.app.Activity
import android.content.Context
import android.content.pm.ApplicationInfo
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
    // ============================================================
    // ⚠️ مهم قبل النشر على Google Play: اجعل القيمتين التاليتين false
    // ============================================================
    // true = تظهر إعلانات اختبار Google حتى في نسخة الإصدار (للتأكد أن الإعلانات تعمل).
    const val FORCE_TEST_ADS = true

    // true = يكتب سبب غياب الإعلان داخل مكان البانر (للتشخيص فقط).
    const val SHOW_AD_STATUS = true

    // معرّفات الوحدات الإعلانية الحقيقية الخاصة بتطبيق ألغاز التاريخ
    private const val BANNER_AD_UNIT_ID = "ca-app-pub-1213862400889542/3780447389"
    private const val INTERSTITIAL_AD_UNIT_ID = "ca-app-pub-1213862400889542/4718585598"
    private const val REWARDED_AD_UNIT_ID = "ca-app-pub-1213862400889542/4325735504"

    // معرّفات الاختبار الرسمية من Google
    private const val TEST_BANNER_AD_UNIT_ID = "ca-app-pub-3940256099942544/6300978111"
    private const val TEST_INTERSTITIAL_AD_UNIT_ID = "ca-app-pub-3940256099942544/1033173712"
    private const val TEST_REWARDED_AD_UNIT_ID = "ca-app-pub-3940256099942544/5224354917"

    private var interstitialAd: InterstitialAd? = null
    private var rewardedAd: RewardedAd? = null
    private var isLoadingInterstitial = false
    private var isLoadingRewarded = false

    private fun useTestAds(context: Context): Boolean =
        FORCE_TEST_ADS || (context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0

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

/**
 * البانر الإعلاني: يحجز دائماً مكاناً ثابتاً ارتفاعه 50dp أسفل الشاشة،
 * فلا تتحرك العناصر عند ظهور الإعلان أو غيابه.
 */
@Composable
fun BannerAdView(
    isAdsReady: Boolean,
    adInitError: String?,
    modifier: Modifier = Modifier
) {
    var statusText by remember { mutableStateOf<String?>(null) }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(50.dp)
            .background(Color(0xFFF5F5F5)),
        contentAlignment = Alignment.Center
    ) {
        if (AdMobManager.SHOW_AD_STATUS) {
            val message = adInitError ?: statusText ?: if (!isAdsReady) "جارٍ تهيئة الإعلانات…" else null
            if (message != null) {
                Text(text = message, fontSize = 12.sp, color = Color(0xFF6B7280))
            }
        }

        AndroidView(
            modifier = Modifier.fillMaxWidth(),
            factory = { context ->
                AdView(context).apply {
                    setAdSize(AdSize.BANNER)
                    adUnitId = AdMobManager.bannerAdUnitId(context)
                    adListener = object : AdListener() {
                        override fun onAdLoaded() {
                            statusText = null
                        }

                        override fun onAdFailedToLoad(error: LoadAdError) {
                            statusText = "الإعلان غير متاح الآن (رمز ${error.code})"
                        }
                    }
                }
            },
            update = { adView ->
                // نطلب الإعلان مرة واحدة فقط بعد اكتمال تهيئة AdMob
                if (isAdsReady && adView.tag == null) {
                    adView.tag = true
                    adView.loadAd(AdRequest.Builder().build())
                }
            }
        )
    }
}
