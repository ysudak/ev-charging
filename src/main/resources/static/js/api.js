/**
 * central api client used by every page.
 * all requests include credentials so session cookies get sent.
 * a 401 redirects to login except on login.html itself where it
 * throws the error message so the form can show it.
 */
const API = (() => {
  const baseUrl = '';

  async function request(method, path, body) {
    const opts = {
      method,
      credentials: 'include',
      headers: { 'Content-Type': 'application/json' }
    };
    if (body !== undefined) opts.body = JSON.stringify(body);

    const res  = await fetch(baseUrl + path, opts);
    const text = await res.text();
    const data = text ? JSON.parse(text) : null;

    if (res.status === 401) {
      sessionStorage.removeItem('user');
      if (!window.location.pathname.endsWith('/login.html')) {
        window.location.href = '/login.html';
        return;
      }
      // On the login page throw so the form's catch block can show the message
      throw new Error(data?.error || data?.message || 'Wrong username or password.');
    }

    if (!res.ok) {
      throw new Error(data?.message || data?.error || `HTTP ${res.status}`);
    }

    return data;
  }

  return {
    get:    (path)        => request('GET',    path),
    post:   (path, body)  => request('POST',   path, body),
    put:    (path, body)  => request('PUT',    path, body),
    delete: (path)        => request('DELETE', path)
  };
})();
