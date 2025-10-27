function startShipment(shipmentId) {
	if (confirm('Xác nhận bắt đầu giao hàng?')) {
		fetch(`/shipper/shipments/${shipmentId}/start`, {
			method: 'POST',
			headers: { 'Content-Type': 'application/json' }
		})
			.then(response => {
				if (response.ok) {
					location.reload();
				} else {
					alert('Lỗi khi bắt đầu giao hàng');
				}
			})
			.catch(error => console.error('Error:', error));
	}
}

function completeShipment(shipmentId, formData) {
	fetch(`/shipper/shipments/${shipmentId}/complete`, {
		method: 'POST',
		body: formData
	})
		.then(response => {
			if (response.ok) {
				alert('Đã hoàn thành giao hàng');
				location.reload();
			} else {
				alert('Lỗi khi hoàn thành giao hàng');
			}
		})
		.catch(error => console.error('Error:', error));
}

function failShipment(shipmentId, notes) {
	if (!notes || notes.trim() === '') {
		alert('Vui lòng nhập lý do thất bại');
		return;
	}

	if (confirm('Xác nhận đánh dấu giao hàng thất bại?')) {
		const formData = new FormData();
		formData.append('notes', notes);

		fetch(`/shipper/shipments/${shipmentId}/fail`, {
			method: 'POST',
			body: formData
		})
			.then(response => {
				if (response.ok) {
					alert('Đã đánh dấu thất bại');
					location.reload();
				} else {
					alert('Lỗi khi cập nhật trạng thái');
				}
			})
			.catch(error => console.error('Error:', error));
	}
}

// /static/js/shippers.js

document.addEventListener('DOMContentLoaded', function () {
	const shipperSelect = document.querySelector('select[name="shipperId"]');

	if (shipperSelect) {
		// Fetch active shippers
		fetch('/warehouse/api/shippers/active')
			.then(response => response.json())
			.then(shippers => {
				shippers.forEach(shipper => {
					const option = document.createElement('option');
					option.value = shipper.id;
					option.textContent = `${shipper.name} - ${shipper.phone} (${shipper.vehicleType})`;
					shipperSelect.appendChild(option);
				});
			})
			.catch(error => {
				console.error('Error loading shippers:', error);
			});
	}
});

function updateShipperLocation() {
	if (navigator.geolocation) {
		navigator.geolocation.getCurrentPosition(function (position) {
			const formData = new FormData();
			formData.append('latitude', position.coords.latitude);
			formData.append('longitude', position.coords.longitude);

			fetch('/shipper/location/update', {
				method: 'POST',
				body: formData
			})
				.then(response => response.text())
				.then(result => {
					console.log('Location updated:', result);
				})
				.catch(error => console.error('Error:', error));
		}, function (error) {
			console.error('Geolocation error:', error);
		}, {
			enableHighAccuracy: true,
			timeout: 5000,
			maximumAge: 0
		});
	}
}

function startLocationTracking() {
	updateShipperLocation();
	setInterval(updateShipperLocation, 60000);
}

function initImagePreview() {
	const fileInput = document.querySelector('input[type="file"]');
	if (fileInput) {
		fileInput.addEventListener('change', function (e) {
			const file = e.target.files[0];
			if (file) {
				const reader = new FileReader();
				reader.onload = function (e) {
					let preview = document.getElementById('image-preview');
					if (!preview) {
						preview = document.createElement('div');
						preview.id = 'image-preview';
						preview.style.marginTop = '10px';
						fileInput.parentNode.appendChild(preview);
					}
					preview.innerHTML = `<img src="${e.target.result}" style="max-width: 300px; border-radius: 8px;">`;
				};
				reader.readAsDataURL(file);
			}
		});
	}
}

function formatDistance(meters) {
	if (meters < 1000) {
		return meters.toFixed(0) + ' m';
	} else {
		return (meters / 1000).toFixed(1) + ' km';
	}
}

function calculateDistance(lat1, lon1, lat2, lon2) {
	const R = 6371e3;
	const φ1 = lat1 * Math.PI / 180;
	const φ2 = lat2 * Math.PI / 180;
	const Δφ = (lat2 - lat1) * Math.PI / 180;
	const Δλ = (lon2 - lon1) * Math.PI / 180;

	const a = Math.sin(Δφ / 2) * Math.sin(Δφ / 2) +
		Math.cos(φ1) * Math.cos(φ2) *
		Math.sin(Δλ / 2) * Math.sin(Δλ / 2);
	const c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

	return R * c;
}

document.addEventListener('DOMContentLoaded', function () {
	initImagePreview();

	const shipmentDetailPage = document.querySelector('[data-shipment-detail]');
	if (shipmentDetailPage) {
		startLocationTracking();
	}

	const dateInputs = document.querySelectorAll('input[type="date"], input[type="datetime-local"]');
	dateInputs.forEach(input => {
		if (!input.value) {
			const now = new Date();
			const dateString = now.toISOString().slice(0, 16);
			input.value = dateString;
		}
	});
});