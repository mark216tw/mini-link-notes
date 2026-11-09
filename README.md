# mini網頁筆記

在手機上快速收集網頁連結、文字片段與圖片。從瀏覽器或其他 App 分享內容，或手動新增，再用標籤、搜尋與排序整理閱讀清單。

## 功能

- 分享網頁網址、純文字、HTML 或圖片到 App
- 在其他 App 選取文字後，直接透過系統選單存成筆記
- 手動新增連結、文字或圖片筆記
- 自動抓取網頁標題（OkHttp + Jsoup，可關閉）
- 已讀／未讀由使用者手動切換；可一鍵將全部筆記標為已讀
- 點擊連結筆記以系統瀏覽器開啟；點擊筆記上的標籤可直接篩選
- 編輯標題、網址、內容與圖片
- 每則筆記可設定一組提醒日期時間；提醒可啟用／停用，通知後自動停用
- 啟用中的提醒會在筆記清單顯示鈴鐺圖示與日期時間
- 圖片筆記可在編輯頁點擊全螢幕檢視，支援雙指縮放、雙擊放大與拖曳平移
- 複製或分享連結網址、文字內容或圖片
- 多標籤：名稱、鮮豔 12 色調色盤、長按拖曳排序（另有上／下移）
- 搜尋標題、網址、內容；多標籤與未讀篩選
- 排序：加入時間、標題、標籤順序
- 刪除後 Snackbar 復原；回收筒保留 30 天，可清空回收桶
- ZIP 匯出／匯入（合併或覆蓋），含標籤關聯與圖片；可匯入舊版 JSON
- 淺色／深色／跟隨系統、Android 12+ 動態色彩
- 桌面小工具：每個小工具顯示一則筆記，可透過「小工具樣式」視窗調整字型大小（50%～500%）、文字顏色與背景顏色並即時預覽；啟用中的提醒會顯示在最下方；點連結開啟瀏覽器，點其他區域開啟筆記編輯頁

## 文件索引

- [變更紀錄](CHANGELOG.md)
- [版本發布說明](RELEASE_NOTES.md)
- [使用者指南](docs/USER_GUIDE.md)
- [開發紀錄](docs/DEV_LOG.md)
- [建置與發佈](docs/BUILD.md)
- [架構說明](docs/ARCHITECTURE.md)
- [設計說明](docs/DESIGN.md)
- [隱私權說明](docs/PRIVACY.md)
- [授權條款](LICENSE)
- [開發規範](AGENTS.md)

## 建置

需求：JDK 17 以上、Android SDK 36。

```bash
./gradlew assembleDebug        # 開發版
./gradlew assemblePrerelease   # 以 debug key 簽署的測試版
./gradlew assembleRelease      # 正式版（未簽署）
./gradlew test lint            # 單元測試與 Lint
```

- `applicationId`：`com.vibecode.minilinknotes`
- 版本：`1.6.0`（versionCode 12）
- 測試版輸出：`app/build/outputs/apk/prerelease/`

## 權限與隱私

- `INTERNET`：開啟「自動取得網頁標題」時讀取該網頁標題。
- `POST_NOTIFICATIONS`：發送筆記提醒通知。
- `RECEIVE_BOOT_COMPLETED`：重開機後重新排程提醒。
- `SCHEDULE_EXACT_ALARM`：在支援的裝置上準時發送提醒；權限不可用時改用非精確鬧鐘。
- 筆記儲存在本機 Room 資料庫；設定儲存在 DataStore。
- App 開啟 Android 系統備份，資料可能由系統備份機制保存。

## 專案結構

```
app/src/main/java/com/vibecode/minilinknotes/
  data/           Room、DataStore、Repository、標題抓取、備份
  reminder/       提醒排程、通知與開機重新排程
  ui/             Compose 畫面與主題
  widget/         Glance 桌面小工具與設定畫面
  util/           URL、分享解析、時間工具
app/schemas/      Room schema（版控）
docs/DEV_LOG.md   開發紀錄
```

## 備份格式

手動備份為 ZIP：

```text
manifest.json      # schemaVersion 2、標籤、筆記與圖片 metadata
images/<sha256>.<ext>
```

- 圖片以 App 私有檔案保存，ZIP 內只放一份，以 SHA-256 驗證。
- 備份不含筆記提醒設定，匯入後提醒皆為停用。
- 仍可匯入舊版 `schemaVersion 1` 的 JSON 備份。
- 系統備份（Android backup）也會包含圖片，實際是否完整取決於平台配額。
