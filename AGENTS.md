# Android App 開發規範（簡化版）

本檔是簡單型 Android App 的共用開發筆記。開發新 App 時複製到專案根目錄，opencode 會自動讀取；專案專屬規則寫在文末。

---

## 1. 技術基線

| 項目 | 版本 |
| --- | --- |
| 語言 | Kotlin |
| UI | Jetpack Compose + Material 3 |
| compileSdk / targetSdk | 36（新專案）；既有專案沿用原值 |
| minSdk | 26 |
| Java / JVM Target | 17 |
| 建置 | Gradle Kotlin DSL + Version Catalog（`libs.versions.toml`） |

- 版本一律寫在 `libs.versions.toml`，不要在 `build.gradle.kts` 寫死。
- 不要任意升級 SDK／函式庫；升級需在 `DEV_LOG.md` 說明原因。

## 2. 專案結構

```
app/src/main/java/<package>/   # ui/ data/ util/ 依功能分包
app/src/main/res/              # 字串、顏色、圖示
app/src/test/                  # 單元測試（選用）
gradle/libs.versions.toml
README.md
CHANGELOG.md
RELEASE_NOTES.md
DEV_LOG.md                     # 開發過程紀錄（必要）
```

- `namespace` 與 `applicationId` 用反向網域命名，發佈後不可更改。
- 文案放 `res/values/strings.xml`，不要在 Compose 寫死；預設繁體中文。

## 3. 程式碼規範

- 4 空格縮排；類名 PascalCase、函式／變數 camelCase、常數 UPPER_SNAKE_CASE。
- Compose 函式 PascalCase；可預覽者加 `@Preview`。
- 狀態用 `ViewModel` + `StateFlow`；UI 保持無狀態。
- 非同步用 Coroutines，不在 UI 執行緒做 I/O。
- 資料儲存：設定用 DataStore，結構化資料用 Room，其餘用檔案。
- 權限使用時才請求；不要引入未在 Catalog 宣告的依賴。

## 4. 共用界面

- 只用 Material 3 元件（`Scaffold`、`TopAppBar`、`Card`、`ListItem`、`Switch`）。
- 間距用 4dp 倍數；圓角 12dp。
- 顏色定義在 `ui/theme/Color.kt`，不要寫死 `Color(0xFF...)`。
- 字型用 Material 3 `Typography`，不要逐處指定 `fontSize`。
- 空狀態、載入中、錯誤都要有畫面；刪除等破壞性操作需二次確認。
- 可點擊元素要有 `contentDescription`。

## 5. 常用功能（需要時再實作）

- **顯示模式**：Material 3 dynamicColor + 淺色／深色／跟隨系統。
- **設定頁**：DataStore Preferences + `ListItem` / `Switch`。
- **About**：顯示 `versionName`、`versionCode`、授權與隱私說明。
- **通知**：Android 13+ 請求 `POST_NOTIFICATIONS`。
- **Edge-to-edge**：`enableEdgeToEdge()` + `Scaffold` 處理留白。
- **返回鍵**：用 `BackHandler`，不要直接關閉 App。

## 6. 建置與版本

```kotlin
buildTypes {
    debug { versionNameSuffix = "-debug" }
    prerelease {
        initWith(getByName("release"))
        isMinifyEnabled = true
        signingConfig = signingConfigs.getByName("debug")
        versionNameSuffix = "-prerelease"
    }
    release {
        isMinifyEnabled = true
        isShrinkResources = true
        proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
    }
}
```

- 開發與測試安裝一律使用 `debug`（`app-debug.apk`）；不要自動建置 `prerelease`，只有使用者明確要求時才執行 `assemblePrerelease`。
- 每次功能變更完成後，自動執行 `assembleDebug` 確認可建置；不要自動建置 `assemblePrerelease` 或 `assembleRelease`。
- `prerelease`（debug key 簽署）僅供使用者要求的測試版；正式上架用 `release`。
- 每次功能變更遞增 `versionCode`；`versionName` 採 `主.次.修`。
- 指令：`./gradlew assembleDebug`、`test`、`lint`（`assemblePrerelease` 僅限明確要求）。

## 7. 開發過程紀錄（DEV_LOG.md）

每次開發工作都要更新專案docs目錄的 `DEV_LOG.md`，不要事後補寫。內容包含：

1. **本次目標**：要做什麼、為什麼。
2. **變更內容**：改了哪些檔案／功能。
3. **遇到的問題與解法**：錯誤訊息、嘗試過的方法、最後怎麼解。
4. **待辦／已知限制**：尚未完成或未驗證的部分。
5. **日期與版本**：對應的 `versionCode` / `versionName`。

格式建議（最新紀錄放最上面）：

```markdown
## 2026-10-28 - v1.1.0

### 目標
新增深色模式切換。

### 變更
- `ui/theme/Theme.kt`：加入主題狀態
- `SettingsScreen.kt`：新增切換項目

### 問題與解法
- DataStore 首次讀取延遲 → 以預設值先顯示

### 待辦
- 尚未實測 Android 12 以下
```

- 只記重點，不貼整段程式碼。
- 不要記錄 API Key、密碼等敏感資訊。

## 8. 文件與禁止事項

- `README.md` 寫功能與建置方式；`CHANGELOG.md` 記錄版本異動；`RELEASE_NOTES.md` 寫給使用者看。
- 不要提交 `local.properties`、keystore、`.gradle/`、`build/`。
- 不要留 `TODO` 當完成功能、不要留測試假資料或除錯 Log。
- 不要更動無關檔案；未經確認不要改 `applicationId`、簽章或 SDK 版本。
- 不要把 API Key、密碼、金鑰寫進原始碼或版本控制。

---

## 專案專屬補充

- 專案名稱：
- applicationId：
- 特殊需求：
- 已知限制：
