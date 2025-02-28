#!/bin/bash

if [ "$#" -ne 2 ]; then
  echo "Usage: $0 <input_file> <output_directory>"
  exit 1
fi

INPUT_FILE=$(realpath "$1")
OUTPUT_DIR=$(realpath "$2")

# Run the container with input and output mounts.
podman run --rm \
  -v "$INPUT_FILE":/input.opml:Z \
  -v "$OUTPUT_DIR":/output:Z \
  rsstopdf /input.opml /output