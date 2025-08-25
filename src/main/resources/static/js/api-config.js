document.addEventListener('DOMContentLoaded', function () {
    // Format JSON in textareas for better display
    const formatJsonTextareas = () => {
        const jsonTextareas = ['responseBody', 'responseHeaders'];

        jsonTextareas.forEach(id => {
            const textarea = document.getElementById(id);
            if (textarea && textarea.value) {
                try {
                    const formattedJson = JSON.stringify(JSON.parse(textarea.value), null, 2);
                    textarea.value = formattedJson;
                } catch (e) {
                    // Not valid JSON, leave as is
                    console.warn(`Could not format JSON for ${id}:`, e);
                }
            }
        });
    };

    formatJsonTextareas();

    // Add event listener for content type change
    const contentTypeSelect = document.getElementById('contentType');
    if (contentTypeSelect) {
        contentTypeSelect.addEventListener('change', function () {
            const responseBody = document.getElementById('responseBody');
            if (responseBody && this.value === 'application/json') {
                try {
                    // Try to format as JSON if it's valid
                    const json = JSON.parse(responseBody.value);
                    responseBody.value = JSON.stringify(json, null, 2);
                } catch (e) {
                    // If not valid JSON and content type is application/json, provide a template
                    if (!responseBody.value.trim()) {
                        responseBody.value = '{\n  "message": "Hello World!"\n}';
                    }
                }
            }
        });
    }

    // Add validation for JSON format
    const form = document.querySelector('form');
    if (form) {
        form.addEventListener('submit', function (event) {
            const contentType = document.getElementById('contentType').value;
            const responseBody = document.getElementById('responseBody').value.trim();
            const responseHeaders = document.getElementById('responseHeaders').value.trim();

            // Validate JSON format for response body if content type is application/json
            if (contentType === 'application/json' && responseBody) {
                try {
                    JSON.parse(responseBody);
                } catch (e) {
                    event.preventDefault();
                    alert('Response body is not valid JSON. Please check the format.');
                    return;
                }
            }

            // Validate JSON format for response headers if not empty
            if (responseHeaders) {
                try {
                    JSON.parse(responseHeaders);
                } catch (e) {
                    event.preventDefault();
                    alert('Response headers is not valid JSON. Please check the format.');
                    return;
                }
            }
        });
    }

    // Copy endpoint URL functionality
    const copyButtons = document.querySelectorAll('.copy-endpoint-btn');
    if (copyButtons.length > 0) {
        // Create toast instance
        const toastEl = document.getElementById('copyToast');
        const toast = new bootstrap.Toast(toastEl, {
            delay: 2000
        });

        const toastMessage = document.getElementById('copyToastMessage');

        copyButtons.forEach(button => {
            button.addEventListener('click', function () {
                const endpoint = this.getAttribute('data-endpoint');
                const method = this.getAttribute('data-method');

                // Copy to clipboard
                navigator.clipboard.writeText(endpoint)
                    .then(() => {
                        // Update toast message
                        toastMessage.textContent = `${method} ${endpoint} copied to clipboard!`;

                        // Show toast
                        toast.show();
                    })
                    .catch(err => {
                        console.error('Failed to copy endpoint: ', err);
                    });
            });
        });
    }
});

// Delete confirmation modal
function confirmDelete(id, path, method) {
    document.getElementById('deletePath').textContent = path;
    document.getElementById('deleteMethod').textContent = method;
    document.getElementById('deleteForm').action = `/api-configs/${id}/delete`;

    const deleteModal = new bootstrap.Modal(document.getElementById('deleteModal'));
    deleteModal.show();
}