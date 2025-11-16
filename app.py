"""
Flask application with Keycloak JWT authentication using HTMX and SSR.
"""
import os
from datetime import timedelta
from functools import wraps

from flask import (
    Flask,
    render_template,
    request,
    redirect,
    url_for,
    session,
    flash,
    jsonify,
    make_response,
)
from keycloak import KeycloakOpenID, KeycloakAdmin
from keycloak.exceptions import KeycloakAuthenticationError, KeycloakGetError
from dotenv import load_dotenv
import jwt
from jwt import PyJWKClient

load_dotenv()

app = Flask(__name__)
app.secret_key = os.getenv("SECRET_KEY", "your-secret-key-change-in-production")
app.config["PERMANENT_SESSION_LIFETIME"] = timedelta(hours=1)

# Keycloak configuration
KEYCLOAK_SERVER_URL = os.getenv("KEYCLOAK_SERVER_URL", "http://localhost:8080")
KEYCLOAK_REALM = os.getenv("KEYCLOAK_REALM", "master")
KEYCLOAK_CLIENT_ID = os.getenv("KEYCLOAK_CLIENT_ID", "my-app")
KEYCLOAK_CLIENT_SECRET = os.getenv("KEYCLOAK_CLIENT_SECRET", "")

# Initialize Keycloak OpenID client
keycloak_openid = KeycloakOpenID(
    server_url=KEYCLOAK_SERVER_URL,
    client_id=KEYCLOAK_CLIENT_ID,
    realm_name=KEYCLOAK_REALM,
    client_secret_key=KEYCLOAK_CLIENT_SECRET,
)


def get_keycloak_public_key():
    """Fetch the public key from Keycloak for JWT verification."""
    try:
        public_key = keycloak_openid.public_key()
        return f"-----BEGIN PUBLIC KEY-----\n{public_key}\n-----END PUBLIC KEY-----"
    except Exception as e:
        app.logger.error(f"Error fetching Keycloak public key: {e}")
        return None


def verify_token(token):
    """Verify and decode a JWT token from Keycloak."""
    try:
        public_key = get_keycloak_public_key()
        if not public_key:
            return None

        decoded_token = jwt.decode(
            token,
            public_key,
            algorithms=["RS256"],
            audience=KEYCLOAK_CLIENT_ID,
            options={"verify_exp": True},
        )
        return decoded_token
    except jwt.ExpiredSignatureError:
        app.logger.warning("Token has expired")
        return None
    except jwt.InvalidTokenError as e:
        app.logger.warning(f"Invalid token: {e}")
        return None
    except Exception as e:
        app.logger.error(f"Token verification error: {e}")
        return None


def login_required(f):
    """Decorator to require authentication for routes."""
    @wraps(f)
    def decorated_function(*args, **kwargs):
        token = session.get("access_token")
        if not token:
            if request.headers.get("HX-Request"):
                # HTMX request - return redirect header
                response = make_response("", 200)
                response.headers["HX-Redirect"] = url_for("login")
                return response
            return redirect(url_for("login"))

        # Verify token
        token_data = verify_token(token)
        if not token_data:
            session.clear()
            if request.headers.get("HX-Request"):
                response = make_response("", 200)
                response.headers["HX-Redirect"] = url_for("login")
                return response
            flash("Session expired. Please login again.", "warning")
            return redirect(url_for("login"))

        # Add user info to request context
        request.user = token_data
        return f(*args, **kwargs)

    return decorated_function


@app.route("/")
def index():
    """Home page."""
    if session.get("access_token"):
        return redirect(url_for("dashboard"))
    return render_template("index.html")


@app.route("/login", methods=["GET", "POST"])
def login():
    """Login page and authentication handler."""
    if request.method == "GET":
        if session.get("access_token"):
            return redirect(url_for("dashboard"))
        return render_template("login.html")

    # Handle POST - HTMX form submission
    username = request.form.get("username", "").strip()
    password = request.form.get("password", "")

    if not username or not password:
        return render_template(
            "partials/login_error.html", error="Please enter both username and password"
        )

    try:
        # Authenticate with Keycloak
        token_response = keycloak_openid.token(username, password)

        # Store tokens in session
        session["access_token"] = token_response["access_token"]
        session["refresh_token"] = token_response.get("refresh_token")
        session["id_token"] = token_response.get("id_token")
        session.permanent = True

        # Decode token to get user info
        user_info = keycloak_openid.userinfo(token_response["access_token"])
        session["user_info"] = user_info

        # Return HTMX redirect
        response = make_response("", 200)
        response.headers["HX-Redirect"] = url_for("dashboard")
        return response

    except KeycloakAuthenticationError as e:
        app.logger.warning(f"Authentication failed for user {username}: {e}")
        return render_template(
            "partials/login_error.html", error="Invalid username or password"
        )
    except Exception as e:
        app.logger.error(f"Login error: {e}")
        return render_template(
            "partials/login_error.html",
            error="Authentication service unavailable. Please try again later.",
        )


@app.route("/register", methods=["GET", "POST"])
def register():
    """Registration page."""
    if request.method == "GET":
        return render_template("register.html")

    # Handle registration via HTMX
    username = request.form.get("username", "").strip()
    email = request.form.get("email", "").strip()
    password = request.form.get("password", "")
    confirm_password = request.form.get("confirm_password", "")
    first_name = request.form.get("first_name", "").strip()
    last_name = request.form.get("last_name", "").strip()

    # Validation
    errors = []
    if not username:
        errors.append("Username is required")
    if not email:
        errors.append("Email is required")
    if not password:
        errors.append("Password is required")
    if password != confirm_password:
        errors.append("Passwords do not match")
    if len(password) < 8:
        errors.append("Password must be at least 8 characters")

    if errors:
        return render_template("partials/register_error.html", errors=errors)

    # Note: Self-registration requires Keycloak admin setup
    # For demo purposes, we'll show a success message
    return render_template(
        "partials/register_success.html",
        message="Registration request submitted. Please contact your administrator for account activation.",
    )


@app.route("/dashboard")
@login_required
def dashboard():
    """Protected dashboard page."""
    user_info = session.get("user_info", {})
    token_data = request.user
    return render_template(
        "dashboard.html", user_info=user_info, token_data=token_data
    )


@app.route("/profile")
@login_required
def profile():
    """User profile page."""
    user_info = session.get("user_info", {})
    return render_template("profile.html", user_info=user_info)


@app.route("/api/profile", methods=["GET"])
@login_required
def api_profile():
    """API endpoint to get user profile (HTMX partial)."""
    user_info = session.get("user_info", {})
    return render_template("partials/profile_card.html", user_info=user_info)


@app.route("/api/token-info")
@login_required
def api_token_info():
    """Get token information (HTMX partial)."""
    token_data = request.user
    return render_template("partials/token_info.html", token_data=token_data)


@app.route("/api/refresh-token", methods=["POST"])
def refresh_token():
    """Refresh the access token using the refresh token."""
    refresh_token = session.get("refresh_token")
    if not refresh_token:
        if request.headers.get("HX-Request"):
            response = make_response("", 200)
            response.headers["HX-Redirect"] = url_for("login")
            return response
        return redirect(url_for("login"))

    try:
        token_response = keycloak_openid.refresh_token(refresh_token)
        session["access_token"] = token_response["access_token"]
        session["refresh_token"] = token_response.get("refresh_token", refresh_token)

        # Update user info
        user_info = keycloak_openid.userinfo(token_response["access_token"])
        session["user_info"] = user_info

        return render_template(
            "partials/toast.html",
            message="Token refreshed successfully",
            type="success",
        )
    except Exception as e:
        app.logger.error(f"Token refresh error: {e}")
        session.clear()
        if request.headers.get("HX-Request"):
            response = make_response("", 200)
            response.headers["HX-Redirect"] = url_for("login")
            return response
        return redirect(url_for("login"))


@app.route("/logout", methods=["POST"])
def logout():
    """Logout user and clear session."""
    try:
        refresh_token = session.get("refresh_token")
        if refresh_token:
            # Logout from Keycloak
            keycloak_openid.logout(refresh_token)
    except Exception as e:
        app.logger.warning(f"Keycloak logout error: {e}")

    session.clear()

    if request.headers.get("HX-Request"):
        response = make_response("", 200)
        response.headers["HX-Redirect"] = url_for("index")
        return response
    return redirect(url_for("index"))


@app.route("/health")
def health():
    """Health check endpoint."""
    return jsonify({"status": "healthy", "keycloak_configured": bool(KEYCLOAK_SERVER_URL)})


@app.errorhandler(404)
def not_found(e):
    """Handle 404 errors."""
    if request.headers.get("HX-Request"):
        return render_template("partials/error.html", error="Page not found"), 404
    return render_template("error.html", error="Page not found", code=404), 404


@app.errorhandler(500)
def server_error(e):
    """Handle 500 errors."""
    if request.headers.get("HX-Request"):
        return render_template("partials/error.html", error="Server error"), 500
    return render_template("error.html", error="Internal server error", code=500), 500


if __name__ == "__main__":
    app.run(debug=True, host="0.0.0.0", port=5000)
