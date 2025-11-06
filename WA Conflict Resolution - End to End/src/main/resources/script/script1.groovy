import com.sap.gateway.ip.core.customdev.util.Message
import groovy.json.JsonSlurper
import groovy.xml.MarkupBuilder
import java.io.StringWriter

def Message processData(Message message) {

    // Get JSON input
    def bodyStr = message.getBody(String)
    def json = new JsonSlurper().parseText(bodyStr)

    // Prepare XML writer
    def writer = new StringWriter()
    def xml = new MarkupBuilder(writer)

    // Build XML
    xml.Records {
        json.each { rec ->
            Record {
                id(rec.id)
                employeeId(rec.employeeId)
                date(rec.date)
                startTime(rec.startTime)
                endTime(rec.endTime)
            }
        }
    }

    // Set message body as XML
    message.setBody(writer.toString())
    message.setHeader("Content-Type", "application/xml")

    return message
}