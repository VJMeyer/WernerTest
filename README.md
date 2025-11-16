# JWT Authentication with Keycloak

A modern, responsive frontend for JWT authentication using Keycloak identity provider. Built with Flask, HTMX, and server-side rendering for a fast, secure, and developer-friendly experience.

## Features

- **JWT Authentication** - Secure token-based authentication with RS256 signature verification
- **Keycloak Integration** - Enterprise-grade identity management with single sign-on
- **HTMX + SSR** - Fast, responsive UI with minimal JavaScript using server-side rendering
- **Modern UI** - Clean, responsive design with dark/light mode support
- **Token Management** - Automatic token refresh and session management
- **Role-Based Access** - Display user roles and permissions from Keycloak
- **Real-time Feedback** - Toast notifications and loading indicators

## Quick Start

### Prerequisites

- Python 3.8+
- Keycloak server (local or remote)
- pip (Python package manager)

### Installation

1. **Clone the repository**
   ```bash
   git clone <repository-url>
   cd WernerTest
   ```

2. **Create virtual environment**
   ```bash
   python -m venv venv
   source venv/bin/activate  # On Windows: venv\Scripts\activate
   ```

3. **Install dependencies**
   ```bash
   pip install -r requirements.txt
   ```

4. **Configure environment**
   ```bash
   cp .env.example .env
   # Edit .env with your Keycloak settings
   ```

5. **Start the application**
   ```bash
   python app.py
   ```

6. **Open in browser**
   ```
   http://localhost:5000
   ```

## Configuration

Edit the `.env` file with your settings:

```bash
# Flask
SECRET_KEY=your-super-secret-key-here

# Keycloak
KEYCLOAK_SERVER_URL=http://localhost:8080
KEYCLOAK_REALM=myapp
KEYCLOAK_CLIENT_ID=my-app
KEYCLOAK_CLIENT_SECRET=your-client-secret
```

## Keycloak Setup

See [config/keycloak_setup.md](config/keycloak_setup.md) for detailed Keycloak configuration instructions.

### Quick Docker Setup

```bash
docker run -d \
  --name keycloak \
  -p 8080:8080 \
  -e KEYCLOAK_ADMIN=admin \
  -e KEYCLOAK_ADMIN_PASSWORD=admin \
  quay.io/keycloak/keycloak:latest start-dev
```

## Project Structure

```
WernerTest/
├── app.py                    # Main Flask application
├── requirements.txt          # Python dependencies
├── .env.example             # Environment variables template
├── config/
│   └── keycloak_setup.md    # Keycloak setup guide
├── static/
│   ├── css/
│   │   └── style.css        # Modern CSS with dark mode
│   └── js/
│       └── main.js          # Frontend JavaScript
└── templates/
    ├── base.html            # Base template with HTMX
    ├── index.html           # Landing page
    ├── login.html           # Login form
    ├── register.html        # Registration form
    ├── dashboard.html       # Protected dashboard
    ├── profile.html         # User profile page
    ├── error.html           # Error page
    └── partials/            # HTMX partial templates
        ├── login_error.html
        ├── register_error.html
        ├── register_success.html
        ├── toast.html
        ├── profile_card.html
        ├── token_info.html
        └── error.html
```

## API Endpoints

| Endpoint | Method | Description | Auth Required |
|----------|--------|-------------|---------------|
| `/` | GET | Landing page | No |
| `/login` | GET/POST | User login | No |
| `/register` | GET/POST | User registration | No |
| `/dashboard` | GET | Protected dashboard | Yes |
| `/profile` | GET | User profile page | Yes |
| `/logout` | POST | Logout user | No |
| `/api/profile` | GET | User profile data (HTMX) | Yes |
| `/api/token-info` | GET | JWT token information (HTMX) | Yes |
| `/api/refresh-token` | POST | Refresh access token | Yes |
| `/health` | GET | Health check | No |

## Key Technologies

- **Flask** - Lightweight Python web framework
- **HTMX** - High power tools for HTML
- **Python-Keycloak** - Keycloak Python client
- **PyJWT** - JSON Web Token implementation
- **Jinja2** - Template engine

## Security Features

- JWT token verification with RS256
- Secure session management
- CSRF protection via Flask session
- Token expiration handling
- Automatic token refresh
- Protected route decorator
- XSS prevention

## Development

### Running in Development Mode

```bash
export FLASK_ENV=development
export FLASK_DEBUG=1
python app.py
```

### Adding New Protected Routes

```python
from functools import wraps

@app.route("/my-route")
@login_required
def my_route():
    user_info = session.get("user_info", {})
    return render_template("my_template.html", user_info=user_info)
```

### Creating HTMX Partials

1. Create a partial template in `templates/partials/`
2. Add a route that returns the partial
3. Use HTMX attributes to fetch and swap content

Example:
```html
<button
    hx-get="/api/my-data"
    hx-target="#content"
    hx-swap="innerHTML"
>
    Load Data
</button>
```

## Customization

### Theming

The CSS uses CSS variables for easy customization. Edit `static/css/style.css`:

```css
:root {
    --primary: #6366f1;
    --primary-dark: #4f46e5;
    /* ... more variables */
}
```

### Adding Features

- **Two-Factor Authentication** - Integrate with Keycloak OTP
- **Social Login** - Configure identity providers in Keycloak
- **User Self-Registration** - Enable in Keycloak realm settings
- **Password Reset** - Configure email in Keycloak

## Troubleshooting

### Common Issues

1. **"Authentication service unavailable"**
   - Check Keycloak is running
   - Verify KEYCLOAK_SERVER_URL

2. **"Invalid token"**
   - Check client secret
   - Verify realm and client ID

3. **Session expires immediately**
   - Check token lifetime in Keycloak
   - Verify SECRET_KEY is set

4. **HTMX not working**
   - Check browser console for errors
   - Ensure HTMX is loaded correctly

## Production Deployment

1. Use production WSGI server (gunicorn, uWSGI)
2. Enable HTTPS for both app and Keycloak
3. Set secure Flask configuration
4. Use environment variables for secrets
5. Configure proper logging

Example with gunicorn:
```bash
gunicorn -w 4 -b 0.0.0.0:5000 app:app
```

## Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Submit a pull request

## License

This project is licensed under the MIT License.

## Support

For issues and feature requests, please open an issue in the repository.
