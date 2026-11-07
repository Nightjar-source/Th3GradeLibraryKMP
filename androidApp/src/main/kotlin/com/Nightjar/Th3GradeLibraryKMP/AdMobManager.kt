package com.Nightjar.Th3GradeLibraryKMP

import android.app.Activity
import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.height
import androidx.compose.ui.unit.dp
import androidx.compose.ui.Alignment
import androidx.compose.animation.animateContentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.*
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback

object AdMobManager {
    // Official Google AdMob Test Ad Unit IDs
    const val TEST_BANNER_ID = "ca-app-pub-3940256099942544/6300978111"
    const val TEST_INTERSTITIAL_ID = "ca-app-pub-3940256099942544/1033173712"

    private var interstitialAd: InterstitialAd? = null
    private var openCounter = 0
    private const val CAPPING_FREQUENCY = 3 // Show interstitial once every 3 book opens

    fun initialize(context: Context) {
        try {
            MobileAds.initialize(context) {}
            preloadInterstitial(context)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun preloadInterstitial(context: Context) {
        val appContext = context.applicationContext
        val adRequest = AdRequest.Builder().build()
        InterstitialAd.load(
            appContext,
            TEST_INTERSTITIAL_ID,
            adRequest,
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    interstitialAd = ad
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    interstitialAd = null
                }
            }
        )
    }

    fun showInterstitialIfAllowed(activity: Activity, onAdDismissed: () -> Unit) {
        openCounter++
        if (openCounter % CAPPING_FREQUENCY == 0 && interstitialAd != null) {
            interstitialAd?.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    interstitialAd?.fullScreenContentCallback = null
                    interstitialAd = null
                    preloadInterstitial(activity.applicationContext)
                    onAdDismissed()
                }

                override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                    interstitialAd?.fullScreenContentCallback = null
                    interstitialAd = null
                    preloadInterstitial(activity.applicationContext)
                    onAdDismissed()
                }
            }
            interstitialAd?.show(activity)
        } else {
            onAdDismissed()
        }
    }
}

@Composable
fun AdaptiveBannerAd(
    modifier: Modifier = Modifier
) {
    var isAdLoaded by remember { mutableStateOf(false) }

    androidx.compose.foundation.layout.BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (isAdLoaded) Modifier.wrapContentHeight()
                else Modifier.height(0.dp)
            )
            .animateContentSize(spring(dampingRatio = androidx.compose.animation.core.Spring.DampingRatioNoBouncy, stiffness = 300f)),
        contentAlignment = androidx.compose.ui.Alignment.Center
    ) {
        val availableWidthDp = maxWidth.value.toInt()
        // AdMob Adaptive Banner specifications (Standard Material 3 / Google AdMob Responsive Guidelines):
        // 1. In large screens / tablets (availableWidth >= 600dp), cap banner width to standard 468dp / 728dp centered,
        //    preventing awkward giant stretched banners spanning full 1200dp desktop/tablet screens.
        // 2. In split-screen / pop-up windows (< 320dp, down to 280dp), ensure width matches container width
        //    rather than physical device display metrics.
        val targetAdWidth = when {
            availableWidthDp <= 0 -> 320
            availableWidthDp >= 840 -> 728.coerceAtMost(availableWidthDp)
            availableWidthDp >= 600 -> 468.coerceAtMost(availableWidthDp)
            else -> availableWidthDp.coerceAtLeast(280)
        }

        AndroidView(
            modifier = Modifier
                .widthIn(max = targetAdWidth.dp)
                .wrapContentHeight(),
            factory = { ctx ->
                AdView(ctx).apply {
                    val adSize = AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(ctx, targetAdWidth)
                    setAdSize(adSize)
                    adUnitId = AdMobManager.TEST_BANNER_ID
                    adListener = object : AdListener() {
                        override fun onAdLoaded() {
                            android.util.Log.d("AdMob", "Adaptive banner loaded successfully with width: $targetAdWidth dp")
                            isAdLoaded = true
                        }

                        override fun onAdFailedToLoad(error: LoadAdError) {
                            android.util.Log.e("AdMob", "Adaptive banner failed to load: ${error.message}, code: ${error.code}")
                            isAdLoaded = false
                        }
                    }
                    loadAd(AdRequest.Builder().build())
                }
            },
            onRelease = { adView ->
                try {
                    adView.destroy()
                } catch (_: Exception) {}
            }
        )
    }
}
