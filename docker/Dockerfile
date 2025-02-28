# Stage 2: Runtime with Java 21 and LaTeX
FROM fedora-minimal:latest

# Install Java 21 runtime and minimal TeX Live packages, including papertex
RUN microdnf install -y \
    java-21-openjdk-headless \
    git \
    maven \
    texlive-papertex \
    texlive-scheme-basic \
    && microdnf clean all

WORKDIR /app

# Clone the repository and build
RUN git clone https://github.com/lucamolteni/rsstopdf.git . && \
    mvn package

RUN chmod +x run.sh

CMD ["run.sh"]