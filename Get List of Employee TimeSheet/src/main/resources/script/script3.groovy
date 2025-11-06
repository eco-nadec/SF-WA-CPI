import com.sap.gateway.ip.core.customdev.util.Message
import groovy.json.*
import java.time.*

def Message processData(Message message) {

    // --- Helpers ---
    def toZoneOffset = { String raw ->
        if (!raw) return ZoneOffset.UTC
        def s = raw.trim()
        if (s == "Z") return ZoneOffset.UTC
        // handles +0300 / -0500
        if (s ==~ /[+-]\d{4}/) return ZoneOffset.of(s[0..2] + ":" + s[3..4])
        // handles +03:00 / -05:00
        if (s ==~ /[+-]\d{2}:\d{2}/) return ZoneOffset.of(s)
        return ZoneOffset.UTC
    }

    def localDateOf = { String tsUtc, String tz ->
        def off = toZoneOffset(tz)
        return Instant.parse(tsUtc).atOffset(off).toLocalDate().toString()
    }

    def localTimeOf = { String tsUtc, String tz ->
        def off = toZoneOffset(tz)
        return Instant.parse(tsUtc).atOffset(off).toLocalTime().toString() // HH:mm:ss
    }

    // --- Read body ---
    def body = message.getBody(String)
    def json = new JsonSlurper().parseText(body)
    def events = (json?.value instanceof List) ? json.value : []

    // Keep only well-formed events with a timestamp and an assignment/employee id
    def cleaned = events.findAll { ev ->
        ev?.timestampUTC && (ev?.workAssignmentId || ev?.employeeId)
    }

    // --- Group by (employee, localDate) ---
    // employee = workAssignmentId (fallback to employeeId if ever needed)
    def grouped = cleaned.groupBy { ev ->
        def emp = (ev.workAssignmentId ?: ev.employeeId)?.toString()
        def d   = localDateOf(ev.timestampUTC as String, ev.timeZoneOffset as String)
        // composite key
        return emp + "§" + d
    }

    def results = []
    grouped.each { key, list ->
        def (emp, localDate) = key.split("§", 2)

        // Sort by UTC timestamp
        def sorted = list.sort { a, b -> (a.timestampUTC as String) <=> (b.timestampUTC as String) }

        // C10 = Check-In, C20 = Check-Out
        def inEv  = sorted.find { (it.timeEventTypeCode as String)?.equalsIgnoreCase("C10") }
        def outEv = sorted.reverse().find { (it.timeEventTypeCode as String)?.equalsIgnoreCase("C20") } // last C20

        // Prefer each event's own offset for its local time; if missing, fallback to the other’s
        def inOffTz  = (inEv?.timeZoneOffset ?: outEv?.timeZoneOffset) as String
        def outOffTz = (outEv?.timeZoneOffset ?: inEv?.timeZoneOffset) as String

        def checkInTimeLocal  = inEv  ? localTimeOf(inEv.timestampUTC as String, inOffTz)   : null
        def checkOutTimeLocal = outEv ? localTimeOf(outEv.timestampUTC as String, outOffTz) : null

        results << [
            employeeId       : emp,
            checkInId        : (inEv?.externalId as String),
            checkOutId       : (outEv?.externalId as String),
            checkInTimeLocal : checkInTimeLocal,
            checkOutTimeLocal: checkOutTimeLocal,
            date             : localDate
        ]
    }

    // Optional: sort output by employeeId then date
    results.sort { a, b ->
        (a.employeeId <=> b.employeeId) ?: (a.date <=> b.date)
    }

    message.setBody(JsonOutput.toJson(results))
    message.setHeader("Content-Type", "application/json")
    return message
}