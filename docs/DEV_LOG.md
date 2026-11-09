# 開發紀錄

最新紀錄放最上面。每次開發工作都要更新，不要事後補寫。

---

## 2026-11-09 - v1.7.0（versionCode 13，設定頁提醒權限）

### 目標
在設定頁顯示通知權限與精確提醒的目前狀態，並提供前往系統設定的入口，讓使用者能自行修正提醒無法顯示或延遲的問題。

### 變更
- 新增 `reminder/ReminderPermissions.kt`：集中判斷通知是否啟用、Android 13+ 通知權限、精確鬧鐘是否可用，並提供開啟系統通知設定與精確鬧鐘設定的 Intent。
- `ui/settings/SettingsScreen.kt`：新增「提醒」區塊，顯示通知權限與精確提醒狀態；通知未授予時先請求權限，拒絕後開啟通知設定；精確提醒可前往系統設定；回到畫面時重新讀取狀態。
- `res/values/strings.xml`：新增提醒區塊標題、狀態、按鈕與失敗提示文案。
- `app/build.gradle.kts`：版本升為 1.7.0（versionCode 13）。
- 同步更新 `README.md`、`CHANGELOG.md`、`RELEASE_NOTES.md`、`docs/BUILD.md`、`docs/USER_GUIDE.md`、`docs/DESIGN.md`、`docs/ARCHITECTURE.md`。
- 建置 `1.7.0-prerelease` APK，發佈 GitHub Pre-release（v1.7.0）。

### 問題與解法
- 從系統設定返回後狀態不會自動更新 → 以 `LifecycleResumeEffect` 在畫面恢復時重新讀取。
- Android 13+ 未授予通知權限時直接開設定頁體驗較差 → 先跳系統權限請求，拒絕後再開啟 App 通知設定。
- 部分裝置的系統設定 Activity 可能不存在 → 以 `runCatching` 包裹並顯示提示。
- 設定頁圖示在支援文字換行時被 `ListItem` 視為三行項目而置頂，與標題不對齊 → 將圖示與標題放入同一個 headline `Row`，並把「前往設定」按鈕也放在同一列。

### 待辦／已知限制
- 尚未在實機驗證各品牌系統設定頁的跳轉與返回後狀態更新。
- 通知頻道被個別關閉時，僅顯示 App 層級通知狀態，未細分頻道狀態。

---

## 2026-11-09 - v1.6.0（versionCode 12，筆記提醒）

### 目標
讓每則筆記可設定一組提醒日期時間，時間到以推播通知提醒；提醒可啟用／停用，通知後自動停用，筆記清單以鈴鐺與時間標示啟用中的提醒。

### 變更
- `data/Model.kt`：`NoteEntity` 新增 `reminderAt`、`reminderEnabled` 與索引；新增 `ReminderEntry` 查詢投影。
- `data/AppDatabase.kt`：資料庫版本升至 4，新增 `MIGRATION_3_4`。
- `data/NotesDao.kt`：新增提醒查詢、同步排程查詢與 `setReminderEnabled`。
- 新增 `reminder/ReminderScheduler.kt`：AlarmManager 排程，Android 12+ 優先精確鬧鐘、不可用時退回非精確；支援取消與重新同步。
- 新增 `reminder/ReminderNotifier.kt`：建立「筆記提醒」通知頻道與推播通知，點擊開啟對應筆記編輯頁。
- 新增 `reminder/ReminderReceiver.kt`：收到鬧鐘後顯示通知並自動停用提醒。
- 新增 `reminder/ReminderRescheduleReceiver.kt`：開機、App 更新、時間或時區變更後重新排程。
- `MiniLinkNotesApp.kt`：建立通知頻道、注入排程器，啟動時同步提醒。
- `data/NotesRepository.kt`：新增／更新筆記時重設鬧鐘；刪除取消鬧鐘並停用提醒；清空回收筒與過期清除後重新同步。
- `AppViewModel.kt`、`MainActivity.kt`：注入排程器；匯入備份前取消鬧鐘、匯入後重新同步。
- `ui/editor/EditorScreen.kt`：新增提醒開關、日期與時間選擇、通知權限請求與時間驗證。
- `ui/notes/NotesScreen.kt`：啟用中的提醒顯示鈴鐺圖示與日期時間。
- `widget/LinkNoteWidget.kt`：啟用中的提醒在小工具最下方顯示鈴鐺圖示與日期時間，日期時間字型較小。
- `AndroidManifest.xml`：新增 `POST_NOTIFICATIONS`、`RECEIVE_BOOT_COMPLETED`、`SCHEDULE_EXACT_ALARM` 與兩個 Receiver。
- `res/drawable/ic_notification.xml`：新增通知小圖示。
- `res/values/strings.xml`：新增提醒相關文案。
- 新增 `util/ReminderUtils.kt` 與 `ReminderUtilsTest`：預設時間、日期／時間組合與有效性測試。
- 版本升為 1.6.0（versionCode 12）。

### 問題與解法
- Android 14 起精確鬧鐘權限預設不開放 → 以 `canScheduleExactAlarms()` 判斷，不可用時改用非精確鬧鐘，仍可收到提醒。
- 裝置關機或 App 被強制停止時鬧鐘會消失 → 開機與 App 啟動時重新同步；已逾期且仍啟用的提醒立即補發通知後停用。
- 刪除筆記後鬧鐘仍可能存在 → 軟刪除時取消鬧鐘並停用提醒，永久刪除時再次取消。
- 匯入覆蓋會清空筆記，舊鬧鐘可能殘留 → 匯入前取消全部鬧鐘，匯入後重新同步。
- 通知權限可能在啟用後被撤銷 → 發送失敗時仍將提醒視為已消耗並停用，避免重複觸發。
- 既有安裝升級後啟動閃退 → `MIGRATION_3_4` 已建立但漏加進 `addMigrations`，補上註冊後可正常從 schema 3 升級至 4。

### 待辦／已知限制
- 尚未在實機驗證精確／非精確鬧鐘的延遲、各品牌省電機制與重開機後補發行為。
- 手動備份不含提醒設定，匯入後筆記提醒皆為停用。
- Android 13 以下若系統通知被關閉，需自行到系統設定開啟，App 無法以對話框引導。

---

## 2026-11-09 - v1.5.1（versionCode 11，小工具背景顏色與樣式調整）

### 目標
讓桌面小工具可自訂淺色背景，並精簡樣式設定介面：按鈕更名為「小工具樣式」、字型大小移到最上方、文字與背景顏色以色塊呈現。

### 變更
- `ui/theme/Color.kt`：新增 7 個小工具淺色背景常數。
- `widget/WidgetStyle.kt`：新增 `WidgetBackgroundColor`（自動＋7 色）與 `fromPreference()`。
- `widget/LinkNoteWidget.kt`：新增 `background_color` 設定，套用自訂背景；深色模式下自訂淺底搭配「自動」文字時改用深色文字。
- `widget/LinkNoteWidgetReceiver.kt`：刪除小工具時一併清除背景設定。
- `widget/configure/LinkNoteWidgetConfigureViewModel.kt`、`LinkNoteWidgetConfigureActivity.kt`：新增背景顏色狀態、初始化與寫入。
- `widget/configure/LinkNoteWidgetConfigureScreen.kt`：設定順序改為字型大小 → 文字顏色 → 背景顏色 → 預覽；顏色選項改為圓形色塊、不顯示文字，已選色塊只在中央顯示勾選圖示；「小工具樣式」按鈕縮短為文字長度並靠右；預覽同步套用背景與深色文字邏輯。
- `res/values/strings.xml`：按鈕文字改為「小工具樣式」，新增背景顏色相關字串。
- `README.md`：新增並補齊文件索引連結，更新小工具說明與版本。
- `WidgetStyleTest.kt`：新增背景色偏好解析測試。
- `docs/BUILD.md`：更新版本與發佈指令。
- 版本升為 1.5.1（versionCode 11），建置 `prerelease` APK 作為 GitHub Pre-release。

### 問題與解法
- 「自動」沒有實際顏色可顯示 → 以色塊漸層呈現，並依首色亮度決定勾選圖示顏色。
- 淺色背景在深色模式搭配自動淺色文字會難以辨識 → 選自訂背景時自動文字固定使用深色。

### 待辦／已知限制
- 尚未在實機確認色塊在不同螢幕尺寸與 TalkBack 下的操作體驗。

---

## 2026-11-08 - v1.5.0（versionCode 10，圖片放大檢視）

### 目標
讓圖片筆記可以在編輯頁點擊圖片後全螢幕放大查看細節，支援縮放、平移與關閉，不需額外依賴。

### 變更
- 新增 `ui/components/ImageZoomViewer.kt`：`ImageZoomDialog` 全螢幕檢視器，支援雙指縮放（1x～5x）、雙擊放大／復原、放大後拖曳平移，右上角關閉與系統返回鍵關閉。
- `data/ImageStorage.kt`：抽出 `decodeScaled`，套用 EXIF 旋轉並以 `inSampleSize` 限制解碼尺寸；新增 `fullImageBitmap` 供檢視器載入原圖（長邊上限 2048px，避免 OOM）。
- `ui/editor/EditorScreen.kt`：圖片預覽改為可點擊，點擊後開啟 `ImageZoomDialog`。
- `ui/theme/Color.kt`：新增檢視器遮罩與內容色 `ImageViewerScrim`、`ImageViewerContent`。
- `res/values/strings.xml`：新增 `image_viewer_open`、`image_viewer_close`、`image_viewer_failed`。
- 版本升為 1.5.0（versionCode 10）。

### 問題與解法
- 原圖可能很大（匯入上限 25MB）→ 解碼前先讀取尺寸並以 2 的倍數取樣，長邊限制在 2048px，載入時顯示進度。
- 縮放後圖片可能被拖出畫面 → 依容器尺寸與 `ContentScale.Fit` 計算最大位移並夾住 `translationX/Y`。
- 檢視器需要同時處理雙擊與縮放手勢 → 以獨立的 `pointerInput` 偵測雙擊，再搭配 `transformable` 處理縮放與平移。

### 待辦／已知限制
- 回收筒與筆記清單的圖片依需求維持不支援點擊放大；清單點圖片仍開啟編輯頁。
- 尚未在實機驗證高解析度圖片、摺疊機與平板上的縮放體驗。

---

## 2026-11-08 - v1.4.1（versionCode 9，標籤配色與小工具樣式對話框）

### 目標
讓標籤在筆記清單中更醒目，將標籤移到標題下方，並把小工具的文字樣式控制收進彈出視窗，減少設定頁佔用的空間。

### 變更
- `ui/theme/Color.kt`：12 組 `TagColors` 改為高飽和色盤，淺色模式使用較深的 600～800 色階維持可讀性，深色模式使用較亮的 300～400 色階。
- `ui/notes/NotesScreen.kt`：`TagPill` 底色透明度由 0.16 提高到 0.20；`NoteCard` 的標籤區塊移到標題正下方，順序改為標題 → 標籤 → 網域 → 摘要 → 時間。
- `widget/configure/LinkNoteWidgetConfigureScreen.kt`：原本內嵌的文字樣式區塊改為全寬「小工具文字樣式」按鈕，點擊後以 `AlertDialog` 顯示文字顏色、字型大小與即時預覽，內容可垂直捲動。
- `res/values/strings.xml`：`widget_style_title` 改為「小工具文字樣式」。
- 版本升為 1.4.1（versionCode 9）。

### 問題與解法
- 對話框內容可能因字型放大或圖片預覽而超出高度 → 以 `verticalScroll` 包住內容，避免元件被截斷。

### 待辦／已知限制
- 尚未在實機確認各螢幕尺寸下對話框的捲動與 Slider 操作體驗。
- 標籤配色僅以亮度與可讀性原則調整，尚未執行無障礙對比度檢測。

---

## 2026-11-08 - v1.4.0（versionCode 8，圖片筆記與 Widget 自訂）

### 目標
修正 Widget 首次設定與重新選取不更新、恢復標籤新增入口，加入每個 Widget 獨立的文字色彩與 50%～500% 字型縮放，並新增單張圖片筆記的完整支援。

### 變更
- Widget 改為在 Glance composition 內以 `currentState<Preferences>()` 觀察設定、以 Room Flow 觀察筆記，修正首次設定與重新選取不更新；加入 loading／unconfigured／missing／ready 狀態。
- 每個 Widget instance 可設定文字色盤（Auto、黑、白、深灰、淺灰、藍、青綠、黃）與 50%～500% 字型（每 10% 一級），設定頁提供即時預覽。
- 標籤管理恢復新增 FAB，並補齊重複名稱與一般儲存失敗的錯誤處理。
- 新增 `NoteType.IMAGE`、圖片欄位與 Room schema 3；`ImageStorage` 以私有檔案保存原圖、產生 512px WebP 縮圖、支援 SHA-256、EXIF 旋轉與孤兒清理。
- 編輯器新增圖片類型、Photo Picker、預覽、替換與移除；主清單、回收筒、Widget 與 Widget 設定頁顯示縮圖。
- 新增圖片分享輸出（FileProvider）與 `ACTION_SEND image/*` 匯入。
- 手動備份改為 ZIP（manifest v2 + images），支援舊版 JSON 匯入、路徑與大小驗證、SHA-256 驗證與 REPLACE 舊圖清理。
- 版本升為 1.4.0（versionCode 8）。

### 問題與解法
- Widget 的 `provideGlance()` 在 `provideContent()` 外只讀一次 state 與 Room，活躍 session 會持續捕獲舊值 → 改用 composition 內的可觀察來源。
- Glance 圖片載入沒有可靠的 suspend/effect 保證 → 在背景 composition 內使用已產生的縮圖 Bitmap，避免阻塞 UI 與 Binder 過大。
- Coil 3.6.3 需要 compileSdk 37／AGP 9，與專案基線衝突 → 不升級 SDK/AGP，改用專案內縮圖與 `BitmapFactory` 載入。
- 圖片檔案與 Room transaction 無法一起 rollback → 先寫入檔案，DB 失敗時刪除新檔；換圖成功後才刪舊檔；啟動時清理逾時孤兒。

### 待辦／已知限制
- 需在實機 Launcher 驗證多 Widget instance、重新設定與圖片縮圖。
- 圖片匯入上限 25MB，ZIP 匯入上限 400MB；尚未提供裁切或壓縮選項。
- 未新增 instrumentation／UI 自動化測試；目前以編譯、單元測試與 Lint 驗證。

---

## 2026-11-08 - v1.3.0（versionCode 7，桌面小工具）

### 目標
新增桌面小工具，讓使用者為每個小工具選擇一則筆記，顯示標題、連結與內容，並可點擊連結以預設瀏覽器開啟。

### 變更
- `gradle/libs.versions.toml`、`app/build.gradle.kts`：新增 Glance 1.2.0；版本升為 1.3.0（versionCode 7）。
- `data/NotesDao.kt`、`data/NotesRepository.kt`：新增 `getActiveNoteEntity` 與 `observeActiveNoteEntities`，只取未刪除資料。
- `widget/LinkNoteWidget.kt`：Glance 小工具，讀取每個 instance 的 note ID，顯示標題、網址與內容；純文字資料不顯示連結；點擊網址以瀏覽器開啟，點擊其他區域開啟該筆記編輯頁；資料失效時提供重新選擇。
- `MainActivity.kt`、`ui/MiniLinkNotesRoot.kt`：新增 `EXTRA_NOTE_ID` 啟動參數，小工具可導覽到指定筆記的編輯頁，並避免轉螢幕時重複導覽。
- `widget/LinkNoteWidgetReceiver.kt`：接收更新，移除小工具時清除選取狀態。
- `widget/configure/`：設定 Activity、ViewModel 與 Compose 選單，支援搜尋、單選與重新設定。
- `MiniLinkNotesApp.kt`：監聽未刪除筆記資料流，資料變更時更新所有小工具。
- `AndroidManifest.xml`、`res/xml/link_note_widget_info.xml`、`res/layout/link_note_widget_loading.xml`、`strings.xml`、`colors.xml`、`values-night/colors.xml`：小工具宣告與資源。
- 更新 README、CHANGELOG、RELEASE_NOTES 與 About 開源元件字串。

### 問題與解法
- Glance 1.2.0 的 `provideContent`、`updateAll` 是擴充函式，需個別 import。
- `updateAppWidgetState` 只有 `(context, glanceId, updateState)` 多載；誤加 `stateDefinition` 會編譯失敗。
- `ColorProvider(@ColorRes)` 在 1.2.0 標記為 `@RestrictTo(LIBRARY_GROUP)`，Lint 會報 RestrictedApi。
- Glance 的 `TextDecoration` 位於 `androidx.glance.text`，不是 Compose UI 的型別。
- `clickable` 需從 `androidx.glance.action` 匯入。

### 修正（實機回饋）
- 小工具沒有底色、文字不清楚：Glance 轉換 RemoteViews 時只接受 `FixedColorProvider`、`ResourceColorProvider`、`DayNightColorProvider`，自訂 `ColorProvider` 會拋出 `Unexpected text/background color`，導致小工具無法正常顯示。改用 `androidx.glance.color.ColorProvider(day, night)` 與 `androidx.glance.appwidget.background(day, night)`，並補上 `appWidgetBackground()` 以支援 Android 12+ 圓角。
- 設定頁下方按鈕被導覽列遮住：`bottomBar` 加上 `navigationBarsPadding()` 與 `imePadding()`。
- 點擊小工具開啟該筆記編輯頁：小工具根節點加上 `clickable`，以 `EXTRA_NOTE_ID` 啟動 `MainActivity`；網址文字維持以瀏覽器開啟。

### 待辦／已知限制
- 尚未在實機驗證各廠牌 launcher 的小工具尺寸、圓角與重新設定流程。
- 未新增小工具 UI 自動化測試；目前以編譯、單元測試與 Lint 驗證。
- 小工具使用固定日夜配色，未跟隨 App 的動態色彩設定。

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
