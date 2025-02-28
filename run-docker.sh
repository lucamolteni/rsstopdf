#!/bin/bash
# Usage: ./run_rsstopdf.sh /path/to/input.opml /path/to/output_directory



if [ "$#" -ne 2 ]; then
  echo "Usage: $0 <input_file> <output_directory>"
  exit 1
fi

INPUT_FILE=$(realpath "$1")
OUTPUT_DIR=$(realpath "$2")

# Run the container with input and output mounts.
podman run --rm \
  --privileged \
  -v "$INPUT_FILE":/input.opml \
  -v "$OUTPUT_DIR":/output \
  rsstopdf /input.opml