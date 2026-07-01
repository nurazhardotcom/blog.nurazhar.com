# syntax=docker/dockerfile:1.7
#
# CI image for nurazhar.com — bakes the four tools needed by `bb build`
# so we stop downloading + curl|sh-ing on every pipeline run.
#
# Tools:
#   * babashka (Clojure interpreter, runs `bb build` and `bb validate-links`)
#   * pandoc   (markdown → html)
#   * d2       (diagram-as-code → svg)
#   * OpenJDK 25 JRE headless (available for any JVM-side deps we add later)
#
# Built once per push to main by `.gitlab-ci.yml` (docker-build job),
# pushed to GitLab project container registry. Consumed by `pages` job.
#
# Base: eclipse-temurin:25-jre-alpine — Alpine (musl) + JDK 25 JRE pre-installed.
# Smaller than FROM debian-slim, smaller than FROM alpine + install jdk,
# and gives us JDK 25 out of the box.
#
# bb and d2 are statically-linked binaries so they work on musl libc with
# no extra shim libs. pandoc is dynamically linked but the apk package
# publishers bundle the right deps.

ARG BB_VERSION=1.12.218
ARG D2_VERSION=0.6.9

FROM eclipse-temurin:25-jre-alpine

RUN apk add --no-cache \
        bash \
        ca-certificates \
        curl \
        gzip \
        make \
        pandoc \
        tar

# Install babashka from upstream tarball (statically linked).
RUN curl -fsSL \
      "https://github.com/babashka/babashka/releases/download/v${BB_VERSION}/babashka-${BB_VERSION}-linux-amd64-static.tar.gz" \
      -o /tmp/bb.tar.gz \
    && tar -xzf /tmp/bb.tar.gz -C /usr/local/bin \
    && rm /tmp/bb.tar.gz \
    && chmod +x /usr/local/bin/bb \
    && bb --version

# Install d2 via the upstream install script — handles release-asset naming
# and is more robust than guessing a specific binary URL.
RUN curl -fsSL "https://d2lang.com/install.sh" | sh -s -- --version "${D2_VERSION}" \
    && d2 --version

# Smoke test the chain dev.clj exercises: bb into pandoc, bb into d2.
RUN bb --version \
    && pandoc --version | head -1 \
    && d2 --version \
    && java -version

CMD ["/bin/sh"]
