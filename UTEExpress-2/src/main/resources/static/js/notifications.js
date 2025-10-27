function markRead(notificationId) {
	fetch(`/ api / notifications / ${notificationId}/read`, {
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
	const recipientType = 'WAREHOUSE';
	const recipientId = document.querySelector('[data-recipient-id]')?.dataset.recipientId;

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

function deleteNotification(notificationId) {
	if (!confirm('Xóa thông báo này?')) return;

	fetch(`/api/notifications/${notificationId}`, {
		method: 'DELETE',
		headers: { 'Content-Type': 'application/json' }
	})
		.then(response => response.json())
		.then(data => {
			location.reload();
		})
		.catch(error => console.error('Error:', error));
}

function loadUnreadCount() {
	const recipientType = 'WAREHOUSE';
	const recipientId = document.querySelector('[data-recipient-id]')?.dataset.recipientId;

	if (!recipientId) return;

	fetch(`/api/notifications/${recipientType}/${recipientId}/count`)
		.then(response => response.json())
		.then(data => {
			const badge = document.querySelector('.badge-notif');
			if (badge && data.count > 0) {
				badge.textContent = data.count;
				badge.style.display = 'inline';
			}
		})
		.catch(error => console.error('Error:', error));
}

document.addEventListener('DOMContentLoaded', function() {
	loadUnreadCount();
	setInterval(loadUnreadCount, 30000);
});
