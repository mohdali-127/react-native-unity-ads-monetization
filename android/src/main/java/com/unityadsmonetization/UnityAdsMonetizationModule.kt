package com.yourappmodule

import android.app.Activity
import com.unity3d.ads.UnityAdsShowOptions
import com.facebook.react.bridge.*
import com.unity3d.ads.UnityAds
import com.unity3d.ads.UnityAds.UnityAdsInitializationError
import com.unity3d.ads.UnityAds.UnityAdsLoadError
import com.unity3d.ads.UnityAds.UnityAdsShowError
import com.unity3d.ads.UnityAds.UnityAdsShowCompletionState
import com.unity3d.ads.IUnityAdsInitializationListener
import com.unity3d.ads.IUnityAdsLoadListener
import com.unity3d.ads.IUnityAdsShowListener
import com.facebook.react.modules.core.DeviceEventManagerModule

class UnityAdsMonetizationModule(
  private val reactContext: ReactApplicationContext
) : ReactContextBaseJavaModule(reactContext),
  IUnityAdsInitializationListener, IUnityAdsLoadListener, IUnityAdsShowListener {

  private val hasLoaded = mutableMapOf<String, Boolean>()
  private var initializePromise: Promise? = null

  // 🔥 FIX: Safe access to current activity (works on all new RN versions)
  private val currentActivitySafe: Activity?
    get() = reactContext.currentActivity

  override fun getName(): String = "UnityAdsMonetization"

  // Initialize Unity Ads
  @ReactMethod
  fun initialize(gameId: String, testMode: Boolean, promise: Promise) {
    this.initializePromise = promise
    UnityAds.initialize(reactContext.applicationContext, gameId, testMode, this)
  }

  // Load Ad
  @ReactMethod
  fun loadAd(placementId: String, promise: Promise) {
    UnityAds.load(placementId, this)
    promise.resolve(null)
  }

  // Check if ad is loaded
  @ReactMethod
  fun isLoad(placementId: String, promise: Promise) {
    val isLoaded = hasLoaded[placementId] ?: false
    promise.resolve(isLoaded)
  }

  // Show Ad
  @ReactMethod
  fun showAd(placementId: String, promise: Promise) {
    val activity = currentActivitySafe   // 🔥 FIX (instead of currentActivity)
    
    if (activity != null) {
      val showOptions = UnityAdsShowOptions()
      UnityAds.show(activity, placementId, showOptions, this)
      promise.resolve(null)
    } else {
      promise.reject("NO_ACTIVITY", "No current activity found to show ad")
    }
  }

  // =============================
  // Unity Ads Initialization
  // =============================
  override fun onInitializationComplete() {
    this.initializePromise?.resolve(true)
    this.initializePromise = null
  }

  override fun onInitializationFailed(
    error: UnityAdsInitializationError,
    message: String
  ) {
    this.initializePromise?.reject("INITIALIZATION_ERROR", message)
    this.initializePromise = null
  }

  // =============================
  // Unity Ads Load Events
  // =============================
  override fun onUnityAdsAdLoaded(placementId: String) {
    hasLoaded[placementId] = true
    val params = Arguments.createMap().apply {
      putString("placementId", placementId)
    }
    sendEvent("unityAdsAdLoaded", params)
  }

  override fun onUnityAdsFailedToLoad(
    placementId: String,
    error: UnityAdsLoadError,
    message: String
  ) {
    hasLoaded[placementId] = false
    val params = Arguments.createMap().apply {
      putString("placementId", placementId)
      putString("message", message)
    }
    sendEvent("unityAdsAdFailed", params)
  }

  // =============================
  // Unity Ads Show Events
  // =============================
  override fun onUnityAdsShowComplete(
    placementId: String,
    state: UnityAdsShowCompletionState
  ) {
    val params = Arguments.createMap().apply {
      putString("placementId", placementId)
      putString("state", state.name)
    }
    sendEvent("unityAdsShowComplete", params)
  }

  override fun onUnityAdsShowStart(placementId: String) {
    val params = Arguments.createMap().apply {
      putString("placementId", placementId)
    }
    sendEvent("unityAdsShowStart", params)
  }

  override fun onUnityAdsShowClick(placementId: String) {
    val params = Arguments.createMap().apply {
      putString("placementId", placementId)
    }
    sendEvent("unityAdsShowClick", params)
  }

  override fun onUnityAdsShowFailure(
    placementId: String,
    error: UnityAdsShowError,
    message: String
  ) {
    val params = Arguments.createMap().apply {
      putString("placementId", placementId)
      putString("message", message)
    }
    sendEvent("unityAdsShowFailed", params)
  }

  // =============================
  // Send Events to JavaScript
  // =============================
  private fun sendEvent(eventName: String, eventData: WritableMap) {
    reactContext
      .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter::class.java)
      .emit(eventName, eventData)
  }
}
