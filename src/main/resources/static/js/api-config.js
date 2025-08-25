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

    // Initialize copy buttons for any existing elements
    initializeCopyButtons();

    // Initialize API Configurations DataTable
    const apiConfigsTable = document.getElementById('apiConfigsTable');
    if (apiConfigsTable) {
        const username = apiConfigsTable.getAttribute('data-username');
        const domain = apiConfigsTable.getAttribute('data-domain');
        
        $('#apiConfigsTable').DataTable({
            "processing": true,
            "serverSide": true,
            "ajax": {
                "url": `/api/configs/@${username}`,
                "type": "GET",
                "dataSrc": function(json) {
                    // Transform Spring Boot Page response to DataTables format
                    return json.content || [];
                },
                "data": function(d) {
                    // Transform DataTables parameters to Spring Boot Pageable format
                    return {
                        page: Math.floor(d.start / d.length),
                        size: d.length,
                        sort: d.columns[d.order[0].column].data + ',' + d.order[0].dir
                    };
                }
            },
            "columns": [
                {
                    "data": "path",
                    "render": function(data, type, row) {
                        const endpointUrl = domain + '/api/@' + username + data;
                        return `
                            <div class="d-flex align-items-center">
                                <code>/api/@${username}${data}</code>
                                <button class="btn btn-sm btn-outline-secondary ms-2 copy-endpoint-btn" 
                                        data-endpoint="${endpointUrl}"
                                        data-method="${row.method}" 
                                        title="Copy full endpoint URL">
                                    <i class="fas fa-copy"></i>
                                </button>
                            </div>
                        `;
                    }
                },
                {
                    "data": "method",
                    "render": function(data, type, row) {
                        return `<span class="badge bg-primary">${data}</span>`;
                    }
                },
                {
                    "data": "statusCode",
                    "render": function(data, type, row) {
                        const badgeClass = data >= 200 && data < 300 ? 'bg-success' : 
                                         (data >= 400 ? 'bg-danger' : 'bg-warning');
                        return `<span class="badge ${badgeClass}">${data}</span>`;
                    }
                },
                {
                    "data": "contentType",
                    "render": function(data, type, row) {
                        return `<span class="badge bg-secondary">${data}</span>`;
                    }
                },
                {
                    "data": "delayMs",
                    "render": function(data, type, row) {
                        return `${data} ms`;
                    }
                },
                {
                    "data": "id",
                    "orderable": false,
                    "render": function(data, type, row) {
                        return `
                            <div class="btn-group btn-group-sm">
                                <a href="/api-configs/${data}" class="btn btn-outline-primary">
                                    <i class="fas fa-edit"></i> Edit
                                </a>
                                <button type="button" class="btn btn-outline-danger"
                                        onclick="confirmDelete(${data}, '${row.path}', '${row.method}')">
                                    <i class="fas fa-trash"></i> Delete
                                </button>
                            </div>
                        `;
                    }
                }
            ],
            "pageLength": 10,
            "order": [[0, "asc"]],
            "language": {
                "search": "Search configurations:",
                "lengthMenu": "Show _MENU_ configurations per page",
                "info": "Showing _START_ to _END_ of _TOTAL_ configurations",
                "infoEmpty": "No configurations found",
                "infoFiltered": "(filtered from _MAX_ total configurations)",
                "zeroRecords": "No API configurations found. Create one above.",
                "processing": "Loading..."
            },
            "drawCallback": function(settings) {
                // Re-initialize copy functionality for newly rendered buttons
                initializeCopyButtons();
            }
        });
    }

    // Function to initialize copy buttons (extracted for reuse)
    function initializeCopyButtons() {
        const copyButtons = document.querySelectorAll('.copy-endpoint-btn');
        if (copyButtons.length > 0) {
            // Create toast instance
            const toastEl = document.getElementById('copyToast');
            const toast = new bootstrap.Toast(toastEl, {
                delay: 2000
            });

            const toastMessage = document.getElementById('copyToastMessage');

            copyButtons.forEach(button => {
                // Remove existing event listeners to avoid duplicates
                button.replaceWith(button.cloneNode(true));
            });

            // Re-select buttons after cloning
            document.querySelectorAll('.copy-endpoint-btn').forEach(button => {
                button.addEventListener('click', function() {
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