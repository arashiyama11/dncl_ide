# DNCL Language Server (LSP) 開発計画 ✅

## ✅ 1. プロジェクト目標

- [ ] Kotlin Multiplatform(android, iOS, desktop)でLSPを実装
- [ ] composeAppからの直接呼び出しを想定しつつ、JVM環境でJSON-RPCサーバーとしても動作
- [ ] DNCLに対して、構文支援・補完・Notebook対応を段階的に実現

## ✅ 2. 機能フェーズとマイルストーン

### 🟩 フェーズ0: 通信基盤

- [ ] `initialize`, `initialized`, `shutdown`, `textDocument/didOpen` の基本ハンドリング
- [ ] JSON-RPC の入出力とログ処理

### 🟩 フェーズ1: 基本機能

- [ ] Diagnostics（構文・意味エラー表示）
- [ ] Completion（予約語・関数補完。domain/~~/SuggestionUseCaseを参考に)
- [ ] Hover（関数・キーワードの説明）

### 🟨 フェーズ2: 高度な機能

- [ ] Go to Definition
- [ ] Find References
- [ ] Rename Symbol
- [ ] Format
- [ ] Code Actions / Quick Fix

### 🟦 フェーズ3: Notebook 対応と拡張

- [ ] `.dnclnb` ファイル対応（セル単位のURI仕様）
- [ ] Semantic Tokens（ハイライト高速化）

### 🟪 フェーズ4: Server実装

- [ ] Ktor などを用いた HTTP/WebSocket サーバー機能追加

## ✅ 3. 技術スタックと方針

- [ ] Kotlin Multiplatform (android, iOS, desktop)
- [ ] Gradle ビルド（Version Catalog 活用）
- [ ] Unit test: Kotest + Mockk
- [ ] JSON-RPC 層は `kotlinx.serialization` を利用
- [ ] 積極的に `kotlinx.coroutines` を活用

## ✅ 4. アーキテクチャ概要

- [ ] `DNCLLanguageServer.kt` にメインロジック集約
- [ ] `TextDocumentService.kt` と `WorkspaceService.kt` に分離
- [ ] `interpreter` は API 呼び出しに留め、変更しない
- [ ] `NotebookDocumentSync` URI スキーマ決定（例: `dnclnb://path#cell=3`）

## ✅ 5. 開発ワークフロー（TDD）

- [ ] レッド: JSON-RPC メッセージを元に失敗テストを書く
- [ ] グリーン: 最小限のコードでテストを通す
- [ ] リファクタ: コードの改善と構造整理

## 🔎 6. 課題・リスクと対応

- [ ] interpreter API の構文エラー復旧能力が弱い → Facade で柔軟性を持たせる
- [ ] 大規模ファイル対応 → 差分解析 + キャッシュ化 + コルーチン活用
- [ ] クライアント連携性の PoC（VSCode / IntelliJ）
- [ ] JSON-RPC 自前実装リスク → 通信層のみ既存ライブラリ利用を許容

## 🔧 7. 補足チェックリスト

- [ ] LSP ログに TRACE レベル対応
- [ ] Semantic Versioning 適用（MAJOR.MINOR.PATCH）
- [ ] Contribution Guide：PR テンプレ + Conventional Commits
- [ ] AST/SymbolTable 中間キャッシュ層設計

## ✅ 8. LSP リクエスト／レスポンス仕様

### 8.1 initialize / initialized / shutdown

- [ ] **initialize**
  ```jsonc
  // Request
  {
    "jsonrpc": "2.0",
    "id": 1,
    "method": "initialize",
    "params": {
      "processId": 1234,
      "rootUri": "file:///path/to/workspace",
      "capabilities": { /* クライアント機能一覧 */ }
    }
  }
  // Response
  {
    "jsonrpc": "2.0",
    "id": 1,
    "result": {
      "capabilities": {
        "textDocumentSync": 1,
        "completionProvider": { "resolveProvider": false, "triggerCharacters": ["："] },
        "hoverProvider": true,
        /* 他のサーバー機能 */
      }
    }
  }

``

* [ ] **initialized** (Notification)

  ```json
  {
    "jsonrpc": "2.0",
    "method": "initialized",
    "params": {}
  }
  ```
* [ ] **shutdown**

  ```jsonc
  // Request
  {
    "jsonrpc": "2.0",
    "id": 2,
    "method": "shutdown",
    "params": null
  }
  // Response
  {
    "jsonrpc": "2.0",
    "id": 2,
    "result": null
  }
  ```

### 8.2 textDocument/didOpen / didChange / didClose

* [ ] **didOpen**

  ```jsonc
  {
    "jsonrpc": "2.0",
    "method": "textDocument/didOpen",
    "params": {
      "textDocument": {
        "uri": "file:///a.dncl",
        "languageId": "dncl",
        "version": 1,
        "text": "表示 x\n計算 y"
      }
    }
  }
  ```
* [ ] **didChange**

  ```jsonc
  {
    "jsonrpc": "2.0",
    "method": "textDocument/didChange",
    "params": {
      "textDocument": { "uri": "file:///a.dncl", "version": 2 },
      "contentChanges": [{ "text": "表示 x\n計算 y\nエラー行" }]
    }
  }
  ```
* [ ] **didClose**

  ```jsonc
  {
    "jsonrpc": "2.0",
    "method": "textDocument/didClose",
    "params": { "textDocument": { "uri": "file:///a.dncl" } }
  }
  ```

### 8.3 textDocument/publishDiagnostics

* [ ] **Publish Diagnostics** (Notification from server)

  ```jsonc
  {
    "jsonrpc": "2.0",
    "method": "textDocument/publishDiagnostics",
    "params": {
      "uri": "file:///a.dncl",
      "diagnostics": [
        {
          "range": { "start": { "line": 2, "character": 0 }, "end": { "line": 2, "character": 4 } },
          "severity": 1,
          "message": "未定義の識別子 'エラー行'",
          "source": "dncl-ls"
        }
      ]
    }
  }
  ```

### 8.4 textDocument/completion

* [ ] **Completion Request**

  ```jsonc
  {
    "jsonrpc": "2.0",
    "id": 3,
    "method": "textDocument/completion",
    "params": {
      "textDocument": { "uri": "file:///a.dncl" },
      "position": { "line": 1, "character": 3 }
    }
  }
  ```
* [ ] **Completion Response**

  ```jsonc
  {
    "jsonrpc": "2.0",
    "id": 3,
    "result": {
      "isIncomplete": false,
      "items": [
        { "label": "計算", "kind": 14, "detail": "組み込み関数" },
        { "label": "表示", "kind": 14 }
      ]
    }
  }
  ```

### 8.5 textDocument/hover

* [ ] **Hover Request**

  ```jsonc
  {
    "jsonrpc": "2.0",
    "id": 4,
    "method": "textDocument/hover",
    "params": {
      "textDocument": { "uri": "file:///a.dncl" },
      "position": { "line": 0, "character": 2 }
    }
  }
  ```
* [ ] **Hover Response**

  ```jsonc
  {
    "jsonrpc": "2.0",
    "id": 4,
    "result": {
      "contents": { "kind": "markdown", "value": "**表示**: 画面に値を出力する" },
      "range": { "start": { "line": 0, "character": 0 }, "end": { "line": 0, "character": 4 } }
    }
  }
  ```

### 8.6 他の主要メソッド（例）

* **textDocument/definition**
* **textDocument/references**
* **textDocument/rename**
* **textDocument/formatting**
* **workspace/executeCommand**
* **textDocument/codeAction**
* **textDocument/semanticTokens/full**

（いずれも Request／Response の JSON フォーマットは LSP
仕様に準拠。詳細は [LSP JSON RPC 仕様](https://microsoft.github.io/language-server-protocol/specifications/specification-current/)
を参照）
