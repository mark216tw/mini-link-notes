# 系統架構與技術文件

本文件說明 mini網頁筆記的技術基線、分層架構、資料流與關鍵實作。

## 1. 專案概觀

| 項目 | 內容 |
| --- | --- |
| 專案名稱 | mini網頁筆記（MiniLinkNotes） |
| applicationId / namespace | `com.vibecode.minilinknotes` |
| 語言 | Kotlin |
| UI | Jetpack Compose + Material 3 |
| 最低版本 | Android 8.0（API 26） |
| 編譯／目標版本 | compileSdk 36 / targetSdk 36 |
| Java / JVM Target | 17 |
| 建置系統 | Gradle Kotlin DSL + Version Catalog |
| 目前版本 | `1.0.0-prerelease`（versionCode 7） |
| 授權 | MIT |

## 2. 技術基線

版本定義於 `gradle/libs.versions.toml`。

| 元件 | 版本 | 用途 |
| --- | --- | --- |
| Gradle Wrapper | 8.14.5 | 建置 |
| Android Gradle Plugin | 8.13.2 | Android 建置 |
| Kotlin | 2.2.20 | 程式語言 |
| KSP | 2.2.20-2.0.3 | Room 編譯器 |
| Compose BOM | 2025.05.01 | Compose 版本對齊 |
| Room | 2.7.1 | 本機關聯式資料庫 |
| DataStore Preferences | 1.1.7 | 設定儲存 |
| Navigation Compose | 2.9.0 | 畫面導覽 |
| Lifecycle | 2.9.0 | ViewModel、狀態收集 |
| OkHttp | 4.12.0 | 網頁標題 HTTP 請求 |
| Jsoup | 1.18.3 | HTML 解析 |
| Kotlin Serialization | 1.8.1 | JSON 備份 |
| Coroutines | 1.10.2 | 非同步 |
| JUnit | 4.13.2 | 單元測試 |

## 3. 分層架構

```
┌─────────────────────────────────────────────┐
│ UI Layer（Jetpack Compose）                  │
│ MainActivity → MiniLinkNotesRoot → Screens  │
└───────────────────────┬─────────────────────┘
                        │ StateFlow / 事件
┌───────────────────────▼─────────────────────┐
│ Presentation（AppViewModel）                 │
│ 搜尋、篩選、排序、UI 狀態、Snackbar 事件      │
└───────────────────────┬─────────────────────┘
                        │ suspend / Flow
┌───────────────────────▼─────────────────────┐
│ Domain / Data Access（NotesRepository）      │
│ 商業規則、正規化、交易、標題抓取、備份        │
└──────┬───────────────────────┬──────────────┘
       │                       │
┌──────▼───────┐     ┌─────────▼────────────┐
│ Room         │     │ DataStore            │
│ AppDatabase  │     │ SettingsStore        │
│ NotesDao     │     └──────────────────────┘
└──────────────┘
       │
┌──────▼──────────────────────────────────────┐
│ Platform / Network                          │
│ OkHttp + Jsoup（TitleFetcher）              │
│ ContentResolver（備份匯入匯出）             │
└─────────────────────────────────────────────┘
```

### 3.1 各層責任

- **UI Layer**：只負責顯示與使用者互動，不直接存取資料庫。畫面接收 `NotesUiState`，透過 `AppViewModel` 送出操作。
- **Presentation**：`AppViewModel` 合併資料流、套用搜尋／篩選／排序、產生 Snackbar 事件。狀態以 `StateFlow` 暴露。
- **Data Access**：`NotesRepository` 封裝資料規則，例如 URL 正規化、重複檢查、標籤關聯、軟刪除、標題抓取。
- **Persistence**：Room 儲存筆記、標籤與關聯；DataStore 儲存顯示模式、動態色彩、自動抓標題與排序設定。
- **Platform / Network**：OkHttp 取得 HTML，Jsoup 解析；`ContentResolver` 處理使用者選擇的備份檔案。

## 4. 套件結構

```
com.vibecode.minilinknotes/
├── MainActivity.kt              # 入口、分享 Intent、開啟連結
├── MiniLinkNotesApp.kt          # Application、AppContainer（手動 DI）
├── AppViewModel.kt              # UI 狀態、事件、操作入口
├── data/
│   ├── AppDatabase.kt           # Room 資料庫、migration
│   ├── Model.kt                 # Entity、Relation、enum、TypeConverter
│   ├── NotesDao.kt              # SQL 查詢
│   ├── NotesRepository.kt       # 資料規則與交易
│   ├── SettingsStore.kt         # DataStore
│   ├── TitleFetcher.kt          # OkHttp 抓取 HTML
│   └── backup/
│       ├── BackupModels.kt      # 備份 DTO、版本、錯誤
│       ├── BackupCodec.kt       # JSON 編解碼
│       └── BackupManager.kt     # 匯入／匯出
├── ui/
│   ├── MiniLinkNotesRoot.kt     # NavHost、Snackbar
│   ├── notes/NotesScreen.kt     # 清單、搜尋、篩選、排序
│   ├── editor/EditorScreen.kt   # 新增／編輯
│   ├── tags/TagsScreen.kt       # 標籤管理
│   ├── trash/TrashScreen.kt     # 回收筒
│   ├── settings/SettingsScreen.kt
│   ├── about/AboutScreen.kt
│   └── theme/                   # Color、Theme
└── util/
    ├── UrlUtils.kt              # URL 正規化、擷取、去重
    ├── ShareParser.kt           # 分享內容解析
    ├── HtmlTitleParser.kt       # 網頁標題解析
    └── TimeUtils.kt             # 時間格式、回收筒期限
```

## 5. 依賴注入與生命週期

- `MiniLinkNotesApp` 在 `onCreate` 建立 `AppContainer`。
- `AppContainer` 建立 Room、DataStore、TitleFetcher、Repository 與 BackupManager，形成單例。
- `MainActivity` 以 `AppViewModel.Factory` 注入上述依賴。
- `AppContainer` 另外持有 `SupervisorJob + Dispatchers.IO` 的 application scope，在啟動時清除過期回收筒。

沒有使用 Hilt／Koin，依賴數量少，以手動 DI 降低建置與維護成本。

## 6. 資料模型

### 6.1 ER 關聯

```
notes 1 ─── N note_tag N ─── 1 tags
```

- `notes`：筆記主體，`deletedAt` 為軟刪除時間。
- `tags`：標籤，名稱唯一（忽略大小寫），`sortOrder` 控制顯示順序。
- `note_tag`：多對多關聯，複合主鍵 `(noteId, tagId)`，兩端皆 `CASCADE`。

### 6.2 `notes` 欄位

| 欄位 | 型別 | 說明 |
| --- | --- | --- |
| `id` | Long | 自動遞增主鍵 |
| `type` | NoteType | `LINK` 或 `TEXT` |
| `title` | String | 標題，可為空字串 |
| `url` | String? | 連結筆記網址 |
| `content` | String? | 文字內容或連結補充 |
| `isRead` | Boolean | 已讀狀態 |
| `createdAt` | Long | 建立時間（epoch millis） |
| `updatedAt` | Long | 更新時間 |
| `deletedAt` | Long? | 軟刪除時間；非 null 表示在回收筒 |
| `titleFetchState` | TitleFetchState | `NONE`、`SUCCESS`、`FAILED` |
| `titleEdited` | Boolean | 使用者是否手動改過標題 |

索引：`deletedAt`、`createdAt`。`isRead` 與 `url` 的一般索引已在 schema v2 移除，降低寫入成本。

### 6.3 `tags` 欄位

| 欄位 | 型別 | 說明 |
| --- | --- | --- |
| `id` | Long | 自動遞增主鍵 |
| `name` | String | 唯一名稱 |
| `colorIndex` | Int | 0–11，對應 12 色調色盤 |
| `sortOrder` | Int | 顯示與拖曳排序 |

### 6.4 資料庫版本與 Migration

- 目前 `version = 2`，`exportSchema = true`，schema 輸出到 `app/schemas/`。
- `MIGRATION_1_2` 以 `DROP INDEX IF EXISTS` 移除 `index_notes_isRead`、`index_notes_url`。
- 尚未提供 2→3；未來變更 schema 必須新增 migration。

## 7. 資料流

### 7.1 讀取清單

```
Room Flow(activeNotes, trash, tagsWithCount)
        + DataStore Flow(settings)
        + 搜尋／篩選 StateFlow
                │
        combine + flowOn(Dispatchers.Default)
                │
        buildState：搜尋、標籤、未讀、排序
                │
        StateFlow<NotesUiState>
                │
        Compose collectAsStateWithLifecycle
```

- 搜尋字串分為「即時 query」與「debounce filterQuery」：UI 輸入立即顯示，過濾延遲 200ms；空字串不延遲。
- 排序在背景執行緒完成，標題排序使用 `Collator(Locale.TRADITIONAL_CHINESE)`。
- `stateIn(WhileSubscribed(5_000))`：畫面離開 5 秒後停止收集，降低背景負擔。

### 7.2 新增分享內容

```
MainActivity.onNewIntent
  → 判斷 ACTION_SEND / ACTION_PROCESS_TEXT
  → ShareParser.parse（URL 擷取、HTML 解析）
  → AppViewModel.handleSharedContent
  → NotesRepository.createFromShared
  → Room 交易：重複檢查 + insert note + tags
  → Snackbar「已儲存筆記 / 已有相同連結」
  → 若無標題且為連結，背景 fetchAndApplyTitle
```

`handledIntent` 避免同一 Intent 重複處理。

### 7.3 標題抓取與競態

1. `TitleFetcher` 以 OkHttp 請求，限制 1 MiB 回應、8 秒連線／讀取、12 秒總逾時。
2. `HtmlTitleParser` 先取 `<title>`，沒有則取 `og:title`，壓縮空白並截斷 200 字。
3. 寫回前重新讀取筆記，若使用者已手動改標題（`titleEdited = true`），不覆寫。
4. 失敗時只更新 `titleFetchState = FAILED`，不影響其他欄位。

### 7.4 刪除與復原

- 刪除：`deletedAt = now`，筆記從清單消失，Snackbar 提供「復原」。
- 復原：`deletedAt = null`。
- 永久刪除：直接 `DELETE`，關聯由外鍵 CASCADE 清除。
- 過期清除：App 啟動與 ViewModel 初始化時刪除 `deletedAt < now - 30 天` 的筆記。

### 7.5 備份匯入／匯出

- 匯出在 `Dispatchers.IO` 讀取未刪除筆記與標籤，序列化為 JSON，透過 `ContentResolver` 寫入使用者選擇的 URI。
- 匯入限制 20 MiB，於單一 Room 交易中執行：
  - **MERGE**：保留現有資料；連結以 `UrlUtils.dedupeKey` 比對，文字以完整內容比對。
  - **REPLACE**：先清空 `note_tag`、`notes`、`tags`，再匯入。
- `schemaVersion` 高於目前版本時拒絕匯入，避免資料誤解。

## 8. JSON 備份格式

```json
{
  "schemaVersion": 1,
  "exportedAt": 1730000000000,
  "tags": [
    { "name": "工作", "colorIndex": 2, "sortOrder": 0 }
  ],
  "notes": [
    {
      "type": "LINK",
      "title": "範例",
      "url": "https://example.com",
      "content": "分享時的文字",
      "isRead": false,
      "createdAt": 1730000000000,
      "updatedAt": 1730000000000,
      "tags": ["工作"]
    }
  ]
}
```

- `type` 允許 `LINK`、`TEXT`。
- `updatedAt` 缺省時以 `createdAt` 補上。
- 未知欄位會被忽略（`ignoreUnknownKeys = true`）。

## 9. 導覽結構

```
notes（起始頁）
├── editor?noteId={noteId}
├── tags
├── trash
├── settings
│   └── about
```

- 使用 Navigation Compose，`noteId = -1` 代表新增。
- Snackbar 的「編輯」與「復原」由 `MiniLinkNotesRoot` 統一處理，事件經 `Channel` 序列化，避免互相覆蓋。

## 10. 執行緒與效能

- Room 查詢與寫入使用 Coroutines／Room 的 suspend 支援。
- 搜尋、篩選、排序在 `Dispatchers.Default`。
- 網路與檔案 I/O 在 `Dispatchers.IO`。
- 搜尋 debounce 200ms，僅作用於過濾，不影響輸入顯示。
- 清單使用 `LazyColumn` 與穩定 key（筆記 ID）。
- 匯入合併只查 URL 與文字內容，不載入全部筆記實體。
- Gradle 開啟平行建置與建置快取。

## 11. 建置變體

| Build Type | Minify | Shrink Resources | 簽署 | versionName 後綴 | 用途 |
| --- | --- | --- | --- | --- | --- |
| `debug` | 否 | 否 | Debug | `-debug` | 開發與測試 |
| `prerelease` | 是 | 是 | Debug key | `-prerelease` | 公開測試 |
| `release` | 是 | 是 | 未設定 | 無 | 正式發佈 |

R8 規則位於 `app/proguard-rules.pro`。`prerelease` 以 `initWith(release)` 繼承 release 設定，再指定 debug 簽署。

## 12. 測試與品質

單元測試位於 `app/src/test/`：

| 測試 | 範圍 |
| --- | --- |
| `UrlUtilsTest` | URL 正規化、擷取、去重、host |
| `ShareParserTest` | 純文字與 HTML 分享解析 |
| `HtmlTitleParserTest` | 標題與 og:title 解析 |
| `BackupCodecTest` | 備份 JSON 編解碼與版本檢查 |
| `TimeUtilsTest` | 回收筒剩餘天數 |

驗證指令：

```bash
./gradlew test lint assembleDebug
```

目前沒有 UI 自動化測試與 Room migration 測試，這是已知限制。

## 13. 安全性與隱私

- 只宣告 `INTERNET` 權限。
- 無帳號、無雲端同步、無分析 SDK。
- 備份由使用者主動選擇儲存位置，App 不自動上傳。
- 網址只接受 `http`／`https`，且拒絕含 userInfo 的網址，降低惡意連結風險。
- 標題抓取限制回應大小與逾時，避免無限下載。
- 資料庫未加密；若需要更高防護，可另行整合 SQLCipher。

## 14. 已知限制

- 中文搜尋採記憶體 `contains` 比對，資料量極大時效能會下降。
- 標題抓取對 JavaScript 動態渲染、需登入或反爬網站會失敗。
- 尚未提供資料庫加密。
- 尚未提供 UI 自動化測試與 migration 測試。
- `prerelease` 使用 debug key，不可用於正式上架。
- 尚未在實機驗證所有品牌桌面圖示遮罩與分享選單行為。
