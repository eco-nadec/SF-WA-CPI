# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Overview

This is a SAP Cloud Platform Integration (CPI) project for NADEC that resolves conflicts between Work Assignments and Employee Timesheets. The core business rule is: **Work Assignment times and Timesheet times must not overlap**. When overlaps occur, the system automatically resolves conflicts through deletion, trimming, or adjustment of records.

## Project Purpose

The integration flows in this project:
1. Fetch Work Assignments and Employee Timesheets from SAP SuccessFactors
2. Detect time overlaps between work assignments and check-in/check-out events
3. Automatically resolve conflicts using defined business rules
4. Execute delete/insert operations back to SuccessFactors to maintain data integrity

## Architecture

### Integration Flow (iFlow) Structure

Each subdirectory represents a deployable CPI integration flow package:

```
<iFlow Name>/
├── META-INF/MANIFEST.MF          # Bundle metadata and version
├── .project                       # Eclipse project file
├── metainfo.prop                  # CPI metadata
└── src/main/resources/
    ├── parameters.prop(def)       # External configuration parameters
    ├── script/                    # Groovy script steps
    │   ├── script1.groovy
    │   ├── script2.groovy
    │   └── ...
    └── scenarioflows/integrationflow/
        └── *.iflw                 # Integration flow definition (XML)
```

### Key Integration Flows

1. **Get Work Assignment And Timesheet** / **Get Work Assignment And Timesheet Bulk**
   - Fetches work assignment data and corresponding timesheet events
   - Combines data from multiple SuccessFactors OData endpoints
   - Prepares paired records for conflict detection

2. **Resolve Work Assignment Conflict Main**
   - Main orchestration flow for conflict resolution
   - Coordinates data retrieval, analysis, and action execution

3. **Resolve Work Assignment Conflict Logic**
   - Core conflict detection and resolution algorithm (see script1.groovy)
   - Implements time overlap detection and resolution rules

4. **Resolve Work Assignment Conflict Action**
   - Executes resolved actions (deletes, inserts) back to SuccessFactors
   - Transforms JSON to XML for SF API calls

5. **Get Location work Assignment List**
   - Retrieves work assignments filtered by location
   - Supports location-based batch processing

6. **Get List of Employee TimeSheet**
   - Fetches timesheet events (C10=check-in, C20=check-out)
   - Filters by employee ID and date range

7. **Delete Work Assignment**
   - Deletes work assignment records when conflicts require removal

8. **test-Create Employee Timesheet** / **test-Delete Employee Timesheet**
   - Test flows for timesheet CRUD operations

## Conflict Resolution Logic

### Core Algorithm (`Resolve Work Assignment Conflict Logic/src/main/resources/script/script1.groovy`)

The script processes paired work assignments and timesheets:

**Input Structure:**
```json
[
  {
    "workAssignment": {
      "id": "...",
      "employeeId": "30933",
      "date": "2025-08-13",
      "startTime": "08:00",
      "endTime": "17:00"
    },
    "timesheet": {
      "employeeId": "30933",
      "date": "2025-08-13",
      "checkIn": { "id": "...", "timeLocal": "07:30" },
      "checkOut": { "id": "...", "timeLocal": "18:00" }
    }
  }
]
```

**Conflict Resolution Rules:**

1. **No Overlap**: No action taken
2. **Work Assignment fully inside Timesheet**: Delete work assignment
3. **Timesheet fully inside Work Assignment**: Delete timesheet (both C10 and C20 events)
4. **Partial Overlap (Timesheet starts before WA)**: Trim timesheet checkout to WA start - 1 minute
5. **Partial Overlap (Timesheet ends after WA)**: Trim timesheet checkin to WA end + 1 minute
6. **Other Overlaps**: Delete work assignment (fallback rule)

**Output Structure:**
```json
{
  "resolvedItems": [ /* resolved pairs with actions */ ],
  "timesheetDelete": { "value": [ { "id": "...", "deleted": true } ] },
  "workAssignmentDelete": { "value": [ { "id": "...", "deleted": true } ] },
  "timeEventInsert": [ /* new C10/C20 events for trimmed times */ ]
}
```

### Time Handling

- **Timezone**: Asia/Riyadh (configurable)
- **Time Format**: HH:mm (e.g., "14:30")
- **Date Format**: yyyy-MM-dd (e.g., "2025-08-13")
- **SuccessFactors Timestamp Format**: `yyyy-MM-dd'T'HH:mm:ssZ` (ISO with timezone offset)
- **Timesheet Event Types**:
  - **C10**: Check-in event
  - **C20**: Check-out event
  - **Time Type**: WORKING_TIME
  - **Terminal**: "Manual" (for CPI-created events)

## Common Development Patterns

### Groovy Script Structure

All Groovy scripts follow this pattern:

```groovy
import com.sap.gateway.ip.core.customdev.util.Message
import groovy.json.JsonSlurper
import groovy.json.JsonOutput

def Message processData(Message message) {
    // 1. Get input
    def body = message.getBody(String)
    def json = new JsonSlurper().parseText(body)

    // 2. Process data
    // ...

    // 3. Set output
    message.setBody(JsonOutput.toJson(result))
    message.setHeader("Content-Type", "application/json")

    return message
}
```

### Data Transformation Patterns

1. **JSON to XML Conversion** (for SuccessFactors API calls):
   ```groovy
   import groovy.xml.MarkupBuilder
   def writer = new StringWriter()
   def xml = new MarkupBuilder(writer)
   xml.Records {
       json.each { rec ->
           Record {
               id(rec.id)
               employeeId(rec.employeeId)
               // ...
           }
       }
   }
   message.setBody(writer.toString())
   ```

2. **HTTP Query Parameter Parsing**:
   ```groovy
   def query = message.getHeaders().get("CamelHttpQuery")
   // Parse: "EmployeeId=30933&date=2025-08-10"
   message.setProperty("EmployeeId", employeeId)
   ```

3. **OData Filter Construction**:
   ```groovy
   def filterValue = "workAssignmentId eq '${employeeId}' and timestampUTC ge ${date}T00:00:00Z"
   def encFilter = java.net.URLEncoder.encode(filterValue, "UTF-8")
   message.setHeader("CamelHttpQuery", "\$filter=${encFilter}")
   ```

### Message Properties vs Headers

- **Properties**: Used for data passing between flow steps within the same iFlow
  ```groovy
  message.setProperty("EmployeeId", employeeId)
  ```

- **Headers**: Used for HTTP/protocol-level communication
  ```groovy
  message.setHeader("CamelHttpQuery", queryString)
  message.setHeader("Content-Type", "application/json")
  ```

## Testing and Debugging

### Local Groovy Script Testing

To test Groovy scripts locally (outside CPI):
1. Extract script from `.groovy` file
2. Mock the Message object and dependencies
3. Test with sample JSON/XML payloads matching expected structure

### CPI Message Logging

Add logging attachments in scripts:
```groovy
def log = messageLogFactory.getMessageLog(message)
if (log) {
    log.addAttachmentAsString("Debug Info",
        "EmployeeId: ${employeeId}, Date: ${date}",
        "text/plain")
}
```

### Common Issues

1. **Timestamp Format Mismatches**: SuccessFactors requires strict ISO format with timezone offset
   - Correct: `2025-08-13T10:00:00+0300`
   - Incorrect: `2025-08-13T10:00:00` or `2025-08-13 10:00:00`

2. **OData Query Encoding**: Use `URLEncoder.encode()` only on values, not on parameter names or operators
   - Correct: `$filter=` + encoded filter expression
   - Incorrect: Encoding the entire query string including `$filter=`

3. **Time Comparison Edge Cases**: Always handle null times and validate time ranges
   ```groovy
   if (waStart && waEnd && tsInT && tsOutT) {
       // Safe to compare
   }
   ```

## Deployment

### Version Management

Each iFlow has a version in `META-INF/MANIFEST.MF`:
```
Bundle-Version: 6.11.2025
```

Update this version when making changes for deployment tracking.

### Integration Flow Naming

Integration flow names use spaces and are defined in:
- Directory name (with spaces)
- Bundle-SymbolicName in MANIFEST.MF (underscores replace spaces)
- .iflw file name

## SuccessFactors Integration

### Entities Used

1. **Work Assignment**: Employee's scheduled work times
2. **TimeEvent**: Check-in (C10) and check-out (C20) events
3. **Employee**: Employee master data

### API Endpoints

The flows integrate with SuccessFactors OData v2 endpoints for:
- Reading work assignments
- Reading time events
- Creating time events (for trimmed times)
- Deleting work assignments
- Deleting time events

Authentication and endpoint URLs are configured via CPI external parameters.

## Key Business Rules

1. **Overlap Prevention**: Work assignment and timesheet times must not overlap
2. **FIFO Time Trimming**: When trimming times, adjust by ±1 minute to eliminate overlap
3. **Atomic Operations**: Each conflict resolution produces a complete set of delete/insert operations
4. **Timezone Consistency**: All times processed in Asia/Riyadh timezone
5. **Employee-Date Matching**: Only compare work assignments and timesheets for same employee and date
