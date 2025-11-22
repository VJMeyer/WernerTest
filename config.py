import os

class Config:
    # Secret key for session management - CHANGE THIS!
    SECRET_KEY = os.environ.get('SECRET_KEY') or 'change-this-to-a-random-secret-key'

    # Base directory for file browsing - CHANGE THIS to your desired folder
    BASE_DIR = os.environ.get('BASE_DIR') or '/path/to/your/files'

    # Simple authentication - CHANGE THESE!
    USERNAME = os.environ.get('USERNAME') or 'admin'
    PASSWORD = os.environ.get('PASSWORD') or 'change-this-password'

    # Allowed file extensions for viewing
    ALLOWED_EXTENSIONS = {
        'pdf', 'epub', 'mobi', 'azw', 'azw3', 'txt',
        'jpg', 'jpeg', 'png', 'gif', 'webp',
        'md', 'markdown'
    }

    # Image extensions
    IMAGE_EXTENSIONS = {'jpg', 'jpeg', 'png', 'gif', 'webp'}

    # Markdown extensions
    MARKDOWN_EXTENSIONS = {'md', 'markdown'}
