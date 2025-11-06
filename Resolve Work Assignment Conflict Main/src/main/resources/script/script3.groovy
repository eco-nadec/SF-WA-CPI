import com.sap.gateway.ip.core.customdev.util.Message
import groovy.json.JsonSlurper
import groovy.xml.MarkupBuilder
import java.io.StringWriter

def Message processData(Message message) {

    // --- 1) Read inputs ---
    // Body: JSON array of timesheet rows
    def bodyStr = message.getBody(String) as String
    def rows = new JsonSlurper().parseText(bodyStr) as List

    // Property: previously saved full XML (work assignment)
    def waXml = (message.getProperty("workAssignmentData") as String) ?: ""
    // Remove XML declaration if present
    waXml = waXml.replaceFirst(/(?s)^\s*<\?xml[^>]*\?>\s*/, "")

    // --- 2) Build combined XML fragment ---
    def sw = new StringWriter()
    def xml = new MarkupBuilder(sw)

    xml.Payload {
        WorkAssignment {
            if (waXml) {
                mkp.yieldUnescaped(waXml)
            }
        }
        Timesheet {
            (rows ?: []).each { r ->
                Entry {
                    employeeId( r.employeeId ?: "" )
                    date(       r.date       ?: "" )
                    CheckIn {
                        id(        r.checkInId        ?: "" )
                        timeLocal( r.checkInTimeLocal ?: "" )
                    }
                    CheckOut {
                        id(        r.checkOutId        ?: "" )
                        timeLocal( r.checkOutTimeLocal ?: "" )
                    }
                }
            }
        }
    }

    // --- 3) Output without XML declaration ---
    message.setBody(sw.toString())
    message.setHeader("Content-Type", "application/xml")
    return message
}