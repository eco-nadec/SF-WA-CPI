# CPI Metadata Descriptions - Copy & Paste Ready

This document contains brief descriptions for each integration flow to add to SAP CPI metadata fields.

---

## 📋 How to Use This Document

1. Open each iFlow in SAP CPI Web UI
2. Go to iFlow properties/metadata
3. Copy the **Display Name** to the display name field
4. Copy the **Description** to the description field
5. Update the technical name (directory/Bundle-SymbolicName) separately

---

## 🔄 Integration Flow Metadata

### 1. SF_WorkAssignment_Delete

**Display Name:**
```
Delete Work Assignment
```

**Description:**
```
Deletes work assignment records in SuccessFactors by setting approvalStatus to CANCELLED. Receives XML with work assignment IDs and performs batch deletion via OData upsert.
```

**Technical Name:** `SF_WorkAssignment_Delete`
**Endpoint:** `/deleteWAList`

---

### 2. SF_Employee_GetByLocation

**Display Name:**
```
Get Employee List by Location
```

**Description:**
```
Retrieves employee list from SuccessFactors filtered by location ID. Returns employee job data including location, seqNumber, startDate, and userId via OData query.
```

**Technical Name:** `SF_Employee_GetByLocation`
**Endpoint:** `/getEmployeeByLocation`

---

### 3. ❌ Get Employee TimeSheet - DELETE THIS FLOW

**Action:** DELETE (100% duplicate of flow #4)

---

### 4. SF_TimeEvent_GetByEmployeeDate

**Display Name:**
```
Get Employee Timesheet List
```

**Description:**
```
Retrieves and processes timesheet events (C10/C20) from SuccessFactors, groups by date, pairs check-ins with check-outs, and converts UTC timestamps to local time. Returns structured JSON with employee timesheet data.
```

**Technical Name:** `SF_TimeEvent_GetByEmployeeDate`
**Endpoint:** `/timeEvent/getListOfEmployeeTimeSheet`

---

### 5. SF_WorkAssignment_GetByDateRange

**Display Name:**
```
Get Work Assignment List
```

**Description:**
```
Fetches work assignments from SuccessFactors for a dynamic date range (last 2 months to current month). Automatically calculates date range and retrieves employee time records via OData.
```

**Technical Name:** `SF_WorkAssignment_GetByDateRange`
**Endpoint:** `/workAssignment`

---

### 6. WA_TS_Orchestrator_ByLocation

**Display Name:**
```
WA & TS Orchestrator - By Location
```

**Description:**
```
Orchestrates retrieval and combination of work assignments and timesheets for employees at a specific location. Loops through work assignments, fetches corresponding timesheets, and merges results into combined XML payload.
```

**Technical Name:** `WA_TS_Orchestrator_ByLocation`
**Endpoint:** `/getWAAndTimesheet`

---

### 7. WA_TS_Combine_Single (OPTIONAL - Consider Deleting)

**Display Name:**
```
WA & TS Data - Single Record
```

**Description:**
```
Fetches and combines single work assignment with corresponding timesheet. Converts JSON to XML, retrieves employee timesheet events, and merges data for conflict detection.
```

**Technical Name:** `WA_TS_Combine_Single`
**Endpoint:** `/getWAAndTimesheet` ⚠️ **CONFLICTS with flow #6**

**⚠️ Recommendation:** Delete if not used, or change endpoint to `/getWAAndTimesheetSingle`

---

### 8. WA_TS_Combine_Bulk

**Display Name:**
```
WA & TS Data - Bulk Retrieval
```

**Description:**
```
Batch processing for multiple work assignments and timesheets. Retrieves WA data for date range, fetches corresponding timesheet events, and combines results into XML for bulk conflict resolution.
```

**Technical Name:** `WA_TS_Combine_Bulk`
**Endpoint:** `/getWAAndTimesheetBulk`

---

### 9. WA_TS_Conflict_Execute

**Display Name:**
```
WA Conflict Resolution - Execute Actions
```

**Description:**
```
Executes conflict resolution actions in SuccessFactors. Deletes timesheet events, inserts new time events for trimmed times, and removes conflicting work assignments based on analysis results.
```

**Technical Name:** `WA_TS_Conflict_Execute`
**Endpoint:** `/resolveWAConflictAction`

---

### 10. WA_TS_Conflict_Analyze

**Display Name:**
```
WA Conflict Resolution - Analyze Logic
```

**Description:**
```
Core conflict detection algorithm. Analyzes time overlaps between work assignments and timesheets, applies resolution rules (delete/trim), and generates action lists for execution. Uses Asia/Riyadh timezone.
```

**Technical Name:** `WA_TS_Conflict_Analyze`
**Endpoint:** `/resovleWAConfilict` ⚠️ **TYPO** (should be `/resolveWAConflict`)

---

### 11. WA_TS_Conflict_CoreOrchestrator

**Display Name:**
```
WA Conflict Resolution - Core Orchestrator
```

**Description:**
```
Core orchestrator for conflict analysis and execution. Receives combined WA+TS data, calls conflict analysis logic, then executes resolved actions. Does not retrieve data - expects data to be provided.
```

**Technical Name:** `WA_TS_Conflict_CoreOrchestrator`
**Endpoint:** `/resolveWAConflictMain`

---

### 12. WA_TS_Conflict_EndToEnd

**Display Name:**
```
WA Conflict Resolution - End to End
```

**Description:**
```
Top-level end-to-end orchestrator for complete conflict resolution process. Retrieves WA and TS data, then orchestrates conflict analysis and execution. Main entry point for automated conflict resolution jobs.
```

**Technical Name:** `WA_TS_Conflict_EndToEnd`
**Endpoint:** `/resolveWAMain`

---

### 13. Test_TimeEvent_Create

**Display Name:**
```
TEST - Create Employee Timesheet
```

**Description:**
```
Test flow for creating timesheet events in SuccessFactors. Accepts employee ID, date, check-in/out times, and timezone. Creates C10 (check-in) and C20 (check-out) time events for testing purposes.
```

**Technical Name:** `Test_TimeEvent_Create`
**Endpoint:** `/createEmployeeTimesheet`

---

### 14. Test_TimeEvent_Delete

**Display Name:**
```
TEST - Delete Employee Timesheet
```

**Description:**
```
Test flow for deleting timesheet events in SuccessFactors. Accepts same input as create flow. Used for testing timesheet deletion functionality.
```

**Technical Name:** `Test_TimeEvent_Delete`
**Endpoint:** `/deleteEmployeeTimesheet`

---

## 📝 Quick Copy Format (All Flows)

Use this for quick reference when updating multiple flows:

```
Flow 1: SF_WorkAssignment_Delete
Display: Delete Work Assignment
Desc: Deletes work assignment records in SuccessFactors by setting approvalStatus to CANCELLED.

Flow 2: SF_Employee_GetByLocation
Display: Get Employee List by Location
Desc: Retrieves employee list from SuccessFactors filtered by location ID.

Flow 3: DELETE (duplicate)

Flow 4: SF_TimeEvent_GetByEmployeeDate
Display: Get Employee Timesheet List
Desc: Retrieves and processes timesheet events (C10/C20), groups by date, pairs check-ins with check-outs.

Flow 5: SF_WorkAssignment_GetByDateRange
Display: Get Work Assignment List
Desc: Fetches work assignments for dynamic date range (last 2 months to current month).

Flow 6: WA_TS_Orchestrator_ByLocation
Display: WA & TS Orchestrator - By Location
Desc: Orchestrates retrieval and combination of work assignments and timesheets by location.

Flow 7: WA_TS_Combine_Single (DELETE or change endpoint)
Display: WA & TS Data - Single Record
Desc: Fetches and combines single work assignment with corresponding timesheet.

Flow 8: WA_TS_Combine_Bulk
Display: WA & TS Data - Bulk Retrieval
Desc: Batch processing for multiple work assignments and timesheets.

Flow 9: WA_TS_Conflict_Execute
Display: WA Conflict Resolution - Execute Actions
Desc: Executes conflict resolution actions (deletes, inserts) in SuccessFactors.

Flow 10: WA_TS_Conflict_Analyze
Display: WA Conflict Resolution - Analyze Logic
Desc: Core conflict detection algorithm. Analyzes overlaps and generates action lists.

Flow 11: WA_TS_Conflict_CoreOrchestrator
Display: WA Conflict Resolution - Core Orchestrator
Desc: Core orchestrator for analysis and execution. Expects data to be provided.

Flow 12: WA_TS_Conflict_EndToEnd
Display: WA Conflict Resolution - End to End
Desc: Top-level orchestrator. Retrieves data and orchestrates complete resolution process.

Flow 13: Test_TimeEvent_Create
Display: TEST - Create Employee Timesheet
Desc: Test flow for creating C10/C20 timesheet events in SuccessFactors.

Flow 14: Test_TimeEvent_Delete
Display: TEST - Delete Employee Timesheet
Desc: Test flow for deleting timesheet events in SuccessFactors.
```

---

## 🎯 Character Limits for CPI Fields

**SAP CPI typical limits:**
- Display Name: ~100 characters (keep under 50 for readability)
- Description: ~255 characters (keep under 200 for readability)
- Technical Name: ~100 characters (no spaces, use underscores)

All descriptions above are within these limits.

---

## ✅ Update Checklist

For each iFlow in CPI:

- [ ] Open iFlow in CPI Web UI
- [ ] Edit Properties/Metadata
- [ ] Update Display Name (copy from above)
- [ ] Update Description (copy from above)
- [ ] Save changes
- [ ] Verify display name shows correctly in iFlow list
- [ ] Update technical name separately (directory/MANIFEST.MF)

---

## 📊 Summary Table

| Flow | Display Name | Technical Name | Length |
|------|-------------|----------------|--------|
| 1 | Delete Work Assignment | SF_WorkAssignment_Delete | ✅ 26 |
| 2 | Get Employee List by Location | SF_Employee_GetByLocation | ✅ 33 |
| 3 | DELETE | DELETE | ❌ |
| 4 | Get Employee Timesheet List | SF_TimeEvent_GetByEmployeeDate | ✅ 31 |
| 5 | Get Work Assignment List | SF_WorkAssignment_GetByDateRange | ✅ 26 |
| 6 | WA & TS Orchestrator - By Location | WA_TS_Orchestrator_ByLocation | ✅ 39 |
| 7 | WA & TS Data - Single Record | WA_TS_Combine_Single | ⚠️ 31 |
| 8 | WA & TS Data - Bulk Retrieval | WA_TS_Combine_Bulk | ✅ 32 |
| 9 | WA Conflict Resolution - Execute Actions | WA_TS_Conflict_Execute | ✅ 45 |
| 10 | WA Conflict Resolution - Analyze Logic | WA_TS_Conflict_Analyze | ✅ 44 |
| 11 | WA Conflict Resolution - Core Orchestrator | WA_TS_Conflict_CoreOrchestrator | ✅ 48 |
| 12 | WA Conflict Resolution - End to End | WA_TS_Conflict_EndToEnd | ✅ 40 |
| 13 | TEST - Create Employee Timesheet | Test_TimeEvent_Create | ✅ 35 |
| 14 | TEST - Delete Employee Timesheet | Test_TimeEvent_Delete | ✅ 35 |

All names are within recommended limits! ✅

---

## 🚀 Next Steps

1. **Backup current iFlows** before making changes
2. **Start with test flows** (#13, #14) to verify process
3. **Update core flows** (#10, #11, #12) - most critical
4. **Update supporting flows** (#4, #5, #6, #8, #9)
5. **Update utility flows** (#1, #2)
6. **Delete duplicate** (#3)
7. **Decide on flow #7** (delete or fix endpoint conflict)

---

**Document Created:** 2025-11-06
**Ready for CPI Metadata Update:** ✅ YES
