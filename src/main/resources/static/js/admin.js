const PAGE_SIZE = 10;
let allAdminBookings = [];
let adminPage   = 1;
let adminFilter = 'ALL';
let adminSearch = '';

document.addEventListener('DOMContentLoaded', () => {
  requireAdmin();
  buildNav('nav-links', '/admin.html');

  document.getElementById('toggle-form-btn').addEventListener('click', () => {
    const wrap = document.getElementById('station-form-wrap');
    wrap.style.display = wrap.style.display === 'none' ? 'block' : 'none';
    if (wrap.style.display === 'block') resetStationForm();
  });

  document.getElementById('add-station-btn').addEventListener('click', saveStation);
  document.getElementById('refresh-bookings-btn').addEventListener('click', loadAllBookings);
  document.getElementById('add-connector-btn').addEventListener('click', addConnector);

  document.querySelectorAll('[data-bstatus]').forEach(pill => {
    pill.addEventListener('click', () => {
      document.querySelectorAll('[data-bstatus]').forEach(p => p.classList.remove('active'));
      pill.classList.add('active');
      adminFilter = pill.dataset.bstatus;
      adminPage   = 1;
      renderAdminBookings();
    });
  });

  let searchTimer;
  document.getElementById('bookings-search').addEventListener('input', e => {
    clearTimeout(searchTimer);
    searchTimer = setTimeout(() => {
      adminSearch = e.target.value.trim().toLowerCase();
      adminPage   = 1;
      renderAdminBookings();
    }, 300);
  });

  loadStats();
  loadAllBookings();
  loadAllUsers();
});

async function loadStats() {
  try {
    const [users, stations, bookings] = await Promise.all([
      API.get('/api/admin/users'),
      API.get('/api/stations'),
      API.get('/api/admin/bookings')
    ]);
    document.getElementById('stat-users').textContent     = users.length;
    document.getElementById('stat-stations').textContent  = stations.length;
    document.getElementById('stat-confirmed').textContent =
      bookings.filter(b => b.status === 'CONFIRMED').length;
    document.getElementById('stat-cancelled').textContent =
      bookings.filter(b => b.status === 'CANCELLED').length;
  } catch (e) {
    console.error('Stats load failed', e);
  }
}

function loadAllBookings() {
  const container = document.getElementById('all-bookings-container');
  container.innerHTML = '<div class="spinner"></div>';
  document.getElementById('bookings-pagination').innerHTML = '';
  document.getElementById('bookings-count').textContent = '';

  API.get('/api/admin/bookings').then(bookings => {
    allAdminBookings = bookings;
    adminPage = 1;
    renderAdminBookings();
  }).catch(() => {
    container.innerHTML = '<div class="alert alert-error">Failed to load bookings.</div>';
  });
}

function renderAdminBookings() {
  const container = document.getElementById('all-bookings-container');

  let filtered;
  if (adminFilter === 'ALL') {
    filtered = allAdminBookings;
  } else {
    filtered = allAdminBookings.filter(b => b.status === adminFilter);
  }

  if (adminSearch) {
    filtered = filtered.filter(b =>
      (b.username    || '').toLowerCase().includes(adminSearch) ||
      (b.stationName || '').toLowerCase().includes(adminSearch)
    );
  }

  const total      = filtered.length;
  const totalPages = Math.max(1, Math.ceil(total / PAGE_SIZE));
  adminPage = Math.min(adminPage, totalPages);

  document.getElementById('bookings-count').textContent = total === allAdminBookings.length
    ? `${total} booking${total !== 1 ? 's' : ''}`
    : `${total} of ${allAdminBookings.length}`;

  const start = (adminPage - 1) * PAGE_SIZE;
  const page  = filtered.slice(start, start + PAGE_SIZE);

  if (!page.length) {
    container.innerHTML = emptyState(
      total === 0 && !adminSearch && adminFilter === 'ALL'
        ? 'No bookings in the system.'
        : 'No bookings match your filter.'
    );
  } else {
    container.innerHTML = renderBookingsTable(page, true, true);
    attachAdminCancelListeners();
  }

  renderPagination(totalPages, total, start + 1, Math.min(start + PAGE_SIZE, total));
}

function renderPagination(totalPages, total, from, to) {
  const el = document.getElementById('bookings-pagination');
  if (totalPages <= 1) { el.innerHTML = ''; return; }
  el.innerHTML = `
    <div class="pagination">
      <span class="pagination-info">Showing ${from}–${to} of ${total}</span>
      <div class="pagination-controls">
        <button class="btn btn-ghost btn-sm" id="pg-prev" ${adminPage <= 1 ? 'disabled' : ''}>← Prev</button>
        <span class="pagination-pages">Page ${adminPage} / ${totalPages}</span>
        <button class="btn btn-ghost btn-sm" id="pg-next" ${adminPage >= totalPages ? 'disabled' : ''}>Next →</button>
      </div>
    </div>`;
  document.getElementById('pg-prev').addEventListener('click', () => {
    if (adminPage > 1) { adminPage--; renderAdminBookings(); }
  });
  document.getElementById('pg-next').addEventListener('click', () => {
    if (adminPage < totalPages) { adminPage++; renderAdminBookings(); }
  });
}

function attachAdminCancelListeners() {
  document.querySelectorAll('.cancel-btn').forEach(btn => {
    btn.addEventListener('click', () => {
      const id  = btn.dataset.id;
      const row = btn.closest('tr');
      if (!row) return;

      document.querySelectorAll('.admin-cancel-confirm').forEach(r => r.remove());

      const confirmRow = document.createElement('tr');
      confirmRow.className = 'admin-cancel-confirm';
      confirmRow.innerHTML = `
        <td colspan="99" class="cancel-confirm-row">
          <span>Cancel booking <strong>#${id}</strong>?</span>
          <div class="confirm-actions">
            <button class="btn btn-danger btn-sm confirm-yes" data-id="${id}">Yes, Cancel</button>
            <button class="btn btn-ghost btn-sm confirm-no">Keep</button>
          </div>
        </td>`;
      row.after(confirmRow);

      confirmRow.querySelector('.confirm-no').addEventListener('click', () => confirmRow.remove());
      confirmRow.querySelector('.confirm-yes').addEventListener('click', async function () {
        this.disabled    = true;
        this.textContent = 'Cancelling…';
        try {
          await API.delete(`/api/admin/bookings/${id}`);
          showToast('Booking cancelled.', 'success');
          const bookings = await API.get('/api/admin/bookings');
          allAdminBookings = bookings;
          renderAdminBookings();
          loadStats();
        } catch (err) {
          showToast(err.message, 'error');
          confirmRow.remove();
          btn.disabled = false;
        }
      });
    });
  });
}

function loadAllUsers() {
  const container = document.getElementById('users-container');
  API.get('/api/admin/users').then(users => {
    if (!users.length) { container.innerHTML = emptyState('No users registered.'); return; }
    container.innerHTML = `
      <div class="table-wrapper">
        <table>
          <thead><tr><th>ID</th><th>Username</th><th>Role</th></tr></thead>
          <tbody>
            ${users.map(u => `
              <tr>
                <td><code>#${u.id}</code></td>
                <td>${esc(u.username)}</td>
                <td><span class="user-badge ${u.role === 'ADMIN' ? 'badge-admin' : 'badge-driver'}">${u.role}</span></td>
              </tr>`).join('')}
          </tbody>
        </table>
      </div>`;
  }).catch(() => {
    container.innerHTML = '<div class="alert alert-error">Failed to load users.</div>';
  });
}

function resetStationForm() {
  document.getElementById('stn-edit-id').value = '';
  document.getElementById('station-form-title').textContent = 'New Station';
  document.getElementById('add-station-btn').textContent = 'Add Station';
  ['stn-name','stn-addr','stn-lat','stn-lng','stn-desc'].forEach(id => {
    document.getElementById(id).value = '';
  });
}

function openEditStation(id, name, addr, lat, lng, desc) {
  document.getElementById('stn-edit-id').value = id;
  document.getElementById('station-form-title').textContent = 'Edit Station';
  document.getElementById('add-station-btn').textContent = 'Save Changes';
  document.getElementById('stn-name').value = name;
  document.getElementById('stn-addr').value = addr;
  document.getElementById('stn-lat').value  = lat;
  document.getElementById('stn-lng').value  = lng;
  document.getElementById('stn-desc').value = desc;
  document.getElementById('station-form-wrap').style.display = 'block';
  document.getElementById('station-form-wrap').scrollIntoView({ behavior: 'smooth', block: 'start' });
}

async function saveStation() {
  const btn    = document.getElementById('add-station-btn');
  const editId = document.getElementById('stn-edit-id').value;
  const name   = document.getElementById('stn-name').value.trim();
  const addr   = document.getElementById('stn-addr').value.trim();
  const lat    = parseFloat(document.getElementById('stn-lat').value);
  const lng    = parseFloat(document.getElementById('stn-lng').value);
  const desc   = document.getElementById('stn-desc').value.trim();

  if (!name || !addr || isNaN(lat) || isNaN(lng)) {
    showToast('Name, address, latitude, and longitude are required.', 'error');
    return;
  }

  btn.disabled = true;
  try {
    if (editId) {
      await API.put(`/api/stations/${editId}`, { name, address: addr, latitude: lat, longitude: lng, description: desc });
      showToast(`Station "${name}" updated.`, 'success');
    } else {
      await API.post('/api/stations', { name, address: addr, latitude: lat, longitude: lng, description: desc });
      showToast(`Station "${name}" added.`, 'success');
    }
    document.getElementById('station-form-wrap').style.display = 'none';
    resetStationForm();
    delete document.getElementById('stations-table-container').dataset.loaded;
    loadStationsList(true);
    loadStats();
  } catch (err) {
    showToast(err.message || 'Failed to save station.', 'error');
  } finally {
    btn.disabled = false;
  }
}

async function deleteStation(id, name) {
  document.querySelectorAll('.admin-cancel-confirm').forEach(r => r.remove());

  const rows = document.querySelectorAll('#stations-table-container tbody tr');
  let targetRow = null;
  rows.forEach(row => {
    if (row.querySelector(`button[onclick*="deleteStation(${id},"]`)) targetRow = row;
  });

  const confirmRow = document.createElement('tr');
  confirmRow.className = 'admin-cancel-confirm';
  confirmRow.innerHTML = `
    <td colspan="99" class="cancel-confirm-row">
      <span>Delete station <strong>${esc(name)}</strong> and all its connectors/bookings?</span>
      <div class="confirm-actions">
        <button class="btn btn-danger btn-sm confirm-yes">Yes, Delete</button>
        <button class="btn btn-ghost btn-sm confirm-no">Cancel</button>
      </div>
    </td>`;
  if (targetRow) targetRow.after(confirmRow);
  else document.getElementById('stations-table-container').appendChild(confirmRow);

  confirmRow.querySelector('.confirm-no').addEventListener('click', () => confirmRow.remove());
  confirmRow.querySelector('.confirm-yes').addEventListener('click', async function () {
    this.disabled    = true;
    this.textContent = 'Deleting…';
    try {
      await API.delete(`/api/stations/${id}`);
      showToast(`Station "${name}" deleted.`, 'success');
      delete document.getElementById('stations-table-container').dataset.loaded;
      loadStationsList(true);
      loadStats();
      if (document.getElementById('conn-station-id').value == id) {
        document.getElementById('connector-mgmt-panel').style.display = 'none';
      }
    } catch (err) {
      showToast(err.message || 'Failed to delete station.', 'error');
      confirmRow.remove();
    }
  });
}

function openConnectorMgmt(stationId, stationName) {
  document.getElementById('conn-station-id').value = stationId;
  document.getElementById('connector-mgmt-title').textContent = `Connectors — ${stationName}`;
  document.getElementById('connector-mgmt-panel').style.display = 'block';
  document.getElementById('conn-power').value = '';
  loadConnectorList(stationId);
  document.getElementById('connector-mgmt-panel').scrollIntoView({ behavior: 'smooth', block: 'start' });
}

async function loadConnectorList(stationId) {
  const list = document.getElementById('connector-list-admin');
  list.innerHTML = '<div class="spinner"></div>';
  try {
    const connectors = await API.get(`/api/stations/${stationId}/connectors`);
    if (!connectors.length) {
      list.innerHTML = '<p style="color:var(--text-muted); font-size:.9rem;">No connectors yet. Add one above.</p>';
      return;
    }
    list.innerHTML = `
      <div class="table-wrapper">
        <table>
          <thead><tr><th>ID</th><th>Type</th><th>Power (kW)</th><th></th></tr></thead>
          <tbody>
            ${connectors.map(c => `
              <tr id="conn-row-${c.id}">
                <td><code>#${c.id}</code></td>
                <td id="conn-type-cell-${c.id}">${esc(c.connectorType)}</td>
                <td id="conn-power-cell-${c.id}">${c.powerKw}</td>
                <td class="table-actions">
                  <button class="btn btn-ghost btn-sm" onclick="editConnectorInline(${c.id},'${esc(c.connectorType)}',${c.powerKw})">Edit</button>
                  <button class="btn btn-danger btn-sm" onclick="deleteConnector(${c.id},${stationId})">Delete</button>
                </td>
              </tr>`).join('')}
          </tbody>
        </table>
      </div>`;
  } catch {
    list.innerHTML = '<div class="alert alert-error">Failed to load connectors.</div>';
  }
}

async function addConnector() {
  const stationId = document.getElementById('conn-station-id').value;
  const type      = document.getElementById('conn-type').value;
  const power     = parseInt(document.getElementById('conn-power').value, 10);
  const btn       = document.getElementById('add-connector-btn');

  if (!power || power < 1) { showToast('Enter a valid power (kW).', 'error'); return; }

  btn.disabled = true;
  try {
    await API.post(`/api/stations/${stationId}/connectors`, { connectorType: type, powerKw: power });
    document.getElementById('conn-power').value = '';
    showToast('Connector added.', 'success');
    loadConnectorList(stationId);
    delete document.getElementById('stations-table-container').dataset.loaded;
    loadStationsList(true);
    loadStats();
  } catch (err) {
    showToast(err.message || 'Failed to add connector.', 'error');
  } finally {
    btn.disabled = false;
  }
}

function editConnectorInline(connId, currentType, currentPower) {
  const row = document.getElementById(`conn-row-${connId}`);
  if (!row) return;

  row.querySelector(`#conn-type-cell-${connId}`).innerHTML =
    `<select class="form-control" id="edit-conn-type-${connId}" style="padding:4px 8px; height:32px;">
       <option value="Type 2"${currentType==='Type 2'?' selected':''}>Type 2</option>
       <option value="CCS"${currentType==='CCS'?' selected':''}>CCS</option>
       <option value="CHAdeMO"${currentType==='CHAdeMO'?' selected':''}>CHAdeMO</option>
       <option value="Tesla"${currentType==='Tesla'?' selected':''}>Tesla</option>
     </select>`;
  row.querySelector(`#conn-power-cell-${connId}`).innerHTML =
    `<input type="number" class="form-control" id="edit-conn-power-${connId}"
            value="${currentPower}" min="1" style="width:80px; padding:4px 8px; height:32px;"/>`;

  const actions = row.querySelector('.table-actions');
  actions.innerHTML = `
    <button class="btn btn-success btn-sm" onclick="saveConnectorEdit(${connId})">Save</button>
    <button class="btn btn-ghost btn-sm" onclick="loadConnectorList(${document.getElementById('conn-station-id').value})">Cancel</button>`;
}

async function saveConnectorEdit(connId) {
  const type  = document.getElementById(`edit-conn-type-${connId}`).value;
  const power = parseInt(document.getElementById(`edit-conn-power-${connId}`).value, 10);
  if (!power || power < 1) { showToast('Enter a valid power.', 'error'); return; }

  try {
    await API.put(`/api/connectors/${connId}`, { connectorType: type, powerKw: power });
    showToast('Connector updated.', 'success');
    loadConnectorList(document.getElementById('conn-station-id').value);
  } catch (err) {
    showToast(err.message || 'Failed to update connector.', 'error');
  }
}

async function deleteConnector(connId, stationId) {
  const row = document.getElementById(`conn-row-${connId}`);
  if (!row) return;

  const confirmRow = document.createElement('tr');
  confirmRow.className = 'admin-cancel-confirm';
  confirmRow.innerHTML = `
    <td colspan="99" class="cancel-confirm-row">
      <span>Delete connector <strong>#${connId}</strong>?</span>
      <div class="confirm-actions">
        <button class="btn btn-danger btn-sm confirm-yes">Yes, Delete</button>
        <button class="btn btn-ghost btn-sm confirm-no">Cancel</button>
      </div>
    </td>`;
  row.after(confirmRow);

  confirmRow.querySelector('.confirm-no').addEventListener('click', () => confirmRow.remove());
  confirmRow.querySelector('.confirm-yes').addEventListener('click', async function () {
    this.disabled    = true;
    this.textContent = 'Deleting…';
    try {
      await API.delete(`/api/connectors/${connId}`);
      showToast('Connector deleted.', 'success');
      loadConnectorList(stationId);
      delete document.getElementById('stations-table-container').dataset.loaded;
      loadStationsList(true);
      loadStats();
    } catch (err) {
      showToast(err.message || 'Failed to delete connector.', 'error');
      confirmRow.remove();
    }
  });
}

