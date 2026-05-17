# DNS Logger

A lightweight, native Android network monitor. DNS Logger intercepts and displays device-wide IPv4 traffic and DNS requests in real-time, completely on-device, without requiring root access.

<img alt="git" src="https://github.com/user-attachments/assets/0a6ac9cb-0bb6-4564-81e9-6fd980b46d4b" width="250" />


## Key Features

* **Real-Time Inspection:** Captures and displays live network requests in a clean, scrollable terminal UI.
* **100% Local Processing:** Leverages Android's native VPN service to route and log traffic entirely on the device. Zero data is sent to external servers.
* **Log Export:** Instantly save captured session logs to your device's local storage as a `.txt` file for offline analysis.
* **Modern Design:** Built with Kotlin and Jetpack Compose, featuring a responsive, dark-mode floating dock interface.

## How to Use

1. **Install:** Download the latest `.apk` from the **Releases** tab, or clone the repository and build via Android Studio.
2. **Start Monitoring:** Tap **START LOGGING** and grant the standard Android VPN permission to allow the local interceptor to run.
3. **Analyze:** Watch network traffic appear live in the console. 
4. **Export Data:** Tap the blue download icon in the dock to save your current log session directly to your device's `Downloads` folder.

---
**Privacy & Security Note:** This tool is built strictly for personal network diagnostics and portfolio demonstration. It operates entirely offline and does not collect, track, or transmit any user data.
