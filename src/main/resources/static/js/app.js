/* ============================================================
   Castorama SPA — Main Application
   ============================================================ */

/* ── State ── */
let state = {
  user:   null,
  token:  localStorage.getItem('token'),
  cart:   null,
};

/* ── Category metadata ── */
const CATEGORIES = [
  { id: 'OUTILLAGE',    label: 'Outillage',        icon: 'fa-tools' },
  { id: 'PEINTURE',     label: 'Peinture',          icon: 'fa-paint-roller' },
  { id: 'SOL',          label: 'Sols & Murs',       icon: 'fa-border-all' },
  { id: 'PLOMBERIE',    label: 'Plomberie',         icon: 'fa-faucet' },
  { id: 'ELECTRICITE',  label: 'Électricité',       icon: 'fa-bolt' },
  { id: 'JARDIN',       label: 'Jardin',            icon: 'fa-leaf' },
  { id: 'MENUISERIE',   label: 'Menuiserie',        icon: 'fa-door-open' },
  { id: 'QUINCAILLERIE',label: 'Quincaillerie',     icon: 'fa-cogs' },
  { id: 'CHAUFFAGE',    label: 'Chauffage',         icon: 'fa-fire' },
  { id: 'CUISINE',      label: 'Cuisine & Bain',    icon: 'fa-sink' },
];

/* ── Bootstrap ── */
window.addEventListener('DOMContentLoaded', async () => {
  if (state.token) {
    try { state.user = await API.getMe(); } catch (_) {
      localStorage.removeItem('token');
      state.token = null;
    }
  }
  await refreshCart();
  updateHeader();
  navigate('home');
});

/* ── Toast ── */
function toast(msg, type = '') {
  const el = document.getElementById('toast');
  el.textContent = msg;
  el.className = 'toast show ' + type;
  setTimeout(() => { el.className = 'toast'; }, 3000);
}

/* ── Format price ── */
function fmt(n) {
  if (n == null) return '';
  return parseFloat(n).toLocaleString('fr-FR', { style: 'currency', currency: 'EUR' });
}

/* ── Product image ── */
function imgSrc(p) {
  // Use picsum with SKU seed as placeholder (Castorama media URLs are not publicly accessible)
  return `https://picsum.photos/seed/${p.skuCode || p.id}/400/300`;
}

/* ── Header update ── */
function updateHeader() {
  const label = document.getElementById('accountLabel');
  if (state.user) {
    label.textContent = state.user.firstName || state.user.email;
  } else {
    label.textContent = 'Connexion';
  }
  updateCartBadge();
}

function updateCartBadge() {
  const badge = document.getElementById('cartBadge');
  const count = state.cart ? state.cart.itemCount : 0;
  badge.textContent = count;
  badge.classList.toggle('hidden', count === 0);
}

/* ── Navigation ── */
function navigate(page, params = {}) {
  closeAccountMenu();
  // update active nav
  document.querySelectorAll('.cat-link').forEach(el => el.classList.remove('active'));
  const navId = params.category ? 'nav-' + params.category : 'nav-' + page;
  const navEl = document.getElementById(navId);
  if (navEl) navEl.classList.add('active');

  const main = document.getElementById('main');
  main.innerHTML = '<div class="page-loading"><i class="fas fa-spinner fa-spin fa-2x"></i></div>';

  switch (page) {
    case 'home':    renderHome();                      break;
    case 'catalog': renderCatalog(params.category, params.page || 0); break;
    case 'search':  renderSearch(params.q, params.page || 0);         break;
    case 'sales':   renderSales(params.page || 0);    break;
    case 'product': renderProduct(params.id);         break;
    case 'orders':  renderOrders();                   break;
    default:        renderHome();
  }
}

/* ── Product card HTML ── */
function productCardHTML(p) {
  const priceBlock = p.onSale
    ? `<div class="product-prices">
         <span class="price-current">${fmt(p.salePrice)}</span>
         <span class="price-old">${fmt(p.listPrice)}</span>
       </div>`
    : `<div class="product-prices"><span class="price-regular">${fmt(p.effectivePrice)}</span></div>`;

  const stockHTML = p.inStock
    ? `<span class="stock-ok"><i class="fas fa-check-circle"></i> En stock</span>`
    : `<span class="stock-out"><i class="fas fa-times-circle"></i> Rupture de stock</span>`;

  return `
    <div class="product-card" onclick="navigate('product',{id:${p.id}})">
      <div class="product-card-img">
        ${p.onSale ? '<span class="badge-sale"><i class="fas fa-tag"></i> Promo</span>' : ''}
        <img src="${imgSrc(p)}" alt="${escHtml(p.name)}"
             onerror="this.src='https://picsum.photos/seed/${p.id}/400/300'">
      </div>
      <div class="product-card-body">
        <div class="product-brand">${escHtml(p.brand || '')}</div>
        <div class="product-name">${escHtml(p.name)}</div>
        ${priceBlock}
        ${stockHTML}
        <button class="btn-add-cart" ${!p.inStock ? 'disabled' : ''}
                onclick="event.stopPropagation();addToCart('${p.skuCode}')">
          <i class="fas fa-cart-plus"></i> Ajouter au panier
        </button>
      </div>
    </div>`;
}

/* ── Pagination HTML ── */
function paginationHTML(page, data, navFn) {
  if (data.totalPages <= 1) return '';
  const prev = page > 0
    ? `<button class="page-btn" onclick="${navFn}(${page - 1})"><i class="fas fa-chevron-left"></i></button>` : '';
  const next = page < data.totalPages - 1
    ? `<button class="page-btn" onclick="${navFn}(${page + 1})"><i class="fas fa-chevron-right"></i></button>` : '';
  let pages = '';
  for (let i = 0; i < data.totalPages; i++) {
    pages += `<button class="page-btn ${i === page ? 'active' : ''}" onclick="${navFn}(${i})">${i + 1}</button>`;
  }
  return `<div class="pagination">${prev}${pages}${next}</div>`;
}

/* ── HOME ── */
async function renderHome() {
  try {
    const [featured, onSale] = await Promise.all([
      API.getProducts(0, 8),
      API.getOnSale(0, 4),
    ]);

    const catGrid = CATEGORIES.map(c => `
      <div class="cat-card" onclick="navigate('catalog',{category:'${c.id}'})">
        <div class="cat-card-icon"><i class="fas ${c.icon}"></i></div>
        <span>${c.label}</span>
      </div>`).join('');

    const saleCards = (onSale.content || []).map(productCardHTML).join('');
    const featCards = (featured.content || []).map(productCardHTML).join('');

    document.getElementById('main').innerHTML = `
      <div class="hero">
        <h1>Le meilleur du bricolage</h1>
        <p>Tout pour rénover, décorer et aménager votre maison</p>
        <a class="hero-cta" onclick="navigate('catalog',{category:'OUTILLAGE'})">
          <i class="fas fa-tools"></i> Découvrir nos produits
        </a>
      </div>

      <div class="section">
        <h2 class="section-title">Nos rayons</h2>
        <div class="cat-grid">${catGrid}</div>
      </div>

      ${saleCards ? `
      <div class="promo-banner">
        <i class="fas fa-fire"></i> Promotions en cours — jusqu'à -30% sur une sélection de produits
      </div>
      <div class="section">
        <h2 class="section-title"><i class="fas fa-tag" style="color:var(--red);margin-right:8px"></i>Promotions</h2>
        <div class="product-grid">${saleCards}</div>
      </div>` : ''}

      <div class="section">
        <h2 class="section-title">Notre sélection</h2>
        <div class="product-grid">${featCards}</div>
      </div>`;
  } catch (e) {
    document.getElementById('main').innerHTML = errorHTML(e.message);
  }
}

/* ── CATALOG ── */
async function renderCatalog(category, page = 0) {
  try {
    const data = await API.getByCategory(category, page, 12);
    const catMeta = CATEGORIES.find(c => c.id === category) || { label: category };
    const cards = (data.content || []).map(productCardHTML).join('');
    const totalStr = `${data.totalElements} produit${data.totalElements !== 1 ? 's' : ''}`;

    const paginFn = `(p)=>navigate('catalog',{category:'${category}',page:p})`;

    document.getElementById('main').innerHTML = `
      <div class="page-header">
        <div class="page-header-inner">
          <h2><i class="fas ${catMeta.icon || 'fa-box'}"></i> ${catMeta.label}</h2>
          <p>${totalStr}</p>
        </div>
      </div>
      <div class="section">
        ${cards ? `<div class="product-grid">${cards}</div>` : noResultsHTML('Aucun produit dans cette catégorie')}
        ${paginationHTML(page, data, `(p)=>navigate('catalog',{category:'${category}',page:p})`)}
      </div>`;
  } catch (e) {
    document.getElementById('main').innerHTML = errorHTML(e.message);
  }
}

/* ── SEARCH ── */
async function renderSearch(q, page = 0) {
  if (!q) { navigate('home'); return; }
  document.getElementById('searchInput').value = q;
  try {
    const data = await API.search(q, page, 12);
    const cards = (data.content || []).map(productCardHTML).join('');
    const totalStr = `${data.totalElements} résultat${data.totalElements !== 1 ? 's' : ''} pour "${escHtml(q)}"`;

    document.getElementById('main').innerHTML = `
      <div class="page-header">
        <div class="page-header-inner">
          <h2><i class="fas fa-search"></i> Recherche</h2>
          <p>${totalStr}</p>
        </div>
      </div>
      <div class="section">
        ${cards ? `<div class="product-grid">${cards}</div>` : noResultsHTML(`Aucun résultat pour "${escHtml(q)}"`)}
        ${paginationHTML(page, data, `(p)=>navigate('search',{q:'${escHtml(q)}',page:p})`)}
      </div>`;
  } catch (e) {
    document.getElementById('main').innerHTML = errorHTML(e.message);
  }
}

/* ── SALES ── */
async function renderSales(page = 0) {
  try {
    const data = await API.getOnSale(page, 12);
    const cards = (data.content || []).map(productCardHTML).join('');
    document.getElementById('main').innerHTML = `
      <div class="page-header">
        <div class="page-header-inner">
          <h2><i class="fas fa-tag" style="color:var(--red)"></i> Promotions</h2>
          <p>${data.totalElements} offre${data.totalElements !== 1 ? 's' : ''} en cours</p>
        </div>
      </div>
      <div class="section">
        ${cards ? `<div class="product-grid">${cards}</div>` : noResultsHTML('Aucune promotion en ce moment')}
        ${paginationHTML(page, data, `(p)=>navigate('sales',{page:p})`)}
      </div>`;
  } catch (e) {
    document.getElementById('main').innerHTML = errorHTML(e.message);
  }
}

/* ── PRODUCT DETAIL ── */
async function renderProduct(id) {
  try {
    const p = await API.getProduct(id);
    const catMeta = CATEGORIES.find(c => c.id === p.category) || { label: p.categoryDisplayName };
    const priceBlock = p.onSale
      ? `<div class="price-current">${fmt(p.salePrice)}</div>
         <div><span class="price-old">${fmt(p.listPrice)}</span>
         <span style="color:var(--red);font-weight:700;margin-left:8px">
           -${Math.round((1 - p.salePrice / p.listPrice) * 100)}%
         </span></div>`
      : `<div class="price-regular">${fmt(p.effectivePrice)}</div>`;

    document.getElementById('main').innerHTML = `
      <div class="breadcrumb">
        <a onclick="navigate('home')">Accueil</a><span class="sep">/</span>
        <a onclick="navigate('catalog',{category:'${p.category}'})">${catMeta.label}</a>
        <span class="sep">/</span>
        <span>${escHtml(p.name)}</span>
      </div>
      <div class="product-detail">
        <div class="product-detail-grid">
          <div class="product-detail-img">
            <img src="${imgSrc(p)}" alt="${escHtml(p.name)}"
                 onerror="this.src='https://picsum.photos/seed/${p.id}/600/450'">
          </div>
          <div class="product-detail-info">
            <div class="product-detail-brand">${escHtml(p.brand || '')}</div>
            <h1>${escHtml(p.name)}</h1>
            <div class="product-detail-price-block">${priceBlock}</div>
            <p class="product-detail-desc">${escHtml(p.description || '')}</p>
            <div class="product-detail-meta">
              <span><b>Référence :</b> ${escHtml(p.skuCode)}</span>
              <span><b>Catégorie :</b> ${catMeta.label}</span>
              ${p.inStock
                ? `<span class="stock-ok"><i class="fas fa-check-circle"></i> En stock (${p.stockQuantity} disponible${p.stockQuantity > 1 ? 's' : ''})</span>`
                : `<span class="stock-out"><i class="fas fa-times-circle"></i> Rupture de stock</span>`}
            </div>
            ${p.inStock ? `
            <div class="qty-row">
              <div class="qty-ctrl">
                <button onclick="detailQty(-1)">−</button>
                <input type="number" id="detailQty" value="1" min="1" max="${p.stockQuantity}">
                <button onclick="detailQty(1)">+</button>
              </div>
            </div>
            <button class="btn-add-cart-lg" onclick="addToCartQty('${p.skuCode}')">
              <i class="fas fa-cart-plus"></i> Ajouter au panier
            </button>` : ''}
          </div>
        </div>
      </div>`;
  } catch (e) {
    document.getElementById('main').innerHTML = errorHTML(e.message);
  }
}

function detailQty(delta) {
  const input = document.getElementById('detailQty');
  const newVal = Math.max(1, parseInt(input.value || 1) + delta);
  input.value = newVal;
}

async function addToCartQty(skuCode) {
  const qty = parseInt(document.getElementById('detailQty').value || 1);
  await addToCart(skuCode, qty);
}

/* ── ORDERS ── */
async function renderOrders() {
  if (!state.user) { openAuthModal('login'); return; }
  try {
    const paged = await API.getOrders();
    const orders = paged.content || [];
    if (!orders.length) {
      document.getElementById('main').innerHTML = `
        <div class="orders-page">
          <h2 class="section-title">Mes commandes</h2>
          ${noResultsHTML('Vous n\'avez pas encore passé de commande')}
        </div>`;
      return;
    }
    const cards = orders.map(o => {
      const items = o.items.slice(0, 3).map(i => escHtml(i.productName)).join(', ')
        + (o.items.length > 3 ? ` (+${o.items.length - 3})` : '');
      const canCancel = ['PENDING', 'CONFIRMED'].includes(o.status);
      const dateStr = o.submittedAt || o.createdAt;
      return `
        <div class="order-card">
          <div class="order-card-hdr">
            <div>
              <div class="order-num">Commande #${escHtml(o.orderNumber)}</div>
              <div class="order-date">${new Date(dateStr).toLocaleDateString('fr-FR', {day:'2-digit',month:'long',year:'numeric'})}</div>
            </div>
            <span class="order-status status-${o.status}">${statusLabel(o.status)}</span>
          </div>
          <div class="order-items-list">${items}</div>
          <div class="order-card-foot">
            <div class="order-total">${fmt(o.totalAmount)}</div>
            ${canCancel ? `<button class="btn-cancel-order" onclick="cancelOrder('${o.orderNumber}')">Annuler</button>` : ''}
          </div>
        </div>`;
    }).join('');

    document.getElementById('main').innerHTML = `
      <div class="orders-page">
        <h2 class="section-title">Mes commandes (${paged.totalElements})</h2>
        ${cards}
      </div>`;
  } catch (e) {
    document.getElementById('main').innerHTML = errorHTML(e.message);
  }
}

function statusLabel(s) {
  const labels = {
    PENDING: 'En attente', CONFIRMED: 'Confirmée', PROCESSING: 'En préparation',
    SHIPPED: 'Expédiée', DELIVERED: 'Livrée', CANCELLED: 'Annulée',
  };
  return labels[s] || s;
}

async function cancelOrder(orderNumber) {
  if (!confirm('Confirmer l\'annulation de cette commande ?')) return;
  try {
    await API.cancelOrder(orderNumber);
    toast('Commande annulée', 'success');
    renderOrders();
  } catch (e) {
    toast(e.message, 'error');
  }
}

/* ── Cart ── */
async function refreshCart() {
  if (!state.token) { state.cart = null; updateCartBadge(); return; }
  try { state.cart = await API.getCart(); } catch (_) { state.cart = null; }
  updateCartBadge();
  renderCartSidebar();
}

async function addToCart(skuCode, qty = 1) {
  if (!state.token) { openAuthModal('login'); return; }
  try {
    await API.addToCart(skuCode, qty);
    await refreshCart();
    toast('Produit ajouté au panier ✓', 'success');
    openCart();
  } catch (e) {
    toast(e.message, 'error');
  }
}

function openCart() {
  document.getElementById('cartSidebar').classList.add('open');
  document.getElementById('cartOverlay').classList.add('open');
  renderCartSidebar();
}

function closeCart() {
  document.getElementById('cartSidebar').classList.remove('open');
  document.getElementById('cartOverlay').classList.remove('open');
}

function renderCartSidebar() {
  const body = document.getElementById('cartBody');
  const foot = document.getElementById('cartFoot');
  const cart = state.cart;

  if (!state.token || !cart || !cart.items || cart.items.length === 0) {
    body.innerHTML = `<div class="empty-cart">
      <i class="fas fa-shopping-cart fa-3x"></i>
      <p>Votre panier est vide</p>
      ${!state.token ? '<p style="margin-top:8px;font-size:13px">Connectez-vous pour retrouver vos articles</p>' : ''}
    </div>`;
    foot.innerHTML = '';
    return;
  }

  const itemsHTML = cart.items.map(item => `
    <div class="cart-item">
      <div class="cart-item-img">
        <img src="https://picsum.photos/seed/${item.skuCode || item.productId}/140/140" alt="">
      </div>
      <div class="cart-item-info">
        <div class="cart-item-name">${escHtml(item.productName)}</div>
        <div class="cart-item-brand">${escHtml(item.brand || '')}</div>
        <div class="cart-item-row">
          <div class="cart-item-price">${fmt(item.lineTotal)}</div>
          <div class="qty-mini">
            <button onclick="changeQty(${item.id}, ${item.quantity - 1})">−</button>
            <span>${item.quantity}</span>
            <button onclick="changeQty(${item.id}, ${item.quantity + 1})">+</button>
          </div>
          <button class="cart-item-remove" onclick="removeItem(${item.id})" title="Retirer">
            <i class="fas fa-trash-alt"></i>
          </button>
        </div>
      </div>
    </div>`).join('');

  body.innerHTML = itemsHTML;

  const shippingFree = parseFloat(cart.subtotal) >= 75;
  foot.innerHTML = `
    <div class="cart-totals">
      <div class="cart-total-row"><span>Sous-total</span><span>${fmt(cart.subtotal)}</span></div>
      <div class="cart-total-row"><span>TVA (20%)</span><span>${fmt(cart.taxAmount)}</span></div>
      <div class="cart-total-row">
        <span>Livraison</span>
        <span class="${shippingFree ? 'shipping-free' : ''}">${shippingFree ? 'Offerte' : '5,99 €'}</span>
      </div>
      <div class="cart-total-row grand"><span>Total</span><span>${fmt(cart.total)}</span></div>
    </div>
    <button class="btn-checkout" onclick="closeCart();openCheckout()">
      <i class="fas fa-lock"></i> Commander
    </button>
    <a class="btn-clear" onclick="clearCart()">Vider le panier</a>`;
}

async function changeQty(itemId, newQty) {
  if (newQty < 1) { await removeItem(itemId); return; }
  try {
    await API.updateCartItem(itemId, newQty);
    await refreshCart();
  } catch (e) { toast(e.message, 'error'); }
}

async function removeItem(itemId) {
  try {
    await API.removeCartItem(itemId);
    await refreshCart();
    toast('Article retiré du panier');
  } catch (e) { toast(e.message, 'error'); }
}

async function clearCart() {
  if (!confirm('Vider le panier ?')) return;
  try {
    await API.clearCart();
    await refreshCart();
    toast('Panier vidé');
  } catch (e) { toast(e.message, 'error'); }
}

/* ── Checkout modal ── */
function openCheckout() {
  if (!state.user) { openAuthModal('login'); return; }
  const cart = state.cart;
  showModal(`
    <div class="checkout-modal">
      <h2><i class="fas fa-lock"></i> Finaliser la commande</h2>
      <p>Vérifiez vos informations et confirmez votre commande.</p>
      <div class="checkout-summary">
        <div class="checkout-summary-row"><span>Articles (${cart.itemCount})</span><span>${fmt(cart.subtotal)}</span></div>
        <div class="checkout-summary-row"><span>TVA</span><span>${fmt(cart.taxAmount)}</span></div>
        <div class="checkout-summary-row"><span>Livraison</span>
          <span>${parseFloat(cart.subtotal) >= 75 ? 'Offerte' : '5,99 €'}</span>
        </div>
        <div class="checkout-summary-row total"><span>Total</span><span>${fmt(cart.total)}</span></div>
      </div>
      <div style="display:flex;flex-direction:column;gap:14px;">
        <div class="form-group">
          <label>Adresse de livraison</label>
          <input type="text" id="shippingAddr" placeholder="12 rue de la Paix, 75001 Paris">
        </div>
        <div class="form-group">
          <label>Mode de paiement</label>
          <select id="paymentMethod">
            <option value="CARTE_BANCAIRE">💳 Carte bancaire</option>
            <option value="PAYPAL">🅿 PayPal</option>
            <option value="VIREMENT">🏦 Virement bancaire</option>
          </select>
        </div>
        <div class="form-error" id="checkoutError"></div>
        <button class="btn-submit" onclick="submitOrder()">
          <i class="fas fa-check"></i> Confirmer la commande
        </button>
      </div>
    </div>`);
}

async function submitOrder() {
  const addr  = document.getElementById('shippingAddr').value.trim();
  const pmeth = document.getElementById('paymentMethod').value;
  const errEl = document.getElementById('checkoutError');
  if (!addr) { errEl.textContent = 'Veuillez saisir une adresse de livraison.'; return; }
  errEl.textContent = '';
  try {
    const order = await API.checkout({ shippingAddress: addr, paymentMethod: pmeth });
    closeModal();
    await refreshCart();
    toast('Commande confirmée ! #' + order.orderNumber, 'success');
    navigate('orders');
  } catch (e) {
    errEl.textContent = e.message;
  }
}

/* ── Auth modal ── */
function openAuthModal(tab = 'login') {
  showModal(`
    <div class="auth-modal">
      <div class="auth-tabs">
        <button class="auth-tab ${tab === 'login' ? 'active' : ''}" id="tab-login" onclick="switchTab('login')">
          Connexion
        </button>
        <button class="auth-tab ${tab === 'register' ? 'active' : ''}" id="tab-register" onclick="switchTab('register')">
          Créer un compte
        </button>
      </div>

      <!-- Login form -->
      <form class="auth-form ${tab === 'login' ? 'active' : ''}" id="form-login"
            onsubmit="event.preventDefault();doLogin()">
        <div class="form-group">
          <label>Email</label>
          <input type="email" id="loginEmail" placeholder="vous@exemple.fr" required>
        </div>
        <div class="form-group">
          <label>Mot de passe</label>
          <input type="password" id="loginPassword" placeholder="••••••••" required>
        </div>
        <div class="form-error" id="loginError"></div>
        <button type="submit" class="btn-submit">Se connecter</button>
      </form>

      <!-- Register form -->
      <form class="auth-form ${tab === 'register' ? 'active' : ''}" id="form-register"
            onsubmit="event.preventDefault();doRegister()">
        <div class="form-row">
          <div class="form-group">
            <label>Prénom</label>
            <input type="text" id="regFirstName" placeholder="Jean" required>
          </div>
          <div class="form-group">
            <label>Nom</label>
            <input type="text" id="regLastName" placeholder="Dupont" required>
          </div>
        </div>
        <div class="form-group">
          <label>Email</label>
          <input type="email" id="regEmail" placeholder="vous@exemple.fr" required>
        </div>
        <div class="form-group">
          <label>Mot de passe</label>
          <input type="password" id="regPassword" placeholder="Minimum 8 caractères" required minlength="8">
        </div>
        <div class="form-error" id="registerError"></div>
        <button type="submit" class="btn-submit">Créer mon compte</button>
      </form>
    </div>`);
}

function switchTab(tab) {
  ['login', 'register'].forEach(t => {
    document.getElementById('tab-' + t).classList.toggle('active', t === tab);
    document.getElementById('form-' + t).classList.toggle('active', t === tab);
  });
}

async function doLogin() {
  const email    = document.getElementById('loginEmail').value;
  const password = document.getElementById('loginPassword').value;
  const errEl    = document.getElementById('loginError');
  errEl.textContent = '';
  try {
    const res = await API.login({ email, password });
    localStorage.setItem('token', res.accessToken);
    state.token = res.accessToken;
    state.user  = res.user || await API.getMe();
    await refreshCart();
    updateHeader();
    closeModal();
    toast('Bienvenue, ' + (state.user.firstName || state.user.email) + ' !', 'success');
  } catch (e) {
    errEl.textContent = e.message;
  }
}

async function doRegister() {
  const firstName = document.getElementById('regFirstName').value;
  const lastName  = document.getElementById('regLastName').value;
  const email     = document.getElementById('regEmail').value;
  const password  = document.getElementById('regPassword').value;
  const errEl     = document.getElementById('registerError');
  errEl.textContent = '';
  // Backend requires a unique login (username); derive it from the email local-part
  const login = email.split('@')[0].replace(/[^a-zA-Z0-9_]/g, '_');
  try {
    const res = await API.register({ login, firstName, lastName, email, password });
    localStorage.setItem('token', res.accessToken);
    state.token = res.accessToken;
    state.user  = res.user || await API.getMe();
    await refreshCart();
    updateHeader();
    closeModal();
    toast('Compte créé ! Bienvenue, ' + firstName + ' !', 'success');
  } catch (e) {
    errEl.textContent = e.message;
  }
}

/* ── Account dropdown ── */
function toggleAccountMenu() {
  const dd = document.getElementById('accountDropdown');
  const isOpen = dd.classList.contains('open');
  if (isOpen) { closeAccountMenu(); return; }

  if (state.user) {
    dd.innerHTML = `
      <div class="acct-section">
        <div class="acct-greeting">Bonjour, ${escHtml(state.user.firstName || state.user.email)} !</div>
        <a class="acct-link" onclick="closeAccountMenu();navigate('orders')">
          <i class="fas fa-box"></i> Mes commandes
        </a>
      </div>
      <div class="acct-section">
        <button class="acct-btn acct-btn-outline" onclick="doLogout()">
          <i class="fas fa-sign-out-alt"></i> Se déconnecter
        </button>
      </div>`;
  } else {
    dd.innerHTML = `
      <div class="acct-section">
        <button class="acct-btn acct-btn-primary" onclick="closeAccountMenu();openAuthModal('login')">
          <i class="fas fa-sign-in-alt"></i> Se connecter
        </button>
        <button class="acct-btn acct-btn-outline" onclick="closeAccountMenu();openAuthModal('register')">
          <i class="fas fa-user-plus"></i> Créer un compte
        </button>
      </div>`;
  }
  dd.classList.add('open');
  setTimeout(() => document.addEventListener('click', outsideAccountClick, { once: true }), 10);
}

function outsideAccountClick(e) {
  const dd  = document.getElementById('accountDropdown');
  const btn = document.getElementById('accountBtn');
  if (!dd.contains(e.target) && !btn.contains(e.target)) closeAccountMenu();
}

function closeAccountMenu() {
  document.getElementById('accountDropdown').classList.remove('open');
}

function doLogout() {
  localStorage.removeItem('token');
  state.token = null; state.user = null; state.cart = null;
  updateHeader();
  closeAccountMenu();
  navigate('home');
  toast('Vous êtes déconnecté(e)');
}

/* ── Modal helpers ── */
function showModal(html) {
  document.getElementById('modalBody').innerHTML = html;
  document.getElementById('modalOverlay').classList.add('open');
  document.getElementById('modal').classList.add('open');
}

function closeModal() {
  document.getElementById('modalOverlay').classList.remove('open');
  document.getElementById('modal').classList.remove('open');
}

/* ── Utility ── */
function escHtml(str) {
  if (!str) return '';
  return String(str)
    .replace(/&/g, '&amp;').replace(/</g, '&lt;')
    .replace(/>/g, '&gt;').replace(/"/g, '&quot;');
}

function errorHTML(msg) {
  return `<div class="no-results">
    <i class="fas fa-exclamation-triangle fa-3x"></i>
    <h3>Une erreur est survenue</h3>
    <p>${escHtml(msg)}</p>
  </div>`;
}

function noResultsHTML(msg) {
  return `<div class="no-results">
    <i class="fas fa-search fa-3x"></i>
    <h3>${msg}</h3>
  </div>`;
}
