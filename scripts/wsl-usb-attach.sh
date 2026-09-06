#!/usr/bin/env bash
# Attach the HackRF, bench nRF BLE sniffer, and motherboard Bluetooth
# to this WSL2 distro. Windows does not share USB with Ubuntu until
# usbipd attaches the device.
#
# Invoked by `make wsl-usb`. Run from WSL, not PowerShell.
# Host Bluetooth (8087:0033) is the Intel adapter — not the nRF sniffer.
set -euo pipefail

HACKRF_IDS="1d50:6089 1d50:604b 1d50:cc15"
JLINK_IDS="1366:1015 1366:0101"
JLINK_ACM_ID="1366:1015"
# Intel Wireless Bluetooth on this motherboard (AX201/AX210-class USB).
BT_IDS="8087:0033"

usage() {
	cat <<'EOF'
Attach HackRF + nRF BLE sniffer + host Bluetooth to this WSL2 Ubuntu via usbipd.

Usage:
  wsl-usb-attach.sh             attach all three (default)
  wsl-usb-attach.sh --list      Windows usbipd list + Linux lsusb
  wsl-usb-attach.sh --detach    return the attached devices to Windows
  wsl-usb-attach.sh --watch     attach and re-attach after unplug/reset
  wsl-usb-attach.sh --hackrf    HackRF only
  wsl-usb-attach.sh --ble       nRF J-Link only
  wsl-usb-attach.sh --bt        motherboard Intel Bluetooth only

Once (Administrator PowerShell), if the devices were never shared:

  usbipd bind --hardware-id 1d50:6089
  usbipd bind --hardware-id 1366:1015
  usbipd bind --hardware-id 8087:0033

Then in WSL:

  make udev          # once: usbfs + ACM MODE 0666
  make wsl-usb       # each session, and after a radio reset

HackRF One is 1d50:6089. The bench sniffer is SEGGER J-Link 1366:1015.
Host Bluetooth is Intel 8087:0033 — attaching it steals BT from Windows.
EOF
}

WANT_HACKRF=1
WANT_JLINK=1
WANT_BT=1
MODE=attach
EXCLUSIVE=0

only() {
	if [ "$EXCLUSIVE" -eq 0 ]; then
		WANT_HACKRF=0
		WANT_JLINK=0
		WANT_BT=0
		EXCLUSIVE=1
	fi
}

while [ $# -gt 0 ]; do
	case "$1" in
	-h | --help)
		usage
		exit 0
		;;
	--list)
		MODE=list
		;;
	--detach)
		MODE=detach
		;;
	--watch)
		MODE=watch
		;;
	--hackrf)
		only
		WANT_HACKRF=1
		;;
	--ble | --jlink | --nrf)
		only
		WANT_JLINK=1
		;;
	--bt | --bluetooth | --host-bt)
		only
		WANT_BT=1
		;;
	--all)
		WANT_HACKRF=1
		WANT_JLINK=1
		WANT_BT=1
		;;
	*)
		echo "unknown option: $1" >&2
		usage >&2
		exit 1
		;;
	esac
	shift
done

is_wsl() {
	[ -n "${WSL_DISTRO_NAME:-}" ] && return 0
	[ -f /proc/sys/fs/binfmt_misc/WSLInterop ] && return 0
	grep -qi microsoft /proc/version 2>/dev/null
}

is_wsl2() {
	grep -qiE 'WSL2|microsoft-standard' /proc/version 2>/dev/null && return 0
	[ -d /run/WSL ] && return 0
	return 1
}

linux_has_id() {
	id=$(echo "$1" | tr 'A-F' 'a-f')
	vid=${id%:*}
	pid=${id#*:}
	[ -d /sys/bus/usb/devices ] || return 1
	for dir in /sys/bus/usb/devices/*; do
		[ -f "$dir/idVendor" ] || continue
		v=$(tr -d ' \r\n' <"$dir/idVendor")
		p=$(tr -d ' \r\n' <"$dir/idProduct")
		[ "$v" = "$vid" ] && [ "$p" = "$pid" ] && return 0
	done
	return 1
}

usbfs_node() {
	id=$(echo "$1" | tr 'A-F' 'a-f')
	vid=${id%:*}
	pid=${id#*:}
	for dir in /sys/bus/usb/devices/*; do
		[ -f "$dir/idVendor" ] || continue
		v=$(tr -d ' \r\n' <"$dir/idVendor")
		p=$(tr -d ' \r\n' <"$dir/idProduct")
		[ "$v" = "$vid" ] && [ "$p" = "$pid" ] || continue
		bus=$(tr -d ' \r\n' <"$dir/busnum")
		dev=$(tr -d ' \r\n' <"$dir/devnum")
		printf '/dev/bus/usb/%03d/%03d\n' "$bus" "$dev"
		return 0
	done
	return 1
}

ensure_rw() {
	path=$1
	[ -e "$path" ] || return 0
	[ -w "$path" ] && return 0
	if chmod a+rw "$path" 2>/dev/null; then
		return 0
	fi
	if command -v sudo >/dev/null 2>&1; then
		if [ -t 0 ]; then
			echo "  chmod a+rw $path (sudo)"
			sudo chmod a+rw "$path" && return 0
		elif sudo -n chmod a+rw "$path" 2>/dev/null; then
			return 0
		fi
	fi
	echo "  not writable: $path  (run: make udev)" >&2
	return 1
}

chmod_id() {
	node=$(usbfs_node "$1" || true)
	[ -n "${node:-}" ] || return 0
	ensure_rw "$node"
}

chmod_acm() {
	found=0
	for p in /dev/ttyACM*; do
		[ -e "$p" ] || continue
		found=1
		ensure_rw "$p" || true
	done
	if [ "$found" -eq 0 ]; then
		if linux_has_id "$JLINK_ACM_ID"; then
			echo "  J-Link is up but no /dev/ttyACM* yet (cdc_acm). Replug or restore VCOM." >&2
		fi
	fi
}

print_linux() {
	echo "Linux USB"
	echo "---------"
	if command -v lsusb >/dev/null 2>&1; then
		lsusb 2>/dev/null || true
	else
		echo "(lsusb not installed; using sysfs)"
		for dir in /sys/bus/usb/devices/*; do
			[ -f "$dir/idVendor" ] || continue
			v=$(tr -d ' \r\n' <"$dir/idVendor")
			p=$(tr -d ' \r\n' <"$dir/idProduct")
			echo "  $v:$p  $dir"
		done
	fi
	echo
	echo "HotIron radios on this Ubuntu:"
	any=0
	for id in $HACKRF_IDS $JLINK_IDS $BT_IDS; do
		if linux_has_id "$id"; then
			any=1
			node=$(usbfs_node "$id" || echo "?")
			case "$id" in
			1d50:6089) name="HackRF One" ;;
			1d50:604b) name="HackRF Jawbreaker" ;;
			1d50:cc15) name="rad1o" ;;
			1366:1015) name="SEGGER J-Link (CDC+MSD+BULK)" ;;
			1366:0101) name="SEGGER J-Link (no VCOM)" ;;
			8087:0033) name="Intel Wireless Bluetooth (motherboard)" ;;
			*) name="USB" ;;
			esac
			echo "  $id  $name  $node"
		fi
	done
	if [ "$any" -eq 0 ]; then
		echo "  (none)"
	fi
	if linux_has_id 1366:0101 && ! linux_has_id 1366:1015; then
		echo "  J-Link is 1366:0101 (J-Link-only). Restore VCOM before BLE sniff:" >&2
		echo "    JLinkExe  ->  vcom enable  ->  power-cycle the DK" >&2
	fi
	echo
	ls -l /dev/ttyACM* /dev/serial/by-id/*J-Link* /dev/jlink-acm 2>/dev/null || true
	if [ -d /sys/class/bluetooth ]; then
		echo
		echo "Bluetooth HCI:"
		for hci in /sys/class/bluetooth/hci*; do
			[ -e "$hci" ] || continue
			if [ -f "$hci/address" ]; then
				addr=$(tr -d ' \r\n' <"$hci/address" 2>/dev/null || true)
				echo "  $(basename "$hci")  ${addr:-?}"
			else
				echo "  $(basename "$hci")  (btusb bound)"
			fi
		done
	fi
}

find_usbipd() {
	if command -v usbipd.exe >/dev/null 2>&1; then
		command -v usbipd.exe
		return 0
	fi
	if command -v usbipd >/dev/null 2>&1; then
		command -v usbipd
		return 0
	fi
	for p in \
		"/mnt/c/Program Files/usbipd-win/usbipd.exe" \
		"/mnt/c/Program Files (x86)/usbipd-win/usbipd.exe"; do
		if [ -f "$p" ]; then
			printf '%s\n' "$p"
			return 0
		fi
	done
	if command -v where.exe >/dev/null 2>&1; then
		w=$(where.exe usbipd 2>/dev/null | tr -d '\r' | head -n1 || true)
		if [ -n "$w" ]; then
			if command -v wslpath >/dev/null 2>&1; then
				wslpath -u "$w"
			else
				printf '%s\n' "$w"
			fi
			return 0
		fi
	fi
	return 1
}

to_win_path() {
	if command -v wslpath >/dev/null 2>&1; then
		wslpath -w "$1"
		return 0
	fi
	echo "$1" | sed 's|^/mnt/\([a-zA-Z]\)/|\1:/|; s|/|\\|g'
}

find_powershell() {
	for p in \
		"/mnt/c/Windows/System32/WindowsPowerShell/v1.0/powershell.exe" \
		"/mnt/c/Windows/System32/powershell.exe"; do
		[ -f "$p" ] && printf '%s\n' "$p" && return 0
	done
	command -v powershell.exe 2>/dev/null || return 1
}

usbipd_run() {
	"$USBIPD" "$@" 2>&1 | tr -d '\r'
}

wanted_ids() {
	ids=
	if [ "$WANT_HACKRF" -eq 1 ]; then
		ids="$ids $HACKRF_IDS"
	fi
	if [ "$WANT_JLINK" -eq 1 ]; then
		ids="$ids $JLINK_IDS"
	fi
	if [ "$WANT_BT" -eq 1 ]; then
		ids="$ids $BT_IDS"
	fi
	echo "$ids"
}

# Connected radios matching the requested IDs (Windows side).
# Prints: busid vidpid state
list_wanted_windows() {
	usbipd_run list | awk -v want="$(wanted_ids)" '
		BEGIN {
			n = split(want, w, " ")
			for (i = 1; i <= n; i++) if (w[i] != "") need[tolower(w[i])] = 1
		}
		$2 ~ /^[0-9a-fA-F]{4}:[0-9a-fA-F]{4}$/ {
			id = tolower($2)
			if (!(id in need)) next
			bus = $1
			if ($(NF-1) == "Not" && $NF == "shared") st = "Not shared"
			else st = $NF
			print bus, id, st
		}
	'
}

present_wanted_ids() {
	list_wanted_windows | awk '{print $2}' | sort -u
}

elevate_bind() {
	id=$1
	extra=${2:-}
	ps1=$(find_powershell || true)
	usbipd_win=$(to_win_path "$USBIPD" 2>/dev/null || true)
	[ -n "${ps1:-}" ] && [ -n "${usbipd_win:-}" ] || return 1
	args="bind --hardware-id $id"
	[ -n "$extra" ] && args="bind $extra --hardware-id $id"
	"$ps1" -NoProfile -Command \
		"Start-Process -FilePath '$usbipd_win' -ArgumentList '$args' -Verb RunAs -Wait" \
		>/dev/null 2>&1 || true
}

bind_id() {
	id=$1
	echo "Sharing $id (needs Administrator once; UAC may prompt)..."
	if usbipd_run bind --hardware-id "$id" >/dev/null; then
		return 0
	fi
	# Motherboard Bluetooth is claimed by Windows; --force unbinds the host stack.
	case "$id" in
	8087:*)
		echo "  Windows owns this Bluetooth adapter; bind --force (Windows BT will drop)..."
		if usbipd_run bind --force --hardware-id "$id" >/dev/null; then
			return 0
		fi
		elevate_bind "$id" --force || true
		return 0
		;;
	esac
	if elevate_bind "$id"; then
		return 0
	fi
	echo "  bind failed. In Administrator PowerShell:" >&2
	echo "    usbipd bind --hardware-id $id" >&2
	case "$id" in
	8087:*) echo "    usbipd bind --force --hardware-id $id" >&2 ;;
	esac
	return 1
}

attach_one() {
	id=$1
	auto=${2:-0}
	if linux_has_id "$id" && [ "$auto" -eq 0 ]; then
		echo "Already on Linux: $id"
		case "$id" in
		8087:*) ;; # btusb claims the node; usbfs chmod is optional
		*) chmod_id "$id" || true ;;
		esac
		return 0
	fi
	args=(attach --wsl)
	if [ -n "${WSL_DISTRO_NAME:-}" ]; then
		args+=("$WSL_DISTRO_NAME")
	fi
	if [ "$auto" -eq 1 ]; then
		args+=(--auto-attach)
	fi
	args+=(--hardware-id "$id")

	set +e
	out=$("$USBIPD" "${args[@]}" 2>&1)
	rc=$?
	set -e
	out=$(printf '%s' "$out" | tr -d '\r')

	if [ "$rc" -eq 0 ]; then
		echo "Attached $id"
		return 0
	fi
	low=$(echo "$out" | tr 'A-Z' 'a-z')
	case "$low" in
	*already\ attached*)
		echo "Already attached: $id"
		return 0
		;;
	*not\ shared* | *not\ bound*)
		bind_id "$id" || return 1
		set +e
		out=$("$USBIPD" "${args[@]}" 2>&1)
		rc=$?
		set -e
		out=$(printf '%s' "$out" | tr -d '\r')
		if [ "$rc" -eq 0 ] || echo "$out" | grep -qi 'already attached'; then
			echo "Attached $id"
			return 0
		fi
		echo "$out" >&2
		return 1
		;;
	*no\ device* | *not\ found* | *no\ usb\ device*)
		echo "Not plugged in (Windows): $id" >&2
		return 2
		;;
	esac
	echo "$out" >&2
	return 1
}

detach_one() {
	id=$1
	set +e
	out=$(usbipd_run detach --hardware-id "$id")
	rc=$?
	set -e
	if [ "$rc" -eq 0 ] || echo "$out" | grep -qiE 'not attached|no device'; then
		echo "Detached $id"
		return 0
	fi
	# Older usbipd: detach is busid-only.
	bus=$(list_wanted_windows | awk -v id="$id" '$2 == id { print $1; exit }')
	if [ -n "${bus:-}" ]; then
		if usbipd_run detach --busid "$bus" >/dev/null; then
			echo "Detached $id (bus $bus)"
			return 0
		fi
	fi
	echo "$out" >&2
	return 1
}

wait_id() {
	id=$1
	n=0
	while [ "$n" -lt 40 ]; do
		if linux_has_id "$id"; then
			return 0
		fi
		n=$((n + 1))
		sleep 0.25
	done
	return 1
}

bring_up_bt() {
	bt_present=0
	for id in $BT_IDS; do
		linux_has_id "$id" && bt_present=1
	done
	[ "$bt_present" -eq 1 ] || return 0
	if [ -d /sys/class/bluetooth/hci0 ]; then
		return 0
	fi
	echo "Loading btusb for Intel host Bluetooth..."
	if ! modprobe btusb 2>/dev/null; then
		if command -v sudo >/dev/null 2>&1; then
			sudo modprobe btusb 2>/dev/null || true
		fi
	fi
	n=0
	while [ "$n" -lt 20 ]; do
		[ -d /sys/class/bluetooth/hci0 ] && return 0
		n=$((n + 1))
		sleep 0.25
	done
	echo "  8087:0033 is on USB but no hci0 yet (sudo modprobe btusb; apt install bluez)." >&2
}

fix_permissions() {
	for id in $HACKRF_IDS $JLINK_IDS; do
		linux_has_id "$id" || continue
		chmod_id "$id" || true
	done
	if [ "$WANT_JLINK" -eq 1 ]; then
		chmod_acm
	fi
	if [ "$WANT_BT" -eq 1 ]; then
		bring_up_bt
	fi
}

bind_unshared() {
	while read -r _bus id st; do
		[ -n "${id:-}" ] || continue
		case "$st" in
		Shared | Attached) continue ;;
		esac
		bind_id "$id" || true
	done <<EOF
$(list_wanted_windows)
EOF
}

warn_groups() {
	groups=$(id -nG 2>/dev/null || true)
	case " $groups " in
	*" plugdev "*) ;;
	*)
		echo "Add this user to plugdev (then log out/in):  sudo usermod -aG plugdev $USER" >&2
		;;
	esac
	if [ "$WANT_JLINK" -eq 1 ]; then
		case " $groups " in
		*" dialout "*) ;;
		*)
			echo "Add this user to dialout (ACM):  sudo usermod -aG dialout $USER" >&2
			;;
		esac
	fi
}

radios_ok() {
	ok=0
	if [ "$WANT_HACKRF" -eq 1 ]; then
		hack=0
		for id in $HACKRF_IDS; do
			linux_has_id "$id" && hack=1
		done
		if [ "$hack" -eq 0 ]; then
			echo "HackRF not visible in this Ubuntu (expect 1d50:6089)." >&2
			ok=1
		fi
	fi
	if [ "$WANT_JLINK" -eq 1 ]; then
		jl=0
		for id in $JLINK_IDS; do
			linux_has_id "$id" && jl=1
		done
		if [ "$jl" -eq 0 ]; then
			echo "nRF J-Link not visible in this Ubuntu (expect 1366:1015)." >&2
			ok=1
		fi
	fi
	if [ "$WANT_BT" -eq 1 ]; then
		bt=0
		for id in $BT_IDS; do
			linux_has_id "$id" && bt=1
		done
		if [ "$bt" -eq 0 ]; then
			echo "Host Bluetooth not visible in this Ubuntu (expect 8087:0033)." >&2
			ok=1
		fi
	fi
	return "$ok"
}

# --- native Linux (not WSL): just report / chmod ---
if ! is_wsl; then
	echo "Not WSL — USB is native to this kernel (no usbipd attach)."
	echo
	print_linux
	fix_permissions
	warn_groups
	if ! radios_ok; then
		echo
		echo "Plug the radios in, then: make udev && make info"
		exit 1
	fi
	echo
	echo "Ready. make info   /   make start"
	exit 0
fi

if ! is_wsl2; then
	echo "USB attach needs WSL2 (WSL1 has no usbip). Convert this distro:" >&2
	echo "  wsl --set-version ${WSL_DISTRO_NAME:-Ubuntu} 2" >&2
	exit 1
fi

USBIPD=$(find_usbipd || true)
if [ -z "${USBIPD:-}" ]; then
	echo "usbipd-win is not installed on Windows (or not on PATH)." >&2
	echo "From an elevated Windows prompt:" >&2
	echo "  winget install usbipd" >&2
	echo "  https://github.com/dorssel/usbipd-win/releases" >&2
	exit 1
fi

echo "usbipd: $USBIPD"
usbipd_run --version 2>/dev/null | head -n1 || true
echo

if [ "$MODE" = list ]; then
	usbipd_run list || true
	echo
	print_linux
	exit 0
fi

echo "Windows usbipd"
echo "--------------"
usbipd_run list || true
echo

if [ "$MODE" = detach ]; then
	rc=0
	seen=
	for id in $(present_wanted_ids); do
		case " $seen " in
		*" $id "*) continue ;;
		esac
		seen="$seen $id"
		detach_one "$id" || rc=1
	done
	if [ -z "${seen:-}" ]; then
		# Try the well-known IDs anyway (already on Linux, or list parse missed).
		for id in $(wanted_ids); do
			detach_one "$id" || true
		done
	fi
	echo
	usbipd_run list || true
	exit "$rc"
fi

# Collect IDs that Windows currently sees, preferring 1015 over 0101.
attach_ids=
have_jlink_acm=0
while read -r _bus id _st; do
	[ -n "${id:-}" ] || continue
	case "$id" in
	1d50:*)
		[ "$WANT_HACKRF" -eq 1 ] || continue
		;;
	1366:1015)
		[ "$WANT_JLINK" -eq 1 ] || continue
		have_jlink_acm=1
		;;
	1366:0101)
		[ "$WANT_JLINK" -eq 1 ] || continue
		;;
	8087:*)
		[ "$WANT_BT" -eq 1 ] || continue
		;;
	*)
		continue
		;;
	esac
	case " $attach_ids " in
	*" $id "*) continue ;;
	esac
	attach_ids="$attach_ids $id"
done <<EOF
$(list_wanted_windows)
EOF

if [ "$have_jlink_acm" -eq 1 ]; then
	trimmed=
	for id in $attach_ids; do
		if [ "$id" = "1366:0101" ]; then
			continue
		fi
		trimmed="$trimmed $id"
	done
	attach_ids=$trimmed
fi

if [ -z "${attach_ids:-}" ]; then
	echo "No matching row in usbipd list; trying well-known IDs anyway."
	if [ "$WANT_HACKRF" -eq 1 ]; then
		attach_ids="$attach_ids 1d50:6089"
	fi
	if [ "$WANT_JLINK" -eq 1 ]; then
		attach_ids="$attach_ids 1366:1015"
	fi
	if [ "$WANT_BT" -eq 1 ]; then
		attach_ids="$attach_ids 8087:0033"
	fi
fi

bind_unshared

if [ "$MODE" = watch ]; then
	echo "Auto-attach (Ctrl+C stops the loop; radios return to Windows):"
	pids=
	for id in $attach_ids; do
		args=(attach --wsl)
		if [ -n "${WSL_DISTRO_NAME:-}" ]; then
			args+=("$WSL_DISTRO_NAME")
		fi
		args+=(--auto-attach --hardware-id "$id")
		"$USBIPD" "${args[@]}" &
		pids="$pids $!"
		echo "  $id  pid $!"
	done
	# shellcheck disable=SC2086
	trap 'echo; echo "stopping auto-attach"; kill $pids 2>/dev/null || true; wait 2>/dev/null || true' INT TERM EXIT
	sleep 1
	for id in $attach_ids; do
		wait_id "$id" || echo "still waiting for $id on Linux..." >&2
	done
	fix_permissions
	echo
	print_linux
	warn_groups
	echo "Watching. Leave this running so a HackRF reset / unplug re-attaches."
	# shellcheck disable=SC2086
	wait $pids
	trap - INT TERM EXIT
	exit 0
fi

failed=0
for id in $attach_ids; do
	set +e
	attach_one "$id" 0
	rc=$?
	set -e
	if [ "$rc" -ne 0 ]; then
		failed=1
	fi
done

for id in $attach_ids; do
	if ! wait_id "$id"; then
		echo "Timed out waiting for $id in this Ubuntu." >&2
		failed=1
	fi
done

echo
fix_permissions
echo
print_linux
warn_groups

if ! radios_ok; then
	echo
	echo "If bind was never done, Administrator PowerShell:" >&2
	echo "  usbipd bind --hardware-id 1d50:6089" >&2
	echo "  usbipd bind --hardware-id 1366:1015" >&2
	echo "  usbipd bind --hardware-id 8087:0033" >&2
	exit 1
fi

if [ "$failed" -ne 0 ]; then
	exit 1
fi

echo
echo "Ready. make info   /   make start"
if [ ! -f /etc/udev/rules.d/53-hackrf.rules ]; then
	echo "Tip: make udev   (once) so the next attach stays writable without chmod."
fi
exit 0
