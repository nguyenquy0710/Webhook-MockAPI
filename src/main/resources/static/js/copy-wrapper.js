function initializeCopyButtons() {
  const copyButtons = document.querySelectorAll('.copy-btn');

  if (copyButtons.length === 0) return;

  // Optional: Bootstrap Toast setup
  const toastEl = document.getElementById('copyToast');
  const toastMessage = document.getElementById('copyToastMessage');
  const toast = toastEl ? new bootstrap.Toast(toastEl, { delay: 2000 }) : null;

  copyButtons.forEach(button => {
    button.addEventListener('click', function () {
      const pre = this.closest('.copy-wrapper')?.querySelector('pre');
      const textToCopy = pre?.innerText || '';

      if (!textToCopy) return;

      navigator.clipboard.writeText(textToCopy)
        .then(() => {
          if (toast && toastMessage) {
            toastMessage.textContent = 'Copied to clipboard!';
            toast.show();
          } else {
            console.log('Copied to clipboard!');
            alert('Copied to clipboard!');
          }
        })
        .catch(err => {
          console.error('Copy failed: ', err);
        });
    });
  });
}

// Run when DOM is ready
document.addEventListener('DOMContentLoaded', function () {
  initializeCopyButtons();
});
