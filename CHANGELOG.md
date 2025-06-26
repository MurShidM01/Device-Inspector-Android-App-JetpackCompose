# Device Inspector Changelog

## v1.1 (2025-06-26)

### 🚀 New Features
- **Navigation Drawer:** Modern, professional drawer navigation replaces bottom navigation for easier access to all sections.
- **Apps Section:**
  - View all installed apps with real app icons, name, package, version, install/update date.
  - Tap to open app settings directly.
- **Camera Section:**
  - Detailed info for each camera: megapixels, resolutions, aperture, focal lengths, orientation, sensor size, hardware level, autofocus, OIS, flash, and more.
- **System Section:**
  - Added uptime, Android codename, kernel version, baseband, Play Services version, language, region, timezone, build fingerprint.
- **Hardware Section:**
  - Added CPU model, min/max frequency, ABI list, GPU info, external storage, Bluetooth version, USB/OTG, color depth, camera count, NFC support.

### 🎨 UI/UX Improvements
- **Distinct Card Gradients:**
  - Each section (Apps, Camera, etc.) uses a unique, attractive gradient for card headers.
- **Consistent Modern Cards:**
  - All info cards use a unified, modern style with rounded corners, shadows, and clear section headers.
- **App Icons in Card Headers:**
  - Apps screen now displays each app's icon in the card header for a professional look.
- **Camera Info Polished:**
  - Camera cards are visually consistent and clean, showing only relevant info.

### 🛠️ Enhancements & Fixes
- Improved error handling for missing/unknown info.
- Removed duplicate/conflicting imports for better build stability.
- Fixed display of app icons in Apps screen.
- Removed unsupported or ambiguous features (e.g., HDR check).
- General code cleanup and performance improvements.

---

## v1.0 (Initial Release)
- Device Inspector launched with:
  - System, Hardware, Network, Battery, and Sensors info
  - Modern UI with splash screen and bottom navigation
  - Open-source, privacy-respecting, works on non-rooted devices

---

*Thank you for using Device Inspector!*
