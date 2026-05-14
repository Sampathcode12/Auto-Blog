# AutoBlog (iOS)

SwiftUI companion to the Android AutoBlog app. **You must build on a Mac with Xcode** (Apple does not ship Xcode for Windows).

## Quick start (XcodeGen — recommended)

1. On your Mac, install [Xcode](https://developer.apple.com/xcode/) from the App Store and [XcodeGen](https://github.com/yonaskolb/XcodeGen):  
   `brew install xcodegen`
2. In Terminal:
   ```bash
   cd /path/to/AutoBlog/ios
   xcodegen generate
   open AutoBlog.xcodeproj
   ```
3. Select an iPhone simulator or your device, press **Run** (⌘R).

## Quick start (manual Xcode project)

1. Open Xcode → **File → New → Project → App**.
2. Product Name: **AutoBlog**, Interface: **SwiftUI**, Language: **Swift**, minimum **iOS 17**.
3. Save the project **inside** this `ios` folder (or move the generated app target next to the `AutoBlog` source folder).
4. Delete the default `ContentView.swift` if Xcode created one as the main entry (this project uses `AutoBlogApp.swift` as `@main`).
5. Drag the **`AutoBlog`** folder** (all Swift files + `Info.plist`) into the project navigator and choose **Copy items if needed**, target **AutoBlog**.
6. In the target **Build Settings**, set **Info.plist File** to `AutoBlog/Info.plist` (path relative to the project).
7. **Signing & Capabilities**: pick your **Team** to run on a physical iPhone.

## Login (same as Android)

- Email: `admin@gmail.com`  
- Password: `1234`

## Notes

- Data is **in memory only** (like the current Android sample), so it resets when the app is killed.
- The Android `.apk` and this iOS app are **separate builds**; they do not share one binary.
