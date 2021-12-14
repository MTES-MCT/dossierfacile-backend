import os
import logging
from logging import Formatter, StreamHandler
from flask import Flask, request, jsonify, render_template, abort, json
from recognize_img import recognize_image_from_url
from functools import wraps
from pre_img import process_pdf
from pathlib import Path
import tempfile
import requests
import sys

basedir = os.path.abspath(os.path.dirname(__file__))
UPLOAD_FOLDER = './uploads'


app = Flask(__name__)
app.config['UPLOAD_FOLDER'] = UPLOAD_FOLDER


def require_appkey(view_function):
    @wraps(view_function)
    def decorated_function(*args, **kwargs):
        key = os.environ.get("TESSERACT_API_KEY")
        if request.headers.get('x-api-key') and request.headers.get('x-api-key') == key:
            return view_function(*args, **kwargs)
        else:
            abort(401)
    return decorated_function


@app.route('/', methods=['GET', 'POST'])
@require_appkey
def index():
    return render_template('index.html')


@app.route('/ocr', methods=['POST'])
@require_appkey
def ocr():
    urls = request.json['url']
    data = request.get_json()
    selected_pages = []
    dpi = 150
    images = None
    output_texts = []
    if 'images' in data:
        images = data['images']
    if 'pages' in data:
        selected_pages = data['pages']
    if 'dpi' in data:
        dpi = data['dpi']
    extensions_to_check = ['pdf', 'jpg', 'jpeg', 'png']
    for url in urls:
        if not url.split('.')[-1] in extensions_to_check:
            return jsonify({"error": "Not Supported file types, please check urls"})
    for url in urls:
        if url.split('.')[-1] in ['pdf']:
            with tempfile.TemporaryDirectory() as tmpDir:
                path = "{}/tmpFile.pdf".format(tmpDir)
                response = requests.get(url)
                filename = Path(path)
                filename.write_bytes(response.content)
                rec_string = process_pdf(path, selected_pages, dpi)
                output_texts.append(rec_string)
        else:
            text = recognize_image_from_url(url)
            output_texts.append(text)
    return jsonify({"output": output_texts})


@app.errorhandler(500)
def internal_error(error):
    print(str(error))  # ghetto logging


@app.errorhandler(404)
def not_found_error(error):
    print(str(error))


@app.errorhandler(405)
def not_allowed_error(error):
    print(str(error))


if not app.debug:
    logging.basicConfig(level=logging.DEBUG)
    handler = StreamHandler(sys.stdout)
    handler.setFormatter(
        Formatter('%(asctime)s %(levelname)s: \
            %(message)s [in %(pathname)s:%(lineno)d]')
    )
    app.logger.setLevel(logging.INFO)
    handler.setLevel(logging.INFO)
    app.logger.addHandler(handler)
    app.logger.info('Logger configured')


if __name__ == '__main__':
    app.run(host="0.0.0.0", port=int(8080), use_reloader=False, debug=False)
