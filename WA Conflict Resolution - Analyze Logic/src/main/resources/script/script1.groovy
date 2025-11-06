import com.sap.gateway.ip.core.customdev.util.Message
import groovy.json.JsonSlurper
import groovy.json.JsonOutput
import java.time.*
import java.time.format.DateTimeFormatter

def Message processData(Message message) {

    def body  = message.getBody(String) as String
    def items = new JsonSlurper().parseText(body) as List

    // --- Helpers ---
    def tfHM  = DateTimeFormatter.ofPattern("HH:mm")
    def tfISO = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssZ") // SF strict
    def zone  = ZoneId.of("Asia/Riyadh")

    def parseT = { String s -> (s && s.trim()) ? LocalTime.parse(s, tfHM) : null }
    def fmtT   = { LocalTime t -> t?.format(tfHM) }
    def clamp  = { LocalTime t ->
        if (t == null) return null
        if (t.isBefore(LocalTime.MIN)) return LocalTime.MIN
        if (t.isAfter(LocalTime.of(23,59))) return LocalTime.of(23,59)
        return t
    }
    def toIsoTs = { String d, String hm ->
        (d && hm) ? LocalDate.parse(d).atTime(LocalTime.parse(hm, tfHM)).atZone(zone).format(tfISO) : null
    }

    // Aggregators
    def delTS = []   // timesheet deletes
    def delWA = []   // work assignment deletes
    def insTE = []   // inserts

    def resolved = items.collect { item ->
        def wa = item.workAssignment
        def ts = item.timesheet

        def outItem = [
            workAssignment: wa ? new LinkedHashMap(wa) : null,   // ✅ safe copy
            timesheet     : ts ? [
                employeeId: ts.employeeId, date: ts.date,
                checkIn:  [ id: ts.checkIn?.id,  timeLocal: ts.checkIn?.timeLocal ],
                checkOut: [ id: ts.checkOut?.id, timeLocal: ts.checkOut?.timeLocal ]
            ] : null,
            actions: []
        ]

        // Only proceed if WA + TS are both valid and comparable
        if (wa && ts && wa.employeeId && ts.employeeId && wa.date && ts.date &&
            wa.employeeId.toString() == ts.employeeId.toString() && wa.date == ts.date) {

            def waStart = parseT(wa.startTime)
            def waEnd   = parseT(wa.endTime)
            def tsInT   = parseT(ts.checkIn?.timeLocal)
            def tsOutT  = parseT(ts.checkOut?.timeLocal)

            if (waStart && waEnd && tsInT && tsOutT) {
                boolean overlaps = !(tsOutT.isBefore(waStart) || tsInT.isAfter(waEnd))

                if (overlaps) {
                    boolean waInsideTs = ( (waStart.equals(tsInT) || waStart.isAfter(tsInT)) &&
                                           (waEnd.equals(tsOutT)  || waEnd.isBefore(tsOutT)) )
                    boolean tsInsideWa = ( (tsInT.equals(waStart) || tsInT.isAfter(waStart)) &&
                                           (tsOutT.equals(waEnd) || tsOutT.isBefore(waEnd)) )

                    if (waInsideTs) {
                        outItem.workAssignment = null
                        outItem.actions << "Deleted workAssignment (fully inside timesheet)"
                        if (wa.id) delWA << [ id: wa.id.toString(), deleted: true ]

                    } else if (tsInsideWa) {
                        outItem.timesheet = null
                        outItem.actions << "Deleted timesheet (fully inside workAssignment)"
                        if (ts.checkIn?.id)  delTS << [ id: ts.checkIn.id.toString(),  deleted: true ]
                        if (ts.checkOut?.id) delTS << [ id: ts.checkOut.id.toString(), deleted: true ]

                    } else {
                        // Partial overlaps
                        if (!tsInT.isAfter(waStart) && !tsOutT.isBefore(waStart) && tsOutT.isBefore(waEnd)) {
                            // Trim checkout
                            def newOut = clamp(waStart.minusMinutes(1))
                            if (newOut.isBefore(tsInT)) {
                                outItem.timesheet = null
                                outItem.actions << "Deleted timesheet (trim end made it empty)"
                                if (ts.checkIn?.id)  delTS << [ id: ts.checkIn.id.toString(),  deleted: true ]
                                if (ts.checkOut?.id) delTS << [ id: ts.checkOut.id.toString(), deleted: true ]
                            } else {
                                if (ts.checkOut?.id) delTS << [ id: ts.checkOut.id.toString(), deleted: true ]
                                def c20Ts = toIsoTs(ts.date.toString(), fmtT(newOut))
                                insTE << [
                                    id:"2", assignmentId: ts.employeeId.toString(), typeCode:"C20",
                                    timestamp:c20Ts, terminalId:"Manual", timeTypeCode:"WORKING_TIME"
                                ]
                                outItem.timesheet.checkOut.timeLocal = fmtT(newOut)
                                outItem.actions << "Trimmed checkout; deleted old C20 and inserted new C20"
                            }

                        } else if (tsInT.isAfter(waStart) && !tsInT.isAfter(waEnd) && !tsOutT.isBefore(waEnd)) {
                            // Trim checkin
                            def newIn = clamp(waEnd.plusMinutes(1))
                            if (newIn.isAfter(tsOutT)) {
                                outItem.timesheet = null
                                outItem.actions << "Deleted timesheet (trim start made it empty)"
                                if (ts.checkIn?.id)  delTS << [ id: ts.checkIn.id.toString(),  deleted: true ]
                                if (ts.checkOut?.id) delTS << [ id: ts.checkOut.id.toString(), deleted: true ]
                            } else {
                                if (ts.checkIn?.id) delTS << [ id: ts.checkIn.id.toString(), deleted: true ]
                                def c10Ts = toIsoTs(ts.date.toString(), fmtT(newIn))
                                insTE << [
                                    id:"1", assignmentId: ts.employeeId.toString(), typeCode:"C10",
                                    timestamp:c10Ts, terminalId:"Manual", timeTypeCode:"WORKING_TIME"
                                ]
                                outItem.timesheet.checkIn.timeLocal = fmtT(newIn)
                                outItem.actions << "Trimmed checkin; deleted old C10 and inserted new C10"
                            }

                        } else {
                            outItem.workAssignment = null
                            outItem.actions << "Deleted workAssignment (overlap – fallback rule)"
                            if (wa.id) delWA << [ id: wa.id.toString(), deleted: true ]
                        }
                    }
                } else {
                    outItem.actions << "No overlap"
                }
            } else {
                outItem.actions << "Skipped (invalid times)"
            }
        } else {
            outItem.actions << "Skipped (missing WA/TS or different employee/date)"
        }

        return outItem
    }

    def output = [
        resolvedItems        : resolved,
        timesheetDelete      : [ value: delTS.unique() ],
        workAssignmentDelete : [ value: delWA.unique() ],
        timeEventInsert      : insTE
    ]

    message.setBody(JsonOutput.prettyPrint(JsonOutput.toJson(output)))
    message.setHeader("Content-Type", "application/json")
    return message
}