package androidx.iot.link

internal class Topics(
    private val productKey: String,
    private val deviceName: String,
) {
    fun PUB_OTA_INFORM(): String {
        return "/ota/device/inform/$productKey/$deviceName"
    }

    fun PUB_OTA_PROGRESS(): String {
        return "/ota/device/progress/$productKey/$deviceName"
    }

    fun SUB_OTA_UPGRADE(): String {
        return "/ota/device/upgrade/$productKey/$deviceName"
    }

    fun SUB_REGISTER(): String {
        return "/ext/register"
    }

    fun SUB_REGNWL(): String {
        return "/ext/regnwl"
    }

    fun PUB_OTA_FIRMWARE_GET(): String {
        return "/sys/$productKey/$deviceName/thing/ota/firmware/get"
    }

    fun SUB_OTA_FIRMWARE_GET(): String {
        return "/sys/$productKey/$deviceName/thing/ota/firmware/get_reply"
    }

    fun PUB_DISABLE(): String {
        return "/sys/$productKey/$deviceName/thing/disable"
    }

    fun SUB_DISABLE_REPLY(): String {
        return "/sys/$productKey/$deviceName/thing/disable_reply"
    }

    fun PUB_ENABLE(): String {
        return "/sys/$productKey/$deviceName/thing/enable"
    }

    fun SUB_ENABLE_REPLY(): String {
        return "/sys/$productKey/$deviceName/thing/enable_reply"
    }

    fun PUB_DELETE(): String {
        return "/sys/$productKey/$deviceName/thing/delete"
    }

    fun SUB_DELETE_REPLY(): String {
        return "/sys/$productKey/$deviceName/thing/delete_reply"
    }

    fun PUB_PROPERTY_POST(): String {
        return "/sys/$productKey/$deviceName/thing/event/property/post"
    }

    fun SUB_PROPERTY_POST_REPLY(): String {
        return "/sys/$productKey/$deviceName/thing/model/post_reply"
    }

    fun SUB_PROPERTY_SET(): String {
        return "/sys/$productKey/$deviceName/thing/service/property/set"
    }

    fun SUB_SECURE_TUNNEL_NOTIFY(): String {
        return "/sys/$productKey/$deviceName/secure_tunnel/notify"
    }

    fun PUB_SECURE_TUNNEL_PROXY(): String {
        return "/sys/$productKey/$deviceName/secure_tunnel/proxy/request"
    }

    fun SUB_SECURE_TUNNEL_PROXY(): String {
        return "/sys/$productKey/$deviceName/secure_tunnel/proxy/request_reply"
    }

    fun PUB_LOG_GET(): String {
        return "/sys/$productKey/$deviceName/thing/config/log/get"
    }

    fun SUB_LOG_GET_REPLY(): String {
        return "/sys/$productKey/$deviceName/thing/config/log/get_reply"
    }

    fun PUB_LOG_POST(): String {
        return "/sys/$productKey/$deviceName/thing/log/post"
    }

    fun SUB_LOG_POST_REPLY(): String {
        return "/sys/$productKey/$deviceName/thing/log/post_reply"
    }

    fun PUB_CONFIG_GET(): String = "/sys/$productKey/$deviceName/thing/config/get"
    fun SUB_CONFIG_GET_REPLY(): String = "/sys/$productKey/$deviceName/thing/config/get_reply"
    fun SUB_CONFIG_PUSH(): String = "/sys/$productKey/$deviceName/thing/config/push"
    fun SUB_CONFIG_PUSH_REPLY(): String = "/sys/$productKey/$deviceName/thing/config/push_reply"

    fun PUB_JOB_GET(): String = "/sys/$productKey/$deviceName/thing/job/get"
    fun SUB_JOB_GET_REPLY(): String = "/sys/$productKey/$deviceName/thing/job/get_reply"
    fun SUB_JOB_NOTIFY(): String = "/sys/$productKey/$deviceName/thing/job/notify"
    fun SUB_JOB_NOTIFY_REPLY(): String = "/sys/$productKey/$deviceName/thing/job/notify_reply"
    fun PUB_JOB_UPDATE(): String = "/sys/$productKey/$deviceName/thing/job/update"
    fun SUB_JOB_UPDATE_REPLY(): String = "/sys/$productKey/$deviceName/thing/job/update_reply"

    fun SUB_BOOTSTRAP_NOTIFY(): String = "/sys/$productKey/$deviceName/thing/bootstrap/notify"
    fun SUB_BOOTSTRAP_NOTIFY_REPLY(): String = "/sys/$productKey/$deviceName/thing/bootstrap/notify_reply"

    fun PUB_TOPO_ADD(): String = "/sys/$productKey/$deviceName/thing/topo/add"
    fun SUB_TOPO_ADD_REPLY(): String = "/sys/$productKey/$deviceName/thing/topo/add_reply"
    fun PUB_TOPO_DELETE(): String = "/sys/$productKey/$deviceName/thing/topo/delete"
    fun SUB_TOPO_DELETE_REPLY(): String = "/sys/$productKey/$deviceName/thing/topo/delete_reply"
    fun PUB_TOPO_GET(): String = "/sys/$productKey/$deviceName/thing/topo/get"
    fun SUB_TOPO_GET_REPLY(): String = "/sys/$productKey/$deviceName/thing/topo/get_reply"
    fun PUB_DEVICE_LIST_FOUND(): String = "/sys/$productKey/$deviceName/thing/list/found"
    fun SUB_DEVICE_LIST_FOUND_REPLY(): String = "/sys/$productKey/$deviceName/thing/list/found_reply"

    fun PUB_SUB_LOGIN(): String = "/ext/session/$productKey/$deviceName/combine/login"
    fun SUB_SUB_LOGIN_REPLY(): String = "/ext/session/$productKey/$deviceName/combine/login_reply"
    fun PUB_SUB_BATCH_LOGIN(): String = "/ext/session/$productKey/$deviceName/combine/batch_login"
    fun SUB_SUB_BATCH_LOGIN_REPLY(): String = "/ext/session/$productKey/$deviceName/combine/batch_login_reply"
    fun PUB_SUB_LOGOUT(): String = "/ext/session/$productKey/$deviceName/combine/logout"
    fun SUB_SUB_LOGOUT_REPLY(): String = "/ext/session/$productKey/$deviceName/combine/logout_reply"
    fun PUB_SUB_BATCH_LOGOUT(): String = "/ext/session/$productKey/$deviceName/combine/batch_logout"
    fun SUB_SUB_BATCH_LOGOUT_REPLY(): String = "/ext/session/$productKey/$deviceName/combine/batch_logout_reply"

    fun PUB_DESIRED_GET(): String = "/sys/$productKey/$deviceName/thing/property/desired/get"
    fun SUB_DESIRED_GET_REPLY(): String = "/sys/$productKey/$deviceName/thing/property/desired/get_reply"
    fun PUB_DESIRED_DELETE(): String = "/sys/$productKey/$deviceName/thing/property/desired/delete"
    fun SUB_DESIRED_DELETE_REPLY(): String = "/sys/$productKey/$deviceName/thing/property/desired/delete_reply"
    fun PUB_TAG_UPDATE(): String = "/sys/$productKey/$deviceName/thing/deviceinfo/update"
    fun SUB_TAG_UPDATE_REPLY(): String = "/sys/$productKey/$deviceName/thing/deviceinfo/update_reply"
    fun PUB_TAG_DELETE(): String = "/sys/$productKey/$deviceName/thing/deviceinfo/delete"
    fun SUB_TAG_DELETE_REPLY(): String = "/sys/$productKey/$deviceName/thing/deviceinfo/delete_reply"

    fun PUB_NETWORK_DIAGNOSTIC(): String = "/sys/$productKey/$deviceName/_thing/diag/post"
    fun SUB_NETWORK_DIAGNOSTIC_REPLY(): String = "/sys/$productKey/$deviceName/_thing/diag/post_reply"
    fun PUB_SHADOW_UPDATE(): String = "/shadow/update/$productKey/$deviceName"
    fun SUB_SHADOW_GET(): String = "/shadow/get/$productKey/$deviceName"

    fun PUB_FILE_UPLOAD_INIT(): String = "/sys/$productKey/$deviceName/thing/file/upload/mqtt/init"
    fun SUB_FILE_UPLOAD_INIT_REPLY(): String = "/sys/$productKey/$deviceName/thing/file/upload/mqtt/init_reply"
    fun PUB_FILE_UPLOAD_SEND(): String = "/sys/$productKey/$deviceName/thing/file/upload/mqtt/send"
    fun SUB_FILE_UPLOAD_SEND_REPLY(): String = "/sys/$productKey/$deviceName/thing/file/upload/mqtt/send_reply"
    fun PUB_FILE_UPLOAD_CANCEL(): String = "/sys/$productKey/$deviceName/thing/file/upload/mqtt/cancel"
    fun SUB_FILE_UPLOAD_CANCEL_REPLY(): String = "/sys/$productKey/$deviceName/thing/file/upload/mqtt/cancel_reply"
}
