document.addEventListener('DOMContentLoaded', function () {
    // Copy base URL functionality
    const copyBaseUrlBtn = document.querySelector('.copy-base-url-btn');
    if (copyBaseUrlBtn) {
        copyBaseUrlBtn.addEventListener('click', function () {
            const baseUrlInput = this.previousElementSibling;
            baseUrlInput.select();
            document.execCommand('copy');

            // Change button text temporarily
            const originalText = this.innerHTML;
            this.innerHTML = '<i class="fas fa-check"></i> Copied!';
            this.classList.remove('btn-outline-primary');
            this.classList.add('btn-success');

            // Restore original text after 2 seconds
            setTimeout(() => {
                this.innerHTML = originalText;
                this.classList.remove('btn-success');
                this.classList.add('btn-outline-primary');
            }, 2000);
        });
    }
});