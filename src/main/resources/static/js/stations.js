/**
 * stations page logic.
 * loads all stations, renders the leaflet map and the station list on the side.
 * handles connector browsing and the booking modal when a driver picks a connector.
 */

let selectedConnectorId = null;

document.addEventListener('DOMContentLoaded', () => {
  buildNav('nav-links', '/stations.html');
  initMap(station => handleStationSelect(station.id));
  loadStationList();
  setupBookingModal();
});

function loadStationList() {
  API.get('/api/stations').then(stations => {
    const listEl = document.getElementById('station-list');
    if (!stations.length) {
      listEl.innerHTML = emptyStatePlain('No stations available.');
      return;
    }
    listEl.innerHTML = stations.map(s => `
      <div class="station-item" id="sitem-${s.id}" onclick="handleStationSelect(${s.id})">
        <div>
          <div class="station-name">${esc(s.name)}</div>
          <div class="station-addr">${esc(s.address)}</div>
        </div>
        <div style="text-align:right; flex-shrink:0;">
          <span style="font-size:.8rem; color:var(--text-muted);">${s.connectorCount} connector(s)</span>
        </div>
      </div>
    `).join('');
  }).catch(() => {
    document.getElementById('station-list').innerHTML =
      '<div class="alert alert-error">Could not load stations.</div>';
  });
}

function handleStationSelect(stationId) {
  document.querySelectorAll('.station-item').forEach(el => el.classList.remove('selected'));
  const item = document.getElementById(`sitem-${stationId}`);
  if (item) item.classList.add('selected');
  highlightMarker(stationId);
  loadConnectors(stationId);
}

function loadConnectors(stationId) {
  const panel = document.getElementById('connector-panel');
  const list  = document.getElementById('connector-list');
  panel.style.display = 'block';
  list.innerHTML = '<div class="spinner"></div>';

  const userRaw = sessionStorage.getItem('user');
  const isAdmin = userRaw ? JSON.parse(userRaw).role === 'ADMIN' : false;

  Promise.all([
    API.get(`/api/stations/${stationId}`),
    API.get(`/api/stations/${stationId}/connectors`)
  ]).then(([station, connectors]) => {
    document.getElementById('panel-station-name').textContent = station.name;
    if (!connectors.length) {
      list.innerHTML = emptyStatePlain('No connectors at this station.');
      return;
    }
    list.innerHTML = `
      <div class="table-wrapper">
        <table>
          <thead>
            <tr><th>ID</th><th>Type</th><th>Power</th>${!isAdmin ? '<th></th>' : ''}</tr>
          </thead>
          <tbody>
            ${connectors.map(c => `
              <tr>
                <td><code>#${c.id}</code></td>
                <td>${esc(c.connectorType)}</td>
                <td>${c.powerKw} kW</td>
                ${!isAdmin ? `
                <td>
                  <button class="btn btn-primary btn-sm"
                    onclick="openBookingModal(${c.id}, '${esc(c.connectorType)}', ${c.powerKw})">
                    Book
                  </button>
                </td>` : ''}
              </tr>
            `).join('')}
          </tbody>
        </table>
      </div>
    `;
  }).catch(() => {
    list.innerHTML = '<div class="alert alert-error">Failed to load connectors.</div>';
  });
}

function localDateStr(d) {
  return `${d.getFullYear()}-${String(d.getMonth()+1).padStart(2,'0')}-${String(d.getDate()).padStart(2,'0')}`;
}

function openBookingModal(connectorId, connectorType, powerKw) {
  if (!sessionStorage.getItem('user')) {
    window.location.href = '/login.html';
    return;
  }
  selectedConnectorId = connectorId;
  document.getElementById('modal-connector-info').textContent =
    `Connector #${connectorId} — ${connectorType} ${powerKw} kW`;
  document.getElementById('modal-alert').innerHTML = '';

  const tomorrow = new Date();
  tomorrow.setDate(tomorrow.getDate() + 1);
  const dateStr = localDateStr(tomorrow);
  document.getElementById('book-date').value = dateStr;
  document.getElementById('book-date').min   = localDateStr(new Date());

  document.getElementById('booking-modal').classList.add('open');
  renderWeekStrip(connectorId, dateStr);
  loadBookedTimes(connectorId, dateStr);
}

function renderWeekStrip(connectorId, selectedDate) {
  const strip = document.getElementById('week-strip-days');
  strip.innerHTML = '';

  const days   = ['Sun','Mon','Tue','Wed','Thu','Fri','Sat'];
  const months = ['Jan','Feb','Mar','Apr','May','Jun','Jul','Aug','Sep','Oct','Nov','Dec'];
  const today  = new Date();
  today.setHours(0,0,0,0);

  // build 7 day objects starting from today
  const dates = Array.from({ length: 7 }, (_, i) => {
    const d = new Date(today);
    d.setDate(today.getDate() + i);
    return d;
  });

  // render the day buttons first, then load booking counts in the background to colour the dots
  dates.forEach(d => {
    const dateStr = localDateStr(d);
    const btn = document.createElement('button');
    btn.type = 'button';
    btn.className = 'week-day-btn' + (dateStr === selectedDate ? ' active' : '');
    btn.dataset.date = dateStr;
    btn.innerHTML = `
      <span class="week-day-name">${days[d.getDay()]}</span>
      <span class="week-day-num">${d.getDate()} ${months[d.getMonth()]}</span>
      <span class="week-day-dot" id="dot-${dateStr}"></span>`;
    btn.addEventListener('click', () => {
      strip.querySelectorAll('.week-day-btn').forEach(b => b.classList.remove('active'));
      btn.classList.add('active');
      document.getElementById('book-date').value = dateStr;
      loadBookedTimes(connectorId, dateStr);
    });
    strip.appendChild(btn);
  });

  // load booking counts for all 7 days in parallel so the dots get colored
  Promise.all(dates.map(d => {
    const ds = localDateStr(d);
    return API.get(`/api/connectors/${connectorId}/booked-times?date=${ds}`)
      .then(times => ({ date: ds, count: times.length }))
      .catch(() => ({ date: ds, count: 0 }));
  })).then(results => {
    results.forEach(({ date, count }) => {
      const dot = document.getElementById(`dot-${date}`);
      if (!dot) return;
      if (count === 0) {
        dot.className = 'week-day-dot dot-free';
        dot.title = 'No bookings';
      } else {
        dot.className = 'week-day-dot dot-busy';
        dot.title = `${count} booking${count > 1 ? 's' : ''}`;
      }
    });
  });
}

async function loadBookedTimes(connectorId, date) {
  const section = document.getElementById('booked-times-section');
  const list    = document.getElementById('booked-times-list');
  if (!connectorId || !date) { section.style.display = 'none'; return; }
  section.style.display = 'block';
  list.innerHTML = '<span style="font-size:.8rem; color:var(--text-muted);">Loading…</span>';
  try {
    const times = await API.get(`/api/connectors/${connectorId}/booked-times?date=${date}`);
    if (!times.length) {
      list.innerHTML = '<span style="font-size:.8rem; color:var(--success);">No bookings on this date — all yours!</span>';
    } else {
      list.innerHTML = times.map(t =>
        `<span class="booked-badge">${t.startTime.substring(0,5)} – ${t.endTime.substring(0,5)}</span>`
      ).join('');
    }
  } catch {
    list.innerHTML = '<span style="font-size:.8rem; color:var(--text-muted);">Could not load booked times.</span>';
  }
}

function setupBookingModal() {
  document.getElementById('cancel-modal-btn').addEventListener('click', () => {
    document.getElementById('booking-modal').classList.remove('open');
  });

  document.getElementById('booking-modal').addEventListener('click', e => {
    if (e.target === document.getElementById('booking-modal')) {
      document.getElementById('booking-modal').classList.remove('open');
    }
  });

  document.getElementById('book-date').addEventListener('change', e => {
    loadBookedTimes(selectedConnectorId, e.target.value);
  });

  document.getElementById('confirm-book-btn').addEventListener('click', async () => {
    const btn   = document.getElementById('confirm-book-btn');
    const date  = document.getElementById('book-date').value;
    const start = document.getElementById('book-start').value;
    const end   = document.getElementById('book-end').value;

    if (!date || !start || !end) {
      showModalAlert('Please fill in all fields.', 'error');
      return;
    }

    btn.disabled = true;
    btn.textContent = 'Booking…';

    try {
      await API.post('/api/bookings', {
        connectorId: selectedConnectorId,
        bookingDate: date,
        startTime: start + ':00',
        endTime:   end   + ':00'
      });
      document.getElementById('booking-modal').classList.remove('open');
      showToast('Booking confirmed! View it in My Bookings.', 'success');
    } catch (err) {
      showModalAlert(err.message || 'Booking failed.', 'error');
    } finally {
      btn.disabled = false;
      btn.textContent = 'Confirm Booking';
    }
  });
}

function showModalAlert(msg, type) {
  document.getElementById('modal-alert').innerHTML =
    `<div class="alert alert-${type}">${msg}</div>`;
}

function emptyStatePlain(msg) {
  return `<p style="color:var(--text-muted); padding:.5rem;">${msg}</p>`;
}

function esc(str) {
  return String(str ?? '')
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;');
}
