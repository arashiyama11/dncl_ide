function createMonacoOnElement(element, options = {}) {
  if (!(element instanceof HTMLElement)) {
    return Promise.reject(new TypeError('element must be an HTMLElement'));
  }

  const VERSION = '0.54.0'; // 必要なら変更可能にしても良い
  const BASE = `https://cdn.jsdelivr.net/npm/monaco-editor@${VERSION}/min/`;
  const LOADER_URL = `${BASE}vs/loader.js`;

  // --- シングルトンキャッシュ ---
  if (!window.__monacoCdnLoader) {
    window.__monacoCdnLoader = {
      loaderLoaded: false,
      loaderPromise: null,
      workerBlobUrl: null,
      monacoLoadedPromise: null,
    };
  }
  const cache = window.__monacoCdnLoader;

  // スクリプトを読み込むヘルパー
  function loadScriptOnce(src) {
    return new Promise((resolve, reject) => {
      // 既に読み込まれていれば resolve
      const existing = Array.from(document.getElementsByTagName('script')).find(s => s.src === src);
      if (existing) {
        if (existing.getAttribute('data-loaded') === 'true') return resolve();
        // 読み込み中なら終わるのを待つ
        existing.addEventListener('load', () => resolve());
        existing.addEventListener('error', (e) => reject(e));
        return;
      }

      const s = document.createElement('script');
      s.src = src;
      s.async = true;
      s.onload = () => {
        s.setAttribute('data-loaded', 'true');
        resolve();
      };
      s.onerror = (e) => reject(new Error(`Failed to load script ${src}`));
      document.head.appendChild(s);
    });
  }

  // loader.js の読み込み（1回だけ）
  function ensureLoader() {
    if (cache.loaderPromise) return cache.loaderPromise;

    cache.loaderPromise = (async () => {
      // 既に require (AMD loader) があるか？（別のページスクリプトが用意している場合）
      if (typeof window.require === 'function' && typeof window.require.config === 'function') {
        cache.loaderLoaded = true;
        return;
      }
      // そうでなければ loader.js を CDN から読み込む
      await loadScriptOnce(LOADER_URL);
      cache.loaderLoaded = true;
      // loader.js は読み込まれるとグローバルに require を定義する
    })();

    return cache.loaderPromise;
  }

  // Worker Blob URL を作る（1バージョンにつき1つだけ）
  function ensureWorkerBlob() {
    if (cache.workerBlobUrl) return cache.workerBlobUrl;

    const workerMain = `${BASE}vs/base/worker/workerMain.js`;
    const blobCode = `
      self.MonacoEnvironment = { baseUrl: '${BASE}' };
      importScripts('${workerMain}');
    `;
    const blob = new Blob([blobCode], { type: 'text/javascript' });
    cache.workerBlobUrl = URL.createObjectURL(blob);
    return cache.workerBlobUrl;
  }

  // Monaco 本体を require で読み込む（1回だけ）
  async function ensureMonaco() {
    if (cache.monacoLoadedPromise) return cache.monacoLoadedPromise;

    cache.monacoLoadedPromise = (async () => {
      await ensureLoader();

      // require.config を設定（必ず paths.vs を指定）
      if (typeof window.require === 'function' && window.require.config) {
        window.require.config({ paths: { 'vs': `${BASE}vs` } });
      } else {
        throw new Error('AMD require is not available even after loading loader.js');
      }

      // worker の読み込み先を与える
      // getWorkerUrl は Monaco がワーカーを要求すると呼ばれる
      window.MonacoEnvironment = window.MonacoEnvironment || {};
      window.MonacoEnvironment.getWorkerUrl = function (moduleId, label) {
        // Blob URL を返す（Monaco のワーカーが importScripts で workerMain.js を読み込む）
        return ensureWorkerBlob();
      };

      // require で monaco を読み込む（Promise 化）
      await new Promise((resolve, reject) => {
        try {
          window.require(['vs/editor/editor.main'], () => resolve(), (err) => reject(err));
        } catch (e) {
          reject(e);
        }
      });

      // monaco がグローバルに存在する
      if (!window.monaco) throw new Error('monaco did not initialize correctly');

      return window.monaco;
    })();

    return cache.monacoLoadedPromise;
  }

  // 実際に editor を作る
  return (async () => {
    const monaco = await ensureMonaco();

    // デフォルトオプション（必要なら上書き）
    const defaultOptions = {
      value: options.value ?? '// Monaco Editor\n',
      language: options.language ?? 'javascript',
      automaticLayout: options.automaticLayout ?? true,
      minimap: options.minimap ?? { enabled: false },
      ...options
    };

    const editor = monaco.editor.create(element, defaultOptions);

    // 返却オブジェクト
    return {
      monaco,
      editor,
      /** Dispose editor and free resources (optionally revoke blob URL on first dispose) */
      dispose: () => {
        try {
          editor.dispose();
        } catch (e) { /* ignore */ }

        // optional: revoke blob url when all editors are disposed
        // Here we do not track editor counts; if you want to revoke, handle lifecycle externally.
      }
    };
  })();
}
