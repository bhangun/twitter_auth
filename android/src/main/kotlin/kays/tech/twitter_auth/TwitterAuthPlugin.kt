package kays.tech.twitter_auth

import android.content.Intent
import android.webkit.CookieManager
import android.webkit.CookieSyncManager

import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import io.flutter.plugin.common.MethodChannel.Result
import io.flutter.plugin.common.PluginRegistry.Registrar
import io.flutter.plugin.common.PluginRegistry

import com.twitter.sdk.android.core.Callback
import com.twitter.sdk.android.core.Twitter
import com.twitter.sdk.android.core.TwitterAuthConfig
import com.twitter.sdk.android.core.TwitterConfig
import com.twitter.sdk.android.core.TwitterCore
import com.twitter.sdk.android.core.TwitterException
import com.twitter.sdk.android.core.TwitterSession
import com.twitter.sdk.android.core.identity.TwitterAuthClient

class TwitterAuthPlugin  private constructor (private val registrar: Registrar)  :  Callback<TwitterSession>(),  PluginRegistry.ActivityResultListener,  MethodCallHandler {
  companion object {
    @JvmStatic
    fun registerWith(registrar: Registrar) {
      val channel = MethodChannel(registrar.messenger(), "twitter_auth")
      channel.setMethodCallHandler(TwitterAuthPlugin( registrar)) 
    }
  }

  private val CHANNEL_NAME = "com.roughike/flutter_twitter_login";
  private val METHOD_GET_CURRENT_SESSION = "getCurrentSession";
  private val METHOD_AUTHORIZE = "authorize";
  private val METHOD_LOG_OUT = "logOut";
  private val PLATFORM_VERSION = "getPlatformVersion";

  var _authClientInstance: TwitterAuthClient? = null
    var _pendingResult: Result? = null

  init {
    registrar.addActivityResultListener(this)
  } 

  override fun onMethodCall(call: MethodCall, result: Result) {
   
    when (call.method) {
      PLATFORM_VERSION -> result.success("Bismillah Android ${android.os.Build.VERSION.RELEASE}")
      METHOD_GET_CURRENT_SESSION -> getCurrentSession(result, call)
      METHOD_AUTHORIZE -> authorize(result, call)
      METHOD_LOG_OUT -> logOut(result, call)
      else -> result.notImplemented()
     }

  }

  private fun authorize(result: Result, call: MethodCall) {
    setPendingResult("authorize", result)
    initializeAuthClient(call)
    _authClientInstance?.authorize(registrar.activity(), this)
}

  private fun initializeAuthClient(call: MethodCall) {
        if (_authClientInstance == null) {
            val consumerKey = call.argument<String>("consumerKey")
            val consumerSecret = call.argument<String>("consumerSecret")

            _authClientInstance = configureClient(consumerKey, consumerSecret)
        }
    }

    private fun getCurrentSession(result: Result, call: MethodCall) {
        initializeAuthClient(call)
        val session = TwitterCore.getInstance().sessionManager.activeSession
        val sessionMap = sessionToMap(session)

        result.success(sessionMap)
    }

  private fun setPendingResult(methodName: String, result: MethodChannel.Result) {
        if (_pendingResult != null) {
            result.error(
                    "TWITTER_LOGIN_IN_PROGRESS",
                    methodName + " called while another Twitter " +
                            "login operation was in progress.", null
            )
        }

        _pendingResult = result
    }

    private fun configureClient(consumerKey: String?, consumerSecret: String?): TwitterAuthClient {
        val authConfig = TwitterAuthConfig(consumerKey!!, consumerSecret!!)
        val config = TwitterConfig.Builder(registrar.context())
                .twitterAuthConfig(authConfig)
                .build()
        Twitter.initialize(config)

        return TwitterAuthClient()
    }


    private fun logOut(result: Result, call: MethodCall) {
        CookieSyncManager.createInstance(registrar.context())
        val cookieManager = CookieManager.getInstance()
        cookieManager.removeSessionCookie()

        initializeAuthClient(call)
        TwitterCore.getInstance().sessionManager.clearActiveSession()
        result.success(null)
    }

    private fun sessionToMap(session: TwitterSession?): HashMap<String, String>? {
        return if (session != null) {
            object : HashMap<String, String>() {
                init {
                    put("secret", session.authToken.secret)
                    put("token", session.authToken.token)
                    put("userId", (session.userId).toString())
                    put("username", session.userName)
                }
            }
        } else null

    }

    override fun success(result: com.twitter.sdk.android.core.Result<TwitterSession>) {
        if (_pendingResult != null) {
            val sessionMap = sessionToMap(result.data)
            val resultMap = object : HashMap<String, Any?>() {
                init {
                    put("status", "loggedIn")
                    put("session", sessionMap)
                }
            }

            _pendingResult!!.success(resultMap)
            _pendingResult = null
        }
    }

    override fun failure(exception: TwitterException) {
        if (_pendingResult != null) {
            val resultMap = object : HashMap<String, Any?>() {
                init {
                    put("status", "error")
                    put("errorMessage", exception.message)
                }
            }

            _pendingResult!!.success(resultMap)
            _pendingResult = null
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent): Boolean {
        if (_authClientInstance != null) {
            _authClientInstance!!.onActivityResult(requestCode, resultCode, data)
        }

        return false
    }

}
