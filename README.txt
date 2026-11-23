HOW TO GET YOUR APK (no Android Studio needed)

1. Unzip this file.
2. Go to github.com, sign in (free account), click "+" > New repository.
   Name it TagReader, leave it Public, click Create repository.
3. Click "uploading an existing file". Drag in everything INSIDE the
   TagReader folder (app, build.gradle.kts, settings.gradle.kts,
   gradle.properties, .github). Click Commit changes.
   - If the .github folder doesn't upload: click Add file > Create new file,
     type  .github/workflows/build.yml  as the name, paste the contents of
     build.yml from this zip, and commit.
4. Open the Actions tab. "Build APK" starts by itself (about 3-5 minutes).
   If it doesn't, click Build APK > Run workflow.
5. When it shows a green check, open the run and download
   "TagReader-apk" under Artifacts. Unzip it to get app-debug.apk.
6. Copy app-debug.apk to your Android phone, tap it, and allow
   "Install unknown apps" when asked. Turn NFC on in phone settings.
