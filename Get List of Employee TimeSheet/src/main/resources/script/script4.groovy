import com.sap.gateway.ip.core.customdev.util.Message
import groovy.json.JsonSlurper
import java.time.OffsetDateTime
import java.time.ZoneOffset

def Message processData(Message message) {

    // === 0) Config ===
    // Allow override from message property "MaxUrlQueryLen"; fallback to 3500
    int MAX_LEN = 0
    try {
        def p = message.getProperty("MaxUrlQueryLen")
        if (p) MAX_LEN = p.toString().trim().isInteger() ? p.toString().toInteger() : 0
    } catch (ignore) {}
    if (MAX_LEN <= 0) MAX_LEN = 3500

    // === 1) Read & parse input ===
    def bodyStr = message.getBody(String) as String
    def rows = new JsonSlurper().parseText(bodyStr) as List

    // === 2) Normalize -> group by (assignmentId, local date in UTC day window) ===
    // Each row has timestamp with +03:00; we need the UTC DAY window 00:00..23:59:59Z
    // We'll compute the UTC date for windowing.
    def groups = [:].withDefault { [types: [] , count: 0, assignmentId: null, dateUtc: null] }

    rows.each { r ->
        def assignmentId = r.assignmentId?.toString()
        def typeCode = r.typeCode?.toString()
        if (!assignmentId || !typeCode || !r.timestamp) return

        OffsetDateTime odt = OffsetDateTime.parse(r.timestamp.toString())
        // Convert to UTC, then take the UTC date for the day window
        def utc = odt.withOffsetSameInstant(ZoneOffset.UTC)
        def dateUtc = utc.toLocalDate().toString() // yyyy-MM-dd

        def key = "${assignmentId}_${dateUtc}"
        def g = groups[key]
        g.assignmentId = assignmentId
        g.dateUtc = dateUtc
        g.types << typeCode
        g.count = (g.count ?: 0) + 1
    }

    // Deduplicate type codes per group
    groups.each { k, g -> g.types = g.types.unique() }

    // === 3) Percent encoder (URL query safe for OData $filter) ===
    def pctEncode = { String s ->
        s.replace("%","%25")
         .replace(" ", "%20")
         .replace("(", "%28")
         .replace(")", "%29")
         .replace("'", "%27")
         .replace(":", "%3A")
         .replace(",", "%2C")
         .replace("+", "%2B")
         .replace("\"","%22")
    }
    // Encoded " or " once so we can add without re-encoding
    final String OR_ENC = "%20or%20"

    // === 4) Build individual group filters
    // If a single group would exceed MAX_LEN due to many type codes, split its types into chunks.
    def groupFilterChunks = []  // each item: [encodedFilter: String, rawFilter: String, count: int]
    groups.values().each { g ->
        def from = "${g.dateUtc}T00:00:00Z"
        def to   = "${g.dateUtc}T23:59:59Z"

        // Helper to make raw filter from a subset of type codes
        def mkRaw = { List<String> typeList ->
            def typeExpr = typeList.collect { "timeEventTypeCode eq '${it}'" }.join(" or ")
            return "(workAssignmentId eq '${g.assignmentId}' and (${typeExpr}) and timestampUTC ge ${from} and timestampUTC le ${to})"
        }

        // First try with all types
        def rawAll = mkRaw(g.types)
        def encAll = pctEncode(rawAll)

        if (encAll.length() <= MAX_LEN) {
            groupFilterChunks << [encodedFilter: encAll, rawFilter: rawAll, count: g.count]
        } else {
            // Split types into smaller chunks until each chunk fits by itself
            List<String> types = g.types
            int start = 0
            while (start < types.size()) {
                int end = Math.min(types.size(), start + 1) // grow chunk
                // Expand chunk greedily while under MAX_LEN
                while (end <= types.size()) {
                    def rawChunk = mkRaw(types.subList(start, end))
                    def encChunk = pctEncode(rawChunk)
                    if (encChunk.length() > MAX_LEN) {
                        // step back one
                        if (end == start + 1) {
                            // Even a single type makes it too long: we must fail-safe by emitting as-is
                            groupFilterChunks << [encodedFilter: encChunk, rawFilter: rawChunk, count: 1]
                            start = end
                        } else {
                            def rawPrev = mkRaw(types.subList(start, end - 1))
                            def encPrev = pctEncode(rawPrev)
                            groupFilterChunks << [encodedFilter: encPrev, rawFilter: rawPrev, count: (end - 1 - start)]
                            start = end - 1
                        }
                        break
                    } else if (end == types.size()) {
                        // fits; push final chunk
                        groupFilterChunks << [encodedFilter: encChunk, rawFilter: rawChunk, count: (end - start)]
                        start = end
                        break
                    } else {
                        end++
                    }
                }
            }
        }
    }

    // === 5) Pack many group filters into batches under MAX_LEN ===
    def batches = []
    def currentEncoded = new StringBuilder()
    int currentCount = 0
    def flushBatch = {
        if (currentEncoded.length() > 0) {
            batches << [encodedFilter: currentEncoded.toString(), count: currentCount]
            currentEncoded.setLength(0)
            currentCount = 0
        }
    }

    groupFilterChunks.eachWithIndex { gf, idx ->
        def enc = gf.encodedFilter
        if (currentEncoded.length() == 0) {
            // start new
            currentEncoded.append(enc)
            currentCount += gf.count
        } else {
            int prospectiveLen = currentEncoded.length() + OR_ENC.length() + enc.length()
            if (prospectiveLen > MAX_LEN) {
                flushBatch()
                currentEncoded.append(enc)
                currentCount += gf.count
            } else {
                currentEncoded.append(OR_ENC).append(enc)
                currentCount += gf.count
            }
        }
    }
    flushBatch()

    // === 6) Emit XML ===
    def sb = new StringBuilder()
    sb.append("<BatchedTimeEventRequests>\n")
    int batchNum = 1
    batches.each { b ->
        sb.append("  <batch>\n")
        sb.append("      <batchNumber>").append(batchNum++).append("</batchNumber>\n")
        sb.append("      <recordCount>").append(b.count).append("</recordCount>\n")
        sb.append("      <filter>").append(b.encodedFilter).append("</filter>\n")
        sb.append("  </batch>\n")
    }
    sb.append("</BatchedTimeEventRequests>")

    message.setBody(sb.toString())
    // Optional: expose the limit used (for MPL visibility)
    message.setProperty("MaxUrlQueryLenUsed", MAX_LEN)
    return message
}
