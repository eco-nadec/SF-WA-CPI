# Integration Flow Naming and Description Guide

This document provides proper names and descriptions for each integration flow (iFlow) in this project.

---

## 📋 Current vs. Recommended Names

| # | Current Name | Recommended Name | Status |
|---|-------------|------------------|--------|
| 1 | Delete Work Assignment | **SF_WorkAssignment_Delete** | ✅ Good |
| 2 | Get Employee List By Location | **SF_Employee_GetByLocation** | 🔄 Rename |
| 3 | Get Employee TimeSheet | **SF_TimeEvent_GetByEmployee** | ❌ DELETE (duplicate) |
| 4 | Get List of Employee TimeSheet | **SF_TimeEvent_GetByEmployeeDate** | 🔄 Rename |
| 5 | Get List of work Assignment | **SF_WorkAssignment_GetByDateRange** | 🔄 Rename |
| 6 | Get Location work Assignment List | **WA_TS_Orchestrator_ByLocation** | 🔄 Rename |
| 7 | Get Work Assignment And Timesheet | **WA_TS_Combine_Single** | ⚠️ Consider DELETE |
| 8 | Get Work Assignment And Timesheet Bulk | **WA_TS_Combine_Bulk** | 🔄 Rename |
| 9 | Resolve Work Assignment Conflict Action | **WA_TS_Conflict_Execute** | 🔄 Rename |
| 10 | Resolve Work Assignment Conflict Logic | **WA_TS_Conflict_Analyze** | 🔄 Rename |
| 11 | Resolve Work Assignment Conflict Main | **WA_TS_Conflict_CoreOrchestrator** | 🔄 Rename |
| 12 | Work Assignment Conflict Main | **WA_TS_Conflict_EndToEnd** | 🔄 Rename |
| 13 | test-Create Employee Timesheet | **Test_TimeEvent_Create** | 🔄 Rename |
| 14 | test-Delete Employee Timesheet | **Test_TimeEvent_Delete** | ⚠️ Consolidate |

---

## 📖 Detailed Descriptions

### 1. SF_WorkAssignment_Delete
**Current:** Delete Work Assignment
**Purpose:** Delete work assignment records from SuccessFactors
**Input:** XML with work assignment IDs marked as deleted=true
**Output:** JSON payload for SF OData upsert (sets approvalStatus=CANCELLED)
**Trigger:** Called by conflict resolution action flow

**Key Script Logic:**
- Reads XML `<Item>` nodes
- Filters items where `deleted=true`
- Creates SF upsert payload with `approvalStatus: "CANCELLED"`

---

### 2. SF_Employee_GetByLocation
**Current:** Get Employee List By Location
**Purpose:** Retrieve employee list filtered by location ID
**Input:** HTTP query parameter `location=<locationId>`
**Output:** Employee data from SuccessFactors
**Trigger:** HTTP endpoint with location query parameter

**Key Script Logic:**
- Parses `location` from CamelHttpQuery
- Builds OData filter: `$filter=location eq '<locationId>'`
- Returns employee records (location, seqNumber, startDate, userId)

---

### 3. ❌ SF_TimeEvent_GetByEmployee (DELETE - DUPLICATE)
**Current:** Get Employee TimeSheet
**Status:** **100% DUPLICATE** of "Get List of Employee TimeSheet"
**Recommendation:** ❌ **DELETE THIS FLOW**

**Why Delete:**
- Scripts are byte-for-byte identical to flow #4
- Flow #4 has MORE functionality (additional scripts 3, 4, 5)
- No unique functionality - complete redundancy

**Evidence:**
```bash
diff script1.groovy: Files are identical (2314 bytes)
diff script2.groovy: Files are identical (2202 bytes)
```

**Action:** Delete this flow and use "Get List of Employee TimeSheet" instead

---

### 4. SF_TimeEvent_GetByEmployeeDate
**Current:** Get List of Employee TimeSheet
**Purpose:** Retrieve and process timesheet events, grouping by date and pairing check-ins/check-outs
**Input:** TimeEvent data from SuccessFactors OData
**Output:** JSON array with paired check-in/check-out per date
**Trigger:** Called after fetching time events from SF

**Key Script Logic:**
- Groups events by local date (handles timezone offsets)
- Sorts events by timestamp
- Pairs first C10 (check-in) with last C20 (check-out) per day
- Converts UTC timestamps to local time strings (HH:mm:ss format)
- Returns structured array with employeeId, date, checkIn/Out IDs and local times

---

### 5. SF_WorkAssignment_GetByDateRange
**Current:** Get List of work Assignment
**Purpose:** Fetch work assignments within a dynamic date range (last 2 months to current month)
**Input:** None (system-calculated date range)
**Output:** Work assignment records from SuccessFactors
**Trigger:** Scheduled or manual execution

**Key Script Logic:**
- Calculates date range: first day of 2 months ago to last day of current month
- Example: If today is 2025-11-06, range is 2025-09-01 to 2025-11-30
- Builds OData filter: `startDate ge datetime'...' and startDate le datetime'...'`
- Uses UTC timezone for date calculations

---

### 6. WA_TS_Orchestrator_ByLocation
**Current:** Get Location work Assignment List
**Purpose:** Orchestrator that retrieves work assignments and timesheets for a specific location, then combines them
**Input:** HTTP query parameter `location=<locationId>`
**Output:** Combined XML with work assignment and timesheet data
**Trigger:** HTTP endpoint `/getWAAndTimesheet`

**Flow Type:** **Orchestrator** (coordinates multiple iFlow calls)

**Orchestration Steps:**
1. Calls external iFlow to get work assignments by location (`{{CPI_WORK_ASSIGNMENT}}`)
2. Loops through each work assignment record (Splitter pattern)
3. For each employee, calls timesheet retrieval (`{{CPI_GET_TIMESHEET}}`)
4. Combines results (Gather pattern)
5. Returns merged XML

**Key Script Logic:**
- **Script 1:** Parses location parameter, builds employee filter
- **Script 2:** Extracts work assignment XML, saves to property `workAssignmentData`
- **Script 3:** Fetches timesheet data, combines with saved work assignment XML
- **Script 4:** Merges both datasets into single XML payload with `<WorkAssignment>` and `<Timesheet>` sections
- Includes 5-second delay (Thread.sleep) before output

---

### 7. WA_TS_Combine_Single
**Current:** Get Work Assignment And Timesheet
**Purpose:** Fetch and combine single work assignment with corresponding timesheet
**Input:** Work assignment record (JSON or XML)
**Output:** Combined XML payload with work assignment and timesheet data
**Trigger:** Single record processing

**Key Script Logic:**
- Converts JSON work assignment to XML
- Extracts employeeId and date from work assignment
- Fetches matching timesheet events from SF
- Combines both into structured XML for conflict detection

---

### 8. WA_TS_Combine_Bulk
**Current:** Get Work Assignment And Timesheet Bulk
**Purpose:** Batch processing of multiple work assignments with their timesheets
**Input:** Multiple work assignment records (JSON array)
**Output:** Combined XML with all work assignments and timesheets
**Trigger:** Bulk/batch processing

**Key Script Logic:**
- **Script 1:** Converts JSON array to XML `<Records>` structure
- **Script 2:** Extracts work assignment data, saves to property
- **Script 3:** Combines work assignment XML with timesheet JSON
- **Script 4-6:** Additional transformation and aggregation steps
- Includes 5-second delay for SF API rate limiting

---

### 9. WA_TS_Conflict_Execute
**Current:** Resolve Work Assignment Conflict Action
**Purpose:** Execute resolved conflict actions (deletes and inserts) in SuccessFactors
**Input:** Resolved conflict payload with delete/insert lists
**Output:** Confirmation of executed actions
**Trigger:** Called by orchestrator after conflict analysis

**Key Script Logic:**
- Receives output from conflict analysis (timesheetDelete, workAssignmentDelete, timeEventInsert)
- Executes deletions via SF OData (upsert with cancelled status)
- Creates new TimeEvent records for trimmed times
- Handles batch operations for multiple records

---

### 10. WA_TS_Conflict_Analyze
**Current:** Resolve Work Assignment Conflict Logic
**Purpose:** Core conflict detection and resolution algorithm
**Input:** JSON array with paired work assignments and timesheets
**Output:** Resolved items with actions (delete/trim/no action)
**Trigger:** Called by orchestrator with combined WA/TS data

**Key Script Logic:**
- **CRITICAL ALGORITHM:** See `script1.groovy:7-145`
- Detects time overlaps between work assignment (startTime-endTime) and timesheet (checkIn-checkOut)
- Applies resolution rules:
  1. WA fully inside TS → Delete WA
  2. TS fully inside WA → Delete TS
  3. Partial overlap (TS starts before) → Trim TS checkout to WA start - 1 min
  4. Partial overlap (TS ends after) → Trim TS checkin to WA end + 1 min
  5. Other overlaps → Delete WA (fallback)
- Generates delete lists and new TimeEvent inserts for trimmed times
- Uses Asia/Riyadh timezone
- Returns structured JSON with resolved items and actions

---

### 11. WA_TS_Conflict_CoreOrchestrator
**Current:** Resolve Work Assignment Conflict Main
**Purpose:** Core orchestrator for conflict analysis and execution (assumes data already provided)
**Input:** Combined WA + TS data (JSON)
**Output:** Complete conflict resolution report
**Trigger:** HTTP endpoint `/resolveWAConflictMain`

**Flow Type:** **Core Orchestrator** (coordinates analysis + execution, NO data retrieval)

**Orchestration Steps:**
1. Call conflict analysis → `{{CPI_RESOLVE_WA_CONFLICT}}` (Resolve Work Assignment Conflict Logic)
2. Call conflict execution → `{{CPI_RESOLVE_WA_ACTION}}` (Resolve Work Assignment Conflict Action)
3. Return resolution results

**iFlow Structure:**
- **NO Groovy Scripts Used** (scripts in directory are unused/dead code)
- **ONLY External Calls** (ServiceTasks to other iFlows)

**Flow Diagram:**
```
Start → External Call: Resolve WA logic → External Call: Resolve WA Action → End
```

**When to Use:**
- You already have combined WA + TS data
- You want only the analysis + execution parts
- Called by higher-level orchestrators

---

### 12. WA_TS_Conflict_EndToEnd
**Current:** Work Assignment Conflict Main
**Purpose:** End-to-end orchestrator that retrieves data THEN resolves conflicts
**Input:** Minimal (just trigger)
**Output:** Complete conflict resolution report
**Trigger:** HTTP endpoint `/resolveWAMain`

**Flow Type:** **Top-Level Wrapper** (data retrieval + conflict resolution)

**Orchestration Steps:**
1. Get data → `{{CPI_GET_WA_TIME_BULK}}` (Get Work Assignment And Timesheet Bulk)
2. Resolve conflicts → `{{CPI_RESOLVE_CONFILICT_MAIN}}` (Resolve Work Assignment Conflict Main)
3. Return resolution results

**iFlow Structure:**
- **NO Groovy Scripts Used** (scripts in directory are unused/dead code)
- **ONLY External Calls** (ServiceTasks to other iFlows)

**Flow Diagram:**
```
Start → External Call: Get WA and Timesheet → External Call: Resolve conflict → End
```

**When to Use:**
- You want a one-stop solution (data + resolution)
- Scheduled jobs that need to fetch data automatically
- Simplest entry point for full process

**Difference from #11:**
- **This flow (#12)**: Gets data FIRST, then resolves → Full end-to-end
- **Flow #11**: Expects data provided, only resolves → Core logic only

**Both flows are NEEDED** - they serve different architectural purposes!

---

### 13. Test_TimeEvent_Create
**Current:** test-Create Employee Timesheet
**Purpose:** Test flow for creating timesheet events in SuccessFactors
**Input:** JSON with `EmpId`, `Date`, `Checkin`, `Checkout`, optional `Timezone`
**Output:** Array of TimeEvent records (C10 and C20)
**Trigger:** Manual testing via HTTP endpoint

**Key Script Logic:**
- Parses input: employeeId, date (yyyy-MM-dd), checkin/checkout times (HH:mm)
- Converts to SF timestamp format: `yyyy-MM-dd'T'HH:mm:ssZ`
- Creates two TimeEvent records:
  - C10 (check-in) with assignmentId, timestamp, terminalId="Manual"
  - C20 (check-out) with assignmentId, timestamp, terminalId="Manual"
- timeTypeCode: "WORKING_TIME" (or "8000" depending on version)

**Sample Input:**
```json
{
  "EmpId": "30933",
  "Date": "2025-08-13",
  "Checkin": "10:00",
  "Checkout": "18:00",
  "Timezone": "Asia/Riyadh"
}
```

---

### 14. Test_TimeEvent_Delete
**Current:** test-Delete Employee Timesheet
**Purpose:** Test flow for deleting timesheet events
**Input:** Same as Test_TimeEvent_Create
**Output:** TimeEvent deletion payload
**Trigger:** Manual testing

**Status:** ⚠️ **99% IDENTICAL** to Test_TimeEvent_Create

**Only Difference:**
```groovy
// test-Create version:
timeTypeCode : "8000" //"WORKING_TIME"

// test-Delete version:
timeTypeCode : "WORKING_TIME"
```

**Recommendation:** Consolidate both test flows into one parameterized flow:
- Create "Test_TimeEvent_CRUD" with configurable timeTypeCode
- Or keep both if they call different SF endpoints (create vs delete)

---

## 🏗️ Naming Convention Pattern

### Prefix Categories:
- **SF_**: Direct SuccessFactors entity operations (Get, Create, Delete)
- **WA_TS_**: Work Assignment & Timesheet combined operations
- **Test_**: Testing/debugging flows

### Action Suffixes:
- **Get**: Retrieve data (read-only)
- **Create**: Insert new records
- **Delete**: Remove records
- **Combine**: Merge multiple data sources
- **Analyze**: Process/analyze data (no SF writes)
- **Execute**: Perform actions in SF (write operations)
- **Orchestrator**: Main coordination flow

### Filter Qualifiers:
- **ByLocation**: Filtered by location ID
- **ByEmployee**: Filtered by employee ID
- **ByDateRange**: Filtered by date range
- **Single**: Processes one record
- **Bulk**: Processes multiple records

---

## 🔄 Migration Checklist

To rename an iFlow:
1. Update directory name
2. Update `META-INF/MANIFEST.MF` → Bundle-SymbolicName (use underscores)
3. Update `.iflw` file name in `src/main/resources/scenarioflows/integrationflow/`
4. Update iFlow display name in CPI Web UI after deployment
5. Update any calling flows that reference this iFlow
6. Update deployment scripts/documentation

---

## 📊 Flow Dependencies (CORRECTED)

```
TOP-LEVEL ENTRY POINTS:

1. WA_TS_Conflict_EndToEnd (Work Assignment Conflict Main)
   │
   ├─→ WA_TS_Combine_Bulk (Get WA And Timesheet Bulk)
   │   │
   │   ├─→ SF_WorkAssignment_GetByDateRange (Get List of WA)
   │   │       └─→ [SuccessFactors Employee Time API]
   │   │
   │   └─→ SF_TimeEvent_GetByEmployeeDate (Get List of Emp TS)
   │           └─→ [SuccessFactors TimeEvent API]
   │
   └─→ WA_TS_Conflict_CoreOrchestrator (Resolve WA Conflict Main)
       │
       ├─→ WA_TS_Conflict_Analyze (Resolve WA Conflict Logic)
       │       └─→ [Groovy Script: Overlap detection logic]
       │
       └─→ WA_TS_Conflict_Execute (Resolve WA Conflict Action)
           │
           ├─→ [SuccessFactors TimeEvent API - DELETE]
           ├─→ [SuccessFactors TimeEvent API - INSERT]
           └─→ SF_WorkAssignment_Delete (Delete Work Assignment)
                   └─→ [SuccessFactors Upsert API]

2. WA_TS_Orchestrator_ByLocation (Get Location WA List)
   │
   ├─→ SF_WorkAssignment_GetByLocation (via CPI)
   │       └─→ [SuccessFactors Work Assignment API]
   │
   └─→ (Loop) SF_TimeEvent_GetByEmployeeDate (via CPI)
           └─→ [SuccessFactors TimeEvent API]

STANDALONE/UTILITY FLOWS:
├── SF_Employee_GetByLocation (Get Employee List By Location)
│       └─→ [SuccessFactors EmpJob API]
│
├── SF_WorkAssignment_Delete (Delete Work Assignment)
│       └─→ [SuccessFactors Upsert API]
│
└── Test Flows (Independent):
    ├── Test_TimeEvent_Create
    │       └─→ [SuccessFactors TimeEvent API]
    └── Test_TimeEvent_Delete
            └─→ [SuccessFactors TimeEvent API]

DUPLICATE/REDUNDANT (DELETE):
├── ❌ Get Employee TimeSheet (100% duplicate of Get List of Employee TimeSheet)
└── ⚠️ Get Work Assignment And Timesheet (subset of Bulk version)
```

---

## 💡 Recommendations

### 1. **Delete Confirmed Duplicates** ❌
- **Get Employee TimeSheet** - 100% duplicate, less functional than "Get List of Employee TimeSheet"
- **Scripts in Main orchestrator flows** - Unused Groovy scripts that can be safely removed

### 2. **Keep Both Main Flows** ✅
- **Work Assignment Conflict Main** (`/resolveWAMain`) - End-to-end (data + resolution)
- **Resolve Work Assignment Conflict Main** (`/resolveWAConflictMain`) - Core orchestrator (analysis + execution)
- **Both are needed** - Different architectural purposes, NOT duplicates!

### 3. **Consider Consolidating**
- **Get Work Assignment And Timesheet** vs **Bulk** - Keep only Bulk version (handles both single and batch)
- **Test flows** - Merge create/delete into one parameterized flow

### 4. **Clean Up Unused Scripts**
The following flows have Groovy scripts that are NOT used (dead code):
- `Work Assignment Conflict Main/src/main/resources/script/` → Can delete entire script folder
- `Resolve Work Assignment Conflict Main/src/main/resources/script/` → Can delete entire script folder

These flows only use ServiceTasks (external HTTP calls), no ScriptTasks.

### 5. **Consistent Naming**
Apply the new naming convention for better clarity:
- Use prefixes: `SF_` for SuccessFactors operations, `WA_TS_` for conflict resolution
- Use clear suffixes: `_EndToEnd`, `_CoreOrchestrator`, `_Analyze`, `_Execute`

### 6. **Version Control**
Use Bundle-Version in MANIFEST.MF to track changes

### 7. **Documentation**
- Add description field in .iflw metadata
- Document which flows use scripts vs which are pure orchestrators

### 8. **Error Handling**
Ensure all flows have proper exception handling and logging

### 9. **Testing**
Keep test flows separate and clearly marked with "Test_" prefix

---

## ⚠️ IMPORTANT CORRECTIONS

### Previous Analysis Was Wrong About:
1. ❌ "Work Assignment Conflict Main is a duplicate" - **WRONG!** It's a different orchestrator
2. ❌ "The Groovy scripts are identical so flows are identical" - **WRONG!** The flows don't use the scripts!

### Correct Understanding:
1. ✅ Both Main flows are needed - different orchestration purposes
2. ✅ Scripts in Main flows are unused dead code - can be deleted
3. ✅ The actual flow logic is in the .iflw file (HTTP calls), not Groovy scripts
4. ✅ "Get Employee TimeSheet" IS a true duplicate and can be deleted

---

## 📝 Summary Table: What to Keep/Delete

| Flow Name | Action | Reason |
|-----------|--------|--------|
| **Work Assignment Conflict Main** | ✅ KEEP | End-to-end orchestrator (gets data + resolves) |
| **Resolve Work Assignment Conflict Main** | ✅ KEEP | Core orchestrator (only resolves) |
| **Scripts in above Main flows** | ❌ DELETE | Unused/dead code (flows use only HTTP calls) |
| **Get Employee TimeSheet** | ❌ DELETE | 100% duplicate of "Get List of Employee TimeSheet" |
| **Get Work Assignment And Timesheet** | ⚠️ CONSIDER DELETE | Bulk version can handle both single and batch |
| **test-Create/Delete** | ⚠️ CONSOLIDATE | 99% identical, merge into one |
| **All other flows** | ✅ KEEP | Unique functionality |
