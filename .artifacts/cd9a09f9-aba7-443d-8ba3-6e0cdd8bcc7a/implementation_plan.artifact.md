# Fix Unresolved Reference 'app_logo_main'

The build error `Unresolved reference 'app_logo_main'` occurs because the drawable resource `app_logo_main` is missing from the project. I will replace it with the existing `placeholder` resource to allow the project to compile.

## Proposed Changes

### [app]

#### [MODIFY] [CallOverlayService.kt](file:///E:/NET2026/Llamadas%20en%20falso/app/src/main/java/com/example/myapplication/service/CallOverlayService.kt)
- Replace `R.drawable.app_logo_main` with `R.drawable.placeholder`.

#### [MODIFY] [HomeScreen.kt](file:///E:/NET2026/Llamadas%20en%20falso/app/src/main/java/com/example/myapplication/ui/HomeScreen.kt)
- Replace `R.drawable.app_logo_main` with `R.drawable.placeholder`.

## Verification Plan

### Automated Tests
- Run `./gradlew :app:assembleDebug` to verify the project builds successfully.

### Manual Verification
- N/A (Build fix)
