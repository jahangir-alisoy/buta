(function () {
  try {
    var stored = localStorage.getItem('buta.theme');
    var prefersDark = window.matchMedia && window.matchMedia('(prefers-color-scheme: dark)').matches;
    var theme = stored === 'LIGHT' ? 'light' : stored === 'DARK' ? 'dark' : prefersDark ? 'dark' : 'light';
    document.documentElement.setAttribute('data-theme', theme);
    var locale = localStorage.getItem('buta.locale');
    if (locale === 'en' || locale === 'az') {
      document.documentElement.setAttribute('lang', locale);
    }
  } catch (error) {
    document.documentElement.setAttribute('data-theme', 'light');
  }
})();
