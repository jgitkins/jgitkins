(() => {
  if (window.__menuHandlerInstalled) {
    return;
  }
  window.__menuHandlerInstalled = true;

  const closeAllMenus = () => {
    document.querySelectorAll(".menu[open]").forEach((menu) => {
      menu.open = false;
    });
  };

  document.addEventListener(
    "click",
    (event) => {
      const summary = event.target.closest("summary");
      const menu = summary ? summary.closest(".menu") : null;

      if (!menu) {
        if (!event.target.closest(".menu")) {
          closeAllMenus();
        }
        return;
      }

      event.preventDefault();
      const willOpen = !menu.open;
      closeAllMenus();
      menu.open = willOpen;
    },
    true
  );
})();
