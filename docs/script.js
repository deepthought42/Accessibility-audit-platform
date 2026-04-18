(function () {
  'use strict';

  const doc = document.documentElement;
  const KEY = 'looksee.theme';
  const reduced = matchMedia('(prefers-reduced-motion: reduce)').matches;

  // ---------- Theme toggle ----------------------------------
  const stored = (() => {
    try { return localStorage.getItem(KEY); } catch { return null; }
  })();
  if (stored === 'night' || stored === 'paper') {
    doc.setAttribute('data-theme', stored);
  }

  const toggle = document.querySelector('.theme-toggle');
  if (toggle) {
    const sync = () => {
      const current = doc.getAttribute('data-theme')
        || (matchMedia('(prefers-color-scheme: dark)').matches ? 'night' : 'paper');
      const isNight = current === 'night';
      toggle.setAttribute('aria-pressed', String(isNight));
      const label = toggle.querySelector('.theme-toggle__label');
      if (label) label.textContent = isNight ? 'Raise the lights' : 'Dim the lights';
    };
    sync();
    toggle.addEventListener('click', () => {
      const current = doc.getAttribute('data-theme')
        || (matchMedia('(prefers-color-scheme: dark)').matches ? 'night' : 'paper');
      const next = current === 'night' ? 'paper' : 'night';
      doc.setAttribute('data-theme', next);
      try { localStorage.setItem(KEY, next); } catch { /* ignore */ }
      sync();
    });
    matchMedia('(prefers-color-scheme: dark)').addEventListener('change', sync);
  }

  // ---------- Scroll reveal --------------------------------
  if (!reduced && 'IntersectionObserver' in window) {
    const targets = document.querySelectorAll(
      '.chapter__head, .prose, .stats, .cards, .pullquote, .reasons li, .diagram, .decision, .timeline__item, .links'
    );
    targets.forEach((el) => el.classList.add('reveal'));
    const io = new IntersectionObserver((entries) => {
      entries.forEach((entry) => {
        if (entry.isIntersecting) {
          entry.target.classList.add('is-visible');
          io.unobserve(entry.target);
        }
      });
    }, { rootMargin: '0px 0px -10% 0px', threshold: 0.05 });
    targets.forEach((el) => io.observe(el));
  }

  // ---------- Diagram node a11y ----------------------------
  // Make each service node in the architecture diagram focusable so keyboard
  // users get the hover state as well.
  document.querySelectorAll('.diagram__svg .node').forEach((node) => {
    node.setAttribute('tabindex', '0');
    node.setAttribute('role', 'img');
    const title = node.querySelector('.node__title');
    if (title) node.setAttribute('aria-label', title.textContent.trim());
  });
})();
