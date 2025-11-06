import com.sap.gateway.ip.core.customdev.util.Message
import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import java.time.*
import java.time.format.DateTimeFormatter

def Message processData(Message message) {

    // Read input JSON
    def bodyStr = message.getBody(String) as String
    def input = new JsonSlurper().parseText(bodyStr)

    // Extract fields
    def empId    = input.EmpId?.toString()
    def dateStr  = input.Date?.toString()     // e.g. "2025-08-13"
    def checkIn  = input.Checkin?.toString()  // "10:00"
    def checkOut = input.Checkout?.toString() // "18:00"
    def tzStr    = input.Timezone ?: "Asia/Riyadh"

    // Ensure date is in yyyy-MM-dd format
    def formatterDate = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    def localDate = LocalDate.parse(dateStr, formatterDate)

    // Timezone
    def zone = ZoneId.of(tzStr)

    // Parse times
    def timeFormatter = DateTimeFormatter.ofPattern("HH:mm")
    def checkInTime = LocalTime.parse(checkIn, timeFormatter)
    def checkOutTime = LocalTime.parse(checkOut, timeFormatter)

    // Strict timestamp formatter for SF: yyyy-MM-dd'T'HH:mm:ssZ
    def sfFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssZ")

    // Build timestamps in correct format
    def checkInTs = ZonedDateTime.of(localDate, checkInTime, zone).format(sfFormatter)
    def checkOutTs = ZonedDateTime.of(localDate, checkOutTime, zone).format(sfFormatter)

    // Create target structure
    def events = [
        [
            id           : "1",
            assignmentId : empId,
            typeCode     : "C10",              // Check-in
            timestamp    : checkInTs,
            terminalId   : "Manual",
            timeTypeCode : "8000" //"WORKING_TIME"
        ],
        [
            id           : "2",
            assignmentId : empId,
            typeCode     : "C20",              // Check-out
            timestamp    : checkOutTs,
            terminalId   : "Manual",
            timeTypeCode : "8000"  // "WORKING_TIME"
        ]
    ]

    // Output JSON
    def outJson = JsonOutput.prettyPrint(JsonOutput.toJson(events))
    message.setBody(outJson)
    message.setHeader("Content-Type", "application/json")

    return message
}