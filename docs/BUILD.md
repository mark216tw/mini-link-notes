# 建置與發佈指南

本文件說明開發環境、建置指令、Build Type、簽署與 GitHub Pre-release 流程。

## 1. 開發環境

| 項目 | 需求 |
| --- | --- |
| JDK | 17 以上（建議 Temurin 17） |
| Android SDK | Platform 36、Build-Tools 36 |
| Gradle | 使用專案內 Wrapper，不需另外安裝 |
| 作業系統 | Windows、macOS、Linux 皆可 |

設定 `JAVA_HOME` 指向 JDK 17，並確認 `ANDROID_HOME` 或 `ANDROID_SDK_ROOT` 指向 Android SDK。

若 SDK 路徑未被環境變數偵測，可在專案根目錄建立 `local.properties`：

```properties
sdk.dir=C\:\\Users\\<使用者>\\AppData\\Local\\Android\\Sdk
```

`local.properties` 已被 `.gitignore` 忽略，請勿提交。

## 2. 版本設定

版本位於 `app/build.gradle.kts` 的 `defaultConfig`：

```kotlin
versionCode = 7
versionName = "1.0.0"
```

- `versionCode` 每次功能變更遞增。
- `versionName` 採 `主.次.修`。
- Build Type 會自動加上後綴：`debug` → `-debug`、`prerelease` → `-prerelease`。
- 因此 `prerelease` 的實際版本字串為 `1.0.0-prerelease`。

## 3. Build Type

| Build Type | Minify | Shrink Resources | 簽署 | 輸出 |
| --- | --- | --- | --- | --- |
| `debug` | 否 | 否 | Debug | `app-debug.apk` |
| `prerelease` | 是（R8） | 是 | Debug key | `app-prerelease.apk` |
| `release` | 是（R8） | 是 | 未設定 | 未簽署 APK |

`prerelease` 定義：

```kotlin
create("prerelease") {
    initWith(getByName("release"))
    isMinifyEnabled = true
    isShrinkResources = true
    signingConfig = signingConfigs.getByName("debug")
    versionNameSuffix = "-prerelease"
    proguardFiles(
        getDefaultProguardFile("proguard-android-optimize.txt"),
        "proguard-rules.pro",
    )
}
```

## 4. 常用指令

### Windows

```powershell
.\gradlew.bat assembleDebug
.\gradlew.bat assemblePrerelease
.\gradlew.bat test
.\gradlew.bat lint
```

### macOS / Linux

```bash
./gradlew assembleDebug
./gradlew assemblePrerelease
./gradlew test
./gradlew lint
```

### 一次驗證

```bash
./gradlew test lint assembleDebug
```

## 5. 建置 Pre-release APK

```bash
./gradlew assemblePrerelease
```

輸出位置：

```
app/build/outputs/apk/prerelease/app-prerelease.apk
```

此 APK 特性：

- 啟用 R8 程式碼壓縮與資源縮減
- 使用 Android Debug 金鑰簽署
- versionName 為 `1.0.0-prerelease`
- 僅供測試，不可上架 Google Play

若要重新命名為發佈用檔名，可複製為 `app-prerelease.apk`（Gradle 預設輸出即為此名稱）。

## 6. 簽署

### Debug 簽署

`prerelease` 使用 `signingConfigs.getByName("debug")`，由 Android Gradle Plugin 自動產生 debug keystore（通常位於 `~/.android/debug.keystore`）。不需要在專案中存放金鑰。

### 正式簽署

若要發佈正式版：

1. 建立 release keystore（不要提交到版本控制）。
2. 在 `app/build.gradle.kts` 或 `keystore.properties` 設定 signingConfig。
3. 使用 `assembleRelease` 或 `bundleRelease`。
4. 將金鑰與密碼保存在安全位置，不要寫進原始碼。

`.gitignore` 已忽略 `*.keystore`、`*.jks`、`local.properties`。

## 7. R8 與 ProGuard

- 啟用 `proguard-android-optimize.txt` 與 `app/proguard-rules.pro`。
- 若有反射、序列化或 JNI 需求，需在 `proguard-rules.pro` 加入 keep 規則。
- Kotlin Serialization、Room、Compose 由各自 consumer rules 處理。
- 建置後建議在實機測試分享、標題抓取、備份匯入匯出等功能，確認 R8 未移除必要程式碼。

## 8. GitHub Pre-release 流程

本專案目前以 `1.0.0-prerelease` 作為首個公開測試版。

```bash
git tag v1.0.0-prerelease
git push origin v1.0.0-prerelease
gh release create v1.0.0-prerelease app/build/outputs/apk/prerelease/app-prerelease.apk \
  --title "mini網頁筆記 1.0.0-prerelease" \
  --notes-file RELEASE_NOTES.md \
  --prerelease
```

重點：

- 發佈時必須加上 `--prerelease`，避免被視為正式版本。
- 上傳的檔案名稱建議為 `app-prerelease.apk`。
- Release notes 需明確標示「Pre-release 測試版本」。
- APK 不提交到 Git 儲存庫，只放在 GitHub Release。

## 9. 文件同步

每次版本變更請同步更新：

- `README.md`：目前版本與下載連結
- `CHANGELOG.md`：版本異動
- `RELEASE_NOTES.md`：使用者可讀的更新說明
- `docs/DEV_LOG.md`：開發過程、問題與待辦
- `docs/ARCHITECTURE.md`、`docs/DESIGN.md`：若架構或設計有變

## 10. 常見問題

### `SDK location not found`

設定 `ANDROID_HOME`，或建立 `local.properties` 指定 `sdk.dir`。

### `Unsupported class file major version`

Gradle 使用的 JDK 版本不符。確認 `JAVA_HOME` 指向 JDK 17，且 IDE 的 Gradle JDK 設定一致。

### KSP 或 Room schema 匯出失敗

確認 `app/schemas/` 可寫入，且沒有同時被其他程序占用。首次建置多個 variant 時，KSP 可能並行匯出 schema；若遇到空檔，重新執行建置即可。

### R8 建置成功但執行時崩潰

檢查 `proguard-rules.pro` 是否需要 keep rule，尤其是序列化、反射或第三方函式庫。使用 `assemblePrerelease` 在實機測試。

### 安裝時顯示「套件衝突」或「簽章不同」

測試版與正式版簽章不同。先解除安裝舊版；解除安裝會清除本機資料，請先匯出備份。
