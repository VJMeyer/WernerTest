#!/usr/bin/python3
# -*- coding: utf-8 -*-

import sys
import os

# Add the application directory to the path
INTERP = "/usr/bin/python3"
if sys.executable != INTERP:
    os.execl(INTERP, INTERP, *sys.argv)

# Add the application directory to sys.path
cwd = os.getcwd()
sys.path.insert(0, cwd)

# Import the Flask application
from app import app as application
