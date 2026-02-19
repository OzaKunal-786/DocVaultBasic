# 🛡️ DocVaultBasic - Secure Local Document Vault

**DocVault** is a premium, privacy-focused Android application designed to protect and manage your sensitive documents. Built with a local-first philosophy, every file you add stays entirely on your device, secured by military-grade encryption and biometric authentication.

---

## 📱 Features

### 🔐 Advanced Security
-   **PIN & Biometric Unlock**: Protect your vault with a 4-6 digit PIN or your fingerprint/face using Android's standard `BiometricPrompt`.
-   **Encrypted Database**: Uses **SQLCipher** to encrypt the entire Room database with a key derived from your secure PIN.
-   **Local Storage**: No cloud uploads. No internet required. Your documents never leave your phone.

### 📁 Smart Document Management
-   **Universal Import**: Easily add documents from your **Gallery** or **File Explorer**.
-   **Recursive Folder Scanning**: Monitor and automatically import PDF files from system folders like **Downloads** or **WhatsApp Documents**.
-   **Search & Organize**: Find documents instantly by name or folder with a high-performance search engine.

### 📄 Professional Document Editing
-   **Native Document Scanner**: Capture sharp, high-quality documents using a custom **CameraX** module.
-   **Refinement Tools**: Built-in editor for **Brightness**, **Contrast**, and **Rotation** to ensure maximum readability.
-   **Compressed PDF Engine**: Automatically converts captures into **PDF format** with **JPEG 80% compression**, saving storage space while keeping documents crisp.

### ⚙️ Premium Settings & Control
-   **Vault Customization**: Toggle biometrics, change your PIN, and manage scan folders easily.
-   **Backup & Restore**: Create secure, **LZMA-compressed** and **AES-256 encrypted** backups of your entire vault.
-   **Cache Management**: One-tap clearing of temporary thumbnails to keep your device clean.

---

## 🛠️ Technical Stack

-   **Language**: Kotlin (with Coroutines for async processing)
-   **UI Framework**: Jetpack Compose (Material 3 Expressive guidelines)
-   **Database**: Room with SQLCipher integration
-   **Camera**: CameraX for local image capture
-   **DI**: Hilt (Dagger)
-   **Navigation**: Jetpack Compose Navigation
-   **Permissions**: Accompanist Permissions

---

## 🚀 Getting Started

1.  **Clone the Repository**: `git clone https://github.com/OzaKunal-786/DocVaultBasic`
2.  **Open in Android Studio**: Open the root folder and let Gradle sync.
3.  **Permissions**: Grant **"All Files Access"** on the first launch to enable deep scanning.
4.  **Security**: Set up your secure PIN and enable biometrics in Settings.

---

## 🗺️ Project Documentation

For a detailed breakdown of the project structure and file paths, refer to the [File Manifest](./DocVault_Project_Documentation/File_Manifest.md).

*Maintained by: OzaKunal-786*
