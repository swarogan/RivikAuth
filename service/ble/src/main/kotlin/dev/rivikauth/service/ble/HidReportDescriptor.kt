package dev.rivikauth.service.ble

/**
 * FIDO HID Report Descriptor for Bluetooth HID Device profile.
 * See: FIDO CTAP §11.2
 */
object HidReportDescriptor {

    /**
     * The standard FIDO HID report descriptor.
     * Defines a 64-byte input and 64-byte output report.
     */
    val DESCRIPTOR: ByteArray = byteArrayOf(
        0x06.toByte(), 0xD0.toByte(), 0xF1.toByte(), // Usage Page (FIDO Alliance)
        0x09.toByte(), 0x01.toByte(),                 // Usage (U2F HID Authenticator Device)
        0xA1.toByte(), 0x01.toByte(),                 // Collection (Application)
        // Input Report
        0x09.toByte(), 0x20.toByte(),                 //   Usage (Input Report Data)
        0x15.toByte(), 0x00.toByte(),                 //   Logical Minimum (0)
        0x26.toByte(), 0xFF.toByte(), 0x00.toByte(),  //   Logical Maximum (255)
        0x75.toByte(), 0x08.toByte(),                 //   Report Size (8)
        0x95.toByte(), 0x40.toByte(),                 //   Report Count (64)
        0x81.toByte(), 0x02.toByte(),                 //   Input (Data, Variable, Absolute)
        // Output Report
        0x09.toByte(), 0x21.toByte(),                 //   Usage (Output Report Data)
        0x15.toByte(), 0x00.toByte(),                 //   Logical Minimum (0)
        0x26.toByte(), 0xFF.toByte(), 0x00.toByte(),  //   Logical Maximum (255)
        0x75.toByte(), 0x08.toByte(),                 //   Report Size (8)
        0x95.toByte(), 0x40.toByte(),                 //   Report Count (64)
        0x91.toByte(), 0x02.toByte(),                 //   Output (Data, Variable, Absolute)
        0xC0.toByte(),                                 // End Collection
    )
}
