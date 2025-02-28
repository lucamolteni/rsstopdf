# RSS to PDF Converter

A Java program to generate a magazine-like PDF to read news in a offline fashion from a ebook reader or a tablet.

[Here's](example.pdf) an example of the generated output. 

It uses the [papertex](https://ctan.org/pkg/papertex) package
You can read more in [this PDF](https://ctan.math.washington.edu/tex-archive/macros/latex/contrib/papertex/papertex.pdf)

Thanks Ignacio Llopis for the package

## Prerequisites

- Java 21 or higher
- Maven 3.6 or higher
- LaTeX distribution

## Dependencies

The project uses the following dependencies:

- LaTeX package: `papertex`

## Installation

1. **Clone the repository:**

    ```sh
    git clone https://github.com/lucamolteni/rsstopdf.git
    cd rsstopdf
    ```

2. **Install Java and Maven:**

   Make sure you have Java and Maven installed on your system. You can download them from:

    - [SDKMAN](https://sdkman.io/install/)

3. **Install LaTeX:**

   Install a LaTeX distribution if you don't have one. For example, you can install TeX Live on macOS using Homebrew:

    ```sh
    brew install --cask mactex
    ```
   
   Make sure the command `pdflatex` is available on your system as the `Latex` class will run `pdflatex` and it's currently not configurable.

   Make sure the `papertex` package is installed. You can install it using the package manager of your LaTeX distribution.

## Building the Project

To run the project, run the following command:

```sh
./start.sh <PATH_TO_OPML_FILE>
```

If you don't provide an opml file path, `.rsstopdf/opml.xml` will be used as a deafult. 

Files will be created inside the `~/.rsstopdf` folder 