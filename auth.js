/**
 * Keycloak Authentication Module
 * Handles OpenID Connect authentication with Keycloak
 */

class AuthService {
    constructor() {
        this.accessToken = null;
        this.refreshToken = null;
        this.idToken = null;
        this.tokenExpiry = null;
        this.userInfo = null;
        this.keycloakConfig = null;
        this.refreshTimer = null;
    }

    /**
     * Initialize Keycloak configuration
     * @param {Object} config - Keycloak configuration
     * @param {string} config.realm - Keycloak realm
     * @param {string} config.clientId - Client ID
     * @param {string} config.baseUrl - Keycloak base URL (optional, defaults to same domain)
     */
    initialize(config) {
        this.keycloakConfig = {
            realm: config.realm,
            clientId: config.clientId,
            baseUrl: config.baseUrl || window.location.origin
        };

        // Check for tokens in session storage
        this.loadTokensFromStorage();

        // Check if we're returning from Keycloak redirect
        this.handleRedirectCallback();
    }

    /**
     * Get authorization URL for login
     */
    getAuthorizationUrl() {
        const { realm, clientId, baseUrl } = this.keycloakConfig;

        const state = this.generateRandomString(32);
        const nonce = this.generateRandomString(32);

        sessionStorage.setItem('auth_state', state);
        sessionStorage.setItem('auth_nonce', nonce);

        const redirectUri = window.location.origin + window.location.pathname;

        const params = new URLSearchParams({
            client_id: clientId,
            redirect_uri: redirectUri,
            response_type: 'code',
            scope: 'openid profile email',
            state: state,
            nonce: nonce
        });

        return `${baseUrl}/realms/${realm}/protocol/openid-connect/auth?${params.toString()}`;
    }

    /**
     * Initiate login flow
     */
    login() {
        const authUrl = this.getAuthorizationUrl();
        window.location.href = authUrl;
    }

    /**
     * Handle redirect callback from Keycloak
     */
    async handleRedirectCallback() {
        const urlParams = new URLSearchParams(window.location.search);
        const code = urlParams.get('code');
        const state = urlParams.get('state');
        const error = urlParams.get('error');

        if (error) {
            console.error('Authentication error:', error);
            throw new Error(`Authentication failed: ${error}`);
        }

        if (code && state) {
            const savedState = sessionStorage.getItem('auth_state');

            if (state !== savedState) {
                throw new Error('Invalid state parameter');
            }

            try {
                await this.exchangeCodeForTokens(code);

                // Clean up URL
                window.history.replaceState({}, document.title, window.location.pathname);

                // Clean up session storage
                sessionStorage.removeItem('auth_state');
                sessionStorage.removeItem('auth_nonce');

                return true;
            } catch (error) {
                console.error('Token exchange failed:', error);
                throw error;
            }
        }

        return false;
    }

    /**
     * Exchange authorization code for tokens
     */
    async exchangeCodeForTokens(code) {
        const { realm, clientId, baseUrl } = this.keycloakConfig;
        const redirectUri = window.location.origin + window.location.pathname;

        const params = new URLSearchParams({
            grant_type: 'authorization_code',
            code: code,
            redirect_uri: redirectUri,
            client_id: clientId
        });

        const tokenEndpoint = `${baseUrl}/realms/${realm}/protocol/openid-connect/token`;

        const response = await fetch(tokenEndpoint, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/x-www-form-urlencoded'
            },
            body: params.toString()
        });

        if (!response.ok) {
            throw new Error(`Token exchange failed: ${response.statusText}`);
        }

        const tokens = await response.json();
        this.setTokens(tokens);

        // Fetch user info
        await this.fetchUserInfo();

        // Schedule token refresh
        this.scheduleTokenRefresh();
    }

    /**
     * Set tokens and save to storage
     */
    setTokens(tokens) {
        this.accessToken = tokens.access_token;
        this.refreshToken = tokens.refresh_token;
        this.idToken = tokens.id_token;

        // Calculate expiry time (expires_in is in seconds)
        this.tokenExpiry = Date.now() + (tokens.expires_in * 1000);

        // Save to session storage
        sessionStorage.setItem('access_token', this.accessToken);
        sessionStorage.setItem('refresh_token', this.refreshToken);
        sessionStorage.setItem('id_token', this.idToken);
        sessionStorage.setItem('token_expiry', this.tokenExpiry.toString());
    }

    /**
     * Load tokens from storage
     */
    loadTokensFromStorage() {
        this.accessToken = sessionStorage.getItem('access_token');
        this.refreshToken = sessionStorage.getItem('refresh_token');
        this.idToken = sessionStorage.getItem('id_token');
        const expiry = sessionStorage.getItem('token_expiry');
        this.tokenExpiry = expiry ? parseInt(expiry) : null;

        if (this.isAuthenticated()) {
            this.scheduleTokenRefresh();
            // Load user info from storage if available
            const userInfoStr = sessionStorage.getItem('user_info');
            if (userInfoStr) {
                this.userInfo = JSON.parse(userInfoStr);
            } else {
                this.fetchUserInfo();
            }
        }
    }

    /**
     * Fetch user information
     */
    async fetchUserInfo() {
        if (!this.accessToken) return;

        const { realm, baseUrl } = this.keycloakConfig;
        const userInfoEndpoint = `${baseUrl}/realms/${realm}/protocol/openid-connect/userinfo`;

        try {
            const response = await fetch(userInfoEndpoint, {
                headers: {
                    'Authorization': `Bearer ${this.accessToken}`
                }
            });

            if (response.ok) {
                this.userInfo = await response.json();
                sessionStorage.setItem('user_info', JSON.stringify(this.userInfo));
            }
        } catch (error) {
            console.error('Failed to fetch user info:', error);
        }
    }

    /**
     * Refresh access token
     */
    async refreshAccessToken() {
        if (!this.refreshToken) {
            throw new Error('No refresh token available');
        }

        const { realm, clientId, baseUrl } = this.keycloakConfig;

        const params = new URLSearchParams({
            grant_type: 'refresh_token',
            refresh_token: this.refreshToken,
            client_id: clientId
        });

        const tokenEndpoint = `${baseUrl}/realms/${realm}/protocol/openid-connect/token`;

        try {
            const response = await fetch(tokenEndpoint, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/x-www-form-urlencoded'
                },
                body: params.toString()
            });

            if (!response.ok) {
                throw new Error('Token refresh failed');
            }

            const tokens = await response.json();
            this.setTokens(tokens);
            this.scheduleTokenRefresh();

            return true;
        } catch (error) {
            console.error('Token refresh failed:', error);
            this.logout();
            return false;
        }
    }

    /**
     * Schedule automatic token refresh
     */
    scheduleTokenRefresh() {
        // Clear existing timer
        if (this.refreshTimer) {
            clearTimeout(this.refreshTimer);
        }

        if (!this.tokenExpiry) return;

        // Refresh 1 minute before expiry
        const refreshTime = this.tokenExpiry - Date.now() - 60000;

        if (refreshTime > 0) {
            this.refreshTimer = setTimeout(() => {
                this.refreshAccessToken();
            }, refreshTime);
        } else {
            // Token already expired or expiring soon, refresh now
            this.refreshAccessToken();
        }
    }

    /**
     * Logout
     */
    async logout() {
        const { realm, clientId, baseUrl } = this.keycloakConfig;

        // Clear local state
        this.accessToken = null;
        this.refreshToken = null;
        this.idToken = null;
        this.tokenExpiry = null;
        this.userInfo = null;

        // Clear storage
        sessionStorage.removeItem('access_token');
        sessionStorage.removeItem('refresh_token');
        sessionStorage.removeItem('id_token');
        sessionStorage.removeItem('token_expiry');
        sessionStorage.removeItem('user_info');

        // Clear refresh timer
        if (this.refreshTimer) {
            clearTimeout(this.refreshTimer);
            this.refreshTimer = null;
        }

        // Redirect to Keycloak logout
        const redirectUri = window.location.origin + window.location.pathname;
        const logoutUrl = `${baseUrl}/realms/${realm}/protocol/openid-connect/logout?redirect_uri=${encodeURIComponent(redirectUri)}`;

        window.location.href = logoutUrl;
    }

    /**
     * Check if user is authenticated
     */
    isAuthenticated() {
        if (!this.accessToken || !this.tokenExpiry) {
            return false;
        }

        // Check if token is expired (with 1 minute buffer)
        return Date.now() < (this.tokenExpiry - 60000);
    }

    /**
     * Get access token for API calls
     */
    getAccessToken() {
        if (this.isAuthenticated()) {
            return this.accessToken;
        }
        return null;
    }

    /**
     * Get authorization header
     */
    getAuthHeader() {
        const token = this.getAccessToken();
        return token ? `Bearer ${token}` : null;
    }

    /**
     * Get user information
     */
    getUserInfo() {
        return this.userInfo;
    }

    /**
     * Generate random string for state/nonce
     */
    generateRandomString(length) {
        const chars = 'ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789';
        let result = '';
        const randomValues = new Uint8Array(length);
        crypto.getRandomValues(randomValues);

        for (let i = 0; i < length; i++) {
            result += chars[randomValues[i] % chars.length];
        }

        return result;
    }

    /**
     * Decode JWT token (for debugging/display purposes)
     */
    decodeToken(token) {
        try {
            const parts = token.split('.');
            if (parts.length !== 3) {
                throw new Error('Invalid token');
            }

            const payload = parts[1];
            const decoded = atob(payload.replace(/-/g, '+').replace(/_/g, '/'));
            return JSON.parse(decoded);
        } catch (error) {
            console.error('Failed to decode token:', error);
            return null;
        }
    }
}

// Export for use in other modules
window.AuthService = AuthService;
