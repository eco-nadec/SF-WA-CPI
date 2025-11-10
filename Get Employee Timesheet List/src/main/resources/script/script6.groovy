import com.sap.gateway.ip.core.customdev.util.Message
import groovy.json.JsonSlurper
import groovy.json.JsonOutput
import java.time.*

def Message processData(Message message) {

    // === 1) Read original work assignments from property ===
    def allWaJson = message.getProperty("AllOriginalWorkAssignments")
    if (!allWaJson) {
        // No work assignments stored, return empty array
        message.setBody(JsonOutput.toJson([]))
        message.setHeader("Content-Type", "application/json")
        return message
    }

    def workAssignments = new JsonSlurper().parseText(allWaJson) as List

    // === 2) Parse aggregated SF TimeEvent responses ===
    // The Gather step with "Combine" concatenates all HTTP responses
    // Format: {"@odata.context":"...","value":[...]}{"@odata.context":"...","value":[...]}
    def bodyStr = message.getBody(String)

    // Parse all SF events from gathered responses
    def allTimesheetEvents = []

    if (!bodyStr || bodyStr.trim().isEmpty()) {
        // No responses from Gather - return empty results
        message.setBody(JsonOutput.toJson([]))
        message.setHeader("Content-Type", "application/json")
        return message
    }

    // Split concatenated JSON objects by finding "}{"
    def jsonObjects = []

    if (bodyStr.contains("}{")) {
        // Multiple concatenated JSON objects
        def parts = bodyStr.split(/\}\{/)
        parts.eachWithIndex { part, idx ->
            if (idx == 0) {
                // First part - add closing brace
                jsonObjects << part + "}"
            } else if (idx == parts.size() - 1) {
                // Last part - add opening brace
                jsonObjects << "{" + part
            } else {
                // Middle parts - add both braces
                jsonObjects << "{" + part + "}"
            }
        }
    } else {
        // Single JSON object
        jsonObjects << bodyStr
    }

    // Parse each JSON object and extract time events
    jsonObjects.each { jsonStr ->
        try {
            def json = new JsonSlurper().parseText(jsonStr)

            if (json?.value instanceof List) {
                allTimesheetEvents.addAll(json.value)
            }
        } catch (Exception e) {
            // Log parsing error
            def messageLog = messageLogFactory.getMessageLog(message)
            if (messageLog) {
                messageLog.addAttachmentAsString(
                    "Script6_JSON_Parse_Error",
                    "Failed to parse JSON: ${jsonStr}\nError: ${e.message}",
                    "text/plain"
                )
            }
        }
    }

    // === 3) Group timesheet events by (employeeId, date) ===
    def toZoneOffset = { String raw ->
        if (!raw) return ZoneOffset.UTC
        def s = raw.trim()
        if (s == "Z") return ZoneOffset.UTC
        if (s ==~ /[+-]\d{4}/) return ZoneOffset.of(s[0..2] + ":" + s[3..4])
        if (s ==~ /[+-]\d{2}:\d{2}/) return ZoneOffset.of(s)
        return ZoneOffset.UTC
    }

    def localDateOf = { String tsUtc, String tz ->
        try {
            def off = toZoneOffset(tz)
            return Instant.parse(tsUtc).atOffset(off).toLocalDate().toString()
        } catch (Exception e) {
            return null
        }
    }

    def localTimeOf = { String tsUtc, String tz ->
        try {
            def off = toZoneOffset(tz)
            return Instant.parse(tsUtc).atOffset(off).toLocalTime().toString() // HH:mm:ss
        } catch (Exception e) {
            return null
        }
    }

    // Group by (employeeId, date)
    def timesheetGroups = [:] // key: "employeeId_date", value: {checkIn: {...}, checkOut: {...}}

    allTimesheetEvents.each { event ->
        def empId = (event.workAssignmentId ?: event.employeeId)?.toString()
        def tsUtc = event.timestampUTC?.toString()
        def tz = event.timeZoneOffset?.toString()
        def typeCode = event.timeEventTypeCode?.toString()

        if (!empId || !tsUtc) return

        def localDate = localDateOf(tsUtc, tz)
        if (!localDate) return

        def key = "${empId}_${localDate}"

        if (!timesheetGroups[key]) {
            timesheetGroups[key] = [
                employeeId: empId,
                date: localDate,
                checkInEvents: [],
                checkOutEvents: []
            ]
        }

        def group = timesheetGroups[key]

        if (typeCode?.equalsIgnoreCase("C10")) {
            group.checkInEvents << event
        } else if (typeCode?.equalsIgnoreCase("C20")) {
            group.checkOutEvents << event
        }
    }

    // === 4) Extract first C10 and last C20 for each group ===
    def timesheetSummary = [:] // key: "employeeId_date", value: {checkIn: {...}, checkOut: {...}}

    timesheetGroups.each { key, group ->
        // Sort by timestampUTC
        def sortedCheckIns = group.checkInEvents.sort { it.timestampUTC as String }
        def sortedCheckOuts = group.checkOutEvents.sort { it.timestampUTC as String }

        def firstCheckIn = sortedCheckIns ? sortedCheckIns.first() : null
        def lastCheckOut = sortedCheckOuts ? sortedCheckOuts.last() : null

        if (firstCheckIn || lastCheckOut) {
            timesheetSummary[key] = [
                employeeId: group.employeeId,
                date: group.date,
                checkIn: firstCheckIn ? [
                    id: firstCheckIn.externalId?.toString() ?: "",
                    timeLocal: localTimeOf(firstCheckIn.timestampUTC as String, firstCheckIn.timeZoneOffset as String) ?: ""
                ] : null,
                checkOut: lastCheckOut ? [
                    id: lastCheckOut.externalId?.toString() ?: "",
                    timeLocal: localTimeOf(lastCheckOut.timestampUTC as String, lastCheckOut.timeZoneOffset as String) ?: ""
                ] : null
            ]
        }
    }

    // === 5) Match work assignments to timesheets ===
    def results = []

    workAssignments.each { wa ->
        def empId = wa.employeeId?.toString()
        def date = wa.date?.toString()

        if (!empId || !date) return

        def key = "${empId}_${date}"
        def timesheet = timesheetSummary[key]

        // Only include work assignments that have matching timesheets
        if (timesheet) {
            results << [
                workAssignment: [
                    id: wa.id?.toString() ?: "",
                    employeeId: empId,
                    date: date,
                    startTime: wa.startTime?.toString() ?: "",
                    endTime: wa.endTime?.toString() ?: ""
                ],
                timesheet: [
                    employeeId: timesheet.employeeId ?: "",
                    date: timesheet.date ?: "",
                    checkIn: timesheet.checkIn ?: [id: "", timeLocal: ""],
                    checkOut: timesheet.checkOut ?: [id: "", timeLocal: ""]
                ]
            ]
        }
    }

    // === 6) Output final result ===
    message.setBody(JsonOutput.toJson(results))
    message.setHeader("Content-Type", "application/json")

    // Optional: Add statistics for monitoring
    message.setProperty("TotalWorkAssignmentsInput", workAssignments.size())
    message.setProperty("TotalTimesheetEventsFound", allTimesheetEvents.size())
    message.setProperty("TotalMatchedResults", results.size())
    message.setProperty("TotalUnmatchedWorkAssignments", workAssignments.size() - results.size())

    return message
}
