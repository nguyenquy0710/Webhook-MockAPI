if (!window.MockAPI) {
    // Ensure MockAPI namespace exists
    window.MockAPI = {};
}

// DataTables initialization for admin users table
$(document).ready(function () {
    if (!window.MockAPI.DataTable) {
        // Ensure MockAPI namespace exists
        window.MockAPI.DataTable = {};
    }

    // Initialize users table if it exists
    if ($('#usersTable').length) {
        MockAPI.DataTable['usersTable'] = $('#usersTable').DataTable({
            "pageLength": 25,
            "order": [[0, "asc"]],
            "columnDefs": [
                { "orderable": false, "targets": 6 } // Disable sorting on Actions column
            ],
            "language": {
                "search": "Search users:",
                "lengthMenu": "Show _MENU_ users per page",
                "info": "Showing _START_ to _END_ of _TOTAL_ users",
                "infoEmpty": "No users found",
                "infoFiltered": "(filtered from _MAX_ total users)",
                "zeroRecords": "No matching users found"
            }
        });
    }

    // Initialize request logs table if it exists
    if ($('#requestLogsTable').length) {
        MockAPI.DataTable['requestLogsTable'] = $('#requestLogsTable').DataTable({
            "ajax": {
                "url": "/api/logs/@quynh",
                "dataSrc": "content"  // Dữ liệu nằm trong trường "content"
            },
            "columns": [
                {
                    data: 'timestamp',
                    render: function (data) {
                        return new Date(data).toISOString().replace('T', ' ').split('.')[0] + ' UTC';
                    }
                },
                {
                    data: 'method',
                    render: function (data) {
                        let badgeClass = 'bg-secondary';
                        switch (data) {
                            case 'GET': badgeClass = 'bg-success'; break;
                            case 'POST': badgeClass = 'bg-primary'; break;
                            case 'PUT': badgeClass = 'bg-info'; break;
                            case 'DELETE': badgeClass = 'bg-danger'; break;
                        }
                        return `<span class="badge ${badgeClass}">${data}</span>`;
                    }
                },
                { data: 'path' },
                {
                    data: 'responseStatus',
                    render: function (data, type, row) {
                        let badgeClass = 'bg-warning'; // default

                        if (data >= 200 && data < 300) {
                            badgeClass = 'bg-success';
                        } else if (data >= 400) {
                            badgeClass = 'bg-danger';
                        }

                        return `<span class="badge ${badgeClass}">${data}</span>`;
                    }
                },
                { data: 'sourceIp' },
                {
                    data: 'id',
                    orderable: false,
                    render: function (data, type, row) {
                        return `
            <button class="btn btn-sm btn-outline-primary view-details view-details-btn"
                data-bs-toggle="modal" data-id="${data}"
                data-bs-target="#detailsModal${data}">
                <i class="fas fa-eye me-1"></i> View
            </button>
        `;
                    }
                }
            ],
            "pageLength": 10,
            "order": [[0, "desc"]], // Sort by time (newest first)
            "columnDefs": [
                { "orderable": false, "targets": 5 } // Disable sorting on Details column
            ],
            "language": {
                "search": "Search logs:",
                "lengthMenu": "Show _MENU_ logs per page",
                "info": "Showing _START_ to _END_ of _TOTAL_ logs",
                "infoEmpty": "No logs found",
                "infoFiltered": "(filtered from _MAX_ total logs)",
                "zeroRecords": "No matching logs found"
            }
        });

        // Bắt sự kiện View
        $('#requestLogsTable tbody').on('click', '.view-details', function () {
            const log = $('#requestLogsTable').DataTable().row($(this).closest('tr')).data();

            // Đổ dữ liệu vào modal
            $('#logTime').text(new Date(log.timestamp).toISOString().replace('T', ' ').split('.')[0] + ' UTC');
            $('#logMethod').text(log.method);
            $('#logPath').text(log.path);
            $('#logStatus').text(log.responseStatus);
            $('#logSourceIp').text(log.sourceIp);

            try {
                $('#logHeaders').text(JSON.stringify(JSON.parse(log.requestHeaders), null, 4) || 'No headers'); // Định dạng JSON đẹp hơn
            } catch (error) {
                $('#logHeaders').text(log.requestHeaders || 'No headers');
            }

            $('#logParams').text(log.queryParams || 'No query parameters');

            try {
                $('#logBody').text(JSON.stringify(JSON.parse(log.requestBody), null, 4) || 'No request body'); // Định dạng JSON đẹp hơn
            } catch (error) {
                $('#logBody').text(log.requestBody || 'No request body');
            }

            try {
                $('#logResponse').text(JSON.stringify(JSON.parse(log.responseBody), null, 4) || 'No response body'); // Định dạng JSON đẹp hơn
            } catch (error) {
                $('#logResponse').text(log.responseBody || 'No response body');
            }

            $('#logCurl').text(log.curl || 'No curl');

            // Mở modal
            $('#logDetailsModal').modal('show');
            // const modal = new bootstrap.Modal(document.getElementById('logDetailsModal'));
            // modal.show();
        });
    }
});