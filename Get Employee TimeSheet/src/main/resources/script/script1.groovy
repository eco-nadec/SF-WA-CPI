import com.sap.gateway.ip.core.customdev.util.Message

def Message processData(Message message) {

    // Get all query parameters from the HTTP request
    def params = message.getHeaders()

    // In CPI, HTTP query parameters are accessible via headers: CamelHttpQuery
    def query = params.get("CamelHttpQuery")  // Example: "EmployeeId=30933&date=2025-08-10"

    def employeeId = null
    def date = null

    if (query) {
        // Split and parse parameters
        query.split("&").each { pair ->
            def kv = pair.split("=")
            if (kv.size() == 2) {
                def key = kv[0]
                def value = java.net.URLDecoder.decode(kv[1], "UTF-8")
                if (key.equalsIgnoreCase("EmployeeId")) {
                    employeeId = value
                } else if (key.equalsIgnoreCase("date")) {
                    date = value
                }
            }
        }
    }

    // Set them as exchange properties
    message.setProperty("EmployeeId", employeeId)
    message.setProperty("Date", date)

    // Build OData filter if both values are present
// Build OData filter/order values (unencoded)
if (employeeId && date) {
    def filterValue  = "workAssignmentId eq '${employeeId}' and (timeEventTypeCode eq 'C10' or timeEventTypeCode eq 'C20')" +
                       " and timestampUTC ge ${date}T00:00:00Z and timestampUTC lt ${date}T23:59:59Z"
    def orderByValue = "timestampUTC asc"

    // Encode values only
    def encFilter  = java.net.URLEncoder.encode(filterValue, "UTF-8")
    def encOrderBy = java.net.URLEncoder.encode(orderByValue, "UTF-8")

    // Set a proper query string: names not encoded, '&' not encoded
    def camelQuery = "\$filter=${encFilter}&\$orderby=${encOrderBy}"
    message.setHeader("CamelHttpQuery", camelQuery)

    // (optional) also expose plain values as properties if you prefer Option A
    message.setProperty("odataFilterValue", filterValue)
    message.setProperty("odataOrderByValue", orderByValue)
}

    // Log for debugging
    def log = messageLogFactory.getMessageLog(message)
    if (log) {
        log.addAttachmentAsString("Extracted Params",
            "EmployeeId: ${employeeId}, Date: ${date}, OData Filter: ${message.getProperty('odatafilter')}",
            "text/plain")
    }

    return message
}