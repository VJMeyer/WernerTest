# Flask File Browser

A secure web-based file browser for viewing and managing PDFs, ebooks, and other files on your Strato web hosting.

## Features

- 🔒 Password-protected access
- 📁 Folder and file navigation
- 🖼️ Automatic image preview for files with matching image files
- 📝 Markdown description display for files with matching .md files
- 📄 Support for PDF, EPUB, MOBI, and other ebook formats
- 🎨 Clean, responsive interface
- 🔐 Secure file access with path validation

## Installation on Strato Web Hosting

### Prerequisites

- Strato web hosting package with Python 3 support
- SSH or FTP access to your web space

### Step 1: Upload Files

Upload all files from this repository to your Strato web space directory (e.g., `/html` or a subdirectory).

### Step 2: Install Python Dependencies

SSH into your Strato account and navigate to the application directory:

```bash
cd /path/to/your/application
pip3 install --user -r requirements.txt
```

If `pip3` is not available, you may need to contact Strato support or install packages manually.

### Step 3: Configure the Application

Edit the `config.py` file and update the following settings:

```python
# Change this to a random secret key
SECRET_KEY = 'your-random-secret-key-here'

# Change this to the directory you want to browse
BASE_DIR = '/path/to/your/files'

# Change these to your desired credentials
USERNAME = 'your-username'
PASSWORD = 'your-password'
```

**Security Note:** Use a strong password and keep it secure!

Alternatively, you can use environment variables by creating a `.env` file:

```bash
cp .env.example .env
# Then edit .env with your values
```

### Step 4: Update .htaccess (if needed)

Edit the `.htaccess` file and update the path if your application is not in `/home/user/WernerTest`:

```apache
PassengerAppRoot /your/actual/path
```

### Step 5: Set Permissions

Make sure the `passenger_wsgi.py` and `app.py` files are executable:

```bash
chmod +x passenger_wsgi.py app.py
```

### Step 6: Test the Application

Visit your domain in a web browser. You should see the login page.

If you encounter issues:
- Check the Passenger error logs (usually in `logs/` directory)
- Verify that Python 3 is available at `/usr/bin/python3`
- Ensure all dependencies are installed
- Check file permissions

## Usage

### Logging In

1. Navigate to your application URL
2. Enter your username and password (as configured in `config.py`)
3. Click "Login"

### Browsing Files

- Click on folders to navigate into them
- Use the breadcrumb navigation to go back
- Click on files to open them in your browser
- Files with associated images will display the image as a preview
- Files with associated markdown files will display the description

### File Associations

The application automatically looks for related files:

**Example:**
If you have a PDF file named `mybook.pdf`, the application will:
- Look for image files like `mybook.jpg`, `mybook.png`, etc., and display them as previews
- Look for markdown files like `mybook.md` and display the content as a description

This is useful for adding cover images and descriptions to your ebooks!

## Supported File Types

### Documents
- PDF (`.pdf`)
- EPUB (`.epub`)
- MOBI (`.mobi`)
- AZW/AZW3 (`.azw`, `.azw3`)
- Text files (`.txt`)

### Images (for previews)
- JPEG (`.jpg`, `.jpeg`)
- PNG (`.png`)
- GIF (`.gif`)
- WebP (`.webp`)

### Descriptions
- Markdown (`.md`, `.markdown`)

## Security Features

- Password authentication required for all pages
- Path traversal protection (prevents accessing files outside BASE_DIR)
- Session-based authentication
- File type validation
- Hidden files (starting with `.`) are not displayed

## Customization

### Changing the Appearance

Edit the CSS file at `static/css/style.css` to customize colors, fonts, and layout.

### Adding More File Types

Edit `config.py` and add file extensions to the `ALLOWED_EXTENSIONS` set:

```python
ALLOWED_EXTENSIONS = {
    'pdf', 'epub', 'mobi', 'azw', 'azw3', 'txt',
    'jpg', 'jpeg', 'png', 'gif', 'webp',
    'md', 'markdown',
    'docx', 'xlsx'  # Add more extensions here
}
```

## Troubleshooting

### Application doesn't start
- Check that Python 3 is installed: `which python3`
- Verify `passenger_wsgi.py` has correct shebang: `#!/usr/bin/python3`
- Check Passenger logs for errors

### Files not displaying
- Verify `BASE_DIR` in `config.py` points to the correct directory
- Check file permissions (files must be readable by the web server)
- Ensure file extensions are in `ALLOWED_EXTENSIONS`

### Images not showing
- Verify image files are in the same directory as the associated file
- Check that image file names match (same base name, different extension)
- Ensure image extensions are in `IMAGE_EXTENSIONS`

### Cannot login
- Double-check username and password in `config.py`
- Clear your browser cookies
- Check that `SECRET_KEY` is set in `config.py`

## Development

To run the application locally for development:

```bash
# Install dependencies
pip install -r requirements.txt

# Run the development server
python3 app.py
```

The application will be available at `http://localhost:5000`

## License

See LICENSE file for details.

## Support

For issues specific to this application, please check the troubleshooting section above.
For Strato-specific hosting questions, contact Strato support.
