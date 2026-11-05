# Implementation Plan - Fix Battery Optimization Request

The application is unable to correctly request exclusion from battery optimizations because the required permission is missing from the manifest. This plan adds the necessary permission and improves the redirection logic to ensure users can properly configure the app for background work, especially on devices with strict power management like HONOR.

## Proposed Changes

### [app]

#### [MODIFY] [AndroidManifest.xml](file:///E:/NET2026/Llamadas%20en%20falso/app/src/main/AndroidManifest.xml)
- Add `<uses-permission android:name="android.permission.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS" />`. This is required to show the system dialog that asks the user to allow the app to run in the background.

#### [MODIFY] [HomeScreen.kt](file:///E:/NET2026/Llamadas%20en%20falso/app/src/main/java/com/example/myapplication/ui/HomeScreen.kt)
- **Improve Redirection**: If the direct request dialog fails or is not supported, provide a clearer fallback to the system battery optimization settings page.
- **Guidance Update**: Ensure the "REVISAR Y ACTIVAR" flow prioritize the battery optimization check as a critical step for Android 15 stability.

## Technical Details

1.  **Permission**: `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` is a normal permission but its use is restricted on the Google Play Store (though perfect for direct installation/APK use).
2.  **Intent Flow**:
    - Primary: `ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` (Shows a direct "Allow?" dialog).
    - Fallback: `ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS` (Takes the user to the list of apps).

## Verification Plan

### Automated Tests
- Run `./gradlew :app:compileDebugKotlin`.

### Manual Verification
1.  Open the app and click **REVISAR Y ACTIVAR**.
2.  Verify that a system dialog now appears asking: "**¿Permitir que AntiFraude se ejecute siempre en segundo plano?**".
3.  Grant the permission and verify that "Optimización de batería" now shows as **ACTIVO** in the dashboard.
