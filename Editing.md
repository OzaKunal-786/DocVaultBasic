🎯 WHAT WE'RE BUILDING
A screen where users can:

Select an image (from gallery or camera)
Auto-detect document edges (find the paper automatically)
Crop & straighten the document (fix angles)
Apply filters (Black & White, Color, Grayscale, Magic Color)
Adjust brightness/contrast (make text clearer)
Rotate if needed
Save as professional PDF (looks like a real scan)


📋 COMPLETE DEVELOPMENT PLAN
WEEK 3: Document Scanner Editor (5 Days)
DayFeatureWhat You'll BuildDay 1Setup + DependenciesAdd image processing librariesDay 2Auto Crop + Corner DetectionDetect document edges automaticallyDay 3Manual Crop + Perspective FixLet user adjust corners, straighten imageDay 4Filters + AdjustmentsB&W, Grayscale, Color, BrightnessDay 5Save as PDF + IntegrationGenerate PDF, save to database

🛠️ DETAILED STEP-BY-STEP BREAKDOWN

DAY 1: SETUP + DEPENDENCIES
What You'll Do:
Add libraries for image processing (like Photoshop tools for Android)
Files to Edit:

app/build.gradle.kts
Create new folder structure


STEP 1: Add Dependencies
File: app/build.gradle.kts
Add these lines in the dependencies section:
kotlin// OpenCV for image processing (crop, filters, perspective)
implementation("com.quickbirdstudios:opencv:4.5.3.0")

// Image loading and manipulation
implementation("io.coil-kt:coil-compose:2.5.0")

// PDF generation (you already have this)
implementation("com.itextpdf:itext7-core:7.2.5")
```

**What each library does:**
- **OpenCV** = Professional image processing (used by Adobe, Google)
- **Coil** = Load and display images smoothly
- **iTextPDF** = Create PDF files

**Then:** Click **"Sync Now"** button (blue bar at top)

---

### **STEP 2: Create New Files/Folders**

**File Structure:**
```
app/src/main/java/com/docvault/
├── scanner/
│   ├── DocumentScanner.kt          ← Auto edge detection
│   ├── ImageProcessor.kt           ← Filters, brightness, etc.
│   └── PerspectiveTransform.kt     ← Straighten tilted images
├── ui/screens/
│   └── ScanEditorScreen.kt         ← Main editing screen
├── ui/components/
│   ├── CropOverlay.kt              ← Draggable corner points
│   └── FilterButton.kt             ← Filter selection buttons

DAY 2: AUTO CROP + CORNER DETECTION
What You'll Build:
Automatically find the document edges (like when CamScanner highlights the paper)

FILE 1: DocumentScanner.kt
Location: app/src/main/java/com/docvault/scanner/DocumentScanner.kt
What it does:

Finds the 4 corners of a document in a photo
Uses edge detection (finds lines and shapes)
Returns corner coordinates

Key Functions:
kotlinclass DocumentScanner {
    // Find document corners automatically
    fun detectDocumentCorners(bitmap: Bitmap): List<Point>?
    
    // Check if detected area is valid (not too small)
    fun isValidDocument(corners: List<Point>): Boolean
}
How it works (simple explanation):

Convert image to black & white
Find all edges (where color changes sharply)
Find 4-sided shapes (rectangles)
Pick the biggest rectangle (that's probably the paper)
Return the 4 corner points


FILE 2: Edge Detection Logic
Algorithm:

Grayscale → Make image black/white (easier to find edges)
Blur → Remove noise (tiny dots that confuse detection)
Canny Edge Detection → Find all edges
Find Contours → Connect edges into shapes
Filter for 4-sided shapes → Only keep rectangles
Pick largest → That's your document!


DAY 3: MANUAL CROP + PERSPECTIVE FIX
What You'll Build:

Draggable corner points (user can adjust if auto-detect is wrong)
Straighten tilted documents (perspective correction)


FILE 1: CropOverlay.kt
Location: app/src/main/java/com/docvault/ui/components/CropOverlay.kt
What it does:
Shows 4 draggable circles at corners + lines connecting them
UI Elements:

4 circles (one at each corner)
4 lines (connecting the corners)
Touch handling (drag to move corners)

kotlin@Composable
fun CropOverlay(
    corners: List<Offset>,           // Current corner positions
    onCornerDragged: (Int, Offset) -> Unit,  // When user drags
    imageSize: Size                  // Image dimensions
) {
    // Draw lines between corners
    // Draw draggable circles at corners
    // Handle touch events
}

FILE 2: PerspectiveTransform.kt
Location: app/src/main/java/com/docvault/scanner/PerspectiveTransform.kt
What it does:
Takes a tilted photo and makes it look straight (like looking at paper from above)
Key Function:
kotlinclass PerspectiveTransform {
    // Transform tilted image to straight rectangle
    fun correctPerspective(
        bitmap: Bitmap,              // Original tilted image
        corners: List<Point>         // 4 corner points
    ): Bitmap                        // Straightened image
}
```

**How it works:**
- Take 4 corners from tilted image
- Map them to a perfect rectangle
- Stretch/squeeze image to fit
- Result: Straight document!

**Example:**
```
BEFORE:                    AFTER:
   /-------\              ┌---------┐
  /         \             │         │
 /           \            │         │
/             \           │         │
\             /           │         │
 \           /            │         │
  \         /             │         │
   \-------/              └---------┘

DAY 4: FILTERS + ADJUSTMENTS
What You'll Build:
Filter buttons like CamScanner (B&W, Color, etc.)

FILE 1: ImageProcessor.kt
Location: app/src/main/java/com/docvault/scanner/ImageProcessor.kt
Filters to implement:
1. Black & White (Document Mode)
kotlinfun applyBlackAndWhite(bitmap: Bitmap): Bitmap

Converts to grayscale
Increases contrast
Makes text super dark, background super white
Best for: Text documents, receipts

2. Grayscale
kotlinfun applyGrayscale(bitmap: Bitmap): Bitmap

Simple black/white/gray
Best for: Photos with no color needed

3. Color Enhanced
kotlinfun applyColorEnhance(bitmap: Bitmap): Bitmap

Boost saturation (make colors brighter)
Increase contrast
Best for: Colorful documents (charts, diagrams)

4. Magic Color
kotlinfun applyMagicColor(bitmap: Bitmap): Bitmap

Auto white balance (fix yellow lighting)
Remove shadows
Enhance text
Best for: Photos taken in bad lighting

5. Brightness/Contrast
kotlinfun adjustBrightness(bitmap: Bitmap, value: Float): Bitmap
fun adjustContrast(bitmap: Bitmap, value: Float): Bitmap
```
- Brightness: -100 to +100
- Contrast: -100 to +100

---

### **FILE 2: FilterButton.kt**

**Location:** `app/src/main/java/com/docvault/ui/components/FilterButton.kt`

**What it shows:**
```
┌─────────┐  ┌─────────┐  ┌─────────┐  ┌─────────┐
│ ORIGINAL│  │  B & W  │  │ COLOR   │  │ MAGIC   │
│ [preview]│  │[preview]│  │[preview]│  │[preview]│
└─────────┘  └─────────┘  └─────────┘  └─────────┘
Each button shows a small preview of what the filter looks like.

DAY 5: SAVE AS PDF + INTEGRATION
What You'll Build:
Save the edited image as a professional PDF

FILE 1: Enhanced PdfConverter.kt
Add new function to existing file:
kotlin// Location: app/src/main/java/com/docvault/pdf/PdfConverter.kt

fun createScanPdf(
    editedImage: Bitmap,          // The processed image
    outputFile: File,             // Where to save PDF
    title: String,                // Document title
    compressionQuality: Int = 85  // 0-100 (higher = better quality)
): Boolean {
    // 1. Compress image (reduce file size)
    // 2. Create PDF with proper page size
    // 3. Add metadata (title, date)
    // 4. Return success/failure
}
```

---

### **FILE 2: ScanEditorScreen.kt**

**Location:** `app/src/main/java/com/docvault/ui/screens/ScanEditorScreen.kt`

**UI Layout:**
```
┌─────────────────────────────┐
│  ← DocVault    [Save] [✓]   │ ← Top bar
├─────────────────────────────┤
│                             │
│     [DOCUMENT IMAGE]        │ ← Main image area
│      with 4 corners         │   (zoomable, pannable)
│                             │
├─────────────────────────────┤
│ [Crop] [Rotate] [Filter]    │ ← Action buttons
├─────────────────────────────┤
│ ┌───┐ ┌───┐ ┌───┐ ┌───┐   │
│ │ B&W│ │GRY│ │CLR│ │MGC│   │ ← Filter previews
│ └───┘ └───┘ └───┘ └───┘   │
├─────────────────────────────┤
│ Brightness: [─────●─────]   │ ← Sliders
│ Contrast:   [─────●─────]   │
└─────────────────────────────┘

🎨 USER FLOW (How It Works)
Scenario: User scans a receipt

User taps "+" button → Opens camera or gallery
User selects photo → App opens ScanEditorScreen
Auto-detect → App automatically finds receipt edges

Shows 4 green circles at corners


User adjusts (if needed):

Drags corners to perfect position


User picks filter:

Taps "B&W" → Receipt becomes crisp black text on white


User adjusts brightness → Makes faint text darker
User taps "Save" → App:

Crops image
Straightens it
Applies filter
Converts to PDF
Saves in database
Shows success message




📦 WHAT GETS SAVED
In Database:
kotlinDocumentEntity(
    id = "doc_001",
    imagePath = "/storage/receipts/receipt_001.jpg",    // Original
    pdfPath = "/storage/receipts/receipt_001.pdf",      // Processed PDF
    thumbnailPath = "/storage/thumbs/receipt_001.jpg",  // Small preview
    category = DocumentCategory.RECEIPTS,
    ocrText = "Walmart ... $45.99 ...",                // Extracted text
    dateScanned = "2026-02-18T10:30:00Z"
)
```

---

## **🔧 TECHNICAL DETAILS**

### **Image Processing Pipeline:**
```
Original Photo (3000x4000px, 5MB)
         ↓
Auto-detect corners
         ↓
Crop to document area
         ↓
Perspective correction (straighten)
         ↓
Apply selected filter (B&W, Color, etc.)
         ↓
Adjust brightness/contrast
         ↓
Compress (JPEG quality 85%)
         ↓
Convert to PDF (A4 size, 300 DPI)
         ↓
Save both:
  - Original JPG (for re-editing)
  - Final PDF (for viewing/sharing)
         ↓
Generate thumbnail (256x256px)
         ↓
Extract text with OCR
         ↓
Save to encrypted database

⚡ PERFORMANCE OPTIMIZATIONS

Process in background thread (don't freeze UI)
Show preview while processing (user sees progress)
Compress images (300 DPI is enough for reading)
Cache processed images (don't re-process if user goes back)
Limit resolution (max 2000px width is enough for PDFs)


🎯 FEATURES COMPARISON
FeatureCamScannerYour AppComplexityAuto edge detection✅✅MediumManual corner adjust✅✅EasyPerspective correction✅✅MediumB&W filter✅✅EasyColor filter✅✅EasyMagic Color✅✅HardBrightness/Contrast✅✅EasyRotation✅✅EasyMulti-page PDF✅❌ (Week 6)MediumBatch scanning✅❌ (Week 6)EasyCloud sync✅❌ (Never)-

📅 REALISTIC TIMELINE

Week 1: ✅ Security (DONE)
Week 2: Database + Navigation (NEXT)
Week 3: Scanner Editor (THIS PLAN)
Week 4: OCR + AI categorization
Week 5: Search + File management
Week 6: Multi-page + Batch scan
Week 7: Settings + Backup
Week 8: Polish + Testing