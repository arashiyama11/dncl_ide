# DNCL IDE

DNCL IDEは共通テスト情報で用いられる擬似プログラミング言語「DNCL」の統合開発環境です
https://www.canva.com/design/DAG0nhGRe7w/1v3MxG1X_RDAeI5vwcFiQQ/edit?utm_content=DAG0nhGRe7w&utm_campaign=designshare&utm_medium=link2&utm_source=sharebutton

## Packages

リリースのAssetsからIDE, CLI, Language Serverをダウンロードできます。
IDEは`dncl-ide-<platform>-<version>.<ext>`と`dncl-ide-android-universa-<version>-release.apk`
で、linux, macOS, Windows, Androidのインストーラが提供されています。
またWeb版もGithub Pagesで公開しています。AndroidはPlayStoreリリース作業中です。

CLIは`dncl-cli-<platform>-<arch>-<version>.<ext>`で、linux, macOS,
Windowsのネイティブバイナリと、jar形式のバイナリが提供されています。

Language Serverは`language-server-all-<version>.jar`で、jar形式のバイナリを提供しています

## Usage

### IDE

Releaseで配布されているインストーラからインストールしてください。

### CLI

CLIはJava 17以上が必要で、以下のコマンドで実行します。
引数なしでREPL, 引数でファイルを指定するとそのファイルを実行します。
ネイティブバイナリを実行する際OSからの警告が発生する場合があります。

### Install from GitHub Releases

```
alias dncl="java -jar path/to/dncl-cli-all-<version>.jar"
# or
alias dncl=path/to/dncl-cli-<platform>-<arch>-<version>.<ext>

dncl # REPL
dncl path/to/file.dncl # run file
```

### Install with mise

- native binary

```toml
[tools."github:arashiyama11/dncl_ide"]
version = "latest"
bin = "dncl"

[tools."github:arashiyama11/dncl_ide".platforms]
linux-x64 = { asset_pattern = "dncl-cli-linux-amd64-*.kexe" }
macos-arm64 = { asset_pattern = "dncl-cli-macos-arm64-*.kexe" }
windows-x64 = { asset_pattern = "dncl-cli-windows-x86_64-*.exe" }

[tasks.dncl-repl]
run = "dncl"

[tasks.dncl-run-file]
run = "dncl {{arg(name='file')}}"
```

- jar binary

```toml
[tools.java]
version = "temurin-17" # or higher

[tools."github:arashiyama11/dncl_ide"]
version = "latest"
asset_pattern = "dncl-cli-all-*.jar"
bin = "dncl"

[tasks.dncl-repl]
run = "java -jar $(which dncl)"

[tasks.dncl-run-file]
run = "java -jar $(which dncl) {{arg(name='file')}}"
```

### Language Server

言語サーバーはJava 17以上が必要で、以下のコマンドで標準入出力で起動します。

```

java -jar language-server-all-<version>.jar

```

環境変数`DNCL_LS_LOG_FILE`でログファイルのパスを指定できます。

```

DNCL_LS_LOG_FILE=./dncl-ls.log java -jar language-server-all-<version>.jar

```
