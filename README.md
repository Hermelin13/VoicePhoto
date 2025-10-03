# VoicePhoto

VoicePhoto is a small Android app that captures photos or records video using voice triggers. This is a university project and uses Vosk on-device speech recognition models. Configure one or more custom keywords (English or Czech) and the app will take a picture or start/stop recording when it detects them.

## Key features
- Trigger photo capture or video recording with voice keywords
- Support for English and Czech (on-device Vosk models included)
- Uses AndroidX CameraX for camera lifecycle and capture
- Simple settings to add/remove keywords and choose language
- Privacy-focused: no account required; processing can run on-device

## How it works (high level)
1. App unpacks a Vosk speech model from assets and creates a recognizer.
2. Microphone audio is fed into the recognizer; partial results are monitored for configured keywords.
3. When a keyword is detected the app triggers CameraX to take a photo or toggle video recording.
4. Captures are saved to device storage; optional beeps/flash give user feedback.

## Quick start — build & run
Prerequisites:
- Android Studio (Arctic Fox or later recommended)
- Java / Android SDK matching project config (check app/build.gradle)
- Device or emulator with camera and microphone

To build and run:
1. Open the project in Android Studio.
2. Let Gradle sync and download dependencies.
3. Run the app on a device (recommended) or an emulator with camera/mic support.

## Permissions
The app requires:
- RECORD_AUDIO — for keyword detection
- CAMERA — for photo/video capture
- WRITE_EXTERNAL_STORAGE / READ_EXTERNAL_STORAGE — to save and access photos (if applicable)

Grant these permissions on install or at runtime.

## Configuration & usage
- Open Settings to add keywords and choose language (English / Czech).
- Use short, distinct words/phrases to reduce false positives.
- Use in quiet environments for best results.
- Toggle flash or sound feedback in the UI.

## Troubleshooting
- No recognition: verify microphone permission and model loaded successfully (logcat shows model/init messages).
- False triggers: shorten or change keywords, test in quieter environment.
- Camera errors: ensure CameraX-compatible device or emulator and proper permissions.

## Project structure (important files)
- app/src/main/java/vut/example/voskapp/MainActivity.java — main app logic, recognition and capture
- app/src/main/java/vut/example/voskapp/SettingsActivity.java — settings and keywords
- app/src/main/java/vut/example/voskapp/HelpActivity.java — help UI
- models/ — Vosk model assets (English/Czech) used by recognizer
- app/build.gradle — app build configuration

## Contributing
Bugs, improvements and pull requests are welcome. Open an issue describing the problem or desired change and include logs or reproduction steps when possible.

## License & credits
Vosk speech recognition (models) and CameraX are used under their respective licenses — check models/ and build files for specifics.