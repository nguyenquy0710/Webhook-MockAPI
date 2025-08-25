// DataTables initialization for admin users table
$(document).ready(function() {
    // Initialize users table if it exists
    if ($('#usersTable').length) {
        $('#usersTable').DataTable({
            "pageLength": 25,
            "order": [[ 0, "asc" ]],
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
        $('#requestLogsTable').DataTable({
            "pageLength": 25,
            "order": [[ 0, "desc" ]], // Sort by time (newest first)
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
    }
});