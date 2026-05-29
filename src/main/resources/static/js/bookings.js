/**
 * shared booking table utilities, used by dashboard, bookings and admin pages.
 */

/**
 * renders a table of bookings as an html string.
 * @param {Array}   bookings       - array of booking objects from the api
 * @param {boolean} showCancel     - whether to show the action buttons column
 * @param {boolean} showUser       - show username column, for admin view
 * @param {boolean} showReschedule - show reschedule button on drivers own bookings
 * @returns {string} html string
 */
function renderBookingsTable(bookings, showCancel, showUser = false, showReschedule = false) {
  const rows = bookings.map(b => {
    const statusClass   = b.status === 'CANCELLED'  ? 'status-cancelled'
                        : b.status === 'COMPLETED'  ? 'status-completed'
                        :                             'status-confirmed';
    const statusLabel   = b.status;
    const canAct        = showCancel && b.status === 'CONFIRMED';

    let actions = '—';
    if (canAct) {
      actions = `<button class="btn btn-danger btn-sm cancel-btn" data-id="${b.id}">Cancel</button>`;
      if (showReschedule) {
        actions += ` <button class="btn btn-ghost btn-sm reschedule-btn" data-id="${b.id}">Reschedule</button>`;
      }
    }

    return `
      <tr>
        ${showUser ? `<td><code>${esc(b.username)}</code></td>` : ''}
        <td><code>#${b.id}</code></td>
        <td>${esc(b.stationName)}</td>
        <td>${esc(b.connectorType)}</td>
        <td>${esc(b.bookingDate)}</td>
        <td><code>${b.startTime.slice(0,5)} – ${b.endTime.slice(0,5)}</code></td>
        <td><span class="status-badge ${statusClass}">${statusLabel}</span></td>
        ${showCancel ? `<td>${actions}</td>` : ''}
      </tr>
    `;
  }).join('');

  return `
    <div class="table-wrapper">
      <table>
        <thead>
          <tr>
            ${showUser ? '<th>User</th>' : ''}
            <th>ID</th>
            <th>Station</th>
            <th>Type</th>
            <th>Date</th>
            <th>Time</th>
            <th>Status</th>
            ${showCancel ? '<th></th>' : ''}
          </tr>
        </thead>
        <tbody>${rows}</tbody>
      </table>
    </div>
  `;
}

/**
 * returns a styled empty state message when theres nothing to show
 */
function emptyState(msg) {
  return `<div class="loading-msg" style="color:var(--text-muted);">${msg}</div>`;
}

/**
 * returns true if the date string is before today
 */
function isPast(dateStr) {
  return new Date(dateStr) < new Date(new Date().toDateString());
}

function esc(str) {
  return String(str ?? '')
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;');
}
