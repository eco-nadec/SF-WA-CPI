import com.sap.gateway.ip.core.customdev.util.Message
import groovy.json.JsonSlurper
import groovy.json.JsonOutput

def Message processData(Message message) {

    // Read body as JSON
    def bodyStr = message.getBody(String) as String
    def root = new JsonSlurper().parseText(bodyStr) as Map

    // Extract sections with safe defaults
    def timesheetDeleteObj      = (root.timesheetDelete      ?: [value: []]) as Map
    def workAssignmentDeleteObj = (root.workAssignmentDelete ?: [value: []]) as Map
    def timeEventInsertArr      = (root.timeEventInsert      ?: [])          as List
    def resolvedItemsArr        = (root.resolvedItems        ?: [])          as List

    // Store as JSON strings in properties (pretty or compact—pick one)
    message.setProperty("deletedTimesheet",
        JsonOutput.prettyPrint(JsonOutput.toJson(timesheetDeleteObj)))
    message.setProperty("deletedWorkAssignment",
        JsonOutput.prettyPrint(JsonOutput.toJson(workAssignmentDeleteObj)))
    message.setProperty("timeEventInsert",
        JsonOutput.prettyPrint(JsonOutput.toJson(timeEventInsertArr)))
    message.setProperty("resolvedItems",
        JsonOutput.prettyPrint(JsonOutput.toJson(resolvedItemsArr)))

    // Handy counts
    message.setProperty("deletedTimesheetCount",
        (timesheetDeleteObj.value ?: []).size())
    message.setProperty("deletedWorkAssignmentCount",
        (workAssignmentDeleteObj.value ?: []).size())
    message.setProperty("timeEventInsertCount",
        timeEventInsertArr.size())

    // Optional: keep body unchanged or replace with a short summary
    // message.setBody(bodyStr)

    // Optional: log a quick summary
    def log = messageLogFactory.getMessageLog(message)
    if (log) {
        def summary = """Deleted TS: ${(timesheetDeleteObj.value ?: []).size()}
Deleted WA: ${(workAssignmentDeleteObj.value ?: []).size()}
Inserts: ${timeEventInsertArr.size()}"""
        log.addAttachmentAsString("Aggregation Summary", summary, "text/plain")
    }

    return message
}