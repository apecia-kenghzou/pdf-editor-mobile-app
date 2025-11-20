# PDF Editor Mobile Apps

Complete PDF editing applications for both Android and iOS platforms. These apps allow users to edit PDFs offline with features like text overlay, signature drawing, page removal, and more.

## Features

Both Android and iOS apps include the following features:

- **PDF Selection**: Select PDF files from your device
- **Text Editing**:
  - Add text overlays to PDF pages
  - Edit existing text
  - Move text around by dragging
  - Adjust font size
  - Choose different font types
- **Signature Management**:
  - Draw signatures using touch input
  - Save signatures for future use
  - Adjust signature size
  - Reuse previously saved signatures
- **Page Management**:
  - Remove unwanted pages from PDF
  - Navigate between pages
- **Save Functionality**:
  - Save edited PDF as a new file
  - Preserve original PDF
- **Offline Support**: All features work completely offline

---

## Android App

### Technology Stack

- **Language**: Java
- **PDF Library**: PDFBox Android 2.0.27.0
- **Minimum SDK**: Android 7.0 (API 24)
- **Target SDK**: Android 14 (API 34)

### Project Structure

```
android/
├── app/
│   ├── src/main/
│   │   ├── java/com/pdfeditor/
│   │   │   ├── MainActivity.java
│   │   │   ├── PdfEditorActivity.java
│   │   │   ├── SignatureActivity.java
│   │   │   ├── PdfEditorView.java
│   │   │   ├── SignatureView.java
│   │   │   ├── TextElement.java
│   │   │   ├── SignatureElement.java
│   │   │   ├── SignatureManager.java
│   │   │   └── SavedSignaturesAdapter.java
│   │   ├── res/
│   │   │   ├── layout/
│   │   │   ├── values/
│   │   │   └── xml/
│   │   └── AndroidManifest.xml
│   └── build.gradle
├── build.gradle
└── settings.gradle
```

### Setup Instructions

1. **Prerequisites**:
   - Android Studio Arctic Fox or later
   - JDK 8 or higher
   - Android SDK with API 24+

2. **Build the App**:
   ```bash
   cd android
   ./gradlew build
   ```

3. **Run on Emulator/Device**:
   - Open the `android` folder in Android Studio
   - Click "Run" or press Shift + F10
   - Select your target device/emulator

4. **Generate APK**:
   ```bash
   cd android
   ./gradlew assembleRelease
   ```
   The APK will be available at: `android/app/build/outputs/apk/release/`

### Permissions

The app requires the following permissions:
- `READ_EXTERNAL_STORAGE` - To read PDF files
- `WRITE_EXTERNAL_STORAGE` - To save edited PDFs (Android 12 and below)
- `READ_MEDIA_IMAGES` - For Android 13+

### Usage

1. **Launch the app** and tap "Select PDF to Edit"
2. **Choose a PDF** from your device
3. **Edit the PDF**:
   - **Add Text**: Tap "Add Text" button, enter text, choose font size and type
   - **Edit Text**: Tap on existing text to select it, then tap "Add Text" again to edit
   - **Move Elements**: Drag text or signatures to reposition
   - **Add Signature**: Tap "Signature" → Draw your signature → Tap "Use" or "Save Signature"
   - **Remove Page**: Tap "Remove Page" to mark current page for removal
4. **Navigate**: Use "Previous" and "Next" buttons to move between pages
5. **Save**: Tap "Save" to create a new PDF with all edits applied

---

## iOS App

### Technology Stack

- **Language**: Swift
- **PDF Framework**: PDFKit (Native iOS framework)
- **Minimum iOS**: iOS 14.0
- **UI Framework**: UIKit

### Project Structure

```
iOS/
└── PDFEditor/
    ├── PDFEditor/
    │   ├── Controllers/
    │   │   ├── MainViewController.swift
    │   │   ├── PDFEditorViewController.swift
    │   │   └── SignatureViewController.swift
    │   ├── Views/
    │   │   ├── EditablePDFView.swift
    │   │   └── SignatureDrawingView.swift
    │   ├── Models/
    │   │   ├── TextElement.swift
    │   │   ├── SignatureElement.swift
    │   │   └── SignatureManager.swift
    │   ├── AppDelegate.swift
    │   └── Info.plist
    └── PDFEditor.xcodeproj/
```

### Setup Instructions

1. **Prerequisites**:
   - macOS with Xcode 14.0 or later
   - iOS 14.0+ device or simulator

2. **Open the Project**:
   ```bash
   cd iOS/PDFEditor
   open PDFEditor.xcodeproj
   ```

3. **Build and Run**:
   - Select your target device/simulator in Xcode
   - Press Cmd + R to build and run
   - Or click the "Play" button in Xcode

4. **Build for Device**:
   - Connect your iOS device
   - Select your device as the target
   - You may need to configure code signing in Xcode:
     - Go to Project Settings → Signing & Capabilities
     - Select your development team

### Capabilities

The app uses:
- Document Browser support for PDF selection
- File system access for saving signatures
- Touch input for signature drawing

### Usage

1. **Launch the app** and tap "Select PDF to Edit"
2. **Choose a PDF** from Files app or iCloud
3. **Edit the PDF**:
   - **Add Text**: Tap "Add Text" → Enter text and font size → Tap "OK"
   - **Edit Text**: Double-tap on existing text to edit
   - **Move Elements**: Drag text or signatures to reposition
   - **Add Signature**: Tap "Signature" → Draw your signature → Tap "Use" or "Save Signature"
   - **Remove Page**: Tap "Remove Page" to mark current page for removal
4. **Navigate**: Use "Previous" and "Next" buttons to move between pages
5. **Save**: Tap "Save" to create a new PDF with all edits applied

---

## Key Differences Between Platforms

| Feature | Android | iOS |
|---------|---------|-----|
| PDF Library | PDFBox Android | PDFKit (Native) |
| Language | Java | Swift |
| UI Framework | XML Layouts + Java | UIKit (Programmatic) |
| File Picker | Android Document Picker | iOS Document Picker |
| Storage | External Storage | App Documents Directory |
| Signature Storage | Internal Files Directory | App Documents Directory |

---

## Architecture

### Android Architecture

- **MainActivity**: Entry point, handles PDF selection
- **PdfEditorActivity**: Main editing screen with toolbar and PDF viewer
- **PdfEditorView**: Custom view that renders PDF and handles touch interactions
- **SignatureActivity**: Signature drawing interface
- **SignatureView**: Custom view for capturing signature drawings
- **Model Classes**: TextElement, SignatureElement for data representation
- **SignatureManager**: Manages signature persistence

### iOS Architecture

- **AppDelegate**: App entry point and lifecycle management
- **MainViewController**: Home screen with PDF selection
- **PDFEditorViewController**: Main editing screen with all editing features
- **EditablePDFView**: Custom PDFView subclass with overlay editing
- **SignatureViewController**: Signature drawing and management
- **SignatureDrawingView**: Custom view for signature capture
- **Model Classes**: TextElement, SignatureElement for data structures
- **SignatureManager**: Singleton for signature persistence

---

## Building for Production

### Android

1. Generate a signing key:
   ```bash
   keytool -genkey -v -keystore release-key.jks -keyalg RSA -keysize 2048 -validity 10000 -alias pdf-editor
   ```

2. Update `android/app/build.gradle` with signing configuration

3. Build release APK:
   ```bash
   cd android
   ./gradlew assembleRelease
   ```

### iOS

1. Configure code signing in Xcode
2. Archive the app (Product → Archive)
3. Distribute via TestFlight or App Store

---

## Troubleshooting

### Android

**Issue**: PDF not rendering
- Ensure PDFBox is properly initialized in MainActivity
- Check that the PDF file is not corrupted

**Issue**: Permission denied
- Request runtime permissions for storage access
- For Android 13+, ensure READ_MEDIA_IMAGES permission is granted

### iOS

**Issue**: Cannot access PDF file
- Ensure the URL has security-scoped resource access
- Check that the file URL is valid

**Issue**: Signature not saving
- Verify app has write access to Documents directory
- Check that the SignatureManager directory is created

---

## Future Enhancements

Potential improvements for both platforms:
- PDF annotation tools (highlight, underline, strikethrough)
- Image insertion
- Form field editing
- Password-protected PDF support
- Cloud storage integration
- PDF merging and splitting
- OCR text recognition
- Dark mode support
- Undo/Redo functionality
- Multi-page selection for deletion

---

## License

This project is provided as-is for educational and commercial use.

---

## Support

For issues, questions, or feature requests, please create an issue in the repository.

---

## Credits

- **Android PDF Library**: [PDFBox Android](https://github.com/TomRoush/PdfBox-Android)
- **iOS PDF Framework**: Apple PDFKit

---

## File Locations

### Android
- **Saved PDFs**: `/Android/data/com.pdfeditor/files/`
- **Saved Signatures**: `/data/data/com.pdfeditor/files/signatures/`

### iOS
- **Saved PDFs**: `Documents/` directory (visible in Files app)
- **Saved Signatures**: `Documents/Signatures/` directory
