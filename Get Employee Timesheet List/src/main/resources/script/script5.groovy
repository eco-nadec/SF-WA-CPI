import com.sap.gateway.ip.core.customdev.util.Message
import groovy.json.JsonOutput

def Message processData(Message message) {

    // === 1) Parse batch XML from message body ===
    def bodyStr = message.getBody(String)
    def batchXml = new XmlSlurper().parseText(bodyStr)

    // === 2) Extract filter value ===
    def filter = batchXml.filter?.text()

    if (filter) {
        // Set filter as property for HTTP adapter to use
        // The HTTP adapter in .iflw uses: $filter=${property.TimeEventFilter}
        message.setProperty("TimeEventFilter", filter)
    }

    // === 3) Extract work assignments and store as JSON ===
    def workAssignments = []

    batchXml.workAssignments.wa.each { waNode ->
        workAssignments << [
            id: waNode.@id.text(),
            employeeId: waNode.@employeeId.text(),
            date: waNode.@date.text(),
            startTime: waNode.@startTime.text(),
            endTime: waNode.@endTime.text()
        ]
    }

    // Store work assignments as JSON string in property for later matching in script6
    def waJson = JsonOutput.toJson(workAssignments)
    message.setProperty("CurrentBatchWorkAssignments", waJson)

    // Optional: Set batch metadata for monitoring
    message.setProperty("BatchNumber", batchXml.batchNumber?.text() ?: "0")
    message.setProperty("RecordCount", batchXml.recordCount?.text() ?: "0")

    return message
}
