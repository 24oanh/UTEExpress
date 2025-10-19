let stompClient = null;

function connect() {
	const socket = new SockJS('/ws');
	stompClient = Stomp.over(socket);

	stompClient.connect({}, function(frame) {
		console.log('Connected: ' + frame);

		const recipientType = 'WAREHOUSE';
		const recipientId = document.querySelector('[data-recipient-id]')?.dataset.recipientId;

		if (recipientId) {
			stompClient.subscribe(`/topic/notifications/${recipientType.toLowerCase()}/${recipientId}`, function(notification) {
				showNotification(JSON.parse(notification.body));
			});
		}

		stompClient.subscribe('/topic/location', function(location) {
			updateShipperLocation(JSON.parse(location.body));
		});
	});
}

function disconnect() {
	if (stompClient !== null) {
		stompClient.disconnect();
	}
	console.log('Disconnected');
}

function showNotification(notification) {
	const badge = document.querySelector('.badge-notif');
	if (badge) {
		let count = parseInt(badge.textContent || '0');
		badge.textContent = count + 1;
		badge.style.display = 'inline';
	}

	if ('Notification' in window && Notification.permission === 'granted') {
		new Notification('UTE Express', {
			body: notification.message,
			icon: '/images/logo.jpg'
		});
	}
}

function updateShipperLocation(location) {
	console.log('Shipper location updated:', location);
}

document.addEventListener('DOMContentLoaded', function() {
	if ('Notification' in window && Notification.permission === 'default') {
		Notification.requestPermission();
	}
	connect();
});

window.addEventListener('beforeunload', function() {
	disconnect();
});