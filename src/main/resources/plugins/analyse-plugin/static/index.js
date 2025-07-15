'use strict';

/* ══════════════════ ЗАГРУЖАЕМ БИБЛИОТЕКИ ═════════════════════ */
/* Если marked / DOMPurify ещё не подключены – загружаем их «на лету». */
(function loadMarkdownLibs () {
  const head = document.head || document.documentElement;

  function inject(src) {
    return new Promise((res, rej) => {
      // уже имеется?
      if ([...document.scripts].some(s => s.src.includes(src))) return res();
      const s = document.createElement('script');
      s.src      = src;
      s.async    = false;          // порядок важен (сначала marked, потом DOMPurify)
      s.onload   = () => res();
      s.onerror  = () => rej(new Error(`Не удалось загрузить ${src}`));
      head.appendChild(s);
    });
  }

  /* marked → DOMPurify (порядок) */
  inject('https://cdn.jsdelivr.net/npm/marked/marked.min.js')
    .then(() => inject('https://cdn.jsdelivr.net/npm/dompurify/dist/purify.min.js'))
    .catch(console.error);
}) ();

/* ══════════════════ ПЕРЕВОДЫ ═════════════════════════════════ */
allure.api.addTranslation('ru', { tab: { analyse: { name: 'Анализ ИИ' } } });
allure.api.addTranslation('en', { tab: { analyse: { name: 'Analyse AI' } } });
allure.api.addTranslation('de', { tab: { analyse: { name: 'Analyse KI' } } });

/* ══════════════════ РЕГИСТРИРУЕМ ВКЛАДКУ ═════════════════════ */
allure.api.addTab('analyse', {
  title : 'tab.analyse.name',
  icon  : 'fa fa-cloud',
  route : 'analyse(/)',

  onEnter() {

    /* UUID, если путь вида …/allure/reports/<uuid>/… */
    const reportUuid = (() => {
      const m = window.location.pathname.match(/allure\/reports\/([^/]+)/);
      return m ? m[1] : null;
    })();

    /* ───── VIEW ────────────────────────────────────────────── */
    const AnalyseView = Backbone.Marionette.View.extend({
      className : 'pane__section',

      template() {
        return `
          <div style="padding:20px">
            <h2>Анализ отчёта Allure при помощи ИИ</h2>

            <button class="btn btn-analyse js-refresh">
              <i class="fa fa-refresh"></i> Обновить
            </button>

            <h3 style="margin-top:55px">Результаты анализа</h3>

            <div class="analysis-box js-box"
                 style="margin-top:10px;border:1px solid #444;border-radius:4px;
                        min-height:220px;padding:10px">
              <span class="analysis-empty">Данные отсутствуют…</span>
            </div>
          </div>`;
      },

      events : { 'click .js-refresh' : 'loadData' },

      onRender() { this.loadData(); },

      /* ───── ЗАГРУЗКА ДАННЫХ (widgets → API) ───────────────── */
      loadData() {
        const box = this.$('.js-box');
        box.html('<i class="fa fa-spinner fa-spin"></i> Загрузка…');

        const render = data => this.renderTable(data);

        if (reportUuid) {
          /* 1) пробуем widgets/analysis.json */
          fetch('widgets/analysis.json')
            .then(r => r.ok ? r.json()
                            : r.status === 404 ? this.loadViaApi()
                                               : Promise.reject())
            .then(render)
            .catch(() => render([]));                // ошибка → «данных нет»
        } else {
          /* отчёт без uuid — сразу API */
          this.loadViaApi().then(render).catch(() => render([]));
        }
      },

      /* ───── API /api/analysis/latest ───────────────────────── */
      loadViaApi() {
        return fetch((allure.baseUrl || '') + 'api/analysis/latest')
          .then(r => r.status === 404 ? [] : r.json());
      },

      /* ───── ВЫВОД ТАБЛИЦЫ ──────────────────────────────────── */
      renderTable(data) {
        const box = this.$('.analysis-box');
        if (!data || !data.length) {
          box.html('<span class="analysis-empty">Данные отсутствуют…</span>');
          return;
        }

        /* Markdown → HTML (+ очистка) */
        const html = data
          .map(({ message = '' }) => {
            try {
              return window.DOMPurify
                     ? DOMPurify.sanitize(marked.parse(message))
                     : marked.parse(message);               // fallback
            } catch (e) {
              console.error('Markdown parse error:', e);
              return message.replace(/\n/g, '<br>');
            }
          })
          .join('');

        box.html(html);
      }
    });

    /* ───── LAYOUT ──────────────────────────────────────────── */
    class AnalyseLayout extends allure.components.AppLayout {
      getContentView() { return new AnalyseView(); }
    }
    return new AnalyseLayout();
  }
});

/* ══════════════════ ДОБАВЛЯЕМ КАПЛЮ CSS ══════════════════════ */
(function addAnalyseStyles () {
  if (document.getElementById('analyse-css')) return;          // один раз
  const css = `
    .analysis-box p        { margin:0 0 8px }
    .analysis-box ul       { margin:0 0 8px 22px; list-style:disc }
    .analysis-box ol       { margin:0 0 8px 22px }
    .analysis-box pre,
    .analysis-box code     { background:#282c34; padding:4px 6px; border-radius:4px }
    .analysis-box blockquote{ margin:0 0 8px; padding-left:12px; border-left:4px solid #888 }
  `;
  const style = Object.assign(document.createElement('style'), {
    id   : 'analyse-css',
    textContent : css
  });
  (document.head || document.documentElement).appendChild(style);
}) ();
