# Root Makefile for hotiron
# Provides convenient top-level targets. The real build logic lives in src/hotiron/Makefile
# (cd there and run make for advanced build options, or use these wrappers).
#
# Run 'make help' for colorful categorized usage.

# Colors (work in most terminals)
BLUE   := \033[0;34m
GREEN  := \033[0;32m
YELLOW := \033[0;33m
CYAN   := \033[0;36m
BOLD   := \033[1m
NC     := \033[0m

.DEFAULT_GOAL := help

.PHONY: help
help: ## Show this help (colorized with categories)
	@sh $(abspath $(dir $(lastword $(MAKEFILE_LIST))))/scripts/hotiron-banner.sh
	@echo "$(YELLOW)QSY: cd src/hotiron && make help for the native targets$(NC)"
	@echo ""
	@awk 'BEGIN {FS = ":.*##"; printf "  $(YELLOW)%-15s$(NC) %s\n", "Target", "Description"} \
		/^##@/ { printf "\n$(BOLD)%s$(NC)\n", substr($$0, 5) } \
		/^[a-zA-Z_0-9-]+:.*?##/ { printf "  $(GREEN)%-15s$(NC) %s\n", $$1, $$2 }' $(MAKEFILE_LIST)
	@echo ""
	@echo "Examples:"
	@echo "  make build"
	@echo "  make test"
	@echo "  make info"
	@echo "  make stats"
	@echo "  make start"
	@echo ""

##@ Setup
deps: ## Install all build and runtime dependencies (Ubuntu/Debian - recommended)
	sudo apt update
	sudo apt install -y \
		build-essential \
		maven \
		git \
		libusb-1.0-0-dev \
		libfftw3-dev \
		libfftw3-bin \
		openjdk-21-jdk \
		mingw-w64 \
		zip \
		ffmpeg \
		libpulse0

runtime-deps: ## Install only what's needed to run (after a build)
	sudo apt update
	sudo apt install -y openjdk-21-jdk libusb-1.0-0 libfftw3-bin ffmpeg libpulse0

##@ Build
build: ## Build the full application (jar + natives + zip). Delegates to subdir.
	$(MAKE) -C src/hotiron all

clean: ## Clean build artifacts (delegates to subdir).
	$(MAKE) -C src/hotiron clean

##@ Test & Quality
test: ## Native C/C++ self-tests (gcov) then Maven unit tests. No radio.
	$(MAKE) -C src/hotiron test

native-test: ## Native C/C++ self-tests + gcov only (no Maven).
	$(MAKE) -C src/hotiron native-test

test-hw: ## Hardware smoke tests (skips if no HackRF). Does not run under make test.
	$(MAKE) -C src/hotiron test-hw

lint: ## Run Maven compile (acts as basic lint/quality check).
	cd src/hotiron && mvn clean compile

stats: ## Refresh docs/stats.md (first-party LOC, packages, tests, git)
	@python3 $(abspath $(dir $(lastword $(MAKEFILE_LIST))))/scripts/repo-stats.py

mermaid: ## Parse-check all first-party Mermaid diagrams (uses mmdc if installed)
	@$(abspath $(dir $(lastword $(MAKEFILE_LIST))))/scripts/check-mermaid.sh

##@ Hardware
info: ## List HackRF devices, app SDK/API versions, and upstream updates
	@$(abspath $(dir $(lastword $(MAKEFILE_LIST))))/scripts/hackrf-info.sh

list-devices: info ## Alias for info

firmware-update: ## Flash official GSG firmware (dry-run; CONFIRM=1 to write)
	@CONFIRM=$(CONFIRM) VERSION=$(VERSION) FIRMWARE=$(FIRMWARE) SERIAL=$(SERIAL) \
		$(abspath $(dir $(lastword $(MAKEFILE_LIST))))/scripts/hackrf-firmware-update.sh

udev: ## Install persistent udev rules (sudo once). Keeps usbfs writable after usbipd attach.
	@$(abspath $(dir $(lastword $(MAKEFILE_LIST))))/scripts/hackrf-udev-install.sh

##@ Run
start: build ## Build if needed, then launch the Linux app.
	./src/hotiron/build/hotiron/hotiron.sh

mcp: build ## Launch the app with a localhost MCP server (127.0.0.1:8765)
	./src/hotiron/build/hotiron/hotiron.sh --mcp

run: start ## Alias for start.

.PHONY: build clean test native-test test-hw lint stats mermaid info list-devices firmware-update udev start run mcp help deps runtime-deps
