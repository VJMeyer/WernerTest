#!/usr/bin/python3
# -*- coding: utf-8 -*-

from flask import Flask, render_template, request, redirect, url_for, session, send_file, abort
from werkzeug.security import check_password_hash, generate_password_hash
from functools import wraps
import os
import markdown
from pathlib import Path

app = Flask(__name__)
app.config.from_object('config.Config')


def login_required(f):
    """Decorator to require login for routes"""
    @wraps(f)
    def decorated_function(*args, **kwargs):
        if 'logged_in' not in session:
            return redirect(url_for('login', next=request.url))
        return f(*args, **kwargs)
    return decorated_function


def is_safe_path(basedir, path, follow_symlinks=True):
    """Check if the path is within the base directory"""
    if follow_symlinks:
        matchpath = os.path.realpath(path)
    else:
        matchpath = os.path.abspath(path)
    return basedir == matchpath or matchpath.startswith(basedir + os.sep)


def get_file_info(filepath, filename):
    """Get information about a file including associated images and markdown"""
    base_name = os.path.splitext(filename)[0]
    file_ext = os.path.splitext(filename)[1][1:].lower()

    directory = os.path.dirname(filepath)

    info = {
        'name': filename,
        'display_name': base_name,
        'extension': file_ext,
        'is_dir': os.path.isdir(filepath),
        'image': None,
        'markdown': None
    }

    if not info['is_dir']:
        # Look for associated image file
        for img_ext in app.config['IMAGE_EXTENSIONS']:
            img_path = os.path.join(directory, f"{base_name}.{img_ext}")
            if os.path.isfile(img_path) and img_path != filepath:
                info['image'] = f"{base_name}.{img_ext}"
                break

        # Look for associated markdown file
        for md_ext in app.config['MARKDOWN_EXTENSIONS']:
            md_path = os.path.join(directory, f"{base_name}.{md_ext}")
            if os.path.isfile(md_path) and md_path != filepath:
                with open(md_path, 'r', encoding='utf-8') as f:
                    md_content = f.read()
                    info['markdown'] = markdown.markdown(md_content)
                break

    return info


@app.route('/login', methods=['GET', 'POST'])
def login():
    """Login page"""
    if request.method == 'POST':
        username = request.form.get('username')
        password = request.form.get('password')

        if username == app.config['USERNAME'] and password == app.config['PASSWORD']:
            session['logged_in'] = True
            next_page = request.args.get('next')
            return redirect(next_page or url_for('browse'))
        else:
            return render_template('login.html', error='Invalid credentials')

    return render_template('login.html')


@app.route('/logout')
def logout():
    """Logout"""
    session.pop('logged_in', None)
    return redirect(url_for('login'))


@app.route('/')
@login_required
def index():
    """Redirect to browse"""
    return redirect(url_for('browse'))


@app.route('/browse')
@app.route('/browse/<path:subpath>')
@login_required
def browse(subpath=''):
    """Browse files and folders"""
    base_dir = os.path.realpath(app.config['BASE_DIR'])
    current_path = os.path.join(base_dir, subpath) if subpath else base_dir
    current_path = os.path.realpath(current_path)

    # Security check: ensure we're still within BASE_DIR
    if not is_safe_path(base_dir, current_path):
        abort(403)

    if not os.path.exists(current_path):
        abort(404)

    if not os.path.isdir(current_path):
        abort(400)

    # Get list of files and directories
    items = []
    try:
        for item_name in sorted(os.listdir(current_path)):
            item_path = os.path.join(current_path, item_name)

            # Skip hidden files
            if item_name.startswith('.'):
                continue

            item_info = get_file_info(item_path, item_name)
            items.append(item_info)
    except PermissionError:
        abort(403)

    # Separate folders and files
    folders = [item for item in items if item['is_dir']]
    files = [item for item in items if not item['is_dir']]

    # Build breadcrumb navigation
    breadcrumbs = []
    if subpath:
        parts = subpath.split('/')
        current = ''
        for part in parts:
            current = os.path.join(current, part) if current else part
            breadcrumbs.append({'name': part, 'path': current})

    return render_template('browse.html',
                         folders=folders,
                         files=files,
                         current_path=subpath,
                         breadcrumbs=breadcrumbs)


@app.route('/view/<path:filepath>')
@login_required
def view_file(filepath):
    """Serve a file for viewing"""
    base_dir = os.path.realpath(app.config['BASE_DIR'])
    file_path = os.path.join(base_dir, filepath)
    file_path = os.path.realpath(file_path)

    # Security check: ensure we're still within BASE_DIR
    if not is_safe_path(base_dir, file_path):
        abort(403)

    if not os.path.isfile(file_path):
        abort(404)

    # Check if file extension is allowed
    file_ext = os.path.splitext(file_path)[1][1:].lower()
    if file_ext not in app.config['ALLOWED_EXTENSIONS']:
        abort(403)

    return send_file(file_path)


@app.route('/image/<path:filepath>')
@login_required
def view_image(filepath):
    """Serve an image file"""
    base_dir = os.path.realpath(app.config['BASE_DIR'])
    file_path = os.path.join(base_dir, filepath)
    file_path = os.path.realpath(file_path)

    # Security check
    if not is_safe_path(base_dir, file_path):
        abort(403)

    if not os.path.isfile(file_path):
        abort(404)

    return send_file(file_path)


if __name__ == '__main__':
    app.run(debug=True, host='0.0.0.0', port=5000)
