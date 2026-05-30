function requireAuth() {
  const raw = sessionStorage.getItem('user');
  if (!raw) { window.location.href = '/login.html'; throw new Error('Not authenticated'); }
  return JSON.parse(raw);
}

function requireAdmin() {
  const user = requireAuth();
  if (user.role !== 'ADMIN') { window.location.href = '/dashboard.html'; throw new Error('Not admin'); }
  return user;
}

function buildNav(containerId, currentPath) {
  const nav = document.getElementById(containerId);
  if (!nav) return;

  const raw  = sessionStorage.getItem('user');
  const user = raw ? JSON.parse(raw) : null;

  if (!user) {
    nav.innerHTML = `
      <li><a href="/stations.html"${currentPath === '/stations.html' ? ' class="active"' : ''}>Stations</a></li>
      <li><a href="/login.html">Sign In</a></li>`;
    return;
  }

  const isAdmin = user.role === 'ADMIN';

  const links = isAdmin
    ? [
        { href: '/stations.html', label: 'Stations' },
        { href: '/admin.html',    label: 'Admin Panel' }
      ]
    : [
        { href: '/dashboard.html', label: 'Dashboard' },
        { href: '/stations.html',  label: 'Stations' },
        { href: '/bookings.html',  label: 'My Bookings' }
      ];

  nav.innerHTML =
    links.map(l =>
      `<li><a href="${l.href}"${l.href === currentPath ? ' class="active"' : ''}>${l.label}</a></li>`
    ).join('') +
    `<li style="margin-left:8px;"><span class="user-badge ${isAdmin ? 'badge-admin' : 'badge-driver'}">${esc(user.username)}</span></li>` +
    `<li style="margin-left:4px;"><button class="btn btn-ghost btn-sm" id="logout-btn">Sign Out</button></li>`;

  document.getElementById('logout-btn').addEventListener('click', async () => {
    await API.post('/api/auth/logout', {});
    sessionStorage.removeItem('user');
    window.location.href = '/login.html';
  });
}

function esc(str) {
  return String(str ?? '')
    .replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/"/g, '&quot;');
}

function showToast(message, type = 'success', duration = 4000) {
  let container = document.querySelector('.toast-container');
  if (!container) {
    container = document.createElement('div');
    container.className = 'toast-container';
    container.setAttribute('role', 'status');
    container.setAttribute('aria-live', 'polite');
    container.setAttribute('aria-label', 'Notifications');
    document.body.appendChild(container);
  }

  const icons = { success: '✓', error: '✕', warning: '⚠', info: 'ℹ' };

  const toast = document.createElement('div');
  toast.className = `toast toast-${type}`;
  toast.innerHTML = `
    <span class="toast__icon" aria-hidden="true">${icons[type] ?? 'ℹ'}</span>
    <span class="toast__message">${esc(message)}</span>
    <button class="toast__close" aria-label="Dismiss notification">✕</button>`;

  container.appendChild(toast);

  const dismiss = () => {
    toast.classList.add('toast-exit');
    toast.addEventListener('animationend', () => toast.remove(), { once: true });
  };

  toast.querySelector('.toast__close').addEventListener('click', dismiss);
  setTimeout(dismiss, duration);
}
