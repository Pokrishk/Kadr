(function () {
  const root = document.documentElement;
  const settings = window.__userSettings || null;
  const fontStacks = window.__userFontStacks || {};

  if (!root.dataset.theme) {
    root.dataset.theme = 'system';
  }

  applyAppearance(settings, fontStacks);

  document.addEventListener('DOMContentLoaded', () => {
    if (settings && settings.authenticated) {
      setupFilterPersistence(settings);
    }
  });

  function applyAppearance(settings, fontStacks) {
    const theme = normalizeTheme(settings?.theme);
    root.dataset.theme = theme;

    const fontFamilyKey = settings?.fontFamily || 'system-ui';
    const fontStack = fontStacks[fontFamilyKey] ||
      "system-ui, -apple-system, 'Segoe UI', Roboto, Arial, sans-serif";
    root.style.setProperty('--font-family-base', fontStack);

    const fontSize = clampFontSize(settings?.fontSize);
    root.style.setProperty('--font-size-base', fontSize + 'px');
  }

  function normalizeTheme(theme) {
    const allowed = new Set(['system', 'dark', 'light']);
    const value = (theme || 'system').toLowerCase();
    return allowed.has(value) ? value : 'system';
  }

  function clampFontSize(size) {
    const value = Number.parseInt(size ?? 14, 10);
    if (Number.isFinite(value)) {
      return Math.min(24, Math.max(10, value));
    }
    return 14;
  }

  function setupFilterPersistence(settings) {
    const panel = document.querySelector('[data-filters-panel]');
    if (!panel) {
      return;
    }

    const key = (panel.getAttribute('data-filters-key') || 'default').toLowerCase();
    if (!key) {
      return;
    }

    const url = `/api/user/settings/filters/${encodeURIComponent(key)}`;
    let cached = collectFilters(panel);
    let sendScheduled = false;
    let flushed = false;

    const scheduleSend = () => {
      sendScheduled = true;
      flushed = false;
    };

    const updateCache = () => {
      cached = collectFilters(panel);
      scheduleSend();
    };

    panel.addEventListener('change', updateCache, { passive: true });
    panel.addEventListener('input', updateCache, { passive: true });

    const flush = () => {
      if (flushed) {
        return;
      }
      if (sendScheduled || !cached) {
        cached = collectFilters(panel);
      }
      flushed = true;
      sendScheduled = false;
      sendPayload(url, cached);
    };

    window.addEventListener('pagehide', flush);
    document.addEventListener('visibilitychange', () => {
      if (document.visibilityState === 'hidden') {
        flush();
      }
    });
  }

  function collectFilters(form) {
    const data = {};
    const formData = new FormData(form);
    for (const [name, value] of formData.entries()) {
      data[name] = normalizeValue(name, value);
    }
    return data;
  }

  function normalizeValue(name, value) {
    if (value === undefined || value === null || value === '' || value === 'null') {
      return null;
    }
    if (['typeId', 'organizerId', 'size', 'page'].includes(name)) {
      const numeric = Number(value);
      return Number.isFinite(numeric) ? numeric : null;
    }
    return value;
  }

  function sendPayload(url, payload) {
    const json = JSON.stringify(payload || {});
    if (navigator.sendBeacon) {
      try {
        const blob = new Blob([json], { type: 'application/json' });
        navigator.sendBeacon(url, blob);
        return;
      } catch (_) {
      }
    }
    fetch(url, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: json,
      keepalive: true,
      credentials: 'same-origin'
    }).catch(() => {
    });
  }
})();