# Unused Scripts Cleanup Guide

## 🎯 Finding: Unused/Dead Scripts in iFlows

After detailed analysis, several iFlows contain Groovy scripts that are **NOT referenced** in the actual integration flow execution. These are likely:
- Leftover files from copy/paste operations
- Template files that were never cleaned up
- Old scripts from previous versions

---

## ❌ iFlows with UNUSED Scripts

### 1. Work Assignment Conflict Main
**Location:** `/Work Assignment Conflict Main/src/main/resources/script/`

**Files:**
- ❌ script1.groovy (UNUSED)
- ❌ script2.groovy (UNUSED)
- ❌ script3.groovy (UNUSED)
- ❌ script4.groovy (UNUSED)

**Why Unused:**
The iFlow definition contains ONLY:
- 1 Start event (HTTP Sender)
- 2 Service Tasks (External HTTP calls - no scripts)
- 1 End event

**Flow Structure:**
```
Start (HTTP) → ServiceTask: Get WA and Timesheet → ServiceTask: Resolve WA conflict → End
```
**No ScriptTasks = No Groovy scripts executed**

---

### 2. Resolve Work Assignment Conflict Main
**Location:** `/Resolve Work Assignment Conflict Main/src/main/resources/script/`

**Files:**
- ❌ script1.groovy (UNUSED)
- ❌ script2.groovy (UNUSED)
- ❌ script3.groovy (UNUSED)
- ❌ script4.groovy (UNUSED)

**Why Unused:**
Same as above - ONLY ServiceTasks (external calls), no ScriptTasks

**Flow Structure:**
```
Start (HTTP) → ServiceTask: Resolve WA logic → ServiceTask: Resolve WA Action → End
```

---

## ✅ iFlows with USED Scripts

For comparison, here are flows that **DO USE** their scripts:

### Resolve Work Assignment Conflict Logic
**Location:** `/Resolve Work Assignment Conflict Logic/src/main/resources/script/`

**Files:**
- ✅ script1.groovy (USED - 145 lines of conflict detection logic)

**Why Used:**
The iFlow contains a ScriptTask that executes the Groovy script for business logic

---

### Get Work Assignment And Timesheet Bulk
**Location:** `/Get Work Assignment And Timesheet Bulk/src/main/resources/script/`

**Files:**
- ✅ script1.groovy (USED - JSON to XML conversion)
- ✅ script2.groovy (USED - Extract WA data)
- ✅ script3.groovy (USED - Combine WA + TS)
- ✅ script4.groovy (USED - Merge results)
- ✅ script5.groovy (USED - Arrange result)
- ✅ script6.groovy (USED - Save result)

**Why Used:**
The iFlow contains multiple ScriptTasks for data transformation

---

## 🧹 Cleanup Actions

### Safe to Delete (Dead Code)
```bash
# Remove unused scripts from Work Assignment Conflict Main
rm -rf "Work Assignment Conflict Main/src/main/resources/script/"

# Remove unused scripts from Resolve Work Assignment Conflict Main
rm -rf "Resolve Work Assignment Conflict Main/src/main/resources/script/"
```

**Impact:** ✅ None - these scripts are not referenced in the iFlow execution

**Benefits:**
- Reduces confusion
- Smaller deployment package
- Cleaner codebase
- Easier maintenance

---

## 📊 How to Identify Unused Scripts

### Method 1: Check .iflw file for ScriptTasks
```bash
grep "scriptTask" <iflow-name>/src/main/resources/scenarioflows/integrationflow/*.iflw
```

**No output** = No scripts used in this iFlow

### Method 2: Check for script references
```bash
grep -E "script[0-9]\.groovy" <iflow-name>/src/main/resources/scenarioflows/integrationflow/*.iflw
```

**No output** = No scripts referenced

### Method 3: Check flow components
```bash
grep -E "<bpmn2:scriptTask|<bpmn2:serviceTask" <iflow-name>/.../...iflw
```

**Only serviceTask, no scriptTask** = No Groovy scripts executed

---

## 🔍 Verification Results

| iFlow Name | ScriptTasks? | Scripts Used? | Action |
|-----------|--------------|---------------|--------|
| Work Assignment Conflict Main | ❌ No | ❌ No | DELETE scripts |
| Resolve Work Assignment Conflict Main | ❌ No | ❌ No | DELETE scripts |
| Resolve Work Assignment Conflict Logic | ✅ Yes | ✅ Yes | KEEP scripts |
| Resolve Work Assignment Conflict Action | ✅ Yes | ✅ Yes | KEEP scripts |
| Get Work Assignment And Timesheet Bulk | ✅ Yes | ✅ Yes | KEEP scripts |
| Get Location work Assignment List | ✅ Yes | ✅ Yes | KEEP scripts |
| Get List of Employee TimeSheet | ✅ Yes | ✅ Yes | KEEP scripts |
| Get List of work Assignment | ✅ Yes | ✅ Yes | KEEP scripts |
| Delete Work Assignment | ✅ Yes | ✅ Yes | KEEP scripts |
| test-Create Employee Timesheet | ✅ Yes | ✅ Yes | KEEP scripts |
| test-Delete Employee Timesheet | ✅ Yes | ✅ Yes | KEEP scripts |

---

## 🎓 Why This Happens

### Common Causes of Unused Scripts:

1. **Copy-Paste Pattern**
   - Developer copies an existing iFlow as template
   - Deletes ScriptTasks from the flow
   - Forgets to delete the script files

2. **Template Inheritance**
   - CPI creates default script files when creating new iFlow
   - Developer uses only ServiceTasks (external calls)
   - Never uses the script files but they remain

3. **Refactoring**
   - Original flow had Groovy logic
   - Logic moved to external iFlow
   - ScriptTasks replaced with ServiceTasks
   - Old script files left behind

---

## 💡 Best Practices

### When Creating New iFlows:

1. **Delete unused artifacts immediately**
   - If you don't use ScriptTasks, delete the script folder
   - Keep the iFlow directory clean

2. **Use clear naming**
   - Orchestrator flows: usually just ServiceTasks (external calls)
   - Processing flows: usually have ScriptTasks (business logic)

3. **Document the pattern**
   - Add comments in the iFlow description
   - "This flow makes external calls only - no scripts"

---

## 🚀 Recommended Cleanup Steps

### Step 1: Backup
```bash
# Create backup before deleting anything
tar -czf unused_scripts_backup.tar.gz \
  "Work Assignment Conflict Main/src/main/resources/script" \
  "Resolve Work Assignment Conflict Main/src/main/resources/script"
```

### Step 2: Verify in CPI Web UI
1. Open each iFlow in CPI Web UI
2. Check the graphical flow diagram
3. Confirm: No "Script" steps, only "External Call" steps

### Step 3: Delete Unused Scripts
```bash
# Remove the script directories
rm -rf "Work Assignment Conflict Main/src/main/resources/script"
rm -rf "Resolve Work Assignment Conflict Main/src/main/resources/script"
```

### Step 4: Test
1. Deploy both iFlows to DEV
2. Test execution
3. Verify: Everything works exactly the same

### Step 5: Document
Update CLAUDE.md to note:
```markdown
## iFlow Types

### Orchestrator Flows (No Scripts)
- Work Assignment Conflict Main
- Resolve Work Assignment Conflict Main
These flows only make HTTP calls to other iFlows

### Processing Flows (With Scripts)
- Resolve Work Assignment Conflict Logic
- Get Work Assignment And Timesheet Bulk
These flows contain Groovy scripts for business logic
```

---

## ✅ Summary

**Your observation was 100% correct!**

The Groovy scripts in both "Main" flows are:
- ❌ NOT used in the iFlow execution
- ❌ NOT referenced in the .iflw definition
- ❌ Dead code that can be safely deleted

**Why they looked identical:**
Because they were both copied from the same template and never cleaned up!

**The actual flow logic:**
Both flows just orchestrate HTTP calls to other CPI iFlows - no custom scripting needed.

Thank you for your careful analysis! This cleanup will make the project much cleaner and easier to maintain.
