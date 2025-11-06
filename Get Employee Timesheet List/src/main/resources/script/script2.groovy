import com.sap.gateway.ip.core.customdev.util.Message
import groovy.json.*
import java.time.*

def Message processData(Message message) {

    def body = message.getBody(String)
    def json = new JsonSlurper().parseText(body)
    def events = (json?.value instanceof List) ? json.value : []

    def toZoneOffset = { String raw ->
        if (!raw) return ZoneOffset.UTC
        def s = raw.trim()
        if (s == "Z") return ZoneOffset.UTC
        if (s ==~ /[+-]\d{4}/) {
            return ZoneOffset.of(s[0..2] + ":" + s[3..4])
        }
        return ZoneOffset.UTC
    }

    def byLocalDate = events.groupBy { ev ->
        try {
            def ts = Instant.parse(ev.timestampUTC as String)
            def off = toZoneOffset(ev.timeZoneOffset as String)
            return ts.atOffset(off).toLocalDate().toString()
        } catch (Throwable ignore) {
            return "_invalid_"
        }
    }

    def results = []
    byLocalDate.each { localDate, list ->
        if (localDate == "_invalid_") return
        def sorted = list.findAll { it?.timestampUTC }.sort { it.timestampUTC as String }

        def checkIns  = sorted.findAll { (it.timeEventTypeCode as String)?.equalsIgnoreCase("C10") }
        def checkOuts = sorted.findAll { (it.timeEventTypeCode as String)?.equalsIgnoreCase("C20") }

        def inEv  = checkIns ? checkIns.first() : null
        def outEv = checkOuts ? checkOuts.last() : null
        def off = toZoneOffset((inEv?.timeZoneOffset ?: outEv?.timeZoneOffset) as String)

        results << [
            employeeId       : (inEv?.workAssignmentId ?: outEv?.workAssignmentId) as String,
            checkInId        : (inEv?.externalId)  as String,
            checkOutId       : (outEv?.externalId) as String,
            checkInTimeLocal : (inEv ? Instant.parse(inEv.timestampUTC as String).atOffset(off).toLocalTime().toString() : null),
            checkOutTimeLocal: (outEv ? Instant.parse(outEv.timestampUTC as String).atOffset(off).toLocalTime().toString() : null),
            date             : localDate
        ]
    }

    message.setBody(JsonOutput.toJson(results))
    message.setHeader("Content-Type", "application/json")
    return message
}
