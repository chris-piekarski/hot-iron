/*
 * List HackRF devices via libhackrf (firmware, board, serial).
 * Built and invoked by scripts/hackrf-info.sh / `make info`.
 */

#include <hackrf.h>

#include <stdio.h>
#include <stdlib.h>

static void print_board_rev(uint8_t board_rev)
{
	if (board_rev == BOARD_REV_UNDETECTED) {
		printf("    hardware revision: (not yet detected by firmware)\n");
		return;
	}
	if (board_rev == BOARD_REV_UNRECOGNIZED) {
		printf("    hardware revision: (unrecognized by firmware)\n");
		return;
	}
	printf("    hardware revision: %s\n", hackrf_board_rev_name(board_rev));
	if (board_rev > BOARD_REV_HACKRF1_OLD) {
		if (board_rev & HACKRF_BOARD_REV_GSG)
			printf("    manufacturer: Great Scott Gadgets\n");
		else
			printf("    manufacturer: (not GSG)\n");
	}
}

int main(void)
{
	int result;
	hackrf_device_list_t* list;
	int i;

	result = hackrf_init();
	if (result != HACKRF_SUCCESS) {
		fprintf(stderr, "hackrf_init() failed: %s (%d)\n", hackrf_error_name(result), result);
		return 0;
	}

	printf("libhackrf: %s (%s)\n", hackrf_library_release(), hackrf_library_version());

	list = hackrf_device_list();
	if (list == NULL || list->devicecount < 1) {
		printf("No HackRF boards found by libhackrf.\n");
		if (list != NULL)
			hackrf_device_list_free(list);
		hackrf_exit();
		return 0;
	}

	printf("libhackrf devices: %d\n", list->devicecount);

	for (i = 0; i < list->devicecount; i++) {
		hackrf_device* device = NULL;
		uint8_t board_id = BOARD_ID_UNDETECTED;
		uint8_t board_rev = BOARD_REV_UNDETECTED;
		char version[256];
		uint16_t usb_version = 0;
		read_partid_serialno_t partid;

		printf("\n  [%d]\n", i);
		if (list->usb_board_ids != NULL)
			printf("    usb board: %s\n", hackrf_usb_board_id_name(list->usb_board_ids[i]));
		if (list->serial_numbers != NULL && list->serial_numbers[i] != NULL)
			printf("    serial: %s\n", list->serial_numbers[i]);

		result = hackrf_device_list_open(list, i, &device);
		if (result != HACKRF_SUCCESS) {
			printf("    open failed: %s (%d)\n", hackrf_error_name(result), result);
			if (result == HACKRF_ERROR_LIBUSB)
				printf("    hint: usbfs needs write permission — see the USB section above\n");
			continue;
		}

		result = hackrf_board_id_read(device, &board_id);
		if (result == HACKRF_SUCCESS)
			printf("    board: %s (id %u)\n", hackrf_board_id_name(board_id), (unsigned) board_id);
		else
			printf("    board id: (read failed: %s)\n", hackrf_error_name(result));

		result = hackrf_version_string_read(device, version, 255);
		if (result != HACKRF_SUCCESS) {
			printf("    firmware: (read failed: %s)\n", hackrf_error_name(result));
		} else {
			result = hackrf_usb_api_version_read(device, &usb_version);
			if (result == HACKRF_SUCCESS)
				printf("    firmware: %s (API %u.%02u)\n",
					version,
					(unsigned) ((usb_version >> 8) & 0xFF),
					(unsigned) (usb_version & 0xFF));
			else
				printf("    firmware: %s\n", version);
		}

		result = hackrf_board_partid_serialno_read(device, &partid);
		if (result == HACKRF_SUCCESS) {
			printf("    part id: 0x%08x 0x%08x\n", partid.part_id[0], partid.part_id[1]);
			printf("    mcu serial: 0x%08x 0x%08x 0x%08x 0x%08x\n",
				partid.serial_no[0],
				partid.serial_no[1],
				partid.serial_no[2],
				partid.serial_no[3]);
		}

		if (usb_version >= 0x0106 && (board_id == 2 || board_id == 4)) {
			result = hackrf_board_rev_read(device, &board_rev);
			if (result == HACKRF_SUCCESS)
				print_board_rev(board_rev);
		}

		hackrf_close(device);
	}

	hackrf_device_list_free(list);
	hackrf_exit();
	return 0;
}
