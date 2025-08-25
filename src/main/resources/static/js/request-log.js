document.addEventListener('DOMContentLoaded', function () {
    // Set the current user and time to match the provided ones
    const currentUser = 'hades12x1'; // Using the provided username
    let currentTime = '2025-03-04 05:45:37'; // Using the updated provided time

    // Format the current time (UTC)
    const updateCurrentTime = () => {
        const currentTimeElement = document.getElementById('current-time-display');
        if (!currentTimeElement) return;

        // Initially set the current time if it's empty
        if (currentTimeElement.textContent === '') {
            currentTimeElement.textContent = currentTime;
        }

        // Parse the current time
        const [datePart, timePart] = currentTime.split(' ');
        const [year, month, day] = datePart.split('-');
        const [hours, minutes, seconds] = timePart.split(':');

        // Create a Date object
        const date = new Date(Date.UTC(
            parseInt(year),
            parseInt(month) - 1,
            parseInt(day),
            parseInt(hours),
            parseInt(minutes),
            parseInt(seconds)
        ));

        // Increment by 1 second
        date.setUTCSeconds(date.getUTCSeconds() + 1);

        // Format back to string
        const updatedYear = date.getUTCFullYear();
        const updatedMonth = String(date.getUTCMonth() + 1).padStart(2, '0');
        const updatedDay = String(date.getUTCDate()).padStart(2, '0');
        const updatedHours = String(date.getUTCHours()).padStart(2, '0');
        const updatedMinutes = String(date.getUTCMinutes()).padStart(2, '0');
        const updatedSeconds = String(date.getUTCSeconds()).padStart(2, '0');

        currentTime = `${updatedYear}-${updatedMonth}-${updatedDay} ${updatedHours}:${updatedMinutes}:${updatedSeconds}`;

        currentTimeElement.textContent = currentTime;
    };

    // Add real-time indicator
    const currentTimeDiv = document.querySelector('.current-time');
    if (currentTimeDiv) {
        const indicator = document.createElement('span');
        indicator.classList.add('real-time-indicator');
        currentTimeDiv.insertBefore(indicator, currentTimeDiv.firstChild);
    }

    // Update time every second
    updateCurrentTime();
    setInterval(updateCurrentTime, 1000);

    // Format JSON in log details for better display
    const formatJsonInDetails = () => {
        document.querySelectorAll('.log-details pre').forEach(pre => {
            const text = pre.textContent.trim();
            if (text.startsWith('{') && text.endsWith('}')) {
                try {
                    const formattedJson = JSON.stringify(JSON.parse(text), null, 2);
                    pre.textContent = formattedJson;
                } catch (e) {
                    // Not valid JSON, leave as is
                }
            }
        });
    };

    // formatJsonInDetails();

    // Attach event listeners to view details buttons to format JSON when modal is shown
    document.querySelectorAll('.view-details-btn').forEach(button => {
        button.addEventListener('click', () => {
            setTimeout(() => {
                formatJsonInDetails();
            }, 100); // delay nhẹ sau khi modal mở
        });
    });

    // WebSocket connection for real-time updates
    let stompClient = null;

    function connectWebSocket() {
        const socket = new SockJS('/ws');
        stompClient = Stomp.over(socket);
        stompClient.debug = null; // Disable debug messages

        const targetUsername = document.querySelector('.current-time')?.getAttribute('data-username') || currentUser;

        stompClient.connect({}, function (frame) {
            console.log('Connected to WebSocket: ' + frame);

            stompClient.subscribe('/topic/requests/' + targetUsername, function (message) {
                const data = JSON.parse(message.body);
                if (data.type === 'REQUEST_UPDATE') {
                    updateRequestCount(data.count);

                    // Check if any modal is open
                    const anyModalOpen = document.querySelector('.modal.show');
                    // Reload the page to show the latest logs if we're on the first page
                    const currentPage = parseInt(document.querySelector('.page-item.active')?.textContent) || 1;
                    if (!anyModalOpen && currentPage === 1) {
                        location.reload();
                    }
                }
            });
        }, function (error) {
            console.error('Error connecting to WebSocket:', error);
            // Try to reconnect after a delay
            setTimeout(connectWebSocket, 5000);
        });
    }

    function updateRequestCount(count) {
        const countElement = document.querySelector('.request-count');
        if (countElement) {
            countElement.textContent = count;
        }
    }

    // Start WebSocket connection
    connectWebSocket();

    // Re-check for new logs every 2 seconds
    var intervalId = undefined
    setTimeout(function () {
        var intervalId = setInterval(function () {
            const targetUsername = document.querySelector('.current-time')?.getAttribute('data-username') || currentUser;
            fetch(`/api/logs/@${targetUsername}/count`)
                .then(response => {
                    if (!response.ok) {
                        // HTTP status code không phải 200–299
                        console.error(`Fetch failed with status: ${response.status}`);
                        clearInterval(intervalId);
                        return Promise.reject(new Error('Fetch failed'));
                    }
                    return response.json();
                })
                //.then(response => response.json())
                .then(data => {
                    updateRequestCount(data.count);
                })
                .catch(error => {
                    console.error('Error fetching log count:', error);
                });
        }, 2000);
    }, 2000)

    // Copy curl command functionality
    const curlButtons = document.querySelectorAll('.copy-curl-btn');
    if (curlButtons.length > 0) {
        const toastEl = document.getElementById('copyCurlToast');
        const toast = new bootstrap.Toast(toastEl, { delay: 2000 });
        const toastMsg = document.getElementById('copyCurlToastMessage');

        curlButtons.forEach(btn => {
            btn.addEventListener('click', function () {
                const curl = this.getAttribute('data-curl');
                navigator.clipboard.writeText(curl)
                    .then(() => {
                        toastMsg.textContent = 'Curl copied to clipboard!';
                        toast.show();
                    })
                    .catch(err => console.error('Failed to copy curl:', err));
            });
        });
    }
});
