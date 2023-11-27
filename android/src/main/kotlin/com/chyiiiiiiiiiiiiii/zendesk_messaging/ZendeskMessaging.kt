import android.content.Intent
import com.chyiiiiiiiiiiiiii.zendesk_messaging.ZendeskMessagingPlugin
import io.flutter.plugin.common.MethodChannel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import zendesk.android.Zendesk
import zendesk.android.ZendeskResult
import zendesk.android.ZendeskUser
import zendesk.messaging.android.DefaultMessagingFactory


class ZendeskMessaging(private val plugin: ZendeskMessagingPlugin, private val channel: MethodChannel) {
    companion object {
        const val tag = "[ZendeskMessaging]"

        // Method channel callback keys
        const val initializeSuccess: String = "initialize_success"
        const val initializeFailure: String = "initialize_failure"
        const val loginSuccess: String = "login_success"
        const val loginFailure: String = "login_failure"
        const val logoutSuccess: String = "logout_success"
        const val logoutFailure: String = "logout_failure"
    }


    fun initialize(channelKey: String) {
        println("$tag - Channel Key - $channelKey")
        Zendesk.initialize(
            plugin.activity!!,
            channelKey,
            successCallback = { value ->
                plugin.isInitialized = true;
                println("$tag - initialize success - $value")
                channel.invokeMethod(initializeSuccess, null)
            },
            failureCallback = { error ->
                plugin.isInitialized = false;
                println("$tag - initialize failure - $error")
                channel.invokeMethod(initializeFailure, mapOf("error" to error.message))
            },
            messagingFactory = DefaultMessagingFactory()
        )
    }

    fun invalidate() {
        Zendesk.invalidate()
        plugin.isInitialized = false;
        println("$tag - invalidated")
    }

    fun show() {
        Zendesk.instance.messaging.showMessaging(plugin.activity!!, Intent.FLAG_ACTIVITY_NEW_TASK)
        println("$tag - show")
    }

    fun getUnreadMessageCount(): Int {
        return try {
            Zendesk.instance.messaging.getUnreadMessageCount()
        }catch (error: Throwable){
            0
        }
    }

    fun setConversationTags(tags: List<String>){
        Zendesk.instance.messaging.setConversationTags(tags)
    }

    fun clearConversationTags(){
        Zendesk.instance.messaging.clearConversationTags()
    }
   fun setConversationFields(data:Map<String,Any>){
        Zendesk.instance.messaging.setConversationFields(data
        )
    }
    fun loginUser(jwt: String) {
        Zendesk.instance.loginUser(
            jwt,
            { value: ZendeskUser? ->
                plugin.isLoggedIn = true;
                value?.let {
                    channel.invokeMethod(loginSuccess, mapOf("id" to it.id, "externalId" to it.externalId))
                } ?: run {
                    channel.invokeMethod(loginSuccess, mapOf("id" to null, "externalId" to null))
                }
            },
            { error: Throwable? ->
                println("$tag - Login failure : ${error?.message}")
                println(error)
                channel.invokeMethod(loginFailure, mapOf("error" to error?.message))
            })
    }

    fun logoutUser() {
        GlobalScope.launch (Dispatchers.Main)  {
            try {
                Zendesk.instance.logoutUser(successCallback = {
                    plugin.isLoggedIn = false;
                    channel.invokeMethod(logoutSuccess, null)
                }, failureCallback = {
                    channel.invokeMethod(logoutFailure, null)
                });
            } catch (error: Throwable) {
                println("$tag - Logout failure : ${error.message}")
                channel.invokeMethod(logoutFailure, mapOf("error" to error.message))
            }
        }
    }
}


