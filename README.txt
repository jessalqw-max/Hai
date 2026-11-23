GET YOUR APK (no folders to upload)

1. Unzip this file. You will see 7 loose files.
2. On github.com (free account): "+" > New repository, name it TagReader,
   Public, Create repository.
3. Click "uploading an existing file" > "choose your files". Select these 6
   files together, then Commit changes:
     settings.gradle.kts, build.gradle.kts, gradle.properties,
     app-build.gradle.kts, AndroidManifest.xml, MainActivity.kt
4. Add file > Create new file. In the name box type exactly:
     .github/workflows/build.yml
   (typing the slashes makes the folders). Open build.yml from this zip in
   Notepad, copy everything, paste it into GitHub, click Commit changes.
5. Open the Actions tab. "Build APK" runs by itself (3-5 minutes). If not,
   click Build APK > Run workflow.
6. When it has a green check, open the run, download "TagReader-apk" under
   Artifacts, unzip it, and you have app-debug.apk.
7. Copy it to your phone, tap it, allow "Install unknown apps", and turn NFC on.
