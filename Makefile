.DEFAULT_GOAL := help

.PHONY: help setup gradle-wrapper envs install dev

help: ## Show this help
	@grep -E '^[a-zA-Z_-]+:.*## ' $(MAKEFILE_LIST) | awk 'BEGIN {FS = ":.*## "}; {printf "  \033[36m%-16s\033[0m %s\n", $$1, $$2}'

setup: gradle-wrapper envs ## One-time bootstrap: Gradle wrapper + .env files
	@echo ""
	@echo "Setup complete. Fill in Plaid Sandbox credentials in services/backend/.env, then run 'make dev'."

gradle-wrapper: services/backend/gradlew ## Generate the Gradle wrapper (via Docker, no local Gradle needed)

services/backend/gradlew: services/backend/gradle/wrapper/gradle-wrapper.properties
	docker run --rm -v "$(CURDIR)/services/backend":/work -w /work gradle:8.11-jdk21 gradle wrapper

envs: infra/.env services/backend/.env services/bff/.env apps/desktop/.env ## Copy .env.example -> .env everywhere (skips files that already exist)

infra/.env: infra/.env.example
	cp $< $@

services/backend/.env: services/backend/.env.example
	cp $< $@

services/bff/.env: services/bff/.env.example
	cp $< $@

apps/desktop/.env: apps/desktop/.env.example
	cp $< $@

install: ## Install JS dependencies (pnpm)
	pnpm install

dev: setup install ## Start the full dev stack (db + backend + bff + desktop)
	@if [ -f services/backend/.env ]; then set -a; . services/backend/.env; set +a; fi; pnpm dev
