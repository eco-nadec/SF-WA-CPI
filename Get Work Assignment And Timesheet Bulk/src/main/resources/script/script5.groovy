import com.sap.gateway.ip.core.customdev.util.Message
import groovy.json.*

def Message processData(Message message) {

    // --- 1) Read inputs ---
    // Body: JSON array of timesheets
    String bodyStr = message.getBody(String) as String
    def timesheets = new JsonSlurper().parseText(bodyStr) as List

    // Property: JSON array of work assignments
    String waProp = (message.getProperty("workAssigmentData") as String) ?: "[]"
    def waList = new JsonSlurper().parseText(waProp) as List

    // --- 2) Index timesheets by (employeeId|date) for lookup ---
    Map<String, Map> tsIndex = [:]
    (timesheets ?: []).each { ts ->
        def emp = (ts.employeeId ?: "").toString()
        def d   = (ts.date ?: "").toString()
        if (emp && d) {
            tsIndex["${emp}|${d}"] = [
                employeeId: emp,
                date      : d,
                checkIn   : [
                    id       : (ts.checkInId ?: null),
                    timeLocal: (ts.checkInTimeLocal ?: null)
                ],
                checkOut  : [
                    id       : (ts.checkOutId ?: null),
                    timeLocal: (ts.checkOutTimeLocal ?: null)
                ]
            ]
        }
    }

    // --- 3) Build combined result ---
    List result = []
    (waList ?: []).each { wa ->
        def emp = (wa.employeeId ?: "").toString()
        def d   = (wa.date ?: "").toString()
        def key = "${emp}|${d}"

        // workAssignment block
        def workAssignment = [
            id        : (wa.id ?: ""),
            employeeId: emp,
            date      : d,
            startTime : (wa.startTime ?: ""),
            endTime   : (wa.endTime   ?: "")
        ]

        // timesheet block (from index; if missing, set nulls)
        def ts = tsIndex[key]
        def timesheet = [
            employeeId: emp,
            date      : d,
            checkIn   : [
                id       : ts?.checkIn?.id,
                timeLocal: ts?.checkIn?.timeLocal
            ],
            checkOut  : [
                id       : ts?.checkOut?.id,
                timeLocal: ts?.checkOut?.timeLocal
            ]
        ]

        result << [workAssignment: workAssignment, timesheet: timesheet]
    }

    // --- 4) Output JSON ---
    message.setBody(JsonOutput.toJson(result))
    message.setHeader("Content-Type", "application/json")

    return message
}