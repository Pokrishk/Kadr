(() => {
  const doc = document;
  let currentRow = null;

  const state = {
    filterPanel: null,
    filterToggle: null,
  };

  const isTypingTarget = (target) => {
    if (!target) {
      return false;
    }
    const tag = target.tagName;
    const editable = target.isContentEditable;
    return editable || tag === 'INPUT' || tag === 'TEXTAREA' || tag === 'SELECT';
  };

  const findActionElement = (action) => {
    const active = doc.activeElement;
    if (active) {
      if (active.matches?.(`[data-hotkey="${action}"]`)) {
        return active;
      }
      if (active.closest) {
        const nested = active.closest(`[data-hotkey="${action}"]`);
        if (nested) {
          return nested;
        }
      }
    }
    return doc.querySelector(`[data-hotkey="${action}"]`);
  };

  const setCurrentRow = (row) => {
    if (currentRow === row) {
      return;
    }
    if (currentRow) {
      currentRow.classList.remove('is-hotkey-active');
    }
    currentRow = row;
    if (currentRow) {
      currentRow.classList.add('is-hotkey-active');
    }
  };

  const getRowDeleteForm = () => {
    if (!currentRow || !currentRow.dataset.deleteForm) {
      return null;
    }
    return doc.getElementById(currentRow.dataset.deleteForm);
  };

  const getRowEditUrl = () => {
    if (!currentRow) {
      return null;
    }
    return currentRow.dataset.editUrl || null;
  };

  const confirmAndSubmit = (element) => {
    if (!element) {
      return false;
    }
    let form = null;
    let message = element.dataset?.confirm;

    if (element instanceof HTMLFormElement) {
      form = element;
      if (!message && form.dataset) {
        message = form.dataset.confirm;
      }
    } else if (element instanceof HTMLButtonElement || element instanceof HTMLInputElement) {
      if (!message && element.dataset) {
        message = element.dataset.confirm;
      }
      form = element.form;
    } else if (element.closest) {
      const closestForm = element.closest('form');
      if (closestForm) {
        form = closestForm;
        if (!message && element.dataset) {
          message = element.dataset.confirm;
        }
        if (!message && closestForm.dataset) {
          message = closestForm.dataset.confirm;
        }
      }
    }

    if (!form) {
      return false;
    }

    const confirmMessage = message || 'Удалить запись?';
    if (!window.confirm(confirmMessage)) {
      return true;
    }

    if (typeof form.requestSubmit === 'function') {
      form.requestSubmit();
    } else {
      form.submit();
    }
    return true;
  };

  const triggerSave = () => {
    const element = findActionElement('save');
    if (!element) {
      return false;
    }

    if ((element instanceof HTMLButtonElement || element instanceof HTMLInputElement) && element.type === 'submit') {
      element.click();
      return true;
    }

    const form = element.form || element.closest?.('form');
    if (form) {
      if (typeof form.requestSubmit === 'function') {
        form.requestSubmit(element instanceof HTMLElement ? element : undefined);
      } else {
        form.submit();
      }
      return true;
    }
    return false;
  };

  const triggerExport = () => {
    const element = findActionElement('export');
    if (!element) {
      return false;
    }
    if (element instanceof HTMLAnchorElement) {
      element.click();
      return true;
    }
    if (element.dataset && element.dataset.url) {
      window.location.href = element.dataset.url;
      return true;
    }
    return false;
  };

  const triggerCreateEvent = () => {
    const element = findActionElement('create-event');
    if (!element) {
      return false;
    }
    if (element instanceof HTMLAnchorElement || element instanceof HTMLButtonElement) {
      element.click();
      return true;
    }
    if (element.dataset && element.dataset.url) {
      window.location.href = element.dataset.url;
      return true;
    }
    return false;
  };

  const triggerEdit = () => {
    const rowUrl = getRowEditUrl();
    if (rowUrl) {
      window.location.href = rowUrl;
      return true;
    }

    const element = findActionElement('edit');
    if (!element) {
      return false;
    }
    if (element instanceof HTMLAnchorElement || element instanceof HTMLButtonElement) {
      element.click();
      return true;
    }
    if (element.dataset && element.dataset.url) {
      window.location.href = element.dataset.url;
      return true;
    }
    return false;
  };

  const triggerDelete = () => {
    const rowForm = getRowDeleteForm();
    if (rowForm && confirmAndSubmit(rowForm)) {
      return true;
    }
    const element = findActionElement('delete');
    return confirmAndSubmit(element);
  };

  const focusSearch = () => {
    const input = findActionElement('search');
    if (!input) {
      return false;
    }
    if (typeof input.focus === 'function') {
      input.focus({ preventScroll: true });
    }
    if (typeof input.select === 'function') {
      input.select();
    }
    return true;
  };

  const toggleFilters = () => {
    if (!state.filterPanel || !state.filterToggle) {
      return false;
    }
    state.filterPanel.classList.toggle('is-collapsed');
    const collapsed = state.filterPanel.classList.contains('is-collapsed');
    state.filterToggle.setAttribute('aria-expanded', String(!collapsed));
    const label = collapsed ? state.filterToggle.dataset.labelCollapsed : state.filterToggle.dataset.labelExpanded;
    if (label) {
      state.filterToggle.textContent = label;
    }
    return true;
  };

  const initFilters = () => {
    state.filterPanel = doc.querySelector('[data-filters-panel]');
    state.filterToggle = doc.querySelector('[data-hotkey="filters"]');
    if (!state.filterPanel || !state.filterToggle) {
      state.filterPanel = null;
      state.filterToggle = null;
      return;
    }

    if (!state.filterToggle.dataset.labelExpanded) {
      state.filterToggle.dataset.labelExpanded = state.filterToggle.textContent.trim();
    }
    if (!state.filterToggle.dataset.labelCollapsed) {
      state.filterToggle.dataset.labelCollapsed = 'Показать фильтры';
    }

    state.filterToggle.addEventListener('click', (event) => {
      event.preventDefault();
      toggleFilters();
    });

    const collapsed = state.filterPanel.classList.contains('is-collapsed');
    state.filterToggle.setAttribute('aria-expanded', String(!collapsed));
    const label = collapsed ? state.filterToggle.dataset.labelCollapsed : state.filterToggle.dataset.labelExpanded;
    if (label) {
      state.filterToggle.textContent = label;
    }
  };

  const initRows = () => {
    const rows = doc.querySelectorAll('[data-hotkey-row]');
    rows.forEach((row) => {
      row.addEventListener('focus', () => setCurrentRow(row));
      row.addEventListener('click', () => setCurrentRow(row));
    });
  };

  const handleKeydown = (event) => {
    const rawKey = event.key;
    const key = rawKey.length === 1 ? rawKey.toLowerCase() : rawKey;

    if (event.metaKey || event.altKey) {
      return;
    }
    if (event.ctrlKey && key !== 's') {
      return;
    }

    const typing = isTypingTarget(event.target);

    if (key === 's') {
      if (!event.ctrlKey && typing) {
        return;
      }
      event.preventDefault();
      triggerSave();
      return;
    }

    if (typing) {
      return;
    }

    switch (key) {
      case 'n':
        event.preventDefault();
        triggerCreateEvent();
        break;
      case 'e':
        event.preventDefault();
        triggerEdit();
        break;
      case 'Delete':
        event.preventDefault();
        triggerDelete();
        break;
      case '/':
        event.preventDefault();
        focusSearch();
        break;
      case 'f':
        if (toggleFilters()) {
          event.preventDefault();
        }
        break;
      case 'x':
        event.preventDefault();
        triggerExport();
        break;
      case 'r':
        event.preventDefault();
        window.location.reload();
        break;
      default:
        break;
    }
  };

  doc.addEventListener('DOMContentLoaded', () => {
    initFilters();
    initRows();
    doc.addEventListener('keydown', handleKeydown, { capture: true });
  });
})();