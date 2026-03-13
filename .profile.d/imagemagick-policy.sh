#!/usr/bin/env bash
# Override the permissive default ImageMagick policy with the project's strict policy.
POLICY_SRC="${HOME}/config/imagemagick/policy.xml"

# Support both ImageMagick 6 (Ubuntu 22.04 apt default) and 7
for POLICY_DST in /etc/ImageMagick-6/policy.xml /etc/ImageMagick-7/policy.xml; do
  if [ -f "${POLICY_DST}" ]; then
    cp "${POLICY_SRC}" "${POLICY_DST}" && echo "ImageMagick policy applied to ${POLICY_DST}."
  fi
done
