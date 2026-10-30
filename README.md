# mini網頁筆記（MiniLinkNotes）

在手機上快速收集網頁連結與文字片段。從瀏覽器或其他 App 分享內容，或手動新增，再用標籤、搜尋與排序整理閱讀清單。所有筆記預設只存在裝置本機。

[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

## 目前版本

- 公開測試版：`1.0.0-prerelease`（versionCode 7）
- 建置類型：`prerelease`
- 特性：啟用 R8 程式碼壓縮與資源縮減，使用 Android Debug 金鑰簽署，僅供測試安裝
- 下載：[GitHub Releases](https://github.com/mark216tw/mini-link-notes/releases/tag/v1.0.0-prerelease)

> `prerelease` 不是正式上架版本，也不是 Google Play 簽署版本。若要正式發佈，請建立自己的 release keystore 並使用 `release` Build Type。

## 功能

- 接收 `ACTION_SEND` 分享的網址、純文字或 HTML 內容
- 支援 `ACTION_PROCESS_TEXT`，在其他 App 選取文字後直接存成筆記
- 手動新增連結或文字筆記，編輯標題、網址與內容
- 自動抓取網頁標題（OkHttp + Jsoup，可關閉、可重新抓取）
- 已讀／未讀由使用者手動切換，可一鍵將全部筆記標為已讀
- 點擊連結以系統瀏覽器開啟；點擊筆記上的標籤可直接篩選
- 多標籤：名稱、12 色調色盤、長按拖曳排序（另有上／下移）
- 搜尋標題、網址、內容；支援多標籤與未讀篩選
- 排序：加入時間、標題、標籤順序
- 刪除後 Snackbar 復原；回收筒保留 30 天，可個別永久刪除或清空
- JSON 匯出／匯入（合併或覆蓋），含標籤關聯
- 淺色／深色／跟隨系統、Android 12+ 動態色彩
- Material 3、Edge-to-edge、自訂 Adaptive Icon 與 Android 13 主題圖示

## 文件

| 文件 | 內容 |
| --- | --- |
| [docs/USER_GUIDE.md](docs/USER_GUIDE.md) | 安裝、操作流程、常見問題 |
| [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md) | 系統架構、模組責任、資料流、技術選型 |
| [docs/DESIGN.md](docs/DESIGN.md) | 系統設計、資料模型、狀態管理、關鍵流程 |
| [docs/BUILD.md](docs/BUILD.md) | 開發環境、建置指令、簽署與發佈流程 |
| [docs/PRIVACY.md](docs/PRIVACY.md) | 權限、資料儲存與隱私說明 |
| [docs/DEV_LOG.md](docs/DEV_LOG.md) | 開發過程與版本紀錄 |
| [CHANGELOG.md](CHANGELOG.md) | 版本異動 |
| [RELEASE_NOTES.md](RELEASE_NOTES.md) | 給使用者的版本說明 |

## 建置

需求：JDK 17 以上、Android SDK 36。

```bash
./gradlew assembleDebug        # 開發版
./gradlew assemblePrerelease   # R8 + debug key 簽署的測試版
./gradlew assembleRelease      # 正式版（未簽署）
./gradlew test lint            # 單元測試與 Lint
```

- `applicationId`：`com.vibecode.minilinknotes`
- `minSdk` 26、`targetSdk` / `compileSdk` 36
- 主要輸出：
  - Debug：`app/build/outputs/apk/debug/app-debug.apk`
  - Pre-release：`app/build/outputs/apk/prerelease/app-prerelease.apk`

詳細設定與發佈步驟請見 [docs/BUILD.md](docs/BUILD.md)。

## 專案結構

```
app/src/main/java/com/vibecode/minilinknotes/
  data/           Room、DataStore、Repository、標題抓取、JSON 備份
  ui/             Compose 畫面、導覽、主題
  util/           URL、分享解析、HTML 標題、時間工具
app/schemas/      Room schema（版控）
app/src/test/     單元測試
docs/             使用、架構、設計、建置、隱私與開發紀錄
```

## 權限與隱私

- 僅宣告 `INTERNET` 權限，用於「自動取得網頁標題」時讀取目標網頁。
- 筆記儲存在本機 Room 資料庫；設定儲存在 DataStore。
- 沒有帳號、沒有雲端同步、沒有分析或廣告 SDK。
- App 開啟 Android 系統備份，資料可能由系統備份機制保存。

完整說明請見 [docs/PRIVACY.md](docs/PRIVACY.md)。

## 授權

本專案採用 [MIT License](LICENSE)。

Copyright (c) 2026 mark216tw

第三方開源元件（Jetpack Compose、Room、DataStore、OkHttp、Jsoup、Kotlin Serialization 等）依其各自授權條款提供。
