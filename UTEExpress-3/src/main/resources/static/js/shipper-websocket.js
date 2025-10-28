let shipperStompClient = null;

function connectShipper() {
	const socket = new SockJS('/ws');
	shipperStompClient = Stomp.over(socket);

	shipperStompClient.connect({}, function(frame) {
		console.log('Connected: ' + frame);

		const shipperId = document.querySelector('[data-shipper-id]')?.dataset.shipperId;

		if (shipperId) {
			shipperStompClient.subscribe(`/topic/notifications/shipper/${shipperId}`, function(notification) {
				showShipperNotification(JSON.parse(notification.body));
			});
		}

		const shipmentId = document.querySelector('[data-shipment-id]')?.dataset.shipmentId;
		if (shipmentId) {
			shipperStompClient.subscribe(`/topic/tracking/${shipmentId}`, function(tracking) {
				updateTrackingInfo(JSON.parse(tracking.body));
			});
		}
	});
}

function disconnectShipper() {
	if (shipperStompClient !== null) {
		shipperStompClient.disconnect();
	}
	console.log('Disconnected');
}

function showShipperNotification(notification) {
	const badge = document.querySelector('.badge-notif');
	if (badge) {
		let count = parseInt(badge.textContent || '0');
		badge.textContent = count + 1;
		badge.style.display = 'inline';
	}

	if ('Notification' in window && Notification.permission === 'granted') {
		new Notification('UTE Express - Đơn hàng mới', {
			body: notification.message,
			icon: '/images/logo.jpg',
			badge: '/images/badge.png'
		});
	}

}

function updateTrackingInfo(tracking) {
	console.log('Tracking updated:', tracking);
	const trackingDiv = document.getElementById('tracking-info');
	if (trackingDiv) {
		trackingDiv.innerHTML = `
            <p>Vĩ độ: ${tracking.latitude}</p>
            <p>Kinh độ: ${tracking.longitude}</p>
            <p>Cập nhật: ${new Date(tracking.createdAt).toLocaleString('vi-VN')}</p>
        `;
	}
}

function sendLocationUpdate(shipmentId, latitude, longitude) {
	if (shipperStompClient && shipperStompClient.connected) {
		shipperStompClient.send(`/app/tracking/${shipmentId}`, {}, JSON.stringify({
			latitude: latitude,
			longitude: longitude,
			description: 'Location update from shipper'
		}));
	}
}

document.addEventListener('DOMContentLoaded', function() {
	if ('Notification' in window && Notification.permission === 'default') {
		Notification.requestPermission();
	}
	connectShipper();
});

window.addEventListener('beforeunload', function() {
	disconnectShipper();
});