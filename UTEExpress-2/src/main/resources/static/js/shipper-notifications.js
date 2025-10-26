function markReadShipper(notificationId) {
	fetch(`/api/notifications/${notificationId}/read`, {
		method: 'POST',
		headers: { 'Content-Type': 'application/json' }
	})
		.then(response => response.json())
		.then(data => {
			location.reload();
		})
		.catch(error => console.error('Error:', error));
}

function markAllRead() {
	const recipientType = 'SHIPPER';
	const recipientId = document.querySelector('.notification-list')?.dataset.recipientId;

	if (!recipientId) {
		console.error('Recipient ID not found');
		return;
	}

	fetch(`/api/notifications/${recipientType}/${recipientId}/read-all`, {
		method: 'POST',
		headers: { 'Content-Type': 'application/json' }
	})
		.then(response => response.json())
		.then(data => {
			location.reload();
		})
		.catch(error => console.error('Error:', error));
}

function loadUnreadCountShipper() {
	const recipientType = 'SHIPPER';
	const recipientId = document.querySelector('.notification-list')?.dataset.recipientId;

	if (!recipientId) return;

	fetch(`/api/notifications/${recipientType}/${recipientId}/count`)
		.then(response => response.json())
		.then(data => {
			const badge = document.querySelector('.badge-notif');
			if (badge && data.count > 0) {
				badge.textContent = data.count;
				badge.style.display = 'inline';
			} else if (badge && data.count === 0) {
				badge.style.display = 'none';
			}
		})
		.catch(error => console.error('Error:', error));
}

document.addEventListener('DOMContentLoaded', function() {
	loadUnreadCountShipper();
	setInterval(loadUnreadCountShipper, 30000);
});