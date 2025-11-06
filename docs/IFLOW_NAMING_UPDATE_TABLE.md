# Integration Flow Naming - Complete Update Table

## 📋 How to Use This Table

For each iFlow, you have **3 names**:

1. **Current Name** - What you have now
2. **Display Name** - What users see in CPI Web UI (human-friendly, can have spaces/hyphens)
3. **Technical Name** - What systems use (directory, Bundle-SymbolicName, URLs, code - NO SPACES)

---

## 🔄 Complete Naming Table

### ✅ KEEP - Production Flows

| # | Current Name | Display Name (UI) | Technical Name (Code/URL) | Endpoint | Action |
|---|-------------|-------------------|---------------------------|----------|--------|
| 1 | Delete Work Assignment | **Delete Work Assignment** | `SF_WorkAssignment_Delete` | `/deleteWAList` | ✅ Update |
| 2 | Get Employee List By Location | **Get Employee List by Location** | `SF_Employee_GetByLocation` | `/getEmployeeByLocation` | ✅ Update |
| 3 | Get List of Employee TimeSheet | **Get Employee Timesheet List** | `SF_TimeEvent_GetByEmployeeDate` | `/timeEvent/getListOfEmployeeTimeSheet` | ✅ Update |
| 4 | Get List of work Assignment | **Get Work Assignment List** | `SF_WorkAssignment_GetByDateRange` | `/workAssignment` | ✅ Update |
| 5 | Get Location work Assignment List | **WA & TS Orchestrator - By Location** | `WA_TS_Orchestrator_ByLocation` | `/getWAAndTimesheet` | ✅ Update |
| 6 | Get Work Assignment And Timesheet Bulk | **WA & TS Data - Bulk Retrieval** | `WA_TS_Combine_Bulk` | `/getWAAndTimesheetBulk` | ✅ Update |
| 7 | Resolve Work Assignment Conflict Action | **WA Conflict Resolution - Execute Actions** | `WA_TS_Conflict_Execute` | `/resolveWAConflictAction` | ✅ Update |
| 8 | Resolve Work Assignment Conflict Logic | **WA Conflict Resolution - Analyze Logic** | `WA_TS_Conflict_Analyze` | `/resovleWAConfilict` | ✅ Update |
| 9 | Resolve Work Assignment Conflict Main | **WA Conflict Resolution - Core Orchestrator** | `WA_TS_Conflict_CoreOrchestrator` | `/resolveWAConflictMain` | ✅ Update |
| 10 | Work Assignment Conflict Main | **WA Conflict Resolution - End to End** | `WA_TS_Conflict_EndToEnd` | `/resolveWAMain` | ✅ Update |

### ⚠️ CONSIDER DELETING - Redundant/Similar Flows

| # | Current Name | Display Name (UI) | Technical Name (Code/URL) | Endpoint | Action |
|---|-------------|-------------------|---------------------------|----------|--------|
| 11 | Get Employee TimeSheet | *(DELETE - 100% duplicate)* | *(DELETE)* | `/getEmployeeTimeSheet` | ❌ DELETE |
| 12 | Get Work Assignment And Timesheet | **WA & TS Data - Single Record** | `WA_TS_Combine_Single` | `/getWAAndTimesheet` | ⚠️ Keep if used |

### 🧪 TEST FLOWS - Keep Separate

| # | Current Name | Display Name (UI) | Technical Name (Code/URL) | Endpoint | Action |
|---|-------------|-------------------|---------------------------|----------|--------|
| 13 | test-Create Employee Timesheet | **TEST - Create Employee Timesheet** | `Test_TimeEvent_Create` | `/createEmployeeTimesheet` | ✅ Update |
| 14 | test-Delete Employee Timesheet | **TEST - Delete Employee Timesheet** | `Test_TimeEvent_Delete` | `/deleteEmployeeTimesheet` | ✅ Update |

---

## 📝 Detailed Update Instructions for Each Flow

### 1. SF_WorkAssignment_Delete
**Current:** Delete Work Assignment

**Updates Needed:**
- Display Name (CPI UI): `Delete Work Assignment` *(keep current - already good)*
- Technical Name/Directory: `SF_WorkAssignment_Delete`
- Bundle-SymbolicName: `SF_WorkAssignment_Delete`
- Endpoint: `/deleteWAList` *(keep current)*

**Files to Update:**
```
Directory: Rename "Delete Work Assignment" → "SF_WorkAssignment_Delete"
MANIFEST.MF: Bundle-SymbolicName: SF_WorkAssignment_Delete
.iflw file: Rename to "SF_WorkAssignment_Delete.iflw"
CPI UI: Display Name = "Delete Work Assignment"
```

---

### 2. SF_Employee_GetByLocation
**Current:** Get Employee List By Location

**Updates Needed:**
- Display Name (CPI UI): `Get Employee List by Location`
- Technical Name/Directory: `SF_Employee_GetByLocation`
- Bundle-SymbolicName: `SF_Employee_GetByLocation`
- Endpoint: `/getEmployeeByLocation` *(keep current)*

**Files to Update:**
```
Directory: Rename "Get Employee List By Location" → "SF_Employee_GetByLocation"
MANIFEST.MF: Bundle-SymbolicName: SF_Employee_GetByLocation
.iflw file: Rename to "SF_Employee_GetByLocation.iflw"
CPI UI: Display Name = "Get Employee List by Location"
```

---

### 3. ❌ Get Employee TimeSheet - DELETE THIS FLOW
**Current:** Get Employee TimeSheet

**Action:** **DELETE ENTIRE FLOW**

**Reason:** 100% duplicate of "Get List of Employee TimeSheet" (flow #4)

**Steps:**
```bash
# Backup first
tar -czf Get_Employee_TimeSheet_backup.tar.gz "Get Employee TimeSheet"

# Delete
rm -rf "Get Employee TimeSheet"

# Update any flows that reference it to use "Get List of Employee TimeSheet" instead
```

---

### 4. SF_TimeEvent_GetByEmployeeDate
**Current:** Get List of Employee TimeSheet

**Updates Needed:**
- Display Name (CPI UI): `Get Employee Timesheet List`
- Technical Name/Directory: `SF_TimeEvent_GetByEmployeeDate`
- Bundle-SymbolicName: `SF_TimeEvent_GetByEmployeeDate`
- Endpoint: `/timeEvent/getListOfEmployeeTimeSheet` *(keep current)*

**Files to Update:**
```
Directory: Rename "Get List of Employee TimeSheet" → "SF_TimeEvent_GetByEmployeeDate"
MANIFEST.MF: Bundle-SymbolicName: SF_TimeEvent_GetByEmployeeDate
.iflw file: Rename to "SF_TimeEvent_GetByEmployeeDate.iflw"
CPI UI: Display Name = "Get Employee Timesheet List"
```

---

### 5. SF_WorkAssignment_GetByDateRange
**Current:** Get List of work Assignment

**Updates Needed:**
- Display Name (CPI UI): `Get Work Assignment List`
- Technical Name/Directory: `SF_WorkAssignment_GetByDateRange`
- Bundle-SymbolicName: `SF_WorkAssignment_GetByDateRange`
- Endpoint: `/workAssignment` *(keep current)*

**Files to Update:**
```
Directory: Rename "Get List of work Assignment" → "SF_WorkAssignment_GetByDateRange"
MANIFEST.MF: Bundle-SymbolicName: SF_WorkAssignment_GetByDateRange
.iflw file: Rename to "SF_WorkAssignment_GetByDateRange.iflw"
CPI UI: Display Name = "Get Work Assignment List"
```

---

### 6. WA_TS_Orchestrator_ByLocation
**Current:** Get Location work Assignment List

**Updates Needed:**
- Display Name (CPI UI): `WA & TS Orchestrator - By Location`
- Technical Name/Directory: `WA_TS_Orchestrator_ByLocation`
- Bundle-SymbolicName: `WA_TS_Orchestrator_ByLocation`
- Endpoint: `/getWAAndTimesheet` *(keep current)*

**Files to Update:**
```
Directory: Rename "Get Location work Assignment List" → "WA_TS_Orchestrator_ByLocation"
MANIFEST.MF: Bundle-SymbolicName: WA_TS_Orchestrator_ByLocation
.iflw file: Rename to "WA_TS_Orchestrator_ByLocation.iflw"
CPI UI: Display Name = "WA & TS Orchestrator - By Location"
```

---

### 7. WA_TS_Combine_Single (OPTIONAL - Consider Deleting)
**Current:** Get Work Assignment And Timesheet

**Updates Needed:**
- Display Name (CPI UI): `WA & TS Data - Single Record`
- Technical Name/Directory: `WA_TS_Combine_Single`
- Bundle-SymbolicName: `WA_TS_Combine_Single`
- Endpoint: `/getWAAndTimesheet` *(conflict with flow #6!)*

**⚠️ WARNING:** This flow has the SAME endpoint as flow #6 (`/getWAAndTimesheet`)

**Recommendation:**
- If NOT actively used → DELETE
- If used → Keep AND change endpoint to `/getWAAndTimesheetSingle`

**Files to Update:**
```
Directory: Rename "Get Work Assignment And Timesheet" → "WA_TS_Combine_Single"
MANIFEST.MF: Bundle-SymbolicName: WA_TS_Combine_Single
.iflw file: Rename to "WA_TS_Combine_Single.iflw"
Endpoint: Change to "/getWAAndTimesheetSingle" (to avoid conflict)
CPI UI: Display Name = "WA & TS Data - Single Record"
```

---

### 8. WA_TS_Combine_Bulk
**Current:** Get Work Assignment And Timesheet Bulk

**Updates Needed:**
- Display Name (CPI UI): `WA & TS Data - Bulk Retrieval`
- Technical Name/Directory: `WA_TS_Combine_Bulk`
- Bundle-SymbolicName: `WA_TS_Combine_Bulk`
- Endpoint: `/getWAAndTimesheetBulk` *(keep current)*

**Files to Update:**
```
Directory: Rename "Get Work Assignment And Timesheet Bulk" → "WA_TS_Combine_Bulk"
MANIFEST.MF: Bundle-SymbolicName: WA_TS_Combine_Bulk
.iflw file: Rename to "WA_TS_Combine_Bulk.iflw"
CPI UI: Display Name = "WA & TS Data - Bulk Retrieval"
```

---

### 9. WA_TS_Conflict_Execute
**Current:** Resolve Work Assignment Conflict Action

**Updates Needed:**
- Display Name (CPI UI): `WA Conflict Resolution - Execute Actions`
- Technical Name/Directory: `WA_TS_Conflict_Execute`
- Bundle-SymbolicName: `WA_TS_Conflict_Execute`
- Endpoint: `/resolveWAConflictAction` *(keep current)*

**Files to Update:**
```
Directory: Rename "Resolve Work Assignment Conflict Action" → "WA_TS_Conflict_Execute"
MANIFEST.MF: Bundle-SymbolicName: WA_TS_Conflict_Execute
.iflw file: Rename to "WA_TS_Conflict_Execute.iflw"
CPI UI: Display Name = "WA Conflict Resolution - Execute Actions"
```

**⚠️ Also Clean Up:**
```
Delete unused scripts:
rm -rf "WA_TS_Conflict_Execute/src/main/resources/script" (if they exist)
```

---

### 10. WA_TS_Conflict_Analyze
**Current:** Resolve Work Assignment Conflict Logic

**Updates Needed:**
- Display Name (CPI UI): `WA Conflict Resolution - Analyze Logic`
- Technical Name/Directory: `WA_TS_Conflict_Analyze`
- Bundle-SymbolicName: `WA_TS_Conflict_Analyze`
- Endpoint: `/resovleWAConfilict` *(keep current - but note the typo "resovle")*

**Files to Update:**
```
Directory: Rename "Resolve Work Assignment Conflict Logic" → "WA_TS_Conflict_Analyze"
MANIFEST.MF: Bundle-SymbolicName: WA_TS_Conflict_Analyze
.iflw file: Rename to "WA_TS_Conflict_Analyze.iflw"
CPI UI: Display Name = "WA Conflict Resolution - Analyze Logic"
```

**⚠️ Endpoint has TYPO:** `/resovleWAConfilict` should be `/resolveWAConflict`
- **Recommendation:** Fix the typo or document it clearly

---

### 11. WA_TS_Conflict_CoreOrchestrator
**Current:** Resolve Work Assignment Conflict Main

**Updates Needed:**
- Display Name (CPI UI): `WA Conflict Resolution - Core Orchestrator`
- Technical Name/Directory: `WA_TS_Conflict_CoreOrchestrator`
- Bundle-SymbolicName: `WA_TS_Conflict_CoreOrchestrator`
- Endpoint: `/resolveWAConflictMain` *(keep current)*

**Files to Update:**
```
Directory: Rename "Resolve Work Assignment Conflict Main" → "WA_TS_Conflict_CoreOrchestrator"
MANIFEST.MF: Bundle-SymbolicName: WA_TS_Conflict_CoreOrchestrator
.iflw file: Rename to "WA_TS_Conflict_CoreOrchestrator.iflw"
CPI UI: Display Name = "WA Conflict Resolution - Core Orchestrator"
```

**⚠️ Also Clean Up:**
```
Delete unused scripts (dead code):
rm -rf "WA_TS_Conflict_CoreOrchestrator/src/main/resources/script"
```

---

### 12. WA_TS_Conflict_EndToEnd
**Current:** Work Assignment Conflict Main

**Updates Needed:**
- Display Name (CPI UI): `WA Conflict Resolution - End to End`
- Technical Name/Directory: `WA_TS_Conflict_EndToEnd`
- Bundle-SymbolicName: `WA_TS_Conflict_EndToEnd`
- Endpoint: `/resolveWAMain` *(keep current)*

**Files to Update:**
```
Directory: Rename "Work Assignment Conflict Main" → "WA_TS_Conflict_EndToEnd"
MANIFEST.MF: Bundle-SymbolicName: WA_TS_Conflict_EndToEnd
.iflw file: Rename to "WA_TS_Conflict_EndToEnd.iflw"
CPI UI: Display Name = "WA Conflict Resolution - End to End"
```

**⚠️ Also Clean Up:**
```
Delete unused scripts (dead code):
rm -rf "WA_TS_Conflict_EndToEnd/src/main/resources/script"
```

---

### 13. Test_TimeEvent_Create
**Current:** test-Create Employee Timesheet

**Updates Needed:**
- Display Name (CPI UI): `TEST - Create Employee Timesheet`
- Technical Name/Directory: `Test_TimeEvent_Create`
- Bundle-SymbolicName: `Test_TimeEvent_Create`
- Endpoint: `/createEmployeeTimesheet` *(keep current)*

**Files to Update:**
```
Directory: Rename "test-Create Employee Timesheet" → "Test_TimeEvent_Create"
MANIFEST.MF: Bundle-SymbolicName: Test_TimeEvent_Create
.iflw file: Rename to "Test_TimeEvent_Create.iflw"
CPI UI: Display Name = "TEST - Create Employee Timesheet"
```

---

### 14. Test_TimeEvent_Delete
**Current:** test-Delete Employee Timesheet

**Updates Needed:**
- Display Name (CPI UI): `TEST - Delete Employee Timesheet`
- Technical Name/Directory: `Test_TimeEvent_Delete`
- Bundle-SymbolicName: `Test_TimeEvent_Delete`
- Endpoint: `/deleteEmployeeTimesheet` *(keep current)*

**Files to Update:**
```
Directory: Rename "test-Delete Employee Timesheet" → "Test_TimeEvent_Delete"
MANIFEST.MF: Bundle-SymbolicName: Test_TimeEvent_Delete
.iflw file: Rename to "Test_TimeEvent_Delete.iflw"
CPI UI: Display Name = "TEST - Delete Employee Timesheet"
```

---

## 🔧 Update Process (Step-by-Step)

### For Each iFlow:

#### **Step 1: Backup**
```bash
# Backup the entire project first
tar -czf CPI_Backup_$(date +%Y%m%d).tar.gz .
```

#### **Step 2: Update Directory Name**
```bash
# Example for flow #1
mv "Delete Work Assignment" "SF_WorkAssignment_Delete"
```

#### **Step 3: Update MANIFEST.MF**
```bash
# Edit: SF_WorkAssignment_Delete/META-INF/MANIFEST.MF
# Change: Bundle-SymbolicName: Delete_Work_Assignment
# To:     Bundle-SymbolicName: SF_WorkAssignment_Delete
```

#### **Step 4: Update .iflw File Name**
```bash
# Rename the integration flow file
cd "SF_WorkAssignment_Delete/src/main/resources/scenarioflows/integrationflow/"
mv "*.iflw" "SF_WorkAssignment_Delete.iflw"
```

#### **Step 5: Update Display Name in .iflw File**
```bash
# Edit the .iflw file
# Find: <ifl:property><key>displayName</key><value>Delete Work Assignment</value></ifl:property>
# Keep as: <value>Delete Work Assignment</value>
# (Display name can keep spaces for readability)
```

#### **Step 6: Update References in Other Flows**
```bash
# Search for references in other iFlows
grep -r "Delete Work Assignment" .
grep -r "CPI_DELETE_WA" .

# Update configuration properties like:
# {{CPI_DELETE_WA_LIST}} → Update to point to new technical name
```

#### **Step 7: Deploy and Test**
```bash
# Deploy updated iFlow to CPI
# Test the endpoint
# Verify in CPI monitoring
```

---

## 📊 Quick Reference: Current → Technical Name

| Current Name | Technical Name |
|-------------|---------------|
| Delete Work Assignment | `SF_WorkAssignment_Delete` |
| Get Employee List By Location | `SF_Employee_GetByLocation` |
| Get Employee TimeSheet | ❌ DELETE |
| Get List of Employee TimeSheet | `SF_TimeEvent_GetByEmployeeDate` |
| Get List of work Assignment | `SF_WorkAssignment_GetByDateRange` |
| Get Location work Assignment List | `WA_TS_Orchestrator_ByLocation` |
| Get Work Assignment And Timesheet | `WA_TS_Combine_Single` ⚠️ |
| Get Work Assignment And Timesheet Bulk | `WA_TS_Combine_Bulk` |
| Resolve Work Assignment Conflict Action | `WA_TS_Conflict_Execute` |
| Resolve Work Assignment Conflict Logic | `WA_TS_Conflict_Analyze` |
| Resolve Work Assignment Conflict Main | `WA_TS_Conflict_CoreOrchestrator` |
| Work Assignment Conflict Main | `WA_TS_Conflict_EndToEnd` |
| test-Create Employee Timesheet | `Test_TimeEvent_Create` |
| test-Delete Employee Timesheet | `Test_TimeEvent_Delete` |

---

## ⚠️ Important Notes

### 1. **Configuration Properties to Update**
After renaming, update these in CPI configuration:
- `{{CPI_GET_WA_TIME_BULK}}` → Should point to `WA_TS_Combine_Bulk`
- `{{CPI_RESOLVE_WA_CONFLICT}}` → Should point to `WA_TS_Conflict_Analyze`
- `{{CPI_RESOLVE_WA_ACTION}}` → Should point to `WA_TS_Conflict_Execute`
- `{{CPI_DELETE_WA_LIST}}` → Should point to `SF_WorkAssignment_Delete`

### 2. **Endpoint URL Conflicts**
Two flows share the same endpoint `/getWAAndTimesheet`:
- Flow #6: `WA_TS_Orchestrator_ByLocation`
- Flow #7: `WA_TS_Combine_Single`

**Resolution:** Change flow #7 endpoint to `/getWAAndTimesheetSingle` OR delete flow #7

### 3. **Typos in Endpoints**
- `/resovleWAConfilict` → Should be `/resolveWAConflict` (typo: "resovle", "confilict")
- Document this or fix it (requires updating all callers)

### 4. **Dead Code Cleanup**
Delete these script directories (unused code):
- `WA_TS_Conflict_EndToEnd/src/main/resources/script/`
- `WA_TS_Conflict_CoreOrchestrator/src/main/resources/script/`

---

## ✅ Final Checklist

Use this checklist for each flow:

```
□ Backup completed
□ Directory renamed to Technical Name
□ MANIFEST.MF updated (Bundle-SymbolicName)
□ .iflw file renamed to Technical Name.iflw
□ Display Name set in CPI UI (human-friendly)
□ Configuration properties updated
□ References in other flows updated
□ Dead code removed (if applicable)
□ Deployed to DEV environment
□ Tested endpoint
□ Verified in CPI monitoring
□ Updated documentation
□ Committed to version control
```

---

## 🚀 Recommended Update Order

**Week 1: Core Flows**
1. WA_TS_Conflict_EndToEnd (most important)
2. WA_TS_Conflict_CoreOrchestrator
3. WA_TS_Conflict_Analyze
4. WA_TS_Conflict_Execute

**Week 2: Data Retrieval**
5. WA_TS_Combine_Bulk
6. SF_TimeEvent_GetByEmployeeDate
7. SF_WorkAssignment_GetByDateRange

**Week 3: Supporting Flows**
8. WA_TS_Orchestrator_ByLocation
9. SF_WorkAssignment_Delete
10. SF_Employee_GetByLocation

**Week 4: Test & Cleanup**
11. Test flows
12. Delete duplicates
13. Remove dead code

---

**Total Time Estimate:** 20-30 hours for complete migration (includes testing)
