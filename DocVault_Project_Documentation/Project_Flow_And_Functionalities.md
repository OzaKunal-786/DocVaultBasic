# DocVault Project Overview: Flow and Functionalities

This document provides a comprehensive overview of the DocVault application's architecture, user flow, and functional components.

## 1. Application Flow

### 1.1 First Launch (Onboarding & Setup)
1.  **Onboarding Screens**: User is introduced to the app's core values: security, local-only storage, and privacy.
    - **Files**: `OnboardingScreen.kt`
2.  **PIN Setup**: User creates a 4-6 digit PIN to protect their documents.
    - **Files**: `PinSetupScreen.kt`, `PinManager.kt`
3.  **Folder Selection**: User chooses initial folders (like Downloads or WhatsApp) to scan for PDF files.
    - **Files**: `FolderSelectionScreen.kt`, `FolderViewModel.kt`

### 1.2 Subsequent Launches (Authentication)
1.  **Lock Screen**: Every time the app is opened after setup, the user must authenticate using their PIN or Biometrics (if enabled).
    - **Files**: `LockScreen.kt`, `PinManager.kt`, `BiometricHelper.kt`

### 1.3 Core Usage
1.  **Home Screen (Dashboard)**: Displays quick statistics (total documents, storage used), recently accessed files, and a list of all documents.
    - **Files**: `HomeScreen.kt`, `HomeViewModel.kt`
2.  **Scanning**: The app automatically monitors selected folders and imports new PDFs into its secure database.
    - **Files**: `DocumentRepository.kt`, `FolderRepository.kt`
3.  **Searching**: Users can find documents quickly using the search bar, filtering by filename or folder.
    - **Files**: `SearchScreen.kt`, `SearchViewModel.kt`
4.  **Viewing**: Documents are decrypted on-the-fly and displayed in an internal PDF viewer.
    - **Files**: `PdfViewerScreen.kt`, `PdfViewerViewModel.kt`

## 2. Key Functionalities & Architecture

### 2.1 Security & Encryption
- **Database Encryption**: Uses SQLCipher to encrypt the entire Room database with a key derived from the user's PIN.
- **File Security**: (Placeholder) In a production environment, files are encrypted using AES-256 before being stored in internal storage.
- **Biometrics**: Integration with Android's BiometricPrompt for seamless, secure login.
- **KeyStore**: Securely stores sensitive cryptographic materials.
- **Files**: `KeyStoreHelper.kt`, `PinManager.kt`, `BiometricHelper.kt`, `AppDatabase.kt`

### 2.2 Data Layer (Room Persistence)
- **Entities**: Represents Documents and Folders in the database.
- **DAOs**: Data Access Objects for querying and modifying document and folder metadata.
- **Repositories**: Abstracts data sources for the UI layer.
- **Files**: `DocumentEntity.kt`, `DocumentDao.kt`, `FolderEntity.kt`, `FolderDao.kt`, `AppDatabase.kt`, `DocumentRepository.kt`, `FolderRepository.kt`

### 2.3 Navigation
- Uses Jetpack Compose Navigation with a centralized `AppNavGraph`.
- Routes are defined in a sealed class for type safety.
- **Files**: `AppNavGraph.kt`, `Screen.kt`, `MainActivity.kt`

### 2.4 Dependency Injection
- Hilt is used for providing singletons and injecting dependencies into ViewModels.
- **Files**: `DocVaultApp.kt`, `AppModule.kt`, `MainActivity.kt`

## 3. Project Configuration
- **Permissions**: Defined in `AndroidManifest.xml` for storage and biometric access.
- **Dependencies**: Managed in `build.gradle.kts`, including Compose, Room, SQLCipher, and Hilt.
- **Files**: `AndroidManifest.xml`, `app/build.gradle.kts`
