import com.sap.gateway.ip.core.customdev.util.Message
import groovy.json.JsonSlurper
import groovy.xml.MarkupBuilder
import java.io.StringWriter

def Message processData(Message message) {

    def log = messageLogFactory.getMessageLog(message)

    // --- 1) Parse JSON safely ---
    def bodyStr = message.getBody(String) as String
    List rows = []
    try {
        if (bodyStr && bodyStr.trim()) {
            def parsed = new JsonSlurper().parseText(bodyStr)
            if (parsed instanceof Map && parsed.value instanceof List) {
                rows = parsed.value
            } else if (parsed instanceof List) {
                rows = parsed
            }
        }
    } catch (Exception ex) {
        if (log) log.addAttachmentAsString("JSON Parse Error", ex.message, "text/plain")
    }

    // --- 2) Build XML safely ---
    def sw = new StringWriter()
    def xml = new MarkupBuilder(sw)

    xml.Items {
        rows.eachWithIndex { row, idx ->
            try {
                if (row instanceof Map) {
                    def idVal = row.id?.toString() ?: ""
                    def delVal = row.deleted
                    String deletedStr
                    if (delVal instanceof Boolean) {
                        deletedStr = delVal.toString()
                    } else if (delVal == null) {
                        deletedStr = "false"
                    } else {
                        deletedStr = delVal.toString().trim().equalsIgnoreCase("true") ? "true" : "false"
                    }

                    Item {
                        id(idVal)
                        deleted(deletedStr)
                    }
                }
            } catch (Exception innerEx) {
                if (log) log.addAttachmentAsString("Item Error at index ${idx}", innerEx.message, "text/plain")
            }
        }
    }

    // --- 3) Set body ---
    message.setBody(sw.toString())
    message.setHeader("Content-Type", "application/xml")
    return message
}