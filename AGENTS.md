## LLMエージェント向け開発ガイド

### はじめに
You are a coding assistant. Internally, you should think and reason in English, step by step, using logical deduction and programming knowledge. However, your final answer should be output in Japanese, clearly and concisely. If a code snippet is included, make sure it's well-formatted and includes brief explanations in Japanese.

このドキュメントは、あなたがこのリポジトリで効率的かつ正確に開発作業を進めるためのガイドです。以下の指示に従い、高品質なコード生成とタスク遂行を目指してください。

**最重要事項:**

*   **Desktopターゲットを優先:** このプロジェクトはKotlin Multiplatform (KMP) でAndroid、iOS、Desktopに対応していますが、あなたの作業は主に **`desktop` ターゲット** で行ってください。これにより、環境構築の複雑さを軽減し、迅速なフィードバックサイクルを実現します。
*   **明確な指示を待つ:** 不明瞭な点や判断に迷う場合は、自己判断で進めずにユーザーに確認を求めてください。
*   **計画と段階的な実行:** 複雑なタスクに取り組む際は、まず関連情報を収集・分析し、実行計画を立ててユーザーに提示してください。承認を得てから実装に着手しましょう。

### プロジェクト概要

*   **技術スタック:** Kotlin Multiplatform, Compose Multiplatform
*   **対応プラットフォーム:** Android, iOS, Desktop (あなたの主戦場は `desktop`)
*   **主要モジュール:**
    *   `composeApp`: UI層。Android, iOS, DesktopのUIを含みます。
        *   Desktopアプリのエントリーポイント: `io.github.arashiyama11.dncl_ide.MainKt`
    *   `domain`: ビジネスロジック層。
    *   `interpreter`: DNCL言語のインタープリタ機能。

### 推奨ワークフロー

1.  **理解と計画:**
    *   タスクの目的と要件を正確に理解してください。
    *   関連するファイルや既存コードを十分に調査してください。 (`ls`, `read_files`, `grep` ツールを活用)
    *   変更計画を立て、ユーザーに提示し承認を得てください。 (`set_plan` ツールを使用)
2.  **実装 (Desktopターゲット):**
    *   `desktop` ターゲットでコードを実装・修正してください。
    *   テストを作成または更新し、`desktopTest` でパスすることを確認してください。 (テスト駆動開発を推奨)
3.  **確認と調整:**
    *   コードが要件を満たし、スタイルガイドライン (もしあれば) に準拠していることを確認してください。
    *   コンパイルエラーがないことを `compileKotlinDesktop` で確認してください。
    *   Lint (`lintFix`) を実行し、問題を修正してください。
4.  **コミット:**
    *   変更内容を明確に記述したコミットメッセージと共に提出してください。 (`submit` ツールを使用)

### よく使うGradleタスク

Gradleタスクを実行する際は、以下のオプションを付与することを **強く推奨** します。これにより、Gradleデーモンに起因する問題を避け、コンソール出力を読みやすくします。

`--no-daemon --console=plain`

*   **コンパイルエラーの確認 (Desktop):**
    ```bash
    ./gradlew :composeApp:compileKotlinDesktop --no-daemon --console=plain
    ./gradlew :domain:compileKotlinDesktop --no-daemon --console=plain
    ./gradlew :interpreter:compileKotlinDesktop --no-daemon --console=plain
    # またはプロジェクト全体
    ./gradlew compileKotlinDesktop --no-daemon --console=plain
    ```

*   **ユニットテストの実行 (Desktop):**
    *   **すべてのユニットテスト:**
        ```bash
        ./gradlew desktopTest --no-daemon --console=plain
        ```
    *   **特定のクラスのユニットテスト (例: domainモジュール):**
        ```bash
        ./gradlew :domain:desktopTest --tests "io.github.arashiyama11.dncl_ide.domain.usecase.ExecuteUseCaseTest" --no-daemon --console=plain
        ```
        *(注意: 上記のクラス名はあくまで例です。実際にテストしたいクラス名に置き換えてください。)*

*   **Lint (コード整形とチェック):**
    ```bash
    ./gradlew lintFix --no-daemon --console=plain
    ```
    *(`lintFix` は自動修正を試みます。修正内容を確認してください。)*

### コードスタイルと規約
*   現時点では、既存のコードスタイルを踏襲するようにしてください。

### 注意事項

*   **既存コードの尊重:** 大規模なリファクタリングや設計変更は、事前にユーザーと合意形成を行ってください。
*   **依存関係の追加・変更:** 新しいライブラリを追加したり、既存の依存関係のバージョンを変更する場合は、必ずユーザーに理由と影響を説明し、承認を得てください。
*   **エラー発生時:** エラーメッセージを正確に読み、原因を特定するように努めてください。解決が難しい場合は、エラー情報と共にユーザーに報告してください。

このガイドがあなたの開発作業の一助となることを願っています。協力して素晴らしい成果を出しましょう！
