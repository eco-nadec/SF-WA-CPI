# CPI Metadata Descriptions - Copy & Paste Ready

This document contains brief descriptions for each integration flow to add to SAP CPI metadata fields.

---

## 📋 How to Use This Document

1. Open each iFlow in SAP CPI Web UI
2. Verify the **Current Name** matches your flow (to ensure you're updating the right one)
3. Go to iFlow properties/metadata
4. Copy the **Display Name** to the display name field
5. Copy the **Description** to the description field
6. Update the technical name (directory/Bundle-SymbolicName) separately

---

## 🔄 Integration Flow Metadata

### 1. SF_WorkAssignment_Delete

**Current Name:**
```
Delete Work Assignment
```

**Display Name:**
```
Delete Work Assignment
```

**Description:**
```
Deletes work assignments in SuccessFactors by setting approvalStatus to CANCELLED via OData upsert.
```

**Technical Name:** `SF_WorkAssignment_Delete`
**Endpoint:** `/deleteWAList`

---

### 2. SF_Employee_GetByLocation

**Current Name:**
```
Get Employee List By Location
```

**Display Name:**
```
Get Employee List by Location
```

**Description:**
```
Retrieves employee list from SuccessFactors filtered by location ID via OData.
```

**Technical Name:** `SF_Employee_GetByLocation`
**Endpoint:** `/getEmployeeByLocation`

---

### 3. ❌ Get Employee TimeSheet - DELETE THIS FLOW

**Current Name:**
```
Get Employee TimeSheet
```

**Action:** DELETE (100% duplicate of flow #4)

---

### 4. SF_TimeEvent_GetByEmployeeDate

**Current Name:**
```
Get List of Employee TimeSheet
```

**Display Name:**
```
Get Employee Timesheet List
```

**Description:**
```
Retrieves timesheet events (C10/C20) from SuccessFactors, groups by date, and pairs check-ins with check-outs.
```

**Technical Name:** `SF_TimeEvent_GetByEmployeeDate`
**Endpoint:** `/timeEvent/getListOfEmployeeTimeSheet`

---

### 5. SF_WorkAssignment_GetByDateRange

**Current Name:**
```
Get List of work Assignment
```

**Display Name:**
```
Get Work Assignment List
```

**Description:**
```
Fetches work assignments from SuccessFactors for dynamic date range (last 2 months to current month).
```

**Technical Name:** `SF_WorkAssignment_GetByDateRange`
**Endpoint:** `/workAssignment`

---

### 6. WA_TS_Orchestrator_ByLocation

**Current Name:**
```
Get Location work Assignment List
```

**Display Name:**
```
WA & TS Orchestrator - By Location
```

**Description:**
```
Retrieves work assignments and timesheets for a specific location and combines them into XML payload.
```

**Technical Name:** `WA_TS_Orchestrator_ByLocation`
**Endpoint:** `/getWAAndTimesheet`

---

### 7. WA_TS_Combine_Single (OPTIONAL - Consider Deleting)

**Current Name:**
```
Get Work Assignment And Timesheet
```

**Display Name:**
```
WA & TS Data - Single Record
```

**Description:**
```
Fetches and combines single work assignment with corresponding timesheet for conflict detection.
```

**Technical Name:** `WA_TS_Combine_Single`
**Endpoint:** `/getWAAndTimesheet` ⚠️ **CONFLICTS with flow #6**

**⚠️ Recommendation:** Delete if not used, or change endpoint to `/getWAAndTimesheetSingle`

---

### 8. WA_TS_Combine_Bulk

**Current Name:**
```
Get Work Assignment And Timesheet Bulk
```

**Display Name:**
```
WA & TS Data - Bulk Retrieval
```

**Description:**
```
Retrieves and combines multiple work assignments with timesheets for bulk conflict resolution.
```

**Technical Name:** `WA_TS_Combine_Bulk`
**Endpoint:** `/getWAAndTimesheetBulk`

---

### 9. WA_TS_Conflict_Execute

**Current Name:**
```
Resolve Work Assignment Conflict Action
```

**Display Name:**
```
WA Conflict Resolution - Execute Actions
```

**Description:**
```
Executes conflict resolution actions: deletes timesheets, inserts new time events, and removes work assignments.
```

**Technical Name:** `WA_TS_Conflict_Execute`
**Endpoint:** `/resolveWAConflictAction`

---

### 10. WA_TS_Conflict_Analyze

**Current Name:**
```
Resolve Work Assignment Conflict Logic
```

**Display Name:**
```
WA Conflict Resolution - Analyze Logic
```

**Description:**
```
Analyzes time overlaps between work assignments and timesheets, applies resolution rules, and generates action lists.
```

**Technical Name:** `WA_TS_Conflict_Analyze`
**Endpoint:** `/resovleWAConfilict` ⚠️ **TYPO** (should be `/resolveWAConflict`)

---

### 11. WA_TS_Conflict_CoreOrchestrator

**Current Name:**
```
Resolve Work Assignment Conflict Main
```

**Display Name:**
```
WA Conflict Resolution - Core Orchestrator
```

**Description:**
```
Orchestrates conflict analysis and execution. Expects combined WA+TS data as input, analyzes conflicts, then executes actions.
```

**Technical Name:** `WA_TS_Conflict_CoreOrchestrator`
**Endpoint:** `/resolveWAConflictMain`

---

### 12. WA_TS_Conflict_EndToEnd

**Current Name:**
```
Work Assignment Conflict Main
```

**Display Name:**
```
WA Conflict Resolution - End to End
```

**Description:**
```
End-to-end orchestrator for complete conflict resolution. Retrieves data, analyzes conflicts, and executes actions.
```

**Technical Name:** `WA_TS_Conflict_EndToEnd`
**Endpoint:** `/resolveWAMain`

---

### 13. Test_TimeEvent_Create

**Current Name:**
```
test-Create Employee Timesheet
```

**Display Name:**
```
TEST - Create Employee Timesheet
```

**Description:**
```
Test flow for creating C10 (check-in) and C20 (check-out) timesheet events in SuccessFactors.
```

**Technical Name:** `Test_TimeEvent_Create`
**Endpoint:** `/createEmployeeTimesheet`

---

### 14. Test_TimeEvent_Delete

**Current Name:**
```
test-Delete Employee Timesheet
```

**Display Name:**
```
TEST - Delete Employee Timesheet
```

**Description:**
```
Test flow for deleting timesheet events in SuccessFactors.
```

**Technical Name:** `Test_TimeEvent_Delete`
**Endpoint:** `/deleteEmployeeTimesheet`

---

## 📝 Quick Copy Format (All Flows)

Use this for quick reference when updating multiple flows:

```
Flow 1: SF_WorkAssignment_Delete
Current: Delete Work Assignment
Display: Delete Work Assignment
Desc: Deletes work assignments in SuccessFactors by setting approvalStatus to CANCELLED.

Flow 2: SF_Employee_GetByLocation
Current: Get Employee List By Location
Display: Get Employee List by Location
Desc: Retrieves employee list from SuccessFactors filtered by location ID.

Flow 3: DELETE (duplicate)
Current: Get Employee TimeSheet

Flow 4: SF_TimeEvent_GetByEmployeeDate
Current: Get List of Employee TimeSheet
Display: Get Employee Timesheet List
Desc: Retrieves timesheet events (C10/C20), groups by date, and pairs check-ins with check-outs.

Flow 5: SF_WorkAssignment_GetByDateRange
Current: Get List of work Assignment
Display: Get Work Assignment List
Desc: Fetches work assignments for dynamic date range (last 2 months to current month).

Flow 6: WA_TS_Orchestrator_ByLocation
Current: Get Location work Assignment List
Display: WA & TS Orchestrator - By Location
Desc: Retrieves work assignments and timesheets for a specific location and combines them.

Flow 7: WA_TS_Combine_Single (DELETE or change endpoint)
Current: Get Work Assignment And Timesheet
Display: WA & TS Data - Single Record
Desc: Fetches and combines single work assignment with corresponding timesheet.

Flow 8: WA_TS_Combine_Bulk
Current: Get Work Assignment And Timesheet Bulk
Display: WA & TS Data - Bulk Retrieval
Desc: Retrieves and combines multiple work assignments with timesheets for bulk processing.

Flow 9: WA_TS_Conflict_Execute
Current: Resolve Work Assignment Conflict Action
Display: WA Conflict Resolution - Execute Actions
Desc: Executes conflict resolution actions: deletes timesheets, inserts events, removes work assignments.

Flow 10: WA_TS_Conflict_Analyze
Current: Resolve Work Assignment Conflict Logic
Display: WA Conflict Resolution - Analyze Logic
Desc: Analyzes time overlaps between work assignments and timesheets, generates action lists.

Flow 11: WA_TS_Conflict_CoreOrchestrator
Current: Resolve Work Assignment Conflict Main
Display: WA Conflict Resolution - Core Orchestrator
Desc: Orchestrates conflict analysis and execution. Expects combined WA+TS data as input.

Flow 12: WA_TS_Conflict_EndToEnd
Current: Work Assignment Conflict Main
Display: WA Conflict Resolution - End to End
Desc: End-to-end orchestrator. Retrieves data, analyzes conflicts, and executes actions.

Flow 13: Test_TimeEvent_Create
Current: test-Create Employee Timesheet
Display: TEST - Create Employee Timesheet
Desc: Test flow for creating C10/C20 timesheet events in SuccessFactors.

Flow 14: Test_TimeEvent_Delete
Current: test-Delete Employee Timesheet
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
