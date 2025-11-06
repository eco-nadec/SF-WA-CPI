import com.sap.gateway.ip.core.customdev.util.Message
import groovy.util.XmlSlurper          // <-- correct import for CPI
import groovy.json.JsonOutput

def Message processData(Message message) {

    // Read XML from body
    def bodyStr = message.getBody(String)
    def root = new XmlSlurper().parseText(bodyStr)

    def records = []

    // Loop through each <Payload>
    root.'**'.findAll { it.name() == 'Payload' }.each { payload ->

        // WorkAssignment
        def wa = payload.WorkAssignment.Record
        def waObj = [
            id         : wa.id.text(),
            employeeId : wa.employeeId.text(),
            date       : wa.date.text(),
            startTime  : wa.startTime.text(),
            endTime    : wa.endTime.text()
        ]

        // Timesheet (only one Entry per Payload in your sample)
        def ts = payload.Timesheet.Entry
        def tsObj = [
            employeeId : ts.employeeId.text(),
            date       : ts.date.text(),
            checkIn    : [
                id        : ts.CheckIn.id.text(),
                timeLocal : ts.CheckIn.timeLocal.text()
            ],
            checkOut   : [
                id        : ts.CheckOut.id.text(),
                timeLocal : ts.CheckOut.timeLocal.text()
            ]
        ]

        // Combine into single object per Payload
        records << [
            workAssignment : waObj,
            timesheet      : tsObj
        ]
    }

    // Convert to JSON
    def outJson = JsonOutput.prettyPrint(JsonOutput.toJson(records))
    message.setBody(outJson)
    message.setHeader("Content-Type", "application/json")

    return message
}