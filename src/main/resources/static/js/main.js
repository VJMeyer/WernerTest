/**
 * Main JavaScript for JWT Authentication Frontend
 */

// Theme Management
function toggleTheme() {
    const html = document.documentElement;
    const currentTheme = html.getAttribute('data-theme');
    const newTheme = currentTheme === 'dark' ? 'light' : 'dark';

    html.setAttribute('data-theme', newTheme);
    localStorage.setItem('theme', newTheme);
}

// Load saved theme on page load
function loadTheme() {
    const savedTheme = localStorage.getItem('theme');
    if (savedTheme) {
        document.documentElement.setAttribute('data-theme', savedTheme);
    } else if (window.matchMedia('(prefers-color-scheme: dark)').matches) {
        document.documentElement.setAttribute('data-theme', 'dark');
    }
}

// Toast notifications
function showToast(message, type = 'info') {
    const container = document.getElementById('toast-container');
    const toast = document.createElement('div');
    toast.className = `toast toast-${type}`;

    let icon = '';
    if (type === 'success') {
        icon = `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"></path>
            <polyline points="22 4 12 14.01 9 11.01"></polyline>
        </svg>`;
    } else if (type === 'error') {
        icon = `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <circle cx="12" cy="12" r="10"></circle>
            <line x1="15" y1="9" x2="9" y2="15"></line>
            <line x1="9" y1="9" x2="15" y2="15"></line>
        </svg>`;
    } else {
        icon = `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <circle cx="12" cy="12" r="10"></circle>
            <line x1="12" y1="16" x2="12" y2="12"></line>
            <line x1="12" y1="8" x2="12.01" y2="8"></line>
        </svg>`;
    }

    toast.innerHTML = `${icon}<span>${message}</span>`;
    toast.onclick = () => toast.remove();

    container.appendChild(toast);

    // Auto remove after 3 seconds
    setTimeout(() => {
        if (toast.parentElement) {
            toast.remove();
        }
    }, 3000);
}

// HTMX Configuration
document.addEventListener('DOMContentLoaded', function() {
    // Load theme
    loadTheme();

    // Add smooth transitions after initial load
    setTimeout(() => {
        document.body.style.transition = 'background-color 0.3s ease, color 0.3s ease';
    }, 100);

    // Configure HTMX to include CSRF token
    document.body.addEventListener('htmx:configRequest', function(event) {
        const csrfToken = document.querySelector('meta[name="_csrf"]');
        const csrfHeader = document.querySelector('meta[name="_csrf_header"]');

        if (csrfToken && csrfHeader) {
            event.detail.headers[csrfHeader.content] = csrfToken.content;
        }
    });
});

// HTMX event listeners
document.addEventListener('htmx:afterSwap', function(event) {
    // Re-initialize any dynamic content after HTMX updates
});

document.addEventListener('htmx:beforeRequest', function(event) {
    // Add loading state
    const target = event.detail.elt;
    if (target.classList.contains('btn')) {
        target.disabled = true;
    }
});

document.addEventListener('htmx:afterRequest', function(event) {
    // Remove loading state
    const target = event.detail.elt;
    if (target.classList.contains('btn')) {
        target.disabled = false;
    }

    // Handle errors
    if (event.detail.failed) {
        showToast('Request failed. Please try again.', 'error');
    }
});

document.addEventListener('htmx:responseError', function(event) {
    showToast('Server error. Please try again later.', 'error');
});

// Expose functions to global scope
window.toggleTheme = toggleTheme;
window.showToast = showToast;
