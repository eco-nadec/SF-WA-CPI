import com.sap.gateway.ip.core.customdev.util.Message
import groovy.util.XmlSlurper
import groovy.json.JsonOutput

def Message processData(Message message) {

    // Read XML body
    def xmlStr = message.getBody(String) as String

    // Parse XML (ignore namespaces/DTD)
    def root = new XmlSlurper(false, false).parseText(xmlStr)

    // Collect <Item> nodes whether root is <Item> or a container like <Items>
    def items = (root.name() == 'Item') ? [root] : root.'**'.findAll { it.name() == 'Item' }

    // Build upsert payload(s) for items with deleted=true
    def payloads = items.collect { it ->
        def id = it.id?.text()?.trim()
        def deleted = it.deleted?.text()?.trim()?.equalsIgnoreCase("true")
        if (id && deleted) {
            return [
                "__metadata": [ "uri": "EmployeeTime" ],
                "externalCode": id,
                "approvalStatus": "CANCELLED"
            ]
        } else {
            return null
        }
    }.findAll { it != null }

    // If only one payload, return single object; else return array
    def outJson = (payloads.size() == 1)
        ? JsonOutput.prettyPrint(JsonOutput.toJson(payloads[0]))
        : JsonOutput.prettyPrint(JsonOutput.toJson(payloads))

    // Write back
    message.setBody(outJson)
    message.setHeader("Content-Type", "application/json")

    // (Optional) also expose in a property for reuse
    message.setProperty("UpsertPayload", outJson)

    return message
}