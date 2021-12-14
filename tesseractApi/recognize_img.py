#!/usr/bin/env python
# coding:utf-8
from pathlib import Path
import requests
import pytesseract
import cv2
import numpy as np
import tempfile
import time
import logging
import pre_img

log = logging.getLogger(__name__)


def recognize_image_from_url(image_url):
    """Receives an image url. Returns a list of recognized texts"""
    config = '-l fra --oem 1 --psm 3'
    with tempfile.TemporaryDirectory() as tmpDir:
        path = "{}".format(tmpDir)
        response = requests.get(image_url)
        full_path = path + '/1'
        filename = Path(full_path)
        filename.write_bytes(response.content)
        img = cv2.imread(full_path)
        text = recognize_image(img, config)
        log.info("Recognition for image: " + image_url + " finished")
        # log.info("images_result", text)
        return text


def recognize_image(img, config):
    """Receives a numpy image and a config parameter. Returns a string recognized text"""
    img = np.array(img)
    img = pre_img.get_grayscale(img)
    blurred1 = cv2.medianBlur(img, 3)
    blurred2 = cv2.medianBlur(img, 51)
    divided = np.ma.divide(blurred1, blurred2).data
    normed = np.uint8(255 * divided / divided.max())
    th, threshed = cv2.threshold(normed, 100, 255, cv2.THRESH_OTSU)
    img = threshed
    start = time.time()
    log.info("Apply tesseract {} ".format(img))
    text = pytesseract.image_to_string(img, config=config)
    text = text.replace('\n', ' ')
    end = time.time()
    log.info(str(end - start) + ' seconds')
    return text







