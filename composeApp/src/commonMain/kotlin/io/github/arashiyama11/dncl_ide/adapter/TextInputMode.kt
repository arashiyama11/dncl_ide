package io.github.arashiyama11.dncl_ide.adapter

/**
 * モバイル端末向けの入力モード。標準のシステムIMEとアプリ内カスタムパネルを切り替える。
 */
enum class TextInputMode {
    STANDARD,
    CUSTOM
}

/**
 * 専用IMEパネルで提示するコードスニペット・テンプレートの定義。
 *
 * @param id      永続化や分析用に用いる識別子
 * @param title   UI 表示に用いる短いタイトル
 * @param body    エディタへ挿入するテキスト本体
 * @param description  補足説明（一覧表示時のサブタイトル）
 */
data class CustomImeSnippet(
    val id: String,
    val title: String,
    val body: String,
    val description: String
)
