#!/usr/bin/env python 
# coding:utf-8
import urllib
import cv2
import numpy as np
from PIL import Image
from pdf2image import convert_from_path
from PyPDF2 import PdfFileReader
import gc
import time
import logging
import recognize_img

BINARY_THRESHOLD = 180
Image.MAX_IMAGE_PIXELS = None
log = logging.getLogger(__name__)


# get grayscale image
def get_grayscale(image):
    return cv2.cvtColor(image, cv2.COLOR_BGR2GRAY)


# noise removal
def remove_noise(image):
    return cv2.medianBlur(image, 5)


# thresholding
def thresholding(image):
    return cv2.threshold(image, 0, 255, cv2.THRESH_BINARY + cv2.THRESH_OTSU)[1]


def thresholdingGaussian(image):
    return cv2.adaptiveThreshold(image, 255, cv2.ADAPTIVE_THRESH_GAUSSIAN_C, cv2.THRESH_BINARY, 11, 2)


def resize(img, factor=1.5):
    return cv2.resize(img, None, fx=factor, fy=factor, interpolation=cv2.INTER_CUBIC)


# dilation
def dilate(image):
    kernel = np.ones((1, 1), np.uint8)
    return cv2.dilate(image, kernel, iterations=1)


# erosion
def erode(image):
    kernel = np.ones((1, 1), np.uint8)
    return cv2.erode(image, kernel, iterations=1)


# opening - erosion followed by dilation
def opening(image):
    kernel = np.ones((5, 5), np.uint8)
    return cv2.morphologyEx(image, cv2.MORPH_OPEN, kernel)


# canny edge detection
def canny(image):
    return cv2.Canny(image, 100, 200)


def denoise(image):
    return cv2.fastNlMeansDenoising(image, 10, 10, 7)


# skew correction
def deskew(image):
    coords = np.column_stack(np.where(image > 0))
    angle = cv2.minAreaRect(coords)[-1]
    if angle < -45:
        angle = -(90 + angle)
    else:
        angle = -angle
    (h, w) = image.shape[:2]
    center = (w // 2, h // 2)
    M = cv2.getRotationMatrix2D(center, angle, 1.0)
    rotated = cv2.warpAffine(image, M, (w, h), flags=cv2.INTER_CUBIC, borderMode=cv2.BORDER_REPLICATE)
    return rotated


def image_smoothening(img):
    ret1, th1 = cv2.threshold(img, BINARY_THRESHOLD, 255, cv2.THRESH_BINARY)
    ret2, th2 = cv2.threshold(th1, 0, 255, cv2.THRESH_BINARY + cv2.THRESH_OTSU)
    blur = cv2.GaussianBlur(th2, (1, 1), 0)
    ret3, th3 = cv2.threshold(blur, 0, 255, cv2.THRESH_BINARY + cv2.THRESH_OTSU)
    return th3


def remove_noise_and_smooth(img):
    filtered = cv2.adaptiveThreshold(img.astype(np.uint8), 255, cv2.ADAPTIVE_THRESH_MEAN_C, cv2.THRESH_BINARY, 41, 3)
    kernel = np.ones((1, 1), np.uint8)
    opening = cv2.morphologyEx(filtered, cv2.MORPH_OPEN, kernel)
    closing = cv2.morphologyEx(opening, cv2.MORPH_CLOSE, kernel)
    img = image_smoothening(img)
    or_image = cv2.bitwise_or(img, closing)
    return or_image


def process_pdf(path, selected_pages, dpi=150):
    log.info("Recognizing...")
    config = '-l fra --oem 1 --psm 3'

    image_counter = count_pages(path)
    log.info("Processing pdf file of {} pages.".format(image_counter))

    if not selected_pages:
        selected_pages = range(1, image_counter + 1)

    rec_string = str("")

    for p in selected_pages:
        start = time.time()

        gc.collect()
        if p > image_counter:
            continue
        log.info("Get image from pdf, page {} ".format(p))
        pages = convert_from_path(path, dpi=dpi, fmt='jpg', thread_count=2, first_page=p, last_page=p)
        end = time.time()
        log.info(str(end - start) + ' seconds')
        for page in pages:
            log.info("Processing OpenCV page {} ".format(p))
            text = recognize_img.recognize_image(page, config)
            rec_string += text

        log.info("Recognition finished")

    return rec_string


def url_to_image(url):
    resp = urllib.urlopen(url)
    image = np.asarray(bytearray(resp.read()), dtype="uint8")
    image = cv2.imdecode(image, cv2.IMREAD_COLOR)
    return image


def count_pages(pdf_path):
    try:
        with open(pdf_path, 'rb') as f:
            pdf = PdfFileReader(f)
            return pdf.getNumPages()
    finally:
        pages = convert_from_path(pdf_path, dpi=70)
        return len(pages)
