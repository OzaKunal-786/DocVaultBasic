package com.docvaultbasic.ui.navigation

sealed class Screen(val route: String) {
    object Onboarding : Screen("onboarding")
    object PinSetup : Screen("pin_setup")
    object Lock : Screen("lock")
    object Home : Screen("home")
    object Search : Screen("search")
    object FolderSelection : Screen("folder_selection")
    object Settings : Screen("settings")
    object DocumentScanner : Screen("document_scanner")
    object AllFiles : Screen("all_files")
    object Faq : Screen("faq") // Added FAQ screen
    object ImportConfirmation : Screen("import_confirmation/{fileUri}") {
        fun createRoute(fileUri: String) = "import_confirmation/${java.net.URLEncoder.encode(fileUri, "UTF-8")}"
    }
    object ImageEditor : Screen("image_editor/{imageUri}") {
        fun createRoute(imageUri: String) = "image_editor/${java.net.URLEncoder.encode(imageUri, "UTF-8")}"
    }
    object PdfViewer : Screen("pdf_viewer/{documentId}") {
        fun createRoute(documentId: Int) = "pdf_viewer/$documentId"
    }
}
