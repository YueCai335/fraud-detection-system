# Jenkins controller for running this repo's Jenkinsfile on a laptop.
#
# The official image has no docker CLI. Our pipeline needs one for two things:
#   - `agent { docker { image ... } }`  -> pulls/starts the per-stage build containers
#   - `docker compose up`               -> the smoke-test stage
# Both talk to the host's Docker daemon through /var/run/docker.sock (mounted in compose.yml);
# nothing runs Docker-in-Docker.
FROM jenkins/jenkins:lts-jdk17

USER root
RUN apt-get update \
 && apt-get install -y --no-install-recommends ca-certificates curl gnupg python3 \
 && install -m 0755 -d /etc/apt/keyrings \
 && curl -fsSL https://download.docker.com/linux/debian/gpg -o /etc/apt/keyrings/docker.asc \
 && echo "deb [signed-by=/etc/apt/keyrings/docker.asc] https://download.docker.com/linux/debian $(. /etc/os-release && echo "$VERSION_CODENAME") stable" \
      > /etc/apt/sources.list.d/docker.list \
 && apt-get update \
 && apt-get install -y --no-install-recommends docker-ce-cli docker-compose-plugin \
 && rm -rf /var/lib/apt/lists/*
USER jenkins
