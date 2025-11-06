import com.sap.gateway.ip.core.customdev.util.Message
import groovy.util.XmlSlurper   // <-- correct package for CPI

def Message processData(Message message) {

    // Read the XML body as String
    def bodyStr = message.getBody(String) as String

    // Save the entire XML in a property for later mixing/merge
    message.setProperty("workAssignmentData", bodyStr)

    // Parse XML (tolerates simple XML without namespaces)
    def root = new XmlSlurper().parseText(bodyStr)

    // Extract fields
    def recordId   = root.id.text()
    def employeeId = root.employeeId.text()
    def date       = root.date.text()
    def startTime  = root.startTime.text()
    def endTime    = root.endTime.text()

    // Save selected values as CPI properties
    message.setProperty("EmployeeId", employeeId)
    message.setProperty("Date", date)

    // Optional logging
    def log = messageLogFactory.getMessageLog(message)
    if (log) {
        log.addAttachmentAsString(
            "Extracted Data",
            "Saved workAssignmentData.\nID=${recordId}, EmployeeId=${employeeId}, Date=${date}, Start=${startTime}, End=${endTime}",
            "text/plain"
        )
    }

    return message
}