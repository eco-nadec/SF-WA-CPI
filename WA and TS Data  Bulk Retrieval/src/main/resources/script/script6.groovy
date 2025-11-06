import com.sap.gateway.ip.core.customdev.util.Message

def Message processData(Message message) {

    // Read the body as String
    String body = message.getBody(String) as String

    // Save body into property "workAssigmentData"
    message.setProperty("workAssigmentData", body)

    // (Optional) Clear the body if you don’t want to pass it further
    // message.setBody("")

    return message
}