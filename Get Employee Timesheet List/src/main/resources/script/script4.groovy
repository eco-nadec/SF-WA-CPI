import com.sap.gateway.ip.core.customdev.util.Message
import groovy.json.JsonSlurper
import groovy.json.JsonOutput

def Message processData(Message message) {

    // === 0) Config ===
    // Max URL query length - allow override from message property
    int MAX_LEN = 0
    try {
        def p = message.getProperty("MaxUrlQueryLen")
        if (p) MAX_LEN = p.toString().trim().isInteger() ? p.toString().toInteger() : 0
    } catch (ignore) {}
    if (MAX_LEN <= 0) MAX_LEN = 3500

    // === 1) Read & parse input ===
    // Expected: [{"id":"xxx", "employeeId":"30109", "date":"2025-11-04", "startTime":"13:00", "endTime":"16:00"}, ...]
    def bodyStr = message.getBody(String) as String
    def workAssignments = new JsonSlurper().parseText(bodyStr) as List

    // === 2) Group by (employeeId, date) for deduplication ===
    // Multiple work assignments for same employee/date = single SF API query
    def groups = [:]

    workAssignments.each { wa ->
        def empId = wa.employeeId?.toString()
        def date = wa.date?.toString()

        if (!empId || !date) return // Skip invalid records

        def key = "${empId}_${date}"

        // Initialize group if it doesn't exist
        if (!groups[key]) {
            groups[key] = [
                employeeId: empId,
                date: date,
                workAssignments: []
            ]
        }

        // Add work assignment to group
        groups[key].workAssignments << [
            id: wa.id?.toString(),
            employeeId: empId,
            date: date,
            startTime: wa.startTime?.toString(),
            endTime: wa.endTime?.toString()
        ]
    }

    // === 3) Build individual group filters ===
    // Each group represents one (employeeId, date) combination
    def groupFilterChunks = []

    groups.values().each { g ->
        def from = "${g.date}T00:00:00Z"
        def to   = "${g.date}T23:59:59Z"

        // OData filter for this employee/date
        def rawFilter = "(workAssignmentId eq '${g.employeeId}' and (timeEventTypeCode eq 'C10' or timeEventTypeCode eq 'C20') and timestampUTC ge ${from} and timestampUTC le ${to})"
        def encFilter = pctEncode(rawFilter)

        groupFilterChunks << [
            encodedFilter: encFilter,
            rawFilter: rawFilter,
            workAssignments: g.workAssignments
        ]
    }

    // === 4) Pack many group filters into batches under MAX_LEN ===
    // Combine multiple employee/date groups with " or " until URL limit reached
    final String OR_ENC = "%20or%20"

    def batches = []
    def currentEncoded = new StringBuilder()
    def currentWorkAssignments = []

    def flushBatch = {
        if (currentEncoded.length() > 0) {
            batches << [
                encodedFilter: currentEncoded.toString(),
                workAssignments: new ArrayList(currentWorkAssignments)
            ]
            currentEncoded.setLength(0)
            currentWorkAssignments.clear()
        }
    }

    groupFilterChunks.each { gf ->
        def enc = gf.encodedFilter

        if (currentEncoded.length() == 0) {
            // Start new batch
            currentEncoded.append(enc)
            currentWorkAssignments.addAll(gf.workAssignments)
        } else {
            int prospectiveLen = currentEncoded.length() + OR_ENC.length() + enc.length()

            if (prospectiveLen > MAX_LEN) {
                // Current batch is full, flush and start new
                flushBatch()
                currentEncoded.append(enc)
                currentWorkAssignments.addAll(gf.workAssignments)
            } else {
                // Add to current batch
                currentEncoded.append(OR_ENC).append(enc)
                currentWorkAssignments.addAll(gf.workAssignments)
            }
        }
    }
    flushBatch()

    // === 5) Emit XML with batches + work assignment mapping ===
    // Build XML manually to avoid MarkupBuilder closure issues
    def xmlBuilder = new StringBuilder()
    xmlBuilder.append('<?xml version="1.0" encoding="UTF-8"?>\n')
    xmlBuilder.append('<BatchedTimeEventRequests>\n')

    batches.eachWithIndex { batch, idx ->
        xmlBuilder.append('  <batch>\n')
        xmlBuilder.append("    <batchNumber>${idx + 1}</batchNumber>\n")
        xmlBuilder.append("    <recordCount>${batch.workAssignments.size()}</recordCount>\n")
        xmlBuilder.append("    <filter>${escapeXml(batch.encodedFilter)}</filter>\n")
        xmlBuilder.append('    <workAssignments>\n')

        batch.workAssignments.each { wa ->
            xmlBuilder.append('      <wa')
            xmlBuilder.append(" id=\"${escapeXml(wa.id ?: '')}\"")
            xmlBuilder.append(" employeeId=\"${escapeXml(wa.employeeId ?: '')}\"")
            xmlBuilder.append(" date=\"${escapeXml(wa.date ?: '')}\"")
            xmlBuilder.append(" startTime=\"${escapeXml(wa.startTime ?: '')}\"")
            xmlBuilder.append(" endTime=\"${escapeXml(wa.endTime ?: '')}\"")
            xmlBuilder.append('/>\n')
        }

        xmlBuilder.append('    </workAssignments>\n')
        xmlBuilder.append('  </batch>\n')
    }

    xmlBuilder.append('</BatchedTimeEventRequests>')

    def xmlOutput = xmlBuilder.toString()

    // Set body as String and force XML content type
    message.setBody(xmlOutput)
    message.setHeader("Content-Type", "text/xml; charset=UTF-8")

    // ALSO store as property as backup (in case body is not propagating)
    message.setProperty("BatchedXML", xmlOutput)

    // Store all original work assignments as JSON for later matching in script6
    // This property persists through splitter/gather and will be available after gather
    def allWaJson = JsonOutput.toJson(workAssignments)
    message.setProperty("AllOriginalWorkAssignments", allWaJson)

    message.setProperty("MaxUrlQueryLenUsed", MAX_LEN)
    message.setProperty("TotalWorkAssignments", workAssignments.size())
    message.setProperty("TotalBatches", batches.size())

    // Log for debugging
    def messageLog = messageLogFactory.getMessageLog(message)
    if (messageLog) {
        messageLog.addAttachmentAsString(
            "Script4_Output_XML",
            xmlBuilder.toString(),
            "text/xml"
        )
        messageLog.addAttachmentAsString(
            "Script4_Debug_Info",
            "Total WAs: ${workAssignments.size()}\nTotal Groups: ${groups.size()}\nTotal Batches: ${batches.size()}",
            "text/plain"
        )
    }

    return message
}

// === Helper: Percent encoder for OData $filter ===
def pctEncode(String s) {
    return s.replace("%", "%25")
            .replace(" ", "%20")
            .replace("(", "%28")
            .replace(")", "%29")
            .replace("'", "%27")
            .replace(":", "%3A")
            .replace(",", "%2C")
            .replace("+", "%2B")
            .replace("\"", "%22")
}

// === Helper: XML escape ===
def escapeXml(String s) {
    if (!s) return ""
    return s.replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&apos;")
}
