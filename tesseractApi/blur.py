#!/usr/bin/env python 
# coding:utf-8
import cv2
from PIL import Image
from PyPDF2 import PdfFileReader
import logging

Image.MAX_IMAGE_PIXELS = None
log = logging.getLogger(__name__)


def variance_of_laplacian(image):
    # compute the Laplacian of the image and then return the focus
    # measure, which is simply the variance of the Laplacian
    return cv2.Laplacian(image, cv2.CV_64F).var()


def blur_factor(image):
    # load the image, convert it to grayscale, and compute the
    # focus measure of the image using the Variance of Laplacian
    # method
    gray = cv2.cvtColor(image, cv2.COLOR_BGR2GRAY)
    return variance_of_laplacian(gray)


def count_pages(pdf_path):
    with open(pdf_path, 'rb') as f:
        pdf = PdfFileReader(f)
        information = pdf.getDocumentInfo()
        return pdf.getNumPages()
