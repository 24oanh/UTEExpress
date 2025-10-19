function confirmDelete(message) {
	return confirm(message || 'Bạn có chắc chắn muốn xóa?');
}

function formatCurrency(amount) {
	return new Intl.NumberFormat('vi-VN', {
		style: 'currency',
		currency: 'VND'
	}).format(amount);
}

function formatDate(dateString) {
	const date = new Date(dateString);
	return date.toLocaleDateString('vi-VN', {
		year: 'numeric',
		month: '2-digit',
		day: '2-digit',
		hour: '2-digit',
		minute: '2-digit'
	});
}

function filterTable(inputId, tableId) {
	const input = document.getElementById(inputId);
	const filter = input.value.toUpperCase();
	const table = document.getElementById(tableId);
	const tr = table.getElementsByTagName('tr');

	for (let i = 1; i < tr.length; i++) {
		let display = 'none';
		const td = tr[i].getElementsByTagName('td');

		for (let j = 0; j < td.length; j++) {
			if (td[j]) {
				const txtValue = td[j].textContent || td[j].innerText;
				if (txtValue.toUpperCase().indexOf(filter) > -1) {
					display = '';
					break;
				}
			}
		}
		tr[i].style.display = display;
	}
}

function sortTable(tableId, columnIndex) {
	const table = document.getElementById(tableId);
	let switching = true;
	let shouldSwitch, i;
	let switchcount = 0;
	let dir = 'asc';

	while (switching) {
		switching = false;
		const rows = table.rows;

		for (i = 1; i < (rows.length - 1); i++) {
			shouldSwitch = false;
			const x = rows[i].getElementsByTagName('td')[columnIndex];
			const y = rows[i + 1].getElementsByTagName('td')[columnIndex];

			if (dir === 'asc') {
				if (x.innerHTML.toLowerCase() > y.innerHTML.toLowerCase()) {
					shouldSwitch = true;
					break;
				}
			} else if (dir === 'desc') {
				if (x.innerHTML.toLowerCase() < y.innerHTML.toLowerCase()) {
					shouldSwitch = true;
					break;
				}
			}
		}

		if (shouldSwitch) {
			rows[i].parentNode.insertBefore(rows[i + 1], rows[i]);
			switching = true;
			switchcount++;
		} else {
			if (switchcount === 0 && dir === 'asc') {
				dir = 'desc';
				switching = true;
			}
		}
	}
}

function printReport() {
	window.print();
}

function exportToCSV(tableId, filename) {
	const table = document.getElementById(tableId);
	let csv = [];
	const rows = table.querySelectorAll('tr');

	for (let row of rows) {
		const cols = row.querySelectorAll('td, th');
		const csvRow = [];

		for (let col of cols) {
			csvRow.push(col.innerText);
		}
		csv.push(csvRow.join(','));
	}

	const csvFile = new Blob([csv.join('\n')], { type: 'text/csv' });
	const downloadLink = document.createElement('a');
	downloadLink.download = filename || 'export.csv';
	downloadLink.href = window.URL.createObjectURL(csvFile);
	downloadLink.style.display = 'none';
	document.body.appendChild(downloadLink);
	downloadLink.click();
	document.body.removeChild(downloadLink);
}

document.addEventListener('DOMContentLoaded', function() {
	const forms = document.querySelectorAll('form[data-confirm]');
	forms.forEach(form => {
		form.addEventListener('submit', function(e) {
			if (!confirm(this.dataset.confirm)) {
				e.preventDefault();
			}
		});
	});

	const numInputs = document.querySelectorAll('input[type="number"]');
	numInputs.forEach(input => {
		input.addEventListener('keypress', function(e) {
			if (e.key === 'e' || e.key === 'E' || e.key === '+' || e.key === '-') {
				e.preventDefault();
			}
		});
	});
});