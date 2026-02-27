# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

RSS-to-PDF converter that fetches RSS feeds from an OPML file, extracts article content, and generates a magazine-style PDF using LaTeX's `papertex` package. Designed for offline reading on e-readers/tablets.

## Build & Run Commands

```bash
# Build and run (requires Java 21+, Maven 3.6+, pdflatex with papertex package)
./run.sh <OPML_FILE> <OUTPUT_DIR>

# Run with defaults (~/.rsstopdf/opml.xml → ~/.rsstopdf/pdfs/)
./run.sh

# Run tests
mvn test

# Run a single test class
mvn test -Dtest=JSoupTest

# Run a single test method
mvn test -Dtest=JSoupTest#quoteUnquoteLatexTag

# Compile only
mvn compile

# Docker build (uses Podman)
cd docker && ./build.sh

# Docker run
./run-docker.sh "feeds.opml" "."
```

**Note:** Compilation requires `--enable-preview` (configured in pom.xml) for virtual threads.

## Architecture

### Pipeline Flow

The application runs a sequential pipeline in `RSSToPDF.main()`:

1. **OPML Parsing** — `Configuration` uses JAXB to deserialize the OPML XML into `Opml`/`Outline` objects. `RssTraversal` recursively flattens the hierarchy into a stream of feed URLs.
2. **RSS Fetching** — Each feed URL is fetched concurrently via virtual threads (`Executors.newVirtualThreadPerTaskExecutor()`). Raw responses saved to `01-fetch/`.
3. **Article Filtering** — `RssParser` uses ROME to parse feeds and filters to articles published in the last 24 hours. Results are sealed records (`ParseSuccess`/`ParseFailure`).
4. **Content Enrichment** — `RSSContent` re-fetches full article HTML (via Readability4J) when the RSS summary is under 5000 chars. Extracts image URLs. Saved to `02-create-articles/`.
5. **Image Fetching** — Images fetched concurrently per article, saved to `03-finalPDF/img/`.
6. **LaTeX Rendering** — `Layout` collects articles and renders `layout.vm` (Velocity template) to produce a `.tex` file. `JSoup` handles HTML→LaTeX conversion (links, images, paragraphs).
7. **PDF Generation** — `Latex` shells out to `pdflatex` to produce the final PDF, then `FileDump.movePdfFile()` places it in the output directory.

### Key Classes (all in `io.triode.rsstopdf`)

| Class | Role |
|-------|------|
| `RSSToPDF` | Entry point, orchestrates the full pipeline |
| `Configuration` | Paths, directory creation, OPML parsing via JAXB |
| `RssTraversal` | Recursive OPML outline flattening |
| `RssParser` | ROME-based feed parsing, 24-hour date filtering, sealed result types |
| `RSSContent` | Full article fetching (Readability4J), image URL extraction |
| `HttpFetch` | HTTP client wrapper with manual 301 redirect handling |
| `JSoup` | HTML→LaTeX conversion (images, links, paragraphs, special char escaping) |
| `Layout` | Velocity template engine, LaTeX special character escaping |
| `Latex` | `pdflatex` ProcessBuilder execution |
| `FileDump` | Phased file I/O (`01-fetch/`, `02-create-articles/`, `03-finalPDF/`) |
| `Opml`/`Outline` | JAXB-annotated data model for OPML XML |

### Concurrency Model

Three levels of virtual thread parallelism (Java 21 preview):
- Feed-level: all RSS feeds fetched concurrently
- Article-level: full content fetched concurrently
- Image-level: images per article fetched concurrently

### External Dependencies

- `pdflatex` must be installed and on PATH with the `papertex` LaTeX package
- Paper size is configured for Fujitsu Quaderno (203mm×270.7mm) in `layout.vm`

### Template

`src/main/resources/layout.vm` — Velocity template that generates the LaTeX document. Uses `papertex` document class with `\begin{news}` blocks for each article.
