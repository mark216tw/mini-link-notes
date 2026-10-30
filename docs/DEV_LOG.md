# 開發紀錄

最新紀錄放最上面。每次開發工作都要更新，不要事後補寫。

---

## 2026-10-30 - v1.0.0-prerelease（versionCode 7，首個公開測試版）

### 目標
補齊專案文件、採用 MIT License，建立 GitHub 公開儲存庫，並產出可公開測試的 `app-prerelease.apk`（R8 壓縮、Debug 金鑰簽署、Build Type `prerelease`）。

### 變更
- 新增 `LICENSE`：MIT License，著作權人 mark216tw。
- 改寫 `README.md`：專案說明、功能、文件索引、建置、隱私、授權與測試版下載資訊。
- 新增 `docs/USER_GUIDE.md`：安裝、分享、新增、搜尋、標籤、回收筒、設定、備份與 FAQ。
- 新增 `docs/ARCHITECTURE.md`：技術基線、分層架構、套件、資料模型、資料流、備份格式、執行緒與限制。
- 新增 `docs/DESIGN.md`：產品目標、資訊架構、畫面設計、狀態模型、關鍵流程與設計取捨。
- 新增 `docs/BUILD.md`：環境、Build Type、R8、簽署、GitHub Pre-release 流程與疑難排解。
- 新增 `docs/PRIVACY.md`：權限、網路行為、系統備份、資料刪除與聯絡方式。
- `app/build.gradle.kts`：versionCode 7、versionName 1.0.0；`prerelease` 後綴產生 `1.0.0-prerelease`。
- 更新 `CHANGELOG.md`、`RELEASE_NOTES.md`：標示首個公開 Pre-release 與測試注意事項。
- 建立 GitHub 公開儲存庫 `mark216tw/mini-link-notes`，提交文件與原始碼。
- 建置 `assemblePrerelease`，將 `app-prerelease.apk` 上傳至 GitHub Pre-release `v1.0.0-prerelease`。

### 問題與解法
- 內部開發版本已到 1.2.0，但本次要求公開測試版標示為 1.0.0-prerelease → 將 `versionName` 設為 1.0.0、versionCode 遞增為 7，由 Build Type 自動加上 `-prerelease`；CHANGELOG 保留內部開發紀錄並加註說明。
- GitHub 的 Pre-release 不會出現在「Latest」標籤 → Release 標題與說明明確標示 Pre-release，並在 README 直接連結 tag。
- APK 被 `.gitignore` 排除 → 只上傳到 GitHub Release，不提交進儲存庫，符合版本控制慣例。

### 待辦／已知限制
- 尚未在實機完整驗證所有品牌裝置的安裝、分享選單與圖示遮罩。
- `prerelease` 使用 Debug 金鑰，不可用於正式上架；正式版需建立 release keystore。
- 尚未加入 UI 自動化測試與 Room migration 測試。

---

## 2026-10-30 - v1.2.0（versionCode 6，介面與功能優化）

### 目標
實作 P0 + P1 + P2 介面／功能優化：修正搜尋輸入延遲，加入未讀篩選、可點擊標籤、全部已讀、選取文字分享、清空回收桶，以及多項小型 UI 調整。

### 變更
- `AppViewModel.kt`：拆成即時 `query` 與 debounce `filterQuery`；新增未讀篩選、`markAllRead`、`emptyTrash`、總筆數。
- `NotesDao.kt`、`NotesRepository.kt`：新增 `markAllRead`、`purgeAllTrash`。
- `NotesScreen.kt`：TopAppBar 改為回收筒 + 更多選單（全部已讀、標籤管理、設定）；標題列顯示總數／未讀／結果數；新增未讀 chip；標籤 chip 顯示筆數；筆記標籤可點擊篩選；排序選單加勾選；搜尋支援 Search 鍵。
- `TrashScreen.kt`：TopAppBar 新增清空回收桶與二次確認。
- `EditorScreen.kt`：網址欄改用 URI 鍵盤；儲存失敗區分必填錯誤與一般錯誤。
- `TagsScreen.kt`：移除 TopAppBar 重複的新增按鈕，保留 FAB；標籤名稱加省略。
- `AndroidManifest.xml`、`MainActivity.kt`：新增 `ACTION_PROCESS_TEXT`，可在其他 App 選取文字後存成筆記。
- `strings.xml`：新增選單、未讀篩選、總數、清空回收桶、儲存失敗等字串。
- 版本：versionCode 6、versionName 1.2.0；同步更新 README、CHANGELOG、RELEASE_NOTES。

### 問題與解法
- 上一輪把 query 直接放進 debounce，導致 `TextField(value = state.query)` 也跟著延遲 → 改為顯示用即時 query、過濾用 debounced query，修正輸入慢半拍。
- 移除標籤頁 TopAppBar 的新增按鈕時一度留下多餘括號，造成編譯錯誤 → 整理 `Scaffold`／`TopAppBar` 結構後恢復。
- `ACTION_PROCESS_TEXT` 與現有 `ACTION_SEND` 共用 intent 處理 → 先判斷 action，再分別解析文字或 HTML。

### 待辦／已知限制
- 尚未在實機驗證標籤 pill 點擊與卡片點擊的觸控衝突、PROCESS_TEXT 在各 App 的選單呈現。
- 未新增 UI 自動化測試；目前以編譯、單元測試與 Lint 驗證。

---

## 2026-10-30 - v1.1.3（versionCode 5，順手清理）

### 目標
清掉未使用的程式碼與資料庫索引，並統一重複常數，降低維護成本與寫入負擔。

### 變更
- `NotesDao.kt`、`NotesRepository.kt`：移除未使用的 `getAllNoteEntities`、`allTagsOnce`。
- `Model.kt`：移除 `notes` 表未使用的 `isRead`、`url` 索引。
- `AppDatabase.kt`：資料庫版本改為 2，新增 `MIGRATION_1_2` 移除上述兩個索引。
- `MiniLinkNotesApp.kt`：資料庫建置加入 `MIGRATION_1_2`。
- `BackupManager.kt`：顏色索引改用 `NotesRepository.TAG_COLOR_COUNT - 1`。
- `HtmlTitleParser.kt`：空白正規表示式提為常數。
- 版本：versionCode 5、versionName 1.1.3；同步更新 README、CHANGELOG、RELEASE_NOTES。

### 問題與解法
- 移除索引屬於 schema 變更，直接改 Entity 會讓既有安裝在開啟時驗證失敗 → 補上 1→2 migration，以 `DROP INDEX IF EXISTS` 移除 `index_notes_isRead`、`index_notes_url`。
- 建置後由 Room 產生 `app/schemas/.../2.json`，確認新 schema 只剩 `deletedAt`、`createdAt` 索引。
- 首次同時建置多個 variant 時，KSP 並行匯出尚未存在的 `2.json`，曾短暫讀到空檔而失敗；schema 產生後重跑即正常，後續檔案已存在。

### 待辦／已知限制
- 尚未加入 Room migration 自動化測試，也尚未在實機驗證 v1→v2 升級。
- FTS、`urlKey` 欄位與複合索引仍留待下一梯隊。

---

## 2026-10-30 - v1.1.2（versionCode 4，第一梯隊效能優化）

### 目標
實作低風險、高效益的底層優化：搜尋／排序移出主執行緒、減少重複查詢與全表載入、原子化已讀切換、建置加速與小型 UI 修正。

### 變更
- `AppViewModel.kt`：查詢加入 200ms debounce（空查詢不延遲）、`combine` 改用 `flowOn(Dispatchers.Default)`、`Collator` 提為欄位、標籤只由 `tagsWithCount` 推導、未讀數改為篩選前的全部未讀。
- `NotesDao.kt`、`NotesRepository.kt`：新增 `getActiveUrls`、`getActiveContents`、原子 `toggleRead`；建立筆記的重複檢查移入交易且只查 URL；移除重複的 `observeTags` Flow、`setRead`、`getActiveNotes`。
- `BackupManager.kt`：合併匯入只查 URL 與文字內容，不再載入全部筆記實體。
- `TimeUtils.kt`：改用 thread-safe 的 `java.time.DateTimeFormatter`。
- `MiniLinkNotesRoot.kt`：Snackbar 事件改為序列處理，有動作時使用 `SnackbarDuration.Long`。
- `NotesScreen.kt`：剪貼簿改用 `LocalClipboard` + `ClipEntry`。
- `gradle.properties`：開啟 `org.gradle.parallel`、`org.gradle.caching`。
- 版本：versionCode 4、versionName 1.1.2；同步更新 README、CHANGELOG、RELEASE_NOTES。

### 問題與解法
- `debounce` 在 kotlinx-coroutines 1.10.2 仍標記 `FlowPreview` → 在 `AppViewModel` 加 `@OptIn(FlowPreview::class)`。
- 若對空查詢也 debounce，首次畫面會延遲 → 使用 lambda 版本，查詢為空時 timeout 設為 0。

### 待辦／已知限制
- Room schema 未變更；FTS、`urlKey` 欄位與複合索引留待下一梯隊。
- 尚未在實機驗證大量筆記下的實際滑動與搜尋表現。

---

## 2026-10-30 - v1.1.1（versionCode 3，套用 App 圖示）

### 目標
將選定的圖示版本 A「連結筆記」套用到 Android Adaptive Icon、Android 13 主題圖示與舊版圖示資源。

### 變更
- `drawable/ic_launcher_background.xml`：新增深青線性漸層、柔光與彩色裝飾點。
- `drawable/ic_launcher_foreground.xml`：新增筆記卡、黃色書籤、文字線、鏈結徽章與星星；核心維持 66dp 安全區的 85%。
- `drawable/ic_launcher_monochrome.xml`：新增 Android 13 主題圖示用單色版本，文字線以 evenOdd 鏤空。
- `mipmap-anydpi-v26`、`mipmap-anydpi-v33`：背景改指向 `@drawable/ic_launcher_background`。
- `mipmap-anydpi/ic_launcher.xml`、`ic_launcher_round.xml`：改為 layer-list，組合背景與前景圖層。
- `values/colors.xml`：移除不再使用的 `launcher_background` 顏色。
- 版本：versionCode 3、versionName 1.1.1；同步更新 README、CHANGELOG、RELEASE_NOTES。

### 問題與解法
- SVG 的 `translate + rotate` 需對應 VectorDrawable 群組的 `translateX/Y`、`rotation`、`pivotX/Y`；換算後矩陣順序與原設計一致。
- VectorDrawable 漸層改用 `aapt:attr` 與 `<gradient>` 實作，維持原本線性與徑向光暈。

### 待辦／已知限制
- 尚未在實機上確認各品牌桌面遮罩、Android 13 主題圖示與舊版 layer-list 的實際呈現。

---

## 2026-10-30 - v1.1.0（App 圖示預覽）

### 目標
依「彩色、活潑、友善、粗線條卡通」方向設計三個 App 圖示版本，先產出預覽檔供確認，暫不改動 App 資源。

### 變更
- 新增 `docs/icon-preview/`：`concept-a.svg`、`concept-b.svg`、`concept-c.svg`，以及對應 1024 × 1024 PNG。
- 新增 `docs/icon-preview/preview.html`：展示三個版本的圓形／squircle／圓角方形遮罩、48／72／96dp 實際尺寸、淺色與深色背景，並可切換 108／72／66／56 尺寸輔助框。
- 三個版本皆以筆記卡搭配鏈結，無笑臉；深青品牌漸層背景、奶油白卡片、黃色書籤、珊瑚橘與亮黃鏈結。
- 核心圖案實測邊界約 54–57dp，符合 66dp 安全區的 85% 目標，且未超出 72dp 裁切區。

### 問題與解法
- 無螢幕可視預覽，改用 Chrome headless 將 SVG 輸出 PNG，並以像素分析檢查核心圖案邊界。

### 待辦／已知限制
- 已選定版本 A，並於同日 v1.1.1 套用；尚未在實機上確認各品牌桌面遮罩與主題圖示效果。

---

## 2026-10-29 - v1.1.0（versionCode 2）

### 目標
調整主畫面操作與提示訊息樣式：修正 Snackbar 未跟隨顯示模式、改為手動設定已讀、排序圖示位置、新增分享選項，並調整標題與回收桶圖示。

### 變更
- `ui/MiniLinkNotesRoot.kt`：Snackbar 改以 `surfaceVariant`／`onSurfaceVariant`／`primary` 上色，跟隨淺色／深色模式。
- `MainActivity.kt`、`AppViewModel.kt`、`data/NotesRepository.kt`：移除開啟連結後自動標為已讀的邏輯；已讀／未讀只由使用者手動切換。
- `ui/notes/NotesScreen.kt`：排序按鈕從 TopAppBar 移到搜尋欄右側；筆記選單在複製選項下方新增「分享網址／分享內容」，以 `ACTION_SEND` 喚起系統分享；回收桶圖示改為 `History`。
- `res/values/strings.xml`：主畫面標題改為「網頁筆記」，新增分享相關字串。
- 版本：versionCode 2、versionName 1.1.0；同步更新 README、CHANGELOG、RELEASE_NOTES。

### 問題與解法
- Material 3 Snackbar 預設使用 `inverseSurface`，深色模式會顯示成亮色，看起來像沒套用主題 → 自訂 Snackbar 顏色。
- 首次 `assembleDebug` 因外掛解析失敗中斷，重試後正常（網路暫時無法取得 Kotlin serialization plugin）。

### 待辦／已知限制
- 尚未在實機或模擬器上操作驗證分享選單與淺色／深色 Snackbar 外觀。

---

## 2026-10-29 - v1.0.0（versionCode 1，建置規範調整）

### 目標
調整開發流程：功能完成後只自動建置 `app-debug.apk`，避免自動產生 prerelease APK。

### 變更
- `AGENTS.md`：明訂開發與測試安裝使用 `debug`；`assembleDebug` 為自動驗證指令；`assemblePrerelease` 僅在使用者明確要求時執行。

### 問題與解法
- 無。

### 待辦／已知限制
- 無。

---

## 2026-10-29 - v1.0.0（versionCode 1）

### 目標
建立「mini網頁筆記」Android App：收集分享的網頁連結與文字片段，支援標籤、已讀狀態、搜尋排序、刪除復原、網頁標題抓取與 JSON 備份。範圍為 P0–P6。

### 變更
- 建置骨架：Gradle Wrapper 8.14.5、AGP 8.13.2、Kotlin 2.2.20、Compose BOM 2025.05.01、compileSdk/targetSdk 36、minSdk 26。
- 套件：Room 2.7.1、DataStore 1.1.7、OkHttp 4.12.0、Jsoup 1.18.3、Kotlin Serialization 1.8.1。
- `data/`：Note/Tag/NoteTagCrossRef 資料表、DAO、Repository、標題抓取、JSON 匯入匯出、DataStore 設定。
- `MainActivity`：接收 `ACTION_SEND`（text/plain、text/html），支援單一任務與重複分享；分享後直接存檔並顯示可編輯的 Snackbar。
- `ui/notes`：搜尋、標籤多選篩選、五種排序、已讀切換、刪除復原；點連結筆記以系統瀏覽器開啟並標為已讀。
- `ui/editor`：連結／文字類型切換、標題／網址／內容編輯、重新取得標題、標籤多選、未儲存變更確認。
- `ui/tags`：標籤新增／改名／刪除（含關聯筆記數量確認）、12 色票、長按拖曳與上／下移排序。
- `ui/trash`：30 天回收筒、復原、永久刪除（二次確認）。
- `ui/settings`、`ui/about`：顯示模式、動態色彩、自動抓標題、JSON 匯入（合併／覆蓋）匯出、版本資訊與隱私說明。
- 12 色標籤調色盤（淺色／深色兩套）、Material 3 主題、Edge-to-edge、自訂 adaptive icon。
- 單元測試：URL 正規化／擷取、分享內容解析、HTML 標題解析、備份格式、回收筒天數。
- 文件：README、CHANGELOG、RELEASE_NOTES、本紀錄。

### 問題與解法
- Jsoup 1.18 的 `parse()` baseUri 不接受 nullable → 傳入空字串。
- Material 3 `ListItem` 在目前版本沒有 `leadingIcon` 參數 → 改用 `leadingContent`。
- HTML 分享的 `document.text()` 會包含 `<title>` → 改用 `document.body().text()`。
- 分享事件可能在 Compose 開始收集前發出，Snackbar 遭丟棄 → 事件流改用帶緩衝的 Channel。
- 標題抓取期間使用者手動改標題的競態 → 抓取完成後重新檢查 `titleEdited` 再決定是否覆寫。
- 刪除篩選中的標籤後可能留下無效篩選 ID → 建立 UI 狀態時與現有標籤取交集。
- 匯入合併原本會把回收筒中的連結視為重複 → 改為只比對未刪除筆記。

### 待辦／已知限制
- 尚未在實機或模擬器上操作驗證（目前僅通過編譯、單元測試、Lint、R8 prerelease 建置）。
- 標題抓取對 JavaScript 動態渲染、需登入或反爬網站會失敗，失敗時顯示網域，可手動重抓。
- 中文搜尋採記憶體 `contains` 比對，資料量極大時效能會下降。
- 資料庫尚無 migration（v1），未來改 schema 需補 Room migration。
- 匯出不含回收筒中的筆記；匯入不還原已刪除狀態。
