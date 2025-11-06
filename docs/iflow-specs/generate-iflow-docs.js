const fs = require('fs');
const { Document, Packer, Paragraph, TextRun, Table, TableRow, TableCell, ImageRun,
        Header, Footer, AlignmentType, PageOrientation, HeadingLevel, BorderStyle, WidthType,
        ShadingType, VerticalAlign, TableOfContents, LevelFormat, PageNumber, PageBreak } = require('docx');

// Flow definitions
const flows = [
  {
    name: "Delete Work Assignment",
    technical: "SF_WorkAssignment_Delete",
    endpoint: "/deleteWAList",
    package: "SF-Nadec-WorkAssignment",
    module: "SAP Cloud Platform",
    subModule: "Hana Cloud Integration",
    processingType: "Background Online",
    frequency: "On-Demand (Called by conflict resolution)",
    overview: "Deletes work assignments in SuccessFactors by setting their approval status to CANCELLED. This flow is called by the conflict resolution execute actions flow when work assignments need to be removed due to time conflicts with timesheet events.",
    requirements: [
      "Receives XML payload with work assignment IDs marked as deleted=true",
      "Filters and extracts only deleted work assignments",
      "Transforms XML to JSON format for SF OData API",
      "Sets approvalStatus to CANCELLED for each work assignment",
      "Executes upsert operation via SuccessFactors OData API"
    ],
    scripts: [
      ["Parse Delete List", "Reads XML <Item> nodes and filters items where deleted=true"],
      ["Transform to SF Format", "Creates SF OData upsert payload with approvalStatus: CANCELLED"],
      ["Prepare API Request", "Sets required headers and authentication for SF API call"]
    ],
    adapter: "Receiver (SF): SuccessFactors OData v2 API for Work Assignment entity\nAuthentication: Basic (NadecIntegAdmin)",
    testConditions: [
      ["Work assignment IDs provided in XML with deleted=true", "Work assignments marked as CANCELLED in SuccessFactors"],
      ["Empty or malformed XML payload", "Error handling triggered, appropriate error message returned"],
      ["SF API authentication failure", "Error captured and logged, retry mechanism activated"]
    ]
  },
  {
    name: "Get Employee Timesheet List",
    technical: "SF_TimeEvent_GetByEmployeeDate",
    endpoint: "/timeEvent/getListOfEmployeeTimeSheet",
    package: "SF-Nadec-WorkAssignment",
    module: "SAP Cloud Platform",
    subModule: "Hana Cloud Integration",
    processingType: "Background Online",
    frequency: "On-Demand (Called by orchestrator flows)",
    overview: "Retrieves timesheet events (C10 check-in and C20 check-out) from SuccessFactors, groups them by date, and pairs check-ins with check-outs for each employee. This flow processes time events and converts UTC timestamps to local time (Asia/Riyadh timezone).",
    requirements: [
      "Fetch TimeEvent data from SuccessFactors OData API",
      "Filter events by employee ID and date range",
      "Group time events by local date (handling timezone offsets)",
      "Sort events by timestamp within each date",
      "Pair first C10 (check-in) with last C20 (check-out) per day",
      "Convert UTC timestamps to local time strings (HH:mm:ss format)",
      "Return structured JSON with employeeId, date, checkIn/Out IDs and times"
    ],
    scripts: [
      ["Fetch Time Events", "Calls SF OData API to retrieve TimeEvent records filtered by employee and date"],
      ["Group by Date", "Groups events by local date, handles timezone offsets (Asia/Riyadh)"],
      ["Pair Check-ins/Outs", "Sorts events and pairs first C10 with last C20 per day"],
      ["Format Response", "Converts UTC to local time, structures output JSON with paired events"],
      ["Handle Edge Cases", "Manages incomplete pairs, multiple check-ins/outs, and timezone conversions"]
    ],
    adapter: "Receiver (SF): SuccessFactors OData v2 API for TimeEvent entity\nAuthentication: OAuth Bearer Assertion (SF-Nadec-TimeEvent)",
    testConditions: [
      ["Employee with normal check-in/check-out pairs", "Returns paired events with correct local times"],
      ["Employee with multiple check-ins on same day", "Pairs first check-in with last check-out"],
      ["Employee with no time events", "Returns empty array for timesheet data"],
      ["Cross-timezone date handling", "Events grouped correctly by local date, not UTC date"]
    ]
  },
  {
    name: "Get Work Assignment List",
    technical: "SF_WorkAssignment_GetByDateRange",
    endpoint: "/workAssignment",
    package: "SF-Nadec-WorkAssignment",
    module: "SAP Cloud Platform",
    subModule: "Hana Cloud Integration",
    processingType: "Background Online",
    frequency: "Scheduled Daily / On-Demand",
    overview: "Fetches work assignments from SuccessFactors within a dynamic date range. The date range is automatically calculated as: first day of 2 months ago to last day of current month. For example, if executed on 2025-11-06, it retrieves work assignments from 2025-09-01 to 2025-11-30.",
    requirements: [
      "Calculate dynamic date range: first day of (current month - 2) to last day of current month",
      "Build OData filter query with calculated date range",
      "Fetch work assignment records from SuccessFactors",
      "Use UTC timezone for date calculations",
      "Return work assignment data with employee IDs, dates, and time ranges"
    ],
    scripts: [
      ["Calculate Date Range", "Computes first day of 2 months ago and last day of current month using UTC"],
      ["Build OData Filter", "Constructs filter: startDate ge datetime'...' and startDate le datetime'...'"],
      ["Format Response", "Structures work assignment data for downstream processing"]
    ],
    adapter: "Receiver (SF): SuccessFactors OData v2 API for Work Assignment entity\nAuthentication: Basic (NadecIntegAdmin)",
    testConditions: [
      ["Scheduled execution on any date", "Returns work assignments for calculated 3-month range"],
      ["Work assignments exist in date range", "All matching records retrieved successfully"],
      ["No work assignments in date range", "Returns empty result set without error"],
      ["SF API rate limit exceeded", "Retry mechanism handles rate limiting gracefully"]
    ]
  },
  {
    name: "WA and TS Data Bulk Retrieval",
    technical: "WA_TS_Combine_Bulk",
    endpoint: "/getWAAndTimesheetBulk",
    package: "SF-Nadec-WorkAssignment",
    module: "SAP Cloud Platform",
    subModule: "Hana Cloud Integration",
    processingType: "Background Online",
    frequency: "On-Demand (Batch Processing)",
    overview: "Batch processing flow that retrieves and combines multiple work assignments with their corresponding timesheets. Optimized for bulk operations, this flow handles large volumes of records efficiently by processing them in batches and implementing appropriate delays for SF API rate limiting.",
    requirements: [
      "Accept multiple work assignment records as JSON array input",
      "Convert JSON array to XML <Records> structure for processing",
      "Extract and save work assignment data to message property",
      "Fetch corresponding timesheet data for each work assignment",
      "Combine work assignment XML with timesheet JSON",
      "Apply 5-second delay for SF API rate limiting compliance",
      "Return combined XML with all work assignments and timesheets"
    ],
    scripts: [
      ["JSON to XML Conversion", "Converts input JSON array to XML <Records> structure"],
      ["Extract WA Data", "Extracts work assignment data and saves to message property 'workAssignmentData'"],
      ["Combine Data", "Merges work assignment XML with fetched timesheet JSON"],
      ["Aggregate Results", "Combines multiple WA-TS pairs into single payload"],
      ["Format Output", "Structures final XML output with <WorkAssignment> and <Timesheet> sections"],
      ["Rate Limit Handler", "Implements 5-second delay (Thread.sleep) between batch operations"]
    ],
    adapter: "Receiver (SF): Multiple API calls\n- Work Assignment OData API\n- TimeEvent OData API\nAuthentication: Basic + OAuth Bearer",
    testConditions: [
      ["Bulk input with 100 work assignments", "All records processed, combined with timesheets successfully"],
      ["Work assignments with no matching timesheets", "Returns WA data with empty timesheet sections"],
      ["SF API rate limiting triggered", "5-second delay prevents rate limit errors"],
      ["Large payload > 10MB", "Chunking mechanism handles large data volumes"]
    ]
  },
  {
    name: "WA Conflict Resolution - Analyze Logic",
    technical: "WA_TS_Conflict_Analyze",
    endpoint: "/resovleWAConfilict",
    package: "SF-Nadec-WorkAssignment",
    module: "SAP Cloud Platform",
    subModule: "Hana Cloud Integration",
    processingType: "Background Online",
    frequency: "On-Demand (Called by orchestrator)",
    overview: "CORE CONFLICT DETECTION ENGINE: Analyzes time overlaps between work assignments (scheduled work times) and timesheet events (actual check-in/check-out times). Implements business rules to resolve conflicts by determining which records to delete or trim. Uses Asia/Riyadh timezone for all time calculations.",
    requirements: [
      "Receive JSON array with paired work assignments and timesheets",
      "Detect time overlaps between WA (startTime-endTime) and TS (checkIn-checkOut)",
      "Apply resolution rules based on overlap type",
      "Generate delete lists for work assignments and timesheets",
      "Create new TimeEvent records for trimmed times (±1 minute adjustment)",
      "Return structured JSON with resolved items and actions"
    ],
    scripts: [
      ["Parse Input Data", "Extracts work assignment and timesheet pairs from input JSON"],
      ["CORE ALGORITHM: Detect Overlaps", "Compares time ranges: !(tsOut < waStart || tsIn > waEnd). Rules:\n1. WA fully inside TS → Delete WA\n2. TS fully inside WA → Delete TS (C10 and C20)\n3. Partial overlap (TS starts before) → Trim TS checkout to WA start - 1 min\n4. Partial overlap (TS ends after) → Trim TS checkin to WA end + 1 min\n5. Other overlaps → Delete WA (fallback)"],
      ["Generate Delete Lists", "Creates timesheetDelete and workAssignmentDelete arrays with IDs"],
      ["Create Trim Events", "Generates new C10/C20 TimeEvent records with adjusted times"],
      ["Format Output", "Structures JSON: {resolvedItems, timesheetDelete, workAssignmentDelete, timeEventInsert}"]
    ],
    adapter: "Internal processing only (no external adapter)\nCalled via HTTP by orchestrator flows",
    testConditions: [
      ["WA: 08:00-17:00, TS: 07:30-18:00 (WA inside TS)", "Delete WA, keep TS"],
      ["WA: 08:00-17:00, TS: 09:00-16:00 (TS inside WA)", "Delete TS C10+C20, keep WA"],
      ["WA: 08:00-17:00, TS: 07:00-09:00 (TS starts before)", "Trim TS C20 to 07:59, keep WA"],
      ["WA: 08:00-17:00, TS: 16:00-19:00 (TS ends after)", "Trim TS C10 to 17:01, keep WA"],
      ["No overlap between WA and TS", "No action taken, both records preserved"]
    ]
  },
  {
    name: "WA Conflict Resolution - Core Orchestrator",
    technical: "WA_TS_Conflict_CoreOrchestrator",
    endpoint: "/resolveWAConflictMain",
    package: "SF-Nadec-WorkAssignment",
    subModule: "Hana Cloud Integration",
    processingType: "Background Online",
    frequency: "On-Demand (Orchestration)",
    overview: "CORE ORCHESTRATOR: Coordinates the conflict resolution process. Expects combined work assignment and timesheet data as input, orchestrates conflict analysis by calling the Analyze Logic flow, then executes the resolved actions by calling the Execute Actions flow. This is a pure orchestrator with no business logic in scripts.",
    requirements: [
      "Receive combined WA+TS data (XML or JSON)",
      "Call Analyze Logic flow to detect and resolve conflicts",
      "Receive resolution output (delete lists, trim events)",
      "Call Execute Actions flow to apply changes in SuccessFactors",
      "Return final execution status and results"
    ],
    scripts: [
      ["Validate Input", "Checks input data structure and completeness"],
      ["Prepare Analysis Call", "Formats payload for Analyze Logic endpoint"],
      ["Prepare Action Call", "Formats resolution output for Execute Actions endpoint"],
      ["Aggregate Results", "Combines analysis and execution results for final response"]
    ],
    adapter: "Internal orchestration:\n- Calls: WA Conflict Resolution - Analyze Logic\n- Calls: WA Conflict Resolution - Execute Actions",
    testConditions: [
      ["Combined WA+TS data with conflicts", "Analyzes and executes resolution successfully"],
      ["Analyze Logic flow returns empty actions", "Execute Actions not called, process completes"],
      ["Execute Actions flow fails", "Error handling captures failure, rolls back if needed"],
      ["Large dataset with 1000+ pairs", "Processes all pairs efficiently, handles timeouts"]
    ]
  },
  {
    name: "WA Conflict Resolution - End to End",
    technical: "WA_TS_Conflict_EndToEnd",
    endpoint: "/resolveWAMain",
    package: "SF-Nadec-WorkAssignment",
    module: "SAP Cloud Platform",
    subModule: "Hana Cloud Integration",
    processingType: "Background Online",
    frequency: "Scheduled Daily / On-Demand",
    overview: "END-TO-END ORCHESTRATOR: Complete conflict resolution process from data retrieval to action execution. Fetches work assignments and timesheets from SuccessFactors, combines them, analyzes conflicts, and executes resolutions. This is the main entry point for automated conflict resolution jobs.",
    requirements: [
      "Fetch work assignments for date range (calls Get Work Assignment List)",
      "Fetch corresponding timesheets (calls Get Employee Timesheet List)",
      "Combine WA and TS data (calls WA and TS Data Bulk Retrieval)",
      "Analyze conflicts (calls WA Conflict Resolution - Analyze Logic)",
      "Execute resolutions (calls WA Conflict Resolution - Execute Actions)",
      "Return comprehensive results with statistics"
    ],
    scripts: [
      ["Initiate Process", "Sets up date range and initializes counters"],
      ["Fetch WA Data", "Calls external iFlow: WA and TS Data Bulk Retrieval"],
      ["Orchestrate Resolution", "Calls: WA Conflict Resolution - Core Orchestrator"],
      ["Aggregate Statistics", "Counts resolved items, deletions, trims, and errors"]
    ],
    adapter: "Orchestrates multiple flows:\n- Get Work Assignment List\n- WA and TS Data Bulk Retrieval\n- WA Conflict Resolution - Core Orchestrator",
    testConditions: [
      ["Scheduled daily execution", "Processes all WA-TS pairs for configured date range"],
      ["100 conflicts detected and resolved", "All conflicts analyzed and actions executed successfully"],
      ["SF API temporarily unavailable", "Retry mechanism handles transient failures"],
      ["Process exceeds 10-minute timeout", "Chunks processing into smaller batches"]
    ]
  },
  {
    name: "WA Conflict Resolution - Execute Actions",
    technical: "WA_TS_Conflict_Execute",
    endpoint: "/resolveWAConflictAction",
    package: "SF-Nadec-WorkAssignment",
    module: "SAP Cloud Platform",
    subModule: "Hana Cloud Integration",
    processingType: "Background Online",
    frequency: "On-Demand (Called by orchestrator)",
    overview: "ACTION EXECUTOR: Executes conflict resolution actions in SuccessFactors based on analysis output. Performs three types of operations: (1) Delete work assignments by setting status to CANCELLED, (2) Delete timesheet events (C10/C20), (3) Insert new timesheet events for trimmed times.",
    requirements: [
      "Receive resolution output from Analyze Logic flow",
      "Extract delete lists: timesheetDelete, workAssignmentDelete",
      "Extract insert list: timeEventInsert (for trimmed times)",
      "Execute work assignment deletions (calls Delete Work Assignment flow)",
      "Execute timesheet deletions via SF TimeEvent OData API",
      "Create new timesheet events for trimmed times via SF TimeEvent API",
      "Handle batch operations for multiple records",
      "Return execution results with success/failure counts"
    ],
    scripts: [
      ["Parse Resolution Data", "Extracts delete and insert arrays from input JSON"],
      ["Execute WA Deletions", "Transforms WA IDs to XML, calls Delete Work Assignment flow"],
      ["Execute TS Deletions", "Formats C10/C20 IDs for SF OData delete API, executes batch delete"],
      ["Create Trimmed Events", "Generates new TimeEvent records with adjusted times (±1 min), posts to SF"],
      ["Aggregate Results", "Counts successful/failed operations, structures response"]
    ],
    adapter: "Receiver (SF): Multiple operations\n- Delete WA: Calls SF_WorkAssignment_Delete flow\n- Delete TS: SF TimeEvent OData API (DELETE)\n- Insert TS: SF TimeEvent API (POST)\nAuthentication: Basic + OAuth Bearer",
    testConditions: [
      ["10 WA deletions, 5 TS deletions, 3 TS inserts", "All operations executed successfully"],
      ["SF API rejects TS insert (validation error)", "Error captured, other operations continue"],
      ["Duplicate TS event in insert list", "SF handles duplicate, returns appropriate error"],
      ["Network timeout during execution", "Retry mechanism handles timeout, resumes from last successful operation"]
    ]
  }
];

// Helper function to create table borders
const tableBorder = { style: BorderStyle.SINGLE, size: 1, color: "000000" };
const cellBorders = { top: tableBorder, bottom: tableBorder, left: tableBorder, right: tableBorder };

// Helper function to create header row
function createHeaderRow(headers) {
  return new TableRow({
    tableHeader: true,
    children: headers.map(text => new TableCell({
      borders: cellBorders,
      shading: { fill: "0070C0", type: ShadingType.CLEAR },
      verticalAlign: VerticalAlign.CENTER,
      children: [new Paragraph({
        alignment: AlignmentType.CENTER,
        children: [new TextRun({ text, bold: true, color: "FFFFFF", size: 20 })]
      })]
    }))
  });
}

// Helper function to create data row
function createDataRow(cells, colWidths = [4680, 4680]) {
  return new TableRow({
    children: cells.map((content, idx) => new TableCell({
      borders: cellBorders,
      width: { size: colWidths[idx], type: WidthType.DXA },
      children: Array.isArray(content) ? content : [new Paragraph({
        children: [new TextRun({ text: content, size: 20 })]
      })]
    }))
  });
}

// Generate document for each flow
function generateDocument(flow) {
  const logo = fs.readFileSync('/tmp/nadec_logo.png');

  const doc = new Document({
    numbering: {
      config: [
        { reference: "requirements-list",
          levels: [{ level: 0, format: LevelFormat.DECIMAL, text: "%1.", alignment: AlignmentType.LEFT,
            style: { paragraph: { indent: { left: 720, hanging: 360 } } } }] },
        { reference: "bullet-list",
          levels: [{ level: 0, format: LevelFormat.BULLET, text: "•", alignment: AlignmentType.LEFT,
            style: { paragraph: { indent: { left: 720, hanging: 360 } } } }] }
      ]
    },
    styles: {
      default: { document: { run: { font: "Arial", size: 22 } } },
      paragraphStyles: [
        { id: "Heading1", name: "Heading 1", basedOn: "Normal", next: "Normal", quickFormat: true,
          run: { size: 32, bold: true, color: "0070C0", font: "Arial" },
          paragraph: { spacing: { before: 240, after: 120 }, outlineLevel: 0 } },
        { id: "Heading2", name: "Heading 2", basedOn: "Normal", next: "Normal", quickFormat: true,
          run: { size: 28, bold: true, color: "0070C0", font: "Arial" },
          paragraph: { spacing: { before: 180, after: 120 }, outlineLevel: 1 } },
        { id: "Heading3", name: "Heading 3", basedOn: "Normal", next: "Normal", quickFormat: true,
          run: { size: 24, bold: true, color: "000000", font: "Arial" },
          paragraph: { spacing: { before: 120, after: 120 }, outlineLevel: 2 } }
      ]
    },
    sections: [{
      properties: {
        page: { margin: { top: 1440, right: 1440, bottom: 1440, left: 1440 } }
      },
      children: [
        // Cover Page
        new Paragraph({
          alignment: AlignmentType.CENTER,
          spacing: { before: 400 },
          children: [new ImageRun({
            type: "png",
            data: logo,
            transformation: { width: 150, height: 200 },
            altText: { title: "NADEC Logo", description: "NADEC Company Logo", name: "Logo" }
          })]
        }),
        new Paragraph({
          alignment: AlignmentType.CENTER,
          spacing: { before: 200, after: 100 },
          children: [new TextRun({ text: "SAP Cloud Platform Integration", bold: true, size: 32 })]
        }),
        new Paragraph({
          alignment: AlignmentType.CENTER,
          spacing: { after: 100 },
          children: [new TextRun({ text: "Technical Specification", bold: true, size: 28 })]
        }),
        new Paragraph({
          alignment: AlignmentType.CENTER,
          spacing: { after: 400 },
          children: [new TextRun({ text: "Technical Specifications Document", size: 24 })]
        }),

        // Document Release Note
        new Paragraph({ children: [new PageBreak()] }),
        new Paragraph({
          spacing: { before: 200, after: 100 },
          children: [new TextRun({ text: "Document Release Note", bold: true, size: 28 })]
        }),
        new Table({
          columnWidths: [2800, 6560],
          margins: { top: 100, bottom: 100, left: 100, right: 100 },
          rows: [
            createDataRow(["Document Name:", flow.name], [2800, 6560]),
            createDataRow(["Version:", "1.0.0"], [2800, 6560]),
            createDataRow(["Description:", flow.overview], [2800, 6560]),
            createDataRow(["Release Date:", new Date().toISOString().split('T')[0]], [2800, 6560])
          ]
        }),

        // Revision History
        new Paragraph({
          spacing: { before: 300, after: 100 },
          children: [new TextRun({ text: "Revision History", bold: true, size: 28 })]
        }),
        new Table({
          columnWidths: [1400, 1400, 2600, 1400, 1560, 1000],
          margins: { top: 100, bottom: 100, left: 100, right: 100 },
          rows: [
            createHeaderRow(["Revision", "Date", "Description", "Page", "Rationale", "Type"]),
            createDataRow(["1.0", new Date().toISOString().split('T')[0], "Initial Draft", "All", "Initial Version", "Add"],
              [1400, 1400, 2600, 1400, 1560, 1000])
          ]
        }),

        // Contact Information
        new Paragraph({
          spacing: { before: 300, after: 100 },
          children: [new TextRun({ text: "Document Contact Information", bold: true, size: 28 })]
        }),
        new Table({
          columnWidths: [2800, 6560],
          margins: { top: 100, bottom: 100, left: 100, right: 100 },
          rows: [
            createDataRow(["Name:", "Abdelrahman Hussein"], [2800, 6560]),
            createDataRow(["Role:", "Technical Consultant"], [2800, 6560])
          ]
        }),

        // Table of Contents
        new Paragraph({ children: [new PageBreak()] }),
        new Paragraph({
          spacing: { before: 200, after: 200 },
          children: [new TextRun({ text: "Table of Contents", bold: true, size: 28 })]
        }),
        new TableOfContents("Contents", { hyperlink: true, headingStyleRange: "1-3" }),

        // Section 1: BUSINESS CONTEXT
        new Paragraph({ children: [new PageBreak()] }),
        new Paragraph({
          heading: HeadingLevel.HEADING_1,
          spacing: { before: 200 },
          children: [new TextRun("1. BUSINESS CONTEXT")]
        }),

        new Paragraph({
          heading: HeadingLevel.HEADING_2,
          children: [new TextRun("1.1 Overview")]
        }),
        new Paragraph({
          spacing: { after: 200 },
          children: [new TextRun({ text: flow.overview, size: 22 })]
        }),

        new Paragraph({
          heading: HeadingLevel.HEADING_2,
          children: [new TextRun("1.2 Development Unit Information")]
        }),
        new Table({
          columnWidths: [2800, 6560],
          margins: { top: 100, bottom: 100, left: 100, right: 100 },
          rows: [
            createDataRow(["Module", flow.module], [2800, 6560]),
            createDataRow(["Sub Module", flow.subModule], [2800, 6560]),
            createDataRow(["iFlow Title", flow.name], [2800, 6560]),
            createDataRow(["Processing Type", flow.processingType], [2800, 6560]),
            createDataRow(["Execution Frequency", flow.frequency], [2800, 6560])
          ]
        }),

        // Section 2: DETAILED DESIGN
        new Paragraph({ children: [new PageBreak()] }),
        new Paragraph({
          heading: HeadingLevel.HEADING_1,
          spacing: { before: 200 },
          children: [new TextRun("2. DETAILED DESIGN")]
        }),

        new Paragraph({
          heading: HeadingLevel.HEADING_2,
          children: [new TextRun("2.1 Configuration Details")]
        }),
        new Paragraph({
          spacing: { after: 100 },
          children: [new TextRun({ text: "Package Name: ", bold: true, size: 22 }),
                     new TextRun({ text: flow.package, size: 22 })]
        }),
        new Paragraph({
          spacing: { after: 100 },
          children: [new TextRun({ text: "iFlow Name: ", bold: true, size: 22 }),
                     new TextRun({ text: flow.name, size: 22 })]
        }),
        new Paragraph({
          spacing: { after: 100 },
          children: [new TextRun({ text: "Technical Name: ", bold: true, size: 22 }),
                     new TextRun({ text: flow.technical, size: 22 })]
        }),
        new Paragraph({
          spacing: { after: 200 },
          children: [new TextRun({ text: "Endpoint: ", bold: true, size: 22 }),
                     new TextRun({ text: flow.endpoint, size: 22 })]
        }),

        new Paragraph({
          heading: HeadingLevel.HEADING_2,
          children: [new TextRun("2.2 SAP CPI iFlow Design")]
        }),
        new Paragraph({
          spacing: { after: 100 },
          children: [new TextRun({
            text: "This is a custom-designed SAP CPI Integration flow for work assignment and timesheet conflict resolution.",
            size: 22
          })]
        }),

        new Paragraph({
          heading: HeadingLevel.HEADING_3,
          children: [new TextRun("Detailed Requirements:")]
        }),
        ...flow.requirements.map((req, idx) => new Paragraph({
          numbering: { reference: "requirements-list", level: 0 },
          children: [new TextRun({ text: req, size: 22 })]
        })),

        new Paragraph({
          heading: HeadingLevel.HEADING_3,
          spacing: { before: 300 },
          children: [new TextRun("Groovy Scripts")]
        }),
        new Table({
          columnWidths: [3000, 6360],
          margins: { top: 100, bottom: 100, left: 100, right: 100 },
          rows: [
            createHeaderRow(["Script Name", "Description"]),
            ...flow.scripts.map(([name, desc]) => createDataRow([name, desc], [3000, 6360]))
          ]
        }),

        new Paragraph({
          heading: HeadingLevel.HEADING_2,
          spacing: { before: 300 },
          children: [new TextRun("2.3 Adapter Configuration (Sender & Receiver)")]
        }),
        new Paragraph({
          spacing: { after: 200 },
          children: [new TextRun({ text: flow.adapter, size: 22 })]
        }),

        new Paragraph({
          heading: HeadingLevel.HEADING_2,
          children: [new TextRun("2.4 Error Handling")]
        }),
        new Paragraph({
          spacing: { after: 200 },
          children: [new TextRun({
            text: "Standard CPI error handling applies. Errors are logged to message processing log. Failed messages are stored in error queue for manual intervention. Retry mechanism is configured for transient failures (3 attempts with 5-second delay).",
            size: 22
          })]
        }),

        // Section 3: TESTING
        new Paragraph({ children: [new PageBreak()] }),
        new Paragraph({
          heading: HeadingLevel.HEADING_1,
          spacing: { before: 200 },
          children: [new TextRun("3. TESTING")]
        }),

        new Paragraph({
          heading: HeadingLevel.HEADING_2,
          children: [new TextRun("3.1 Test Conditions and Expected Results")]
        }),
        new Table({
          columnWidths: [4680, 4680],
          margins: { top: 100, bottom: 100, left: 100, right: 100 },
          rows: [
            createHeaderRow(["Test Condition", "Expected Result"]),
            ...flow.testConditions.map(([condition, result]) =>
              createDataRow([condition, result], [4680, 4680]))
          ]
        }),

        new Paragraph({
          heading: HeadingLevel.HEADING_2,
          spacing: { before: 300 },
          children: [new TextRun("3.2 Test Data Considerations")]
        }),
        new Paragraph({
          spacing: { after: 200 },
          children: [new TextRun({
            text: "Test data should include: (1) Typical scenarios with standard work assignments and timesheets, (2) Edge cases with time zone boundaries, (3) Error scenarios with malformed data, (4) Load testing with bulk data volumes.",
            size: 22
          })]
        }),

        new Paragraph({
          heading: HeadingLevel.HEADING_2,
          children: [new TextRun("3.3 Performance Considerations")]
        }),
        new Paragraph({
          spacing: { after: 200 },
          children: [new TextRun({
            text: "Expected processing time: <5 seconds for single record, <60 seconds for batch of 100 records. SF API rate limits: 5000 calls/hour. Memory usage: <500MB for typical batch operations.",
            size: 22
          })]
        }),

        // Section 4: APPENDIX
        new Paragraph({ children: [new PageBreak()] }),
        new Paragraph({
          heading: HeadingLevel.HEADING_1,
          spacing: { before: 200 },
          children: [new TextRun("4. APPENDIX")]
        }),
        new Paragraph({
          spacing: { after: 200 },
          children: [new TextRun({
            text: "Additional technical documentation, API specifications, and code samples are available in the project repository: https://github.com/eco-nadec/SF-WA-CPI",
            size: 22
          })]
        })
      ]
    }]
  });

  return doc;
}

// Generate all documents
async function generateAll() {
  const outputDir = "/Users/abdelrahmanelamin/ECO-Projects/NADEC/CPI/SFNadecWorkAssignment_artifacts/docs/iflow-specs";

  // Create output directory if it doesn't exist
  if (!fs.existsSync(outputDir)) {
    fs.mkdirSync(outputDir, { recursive: true });
  }

  for (const flow of flows) {
    console.log(`Generating documentation for: ${flow.name}`);
    const doc = generateDocument(flow);
    const buffer = await Packer.toBuffer(doc);
    const filename = `${outputDir}/${flow.technical}.docx`;
    fs.writeFileSync(filename, buffer);
    console.log(`✓ Created: ${filename}`);
  }

  console.log(`\n✅ Generated ${flows.length} documentation files in: ${outputDir}`);
}

generateAll().catch(console.error);
