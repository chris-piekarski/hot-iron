# Native Linux + Windows (mingw) libraries, then Maven fat JAR.
# Host packages (Ubuntu 22.04+): build-essential git maven openjdk-21-jdk
# libusb-1.0-0-dev libfftw3-dev libfftw3-bin mingw-w64  

# Use 'make help' for a colorful categorized list of targets.

# ANSI colors for pretty help output (RGB-ish / terminal colors)
BLUE   := \033[0;34m
GREEN  := \033[0;32m
YELLOW := \033[0;33m
RED    := \033[0;31m
CYAN   := \033[0;36m
BOLD   := \033[1m
NC     := \033[0m

.DEFAULT_GOAL := help

.PHONY: help
help: ## Show this help (colorized, categorized)
	@sh $(abspath $(CURDIR)/../../scripts/hotiron-banner.sh)
	@echo "$(YELLOW)QRZ: make <target> [options]$(NC)"
	@echo ""
	@awk 'BEGIN {FS = ":.*##"; printf "  $(YELLOW)%-20s$(NC) %s\n", "Target", "Description"} \
		/^##@/ { printf "\n$(BOLD)%s$(NC)\n", substr($$0, 5) } \
		/^[a-zA-Z_0-9-]+:.*?##/ { printf "  $(GREEN)%-20s$(NC) %s\n", $$1, $$2 }' $(MAKEFILE_LIST)
	@echo ""
	@echo "$(YELLOW)Common options: IS_RELEASE=y (default) etc.$(NC)"
	@echo ""

#Settings:
IS_RELEASE= y

# object files path
BUILD_PATH = obj
# build output 
OUTPUT_PATH= build/hotiron
#where the resulting release zip will be copied
RELEASE_DIR= ../../release/

SOURCES= \
lib/hackrf/host/libhackrf/src/hackrf.c \
lib/hackrf/host/hackrf-tools/src/hackrf_sweep.c \
src-c/hackrf_fm.c \
src-c/sweep_power_average.c

ATSC_CC= \
src-c/atsc/lib/atsc_plinfo.cc \
src-c/atsc/lib/atsc_fpll_impl.cc \
src-c/atsc/lib/atsc_sync_impl.cc \
src-c/atsc/lib/atsc_fs_checker_impl.cc \
src-c/atsc/lib/atsc_equalizer_impl.cc \
src-c/atsc/lib/atsc_viterbi_decoder_impl.cc \
src-c/atsc/lib/atsc_single_viterbi.cc \
src-c/atsc/lib/atsc_deinterleaver_impl.cc \
src-c/atsc/lib/atsc_rs_decoder_impl.cc \
src-c/atsc/lib/atsc_derandomizer_impl.cc \
src-c/atsc/lib/atsc_randomize.cc \
src-c/atsc/atsc_rx_filter.cpp \
src-c/atsc/atsc_pipeline.cpp

ATSC_C= \
src-c/atsc/fec/init_rs_char.c \
src-c/atsc/fec/decode_rs_char.c

ATSC_INCLUDES= -Isrc-c/atsc/shim -Isrc-c/atsc/lib -Isrc-c/atsc/fec -Isrc-c/atsc
ATSC_CXXFLAGS= -O3 -ffast-math

INCLUDE_PATHS= -Ilib/hackrf/host/libhackrf/src -I$(LIBUSB_DIR)/include/libusb-1.0 -Ilib/fftw-3.3.5-dll64
LIBUSB_DIR= lib/libusb-1.0.21
MAIN_JAR= $(OUTPUT_PATH)/lib/hotiron.jar

OBJECT_SUFFIX= o
OBJECT_SUFFIX_WIN= ow
 
OBJECTS= $(addsuffix .$(OBJECT_SUFFIX), $(addprefix $(BUILD_PATH)/, $(SOURCES)))
OBJECTS_ATSC= $(addsuffix .$(OBJECT_SUFFIX), $(addprefix $(BUILD_PATH)/, $(ATSC_CC))) \
	$(addsuffix .$(OBJECT_SUFFIX), $(addprefix $(BUILD_PATH)/, $(ATSC_C)))
OBJECTS_WIN= $(addsuffix .$(OBJECT_SUFFIX_WIN), $(addprefix $(BUILD_PATH)/, $(SOURCES)))
OBJECTS_ATSC_WIN= $(addsuffix .$(OBJECT_SUFFIX_WIN), $(addprefix $(BUILD_PATH)/, $(ATSC_CC))) \
	$(addsuffix .$(OBJECT_SUFFIX_WIN), $(addprefix $(BUILD_PATH)/, $(ATSC_C)))

HACKRF_SDK_PIN= v2026.01.3
HACKRF_SWEEP_PATCH= src-c/0001-hackrf_sweep-to-library-conversion-v2026.01.3.patch
CFLAGS= -DHACKRF_SWEEP_AS_LIBRARY -c -fPIC \
	-DLIBRARY_VERSION=\"$(HACKRF_SDK_PIN)\" -DLIBRARY_RELEASE=\"$(HACKRF_SDK_PIN)\"
LDFLAGS=  -shared 
CFLAGS_WIN= $(CFLAGS)

# Compiler setup for linux
CC= gcc
CXX= g++
# ...and for windows
CC_WIN= x86_64-w64-mingw32-gcc 
CXX_WIN= x86_64-w64-mingw32-g++

ifeq ($(IS_RELEASE), y)
CFLAGS += -fdata-sections -ffunction-sections -O2
LDFLAGS+= -Wl,--gc-sections #-s 
else
CFLAGS += -O0 -g #-O2
LDFLAGS+= -g 
endif

LDPATHS_STATIC= -L""

#paths
#path to build dlls
OUTPUT_DLL_DIR    = $(OUTPUT_PATH)/lib/linux-x86-64
OUTPUT_DLL_DIR_WIN     = $(OUTPUT_PATH)/lib/win32-x86-64

# Windows build setup
LDPATHS_WIN= -L"lib/fftw-3.3.5-dll64" -L"$(LIBUSB_DIR)/MinGW64/dll"
INCLUDE_PATHS_WIN= $(INCLUDE_PATHS) -Ilib/fftw-3.3.5-dll64 
LDLIBS_WIN= -lusb-1.0 -lfftw3f-3 -lpthread 
DLL_LIB_PTHREAD= lib/win32-x86-64/libwinpthread-1.dll
DLL_LIB_WIN= $(OUTPUT_DLL_DIR_WIN)/hackrf-sweep.dll
DLL_LIB_FFTW= lib/fftw-3.3.5-dll64/libfftw3f-3.dll
DLL_LIB_USB= $(LIBUSB_DIR)/MinGW64/dll/libusb-1.0.dll
LDFLAGS_WIN+= -Wl,--kill-at  -static-libgcc -static-libstdc++ -shared

# Linux build setup
# required installed libraries: openjdk-21 (headful), libfftw3, libusb-1.0
DLL_LIB= $(OUTPUT_DLL_DIR)/libhackrf-sweep.so
LDLIBS= -lusb-1.0 -lfftw3 -lfftw3f
CFLAGS+= -march=x86-64
LIB_DIR= linux-x86-64

ZIP_FILE= hackrf_spectrum_analyzer.zip
ZIP_FILE_PATH= $(OUTPUT_PATH)/../
SWEEP_POWER_AVERAGE_SELFTEST= $(BUILD_PATH)/sweep_power_average_selftest
GCOV_PATH= $(BUILD_PATH)/gcov
ATSC_SELFTEST= $(GCOV_PATH)/atsc_selftest
NATIVE_COVERAGE_TXT= $(GCOV_PATH)/coverage.txt
GCOV_CFLAGS= -std=c11 -O0 -g --coverage -Wall -Wextra
GCOV_CXXFLAGS= -std=c++17 -O0 -g --coverage -Wall -DNDEBUG $(ATSC_INCLUDES)
GCOV_ATSC_OBJS= $(addprefix $(GCOV_PATH)/,$(addsuffix .o,$(basename $(ATSC_CC) $(ATSC_C))))
GCOV_ATSC_SELFTEST_OBJ= $(GCOV_PATH)/src-c/atsc/atsc_selftest.o
GCOV_SWEEP_AVG_OBJ= $(GCOV_PATH)/src-c/sweep_power_average.o
GCOV_SWEEP_TEST_OBJ= $(GCOV_PATH)/src-c/sweep_power_average_selftest.o


.NOTPARALLEL: all
.PHONY: all

##@ Build
all: patch_hackrf prepare $(MAIN_JAR) $(DLL_LIB) zip_file ## Build everything (patch, prepare, jar, natives, zip)

.PHONY: dirs
dirs: ## (internal) create object directories
	@echo "Creating directories"
	@mkdir -p $(dir $(OBJECTS)) $(dir $(OBJECTS_ATSC)) $(dir $(OBJECTS_WIN)) $(dir $(OBJECTS_ATSC_WIN))

$(MAIN_JAR): ## (internal) build the fat jar via Maven
	mvn clean compile assembly:single
	mv build/*.jar $(OUTPUT_PATH)/lib/

.PHONY: zip_file
zip_file: $(DLL_LIB) $(DLL_LIB_WIN) ## (internal) create release zip
	mkdir -p $(RELEASE_DIR)
	rm -f $(ZIP_FILE_PATH)/$(ZIP_FILE)
	cd $(ZIP_FILE_PATH) && zip -r $(ZIP_FILE) *
	yes | cp -rf $(ZIP_FILE_PATH)/$(ZIP_FILE) $(RELEASE_DIR) 

.PHONY: patch_hackrf
patch_hackrf: ## Re-checkout and patch hackrf sources to library mode for v2026.01.3
	# format the src # clang-format --style=file:../../../.clang-format -i hackrf_sweep.c
	(cd lib/hackrf/ && git reset --hard $(HACKRF_SDK_PIN) && git apply < ../../$(HACKRF_SWEEP_PATCH))

.PHONY: prepare
prepare: dirs $(OBJECTS) $(OBJECTS_WIN) $(DLL_LIB_FFTW) ## (internal) prepare output dirs, objects, assets, launchers
	mkdir -p $(OUTPUT_DLL_DIR) $(OUTPUT_DLL_DIR_WIN)
	cp -f lib/program.ico lib/program.png $(OUTPUT_DLL_DIR)/../
	cp lib/launchers/hotiron.sh $(OUTPUT_PATH)
	cp lib/launchers/hotiron.cmd $(OUTPUT_PATH)
	cp ../../scripts/hotiron-banner.txt $(OUTPUT_PATH)
	chmod +x $(OUTPUT_PATH)/hotiron.sh
	cp lib/launchers/hotiron.sh $(OUTPUT_PATH)/../
	cp lib/launchers/hotiron.cmd $(OUTPUT_PATH)/../
	cp ../../scripts/hotiron-banner.txt $(OUTPUT_PATH)/../
	chmod +x $(OUTPUT_PATH)/../hotiron.sh
	cp -f $(DLL_LIB_FFTW) $(DLL_LIB_PTHREAD) $(DLL_LIB_USB) $(OUTPUT_DLL_DIR_WIN)

# HackrfSweepLibrary.java is hand-maintained (see src-c/hackrf_sweep.h). JNAerator is not used.
.PHONY: jnabridge
jnabridge: ## JNA wrapper is source-controlled; does not run JNAerator
	@echo "hotiron/jna/HackrfSweepLibrary.java is hand-maintained."
	@echo "Edit it to match src-c/hackrf_sweep.h; do not run JNAerator."

##@ Native Libraries
$(DLL_LIB_WIN): $(OBJECTS_WIN) $(OBJECTS_ATSC_WIN)
	echo "---- Building windows version [start] ----"
	$(CXX_WIN) $(LDFLAGS_WIN) $(LDPATHS_WIN) $(OBJECTS_WIN) $(OBJECTS_ATSC_WIN) -o $(DLL_LIB_WIN) $(LDLIBS_WIN)
	x86_64-w64-mingw32-strip --strip-unneeded $(DLL_LIB_WIN)
	echo "---- Building windows version [done ] ----"

$(DLL_LIB):  $(OBJECTS) $(OBJECTS_ATSC)
	echo "---- Building linux version [start] ----"
	$(CXX) $(LDFLAGS) $(LDPATHS) $(OBJECTS) $(OBJECTS_ATSC) -o $(DLL_LIB) $(LDLIBS)
	strip --strip-unneeded $(DLL_LIB)
	echo "---- Building linux version [done ] ----"

$(BUILD_PATH)/%.c.$(OBJECT_SUFFIX): %.c
	@mkdir -p $(dir $@)
	$(CC) $(CFLAGS) $(INCLUDE_PATHS) $(ATSC_INCLUDES) $< -o $@

$(BUILD_PATH)/%.cc.$(OBJECT_SUFFIX): %.cc
	@mkdir -p $(dir $@)
	$(CXX) $(CFLAGS) $(ATSC_CXXFLAGS) -DNDEBUG -std=c++17 $(INCLUDE_PATHS) $(ATSC_INCLUDES) $< -o $@

$(BUILD_PATH)/%.cpp.$(OBJECT_SUFFIX): %.cpp
	@mkdir -p $(dir $@)
	$(CXX) $(CFLAGS) $(ATSC_CXXFLAGS) -DNDEBUG -std=c++17 $(INCLUDE_PATHS) $(ATSC_INCLUDES) $< -o $@

$(BUILD_PATH)/%.c.$(OBJECT_SUFFIX_WIN): %.c
	@mkdir -p $(dir $@)
	$(CC_WIN) $(CFLAGS_WIN) $(INCLUDE_PATHS_WIN) $(ATSC_INCLUDES) $< -o $@

$(BUILD_PATH)/%.cc.$(OBJECT_SUFFIX_WIN): %.cc
	@mkdir -p $(dir $@)
	$(CXX_WIN) $(CFLAGS_WIN) $(ATSC_CXXFLAGS) -DNDEBUG -std=c++17 $(INCLUDE_PATHS_WIN) $(ATSC_INCLUDES) $< -o $@

$(BUILD_PATH)/%.cpp.$(OBJECT_SUFFIX_WIN): %.cpp
	@mkdir -p $(dir $@)
	$(CXX_WIN) $(CFLAGS_WIN) $(ATSC_CXXFLAGS) -DNDEBUG -std=c++17 $(INCLUDE_PATHS_WIN) $(ATSC_INCLUDES) $< -o $@

.PHONY: clean
clean: ## Clean all build artifacts (mvn + obj + build dirs)
	mvn clean
	rm -f  $(MAIN_JAR) $(OBJECTS) $(OBJECTS_ATSC) $(OBJECTS_WIN) $(OBJECTS_ATSC_WIN) $(DLL_LIB) $(DLL_LIB_WIN) $(SWEEP_POWER_AVERAGE_SELFTEST) $(ATSC_SELFTEST)
	rm -rf $(OUTPUT_DLL_DIR) $(BUILD_PATH) $(OUTPUT_PATH)/* bin/*

##@ Test & Quality
.PHONY: test native-test native-coverage test-hw lint stats mermaid info list-devices
test: native-test ## Native self-tests (gcov) then Maven unit tests. No radio.
	mvn clean test

native-test: $(SWEEP_POWER_AVERAGE_SELFTEST) $(ATSC_SELFTEST) ## C/C++ self-tests + gcov summary
	@find $(GCOV_PATH) -name '*.gcda' -delete 2>/dev/null || true
	$(SWEEP_POWER_AVERAGE_SELFTEST)
	$(ATSC_SELFTEST)
	@$(MAKE) --no-print-directory native-coverage

$(GCOV_PATH)/%.o: %.c
	@mkdir -p $(dir $@)
	$(CC) $(GCOV_CFLAGS) $(ATSC_INCLUDES) -c $< -o $@

$(GCOV_PATH)/%.o: %.cc
	@mkdir -p $(dir $@)
	$(CXX) $(GCOV_CXXFLAGS) -c $< -o $@

$(GCOV_PATH)/%.o: %.cpp
	@mkdir -p $(dir $@)
	$(CXX) $(GCOV_CXXFLAGS) -c $< -o $@

$(SWEEP_POWER_AVERAGE_SELFTEST): $(GCOV_SWEEP_AVG_OBJ) $(GCOV_SWEEP_TEST_OBJ) src-c/sweep_power_average.h
	@mkdir -p $(dir $@)
	$(CC) --coverage -o $@ $(GCOV_SWEEP_AVG_OBJ) $(GCOV_SWEEP_TEST_OBJ) -lm

$(ATSC_SELFTEST): $(GCOV_ATSC_SELFTEST_OBJ) $(GCOV_ATSC_OBJS)
	@mkdir -p $(dir $@)
	$(CXX) --coverage -o $@ $(GCOV_ATSC_SELFTEST_OBJ) $(GCOV_ATSC_OBJS) -lm

native-coverage: ## Summarize gcov from the last native-test run
	@python3 $(abspath $(CURDIR)/../../scripts/native-coverage.py) \
		--objects $(GCOV_PATH) \
		--source-root src-c \
		--module-root $(CURDIR) \
		--out $(NATIVE_COVERAGE_TXT)

test-hw: ## Hardware ITs (@Tag hardware / *IT). Skips if no HackRF USB device.
	mvn test -Phardware -DfailIfNoTests=false

lint: ## Run compile/lint checks via Maven (basic quality gate)
	mvn clean compile

stats: ## Refresh docs/stats.md (first-party LOC, packages, tests, git)
	@python3 $(abspath $(CURDIR)/../../scripts/repo-stats.py)

mermaid: ## Parse-check first-party Mermaid diagrams (uses mmdc if installed)
	@$(abspath $(CURDIR)/../../scripts/check-mermaid.sh)

##@ Hardware
.PHONY: info list-devices firmware-update udev
info: ## List HackRF devices, app SDK/API versions, and upstream updates
	@$(abspath $(CURDIR)/../../scripts/hackrf-info.sh)

list-devices: info ## Alias for info

firmware-update: ## Flash official GSG firmware (dry-run; CONFIRM=1 to write)
	@CONFIRM=$(CONFIRM) VERSION=$(VERSION) FIRMWARE=$(FIRMWARE) SERIAL=$(SERIAL) \
		$(abspath $(CURDIR)/../../scripts/hackrf-firmware-update.sh)

udev: ## Install persistent udev rules (sudo once)
	@$(abspath $(CURDIR)/../../scripts/hackrf-udev-install.sh)

##@ Run & Dev
start: build ## Build if needed, then start the Linux app
	./build/hotiron/hotiron.sh

mcp: build ## Start the app and listen for MCP on 127.0.0.1:8765
	./build/hotiron/hotiron.sh --mcp

run: start ## Alias for 'start'

build: all ## Alias for the default build target

##@ Utilities (advanced / internal)
# (see also the top-level Makefile at repo root for more convenient wrappers)
