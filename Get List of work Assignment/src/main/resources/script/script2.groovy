import com.sap.gateway.ip.core.customdev.util.Message
import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import java.time.*

def Message processData(Message message) {

    // --- Helpers ---
    // Parse SAP OData date format: "/Date(1754784000000)/"
    def parseSapODataDate = { String s, ZoneId zone ->
        if (!s) return null
        def m = (s =~ /Date\((\d+)([+-]\d{4})?\)/)
        if (!m.find()) return null
        long ms = m.group(1) as long
        return Instant.ofEpochMilli(ms).atZone(zone).toLocalDate()
    }

    // Parse ISO-8601 duration like "PT14H"
    def parseIsoDurationToTime = { String dur ->
        if (!dur) return null
        def d = Duration.parse(dur)
        return LocalTime.MIDNIGHT.plus(d)
    }

    // --- Read body ---
    def bodyStr = message.getBody(String) as String
    def json = new JsonSlurper().parseText(bodyStr)

    // Business timezone
    ZoneId zone = ZoneId.of("Asia/Riyadh")

    // --- Transform ---
    def list = []
    (json?.d?.results ?: []).each { r ->
        def date      = parseSapODataDate(r.startDate as String, zone)
        def startTime = parseIsoDurationToTime(r.startTime as String)
        def endTime   = parseIsoDurationToTime(r.endTime as String)

        list << [
            id         : r.externalCode,
            employeeId : r.userId,
            date       : date?.toString(),
            startTime  : startTime?.toString()?.substring(0,5),
            endTime    : endTime?.toString()?.substring(0,5)
        ]
    }

    // --- Write result ---
    def outJson = JsonOutput.toJson(list)
    message.setBody(outJson)
    message.setHeader("Content-Type", "application/json")

    return message
}
