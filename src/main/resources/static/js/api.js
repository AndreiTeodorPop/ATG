/* ============================================================
   Castorama API Client — thin wrapper around fetch
   ============================================================ */

const API = (() => {
  const BASE = '';   // same origin

  function token() { return localStorage.getItem('token'); }

  function authHeader() {
    const t = token();
    return t ? { 'Authorization': 'Bearer ' + t } : {};
  }

  async function request(method, path, body) {
    const opts = {
      method,
      headers: { 'Content-Type': 'application/json', ...authHeader() }
    };
    if (body !== undefined) opts.body = JSON.stringify(body);
    const res = await fetch(BASE + path, opts);
    if (res.status === 204) return null;
    const data = await res.json().catch(() => ({}));
    if (!res.ok) {
      let msg = data.detail || data.message || data.error || `Erreur ${res.status}`;
      if (data.fieldErrors) {
        msg += ' — ' + Object.entries(data.fieldErrors).map(([f, m]) => `${f}: ${m}`).join(', ');
      }
      throw new Error(msg);
    }
    return data;
  }

  return {
    /* ── Auth ── */
    register: (payload) => request('POST', '/api/v1/auth/register', payload),
    login:    (payload) => request('POST', '/api/v1/auth/login',    payload),
    getMe:    ()        => request('GET',  '/api/v1/auth/me'),

    /* ── Catalog ── */
    getProducts:     (page = 0, size = 12) =>
      request('GET', `/api/v1/catalog/products?page=${page}&size=${size}`),
    getProduct:      (id) =>
      request('GET', `/api/v1/catalog/products/${id}`),
    getByCategory:   (cat, page = 0, size = 12) =>
      request('GET', `/api/v1/catalog/categories/${cat}/products?page=${page}&size=${size}`),
    search:          (q, page = 0, size = 12) =>
      request('GET', `/api/v1/catalog/search?q=${encodeURIComponent(q)}&page=${page}&size=${size}`),
    getOnSale:       (page = 0, size = 12) =>
      request('GET', `/api/v1/catalog/products/on-sale?page=${page}&size=${size}`),

    /* ── Cart ── */
    getCart:       ()                 => request('GET',    '/api/v1/cart'),
    addToCart:     (skuCode, qty)     => request('POST',   '/api/v1/cart/items', { skuCode, quantity: qty }),
    updateCartItem:(id, quantity)     => request('PUT',    `/api/v1/cart/items/${id}?quantity=${quantity}`),
    removeCartItem:(id)               => request('DELETE', `/api/v1/cart/items/${id}`),
    clearCart:     ()                 => request('DELETE', '/api/v1/cart'),

    /* ── Orders ── */
    checkout:    (payload) => request('POST',   '/api/v1/orders/checkout', payload),
    getOrders:   ()        => request('GET',    '/api/v1/orders'),
    getOrder:    (num)     => request('GET',    `/api/v1/orders/${num}`),
    cancelOrder: (num)     => request('POST',   `/api/v1/orders/${num}/cancel`),
  };
})();
