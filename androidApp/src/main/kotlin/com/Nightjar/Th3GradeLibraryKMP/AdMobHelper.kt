package com.Nightjar.Th3GradeLibraryKMP

import android.app.Activity
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback

/**
 * Official Google AdMob Production-Ready Helper (October 2026 / 2027 Standards).
 * Uses Google's official test ad unit IDs for safe verification without account bans.
 */
object AdMobHelper {

    // Official Google Test Ad Unit IDs
    const val TEST_BANNER_AD_UNIT_ID = "ca-app-pub-3940256099942544/6300978111"
    const val TEST_INTERSTITIAL_AD_UNIT_ID = "ca-app-pub-3940256099942544/1033173712"

    private const val PREFS_NAME = "admob_prefs"
    private const val KEY_INTERSTITIAL_OPEN_COUNTER = "interstitial_reader_open_counter"
    private const val INTERSTITIAL_EVERY_N_OPENS = 4

    private var interstitialAd: InterstitialAd? = null
    private var isLoadingInterstitial = false

    fun initialize(context: Context) {
        try {
            MobileAds.initialize(context) {}
            preloadInterstitial(context)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun preloadInterstitial(context: Context) {
        if (interstitialAd != null || isLoadingInterstitial) return
        isLoadingInterstitial = true
        val adRequest = AdRequest.Builder().build()
        InterstitialAd.load(
            context,
            TEST_INTERSTITIAL_AD_UNIT_ID,
            adRequest,
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

    /**
     * Reader instrumentation point: called on EVERY reader open, but the interstitial
     * is only actually shown on every 4th open (persisted across sessions).
     */
    fun showInterstitial(activity: Activity, onDismiss: () -> Unit = {}) {
        val prefs = activity.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val opens = prefs.getInt(KEY_INTERSTITIAL_OPEN_COUNTER, 0) + 1
        prefs.edit().putInt(KEY_INTERSTITIAL_OPEN_COUNTER, opens).apply()
        if (opens % INTERSTITIAL_EVERY_N_OPENS != 0) {
            // Not the 4th time: skip the ad entirely this session.
            onDismiss()
            return
        }
        val ad = interstitialAd
        if (ad != null) {
            ad.fullScreenContentCallback = object : com.google.android.gms.ads.FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    interstitialAd = null
                    preloadInterstitial(activity)
                    onDismiss()
                }

                override fun onAdFailedToShowFullScreenContent(error: com.google.android.gms.ads.AdError) {
                    interstitialAd = null
                    preloadInterstitial(activity)
                    onDismiss()
                }
            }
            ad.show(activity)
        } else {
            preloadInterstitial(activity)
            onDismiss()
        }
    }
}

/**
 * Floating Oval Liquid Banner Ad Container.
 * Adapts to screens, keeps elevation, and floats gracefully above navigation.
 */
@Composable
fun FloatingAdMobBanner(
    modifier: Modifier = Modifier,
    isDark: Boolean = false
) {
    Box(
        modifier = modifier
            .wrapContentSize()
            .clip(RoundedCornerShape(32.dp))
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(
                    listOf(
                        Color.White.copy(alpha = if (isDark) 0.15f else 0.35f),
                        Color.White.copy(alpha = if (isDark) 0.05f else 0.10f)
                    )
                ),
                shape = RoundedCornerShape(32.dp)
            )
            .background(
                if (isDark) Color(0xFF1E293B).copy(alpha = 0.92f)
                else Color(0xFFF8FAFC).copy(alpha = 0.94f)
            )
            .padding(horizontal = 8.dp, vertical = 2.dp),
        contentAlignment = Alignment.Center
    ) {
        AndroidView(
            modifier = Modifier.defaultMinSize(minWidth = 320.dp, minHeight = 50.dp),
            factory = { context ->
                AdView(context).apply {
                    setAdSize(AdSize.BANNER)
                    adUnitId = AdMobHelper.TEST_BANNER_AD_UNIT_ID
                    loadAd(AdRequest.Builder().build())
                }
            }
        )
    }
}
