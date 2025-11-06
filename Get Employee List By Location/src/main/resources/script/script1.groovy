import com.sap.gateway.ip.core.customdev.util.Message
import java.net.URLEncoder

def Message processData(Message message) {

    // --- 1) Read query parameter "location" from the Camel exchange ---
    def locationId = message.getProperty("CamelHttpQuery") ?: ""
    def locValue = ""

    if(locationId) {
        // Example CamelHttpQuery = "location=1015&user=abc"
        def params = locationId.split("&")
        params.each { p ->
            def kv = p.split("=")
            if(kv.size() == 2 && kv[0] == "location") {
                locValue = kv[1]
            }
        }
    }

    // --- 2) Build OData filter ---
    // URL encode the value for safety
    def encodedLoc = URLEncoder.encode(locValue, "UTF-8")
    def filter = "\$select=location,seqNumber,startDate,userId&\$filter=location eq '${encodedLoc}'&\$format=json"

    // --- 3) Set as property ---
    message.setProperty("empoloyFilter", filter)

    return message
}