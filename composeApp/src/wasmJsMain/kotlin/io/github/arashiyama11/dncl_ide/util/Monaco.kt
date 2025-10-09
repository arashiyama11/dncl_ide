package io.github.arashiyama11.dncl_ide.util

import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.promise

external val monaco: Monaco

@OptIn(ExperimentalWasmJsInterop::class, DelicateCoroutinesApi::class)
fun onMonacoLoaded(callback: (Monaco) -> Unit) {
    GlobalScope.promise {
        while (true) {
            try {
                monaco.getModel()
                return@promise callback(monaco)
            } catch (e: Throwable) {
            }

            delay(1)
        }
    }
}


@OptIn(ExperimentalWasmJsInterop::class)
external interface EditorNamespace {
    fun create(domElement: JsAny, options: EditorOptions = definedExternally): Monaco

    fun createModel(
        value: String = definedExternally,
        language: String = definedExternally,
        uri: JsAny = definedExternally
    ): ITextModel

    fun getModels(): JsArray<ITextModel>

    fun getModel(uri: JsAny = definedExternally): ITextModel?
    fun setTheme(theme: String): Unit
}

external interface EditorOptions : JsAny {
    var value: String?
    var language: String?
    var readOnly: Boolean?
    var minimap: JsAny /* object or boolean */?
}

@OptIn(ExperimentalWasmJsInterop::class)
external interface Monaco : JsAny {
    // コンテナサイズが変わったときに呼ぶ
    fun layout(
        dimension: Dimension = definedExternally,
        postponeRendering: Boolean = definedExternally
    ): Unit

    // モデルの文字列を取得 / 設定
    fun getValue(options: JsAny = definedExternally): String
    fun setValue(newValue: String): Unit

    // 現在のテキストモデルを取得 / 差し替え
    fun getModel(): ITextModel?
    fun setModel(model: ITextModel?): Unit

    // 編集内容が変わったときのイベント購読（戻り値は IDisposable 的なもの）
    fun onDidChangeModelContent(listener: (e: IModelContentChangedEvent) -> Unit): IDisposable

    // editor を破棄する
    fun dispose(): Unit

    // オプションの更新
    fun updateOptions(newOptions: EditorOptions): Unit

    // フォーカスやカーソル位置取得・設定
    fun focus(): Unit
    fun getPosition(): Position?
    fun setPosition(pos: Position): Unit

    // ユーティリティ
    fun trigger(source: String?, handlerId: String, payload: JsAny = definedExternally): Unit
}

@OptIn(ExperimentalWasmJsInterop::class)
external interface Dimension : JsAny {
    var width: Int?
    var height: Int?
}

@OptIn(ExperimentalWasmJsInterop::class)
external interface Position : JsAny {
    var lineNumber: Int
    var column: Int
}

@OptIn(ExperimentalWasmJsInterop::class)
external interface ITextModel : JsAny {
    fun getValue(options: JsAny = definedExternally): String
    fun setValue(newValue: String): Unit
    fun uri(): JsAny
    fun dispose(): Unit
    // 必要なら onDidChangeContent 等も追加
}

@OptIn(ExperimentalWasmJsInterop::class)
external interface IModelContentChangedEvent : JsAny {
    val changes: JsArray<JsAny>
    val isFlush: Boolean
    val versionId: Int
}

@OptIn(ExperimentalWasmJsInterop::class)
external interface IDisposable : JsAny {
    fun dispose(): Unit
}