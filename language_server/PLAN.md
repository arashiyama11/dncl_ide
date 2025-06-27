# DNCL Language Server (LSP) 開発計画 ✅

## ✅ 1. プロジェクト目標

- [ ] Kotlin Multiplatform(android,iOS,desktop)でLSPを実装
- [ ] composeAppからのJSONなどを介さない直接の呼び出しを想定とし、副次的にjvm環境でLSPとしても動作するようにする
- [ ] DNCLに対して、構文支援・補完・Notebook対応を段階的に実現

## ✅ 2. 機能フェーズとマイルストーン

### 🟩 フェーズ0: 通信基盤

- [ ] `initialize`, `shutdown`, `textDocument/didOpen` の基本ハンドリング
- [ ] JSON-RPCの入出力とログ処理

### 🟩 フェーズ1: 基本機能

- [ ] Diagnostics（構文・意味エラー表示）
- [ ] Completion（予約語・関数補完）
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

### フェーズ4: Server実装

- [ ] ktorなどを用いて実際にサーバーとしても利用可能にする

---

## ✅ 3. 技術スタックと方針

- [ ] Kotlin Multiplatform (android, iOS, desktop)
- [ ] Gradleビルド（Version Catalog活用)
- [ ] Unit test
- [ ] JSON-RPC層は `kotlinx.serialization` の利用
- [ ] 積極的に `kotlinx.coroutines` を活用

---

## ✅ 4. アーキテクチャ概要

- [ ] `DNCLLanguageServer.kt` にメインロジック集約
- [ ] `TextDocumentService.kt` と `WorkspaceService.kt` に分離
- [ ] `interpreter` はAPI呼び出しに留め、変更しない
- [ ] `NotebookDocumentSync` URIスキーマの決定（例: `dnclnb://path#cell=3`）

---

## ✅ 5. 開発ワークフロー（TDD）

- [ ] レッド: JSON-RPCメッセージを元に失敗するテストを書く
- [ ] グリーン: 最小限のコードでテストを通す
- [ ] リファクタ: コードの改善と構造整理

---

## 🔎 6. 課題・リスクと対応

- [ ] interpreter API の構文エラー復旧能力が弱い
- [ ] 大規模ファイルへの対応 → 差分解析 + キャッシュ化 + コルーチン活用
- [ ] クライアントとの相互接続性（VSCode / IntelliJ）を事前にPoC確認
- [ ] JSON-RPC完全自前実装のリスク → 部分的に既存ライブラリ利用を許容

---

## 🔧 7. 補足チェックリスト

- [ ] LSPログにTRACEレベル設定対応
- [ ] Semantic Versioning の適用（MAJOR.MINOR.PATCH）
- [ ] Contribution Guide：PRテンプレート + Conventional Commits
- [ ] AST/SymbolTableの中間キャッシュ層設計
