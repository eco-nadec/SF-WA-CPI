import com.sap.gateway.ip.core.customdev.util.Message

def Message processData(Message message) {

    // This script runs immediately before the splitter
    // to ensure the XML is in the message body

    // Check if body is already XML
    def body = message.getBody(String)

    if (body && body.trim().startsWith('<?xml')) {
        // Body already has XML, just ensure correct content type
        message.setHeader("Content-Type", "text/xml; charset=UTF-8")

        // Log for debugging
        def messageLog = messageLogFactory.getMessageLog(message)
        if (messageLog) {
            messageLog.addAttachmentAsString(
                "PreSplit_Body_Already_XML",
                "Body starts with: " + body.substring(0, Math.min(200, body.length())),
                "text/plain"
            )
        }
    } else {
        // Body doesn't have XML - restore from property
        def xmlFromProperty = message.getProperty("BatchedXML")

        if (xmlFromProperty) {
            message.setBody(xmlFromProperty)
            message.setHeader("Content-Type", "text/xml; charset=UTF-8")

            // Log for debugging
            def messageLog = messageLogFactory.getMessageLog(message)
            if (messageLog) {
                messageLog.addAttachmentAsString(
                    "PreSplit_Body_Restored_From_Property",
                    "Restored XML: " + xmlFromProperty.substring(0, Math.min(200, xmlFromProperty.length())),
                    "text/plain"
                )
            }
        } else {
            // No XML found anywhere - log error
            def messageLog = messageLogFactory.getMessageLog(message)
            if (messageLog) {
                messageLog.addAttachmentAsString(
                    "PreSplit_ERROR_No_XML_Found",
                    "Body: " + (body ? body.substring(0, Math.min(500, body.length())) : "null") +
                    "\nBatchedXML property: " + (xmlFromProperty ?: "null"),
                    "text/plain"
                )
            }
        }
    }

    return message
}
