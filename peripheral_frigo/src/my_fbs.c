/*
 * Copyright (c) 2018 Nordic Semiconductor ASA
 *
 * SPDX-License-Identifier: LicenseRef-Nordic-5-Clause
 */

/** @file
 *  @brief LED Button Service (LBS) sample
 */

#include <zephyr/types.h>
#include <stddef.h>
#include <string.h>
#include <errno.h>
#include <zephyr/sys/printk.h>
#include <zephyr/sys/byteorder.h>
#include <zephyr/kernel.h>
#include <zephyr/logging/log.h>
#include <zephyr/bluetooth/bluetooth.h>
#include <zephyr/bluetooth/hci.h>
#include <zephyr/bluetooth/conn.h>
#include <zephyr/bluetooth/uuid.h>
#include <zephyr/bluetooth/gatt.h>

#include "my_fbs.h"

LOG_MODULE_DECLARE(Fan_Frigo_Camper, 1);

static bool notify_temperature_enabled;
static bool notify_dutycycle_enabled;

static int32_t temperature_value;
static int32_t dutyCycle_value;
static int32_t threshold_value;
static uint8_t fan2_state;
static bool power_state;
static bool leds_state;
static uint32_t pswVal;


static struct my_fbs_cb fbs_cb;


/*  Define the configuration change callback function for the TEMPERATURE characteristic */
static void myfbsbc_ccc_temperature_cfg_changed(const struct bt_gatt_attr *attr, uint16_t value)
{
	notify_temperature_enabled = (value == BT_GATT_CCC_NOTIFY);
}
/* Define the configuration change callback function for the DUTYCYCLE characteristic */
static void myfbsbc_ccc_dutycycle_cfg_changed(const struct bt_gatt_attr *attr, uint16_t value)
{
	notify_dutycycle_enabled = (value == BT_GATT_CCC_NOTIFY);
}


static ssize_t readTemperature(struct bt_conn *conn, const struct bt_gatt_attr *attr, void *buf,
			   uint16_t len, uint16_t offset)
{
	// get a pointer to temperature_value which is passed in the BT_GATT_CHARACTERISTIC() and stored in attr->user_data
	const int32_t *value = attr->user_data;

	LOG_DBG("Attribute read, handle: %u, conn: %p\n", attr->handle, (void *)conn);

	if (fbs_cb.temperature_cb) {
		// Call the application callback function to update the get the current value of the threshold
		temperature_value = fbs_cb.temperature_cb();
		return bt_gatt_attr_read(conn, attr, buf, len, offset, value, sizeof(*value));
	}

	return 0;
}


static ssize_t setDutyCycle(struct bt_conn *conn, const struct bt_gatt_attr *attr, const void *buf,
			 uint16_t len, uint16_t offset, uint8_t flags)
{
	LOG_DBG("Attribute write, handle: %u, conn: %p\n", attr->handle, (void *)conn);

	if (len != 4U) {
		LOG_DBG("setDutyCycle: Incorrect data length\n");
		return BT_GATT_ERR(BT_ATT_ERR_INVALID_ATTRIBUTE_LEN);
	}

	if (offset != 0) {
		LOG_DBG("setDutyCycle: Incorrect data offset\n");
		return BT_GATT_ERR(BT_ATT_ERR_INVALID_OFFSET);
	}

	if (fbs_cb.setDuty_cb) {
		// Read the received value
		uint32_t val = *((uint32_t *)buf);
		fbs_cb.setDuty_cb(val);
	}

	return len;
}
static ssize_t getDutyCycle(struct bt_conn *conn, const struct bt_gatt_attr *attr, void *buf,
			   uint16_t len, uint16_t offset)
{
	// get a pointer to button_state which is passed in the BT_GATT_CHARACTERISTIC() and stored in attr->user_data
	const int32_t *value = attr->user_data;

	LOG_DBG("Attribute read, handle: %u, conn: %p\n", attr->handle, (void *)conn);

	if (fbs_cb.getDuty_cb) {
		// Call the application callback function to update the get the current value of the button
		dutyCycle_value = fbs_cb.getDuty_cb();
		return bt_gatt_attr_read(conn, attr, buf, len, offset, value, sizeof(*value));
	}

	return 0;
}

static ssize_t setThreshold(struct bt_conn *conn, const struct bt_gatt_attr *attr, const void *buf,
			 uint16_t len, uint16_t offset, uint8_t flags)
{
	LOG_DBG("Attribute write, handle: %u, conn: %p\n", attr->handle, (void *)conn);

	if (len != 4U) {
		LOG_DBG("setTemperature: Incorrect data length\n");
		return BT_GATT_ERR(BT_ATT_ERR_INVALID_ATTRIBUTE_LEN);
	}

	if (offset != 0) {
		LOG_DBG("setTemperature: Incorrect data offset\n");
		return BT_GATT_ERR(BT_ATT_ERR_INVALID_OFFSET);
	}

	if (fbs_cb.setThreshold_cb) {
		// Read the received value
		uint32_t val = *((uint32_t *)buf);
		fbs_cb.setThreshold_cb(val);
	}

	return len;
}

static ssize_t readThreshold(struct bt_conn *conn, const struct bt_gatt_attr *attr, void *buf,
			   uint16_t len, uint16_t offset)
{
	// get a pointer to threshold_value which is passed in the BT_GATT_CHARACTERISTIC() and stored in attr->user_data
	const int32_t *value = attr->user_data;

	LOG_DBG("Attribute read, handle: %u, conn: %p\n", attr->handle, (void *)conn);

	if (fbs_cb.getThreshold_cb) {
		// Call the application callback function to update the get the current value of the threshold
		threshold_value = fbs_cb.getThreshold_cb();
		return bt_gatt_attr_read(conn, attr, buf, len, offset, value, sizeof(*value));
	}

	return 0;
}


static ssize_t setFan2(struct bt_conn *conn, const struct bt_gatt_attr *attr, const void *buf,
			 uint16_t len, uint16_t offset, uint8_t flags)
{
	LOG_DBG("Attribute write, handle: %u, conn: %p\n", attr->handle, (void *)conn);

	if (len != 1U) {
		LOG_DBG("setFan2: Incorrect data length\n");
		return BT_GATT_ERR(BT_ATT_ERR_INVALID_ATTRIBUTE_LEN);
	}

	if (offset != 0) {
		LOG_DBG("setFan2: Incorrect data offset\n");
		return BT_GATT_ERR(BT_ATT_ERR_INVALID_OFFSET);
	}

	if (fbs_cb.setFan2_cb) {
		// Read the received value
		uint8_t val = *((uint8_t *)buf);

		if (val == 0x00 || val == 0x01 || val == 0x80 || val == 0x81) {
			// Call the application callback function to update the LED state
			fbs_cb.setFan2_cb(val);
		} else {
			LOG_DBG("Write Fan2: Incorrect value");
			return BT_GATT_ERR(BT_ATT_ERR_VALUE_NOT_ALLOWED);
		}
	}

	return len;
}


static ssize_t readFan2(struct bt_conn *conn, const struct bt_gatt_attr *attr, void *buf,
			   uint16_t len, uint16_t offset)
{
	// get a pointer to button_state which is passed in the BT_GATT_CHARACTERISTIC() and stored in attr->user_data
	const uint8_t *value = attr->user_data;

	LOG_DBG("Attribute read, handle: %u, conn: %p\n", attr->handle, (void *)conn);

	if (fbs_cb.getFan2_cb) {
		// Call the application callback function to update the get the current value of the button
		fan2_state = fbs_cb.getFan2_cb();
		return bt_gatt_attr_read(conn, attr, buf, len, offset, value, sizeof(*value));
	}

	return 0;
}


static ssize_t setPower(struct bt_conn *conn, const struct bt_gatt_attr *attr, const void *buf,
			 uint16_t len, uint16_t offset, uint8_t flags)
{
	LOG_DBG("Attribute write, handle: %u, conn: %p\n", attr->handle, (void *)conn);

	if (len != 1U) {
		LOG_DBG("setPower: Incorrect data length\n");
		return BT_GATT_ERR(BT_ATT_ERR_INVALID_ATTRIBUTE_LEN);
	}

	if (offset != 0) {
		LOG_DBG("setPower: Incorrect data offset\n");
		return BT_GATT_ERR(BT_ATT_ERR_INVALID_OFFSET);
	}

	if (fbs_cb.setPowerOn_cb) {
		// Read the received value
		uint8_t val = *((uint8_t *)buf);

		if (val == 0x00 || val == 0x01) {
			// Call the application callback function to update the LED state
			fbs_cb.setPowerOn_cb(val);
		} else {
			LOG_DBG("Write PowerOn: Incorrect value");
			return BT_GATT_ERR(BT_ATT_ERR_VALUE_NOT_ALLOWED);
		}
	}

	return len;
}

static ssize_t readPower(struct bt_conn *conn, const struct bt_gatt_attr *attr, void *buf,
			   uint16_t len, uint16_t offset)
{
	// get a pointer to button_state which is passed in the BT_GATT_CHARACTERISTIC() and stored in attr->user_data
	const char *value = attr->user_data;

	LOG_DBG("Attribute read, handle: %u, conn: %p\n", attr->handle, (void *)conn);

	if (fbs_cb.getPowerOn_cb) {
		// Call the application callback function to update the get the current value of the button
		power_state = fbs_cb.getPowerOn_cb();
		return bt_gatt_attr_read(conn, attr, buf, len, offset, value, sizeof(*value));
	}

	return 0;
}


static ssize_t setLeds(struct bt_conn *conn, const struct bt_gatt_attr *attr, const void *buf,
			 uint16_t len, uint16_t offset, uint8_t flags)
{
	LOG_DBG("Attribute write, handle: %u, conn: %p\n", attr->handle, (void *)conn);

	if (len != 1U) {
		LOG_DBG("setLeds: Incorrect data length\n");
		return BT_GATT_ERR(BT_ATT_ERR_INVALID_ATTRIBUTE_LEN);
	}

	if (offset != 0) {
		LOG_DBG("setLeds: Incorrect data offset\n");
		return BT_GATT_ERR(BT_ATT_ERR_INVALID_OFFSET);
	}

	if (fbs_cb.setLedsOn_cb) {
		// Read the received value
		uint8_t val = *((uint8_t *)buf);

		if (val == 0x00 || val == 0x01) {
			// Call the application callback function to update the LED state
			fbs_cb.setLedsOn_cb(val);
		} else {
			LOG_DBG("Write LedsOn: Incorrect value");
			return BT_GATT_ERR(BT_ATT_ERR_VALUE_NOT_ALLOWED);
		}
	}

	return len;
}

static ssize_t readLeds(struct bt_conn *conn, const struct bt_gatt_attr *attr, void *buf,
			   uint16_t len, uint16_t offset)
{
	// get a pointer to button_state which is passed in the BT_GATT_CHARACTERISTIC() and stored in attr->user_data
	const char *value = attr->user_data;

	LOG_DBG("Attribute read, handle: %u, conn: %p\n", attr->handle, (void *)conn);

	if (fbs_cb.getLedsOn_cb) {
		// Call the application callback function to update the get the current value of the button
		leds_state = fbs_cb.getLedsOn_cb();
		return bt_gatt_attr_read(conn, attr, buf, len, offset, value, sizeof(*value));
	}

	return 0;
}



static ssize_t setPsw(struct bt_conn *conn, const struct bt_gatt_attr *attr, const void *buf,
			 uint16_t len, uint16_t offset, uint8_t flags)
{
	LOG_DBG("Attribute write, handle: %u, conn: %p\n", attr->handle, (void *)conn);

	if (len != 4U) {
		LOG_DBG("setPsw: Incorrect data length\n");
		return BT_GATT_ERR(BT_ATT_ERR_INVALID_ATTRIBUTE_LEN);
	}

	if (offset != 0) {
		LOG_DBG("setPsw: Incorrect data offset\n");
		return BT_GATT_ERR(BT_ATT_ERR_INVALID_OFFSET);
	}

	if (fbs_cb.setPsw_cb) {
		// Read the received value
		uint32_t val = *((uint32_t *)buf);
		fbs_cb.setPsw_cb(val);
	}

	return len;
}

/* LED Button Service Declaration */
BT_GATT_SERVICE_DEFINE(
	my_fbs_svc, BT_GATT_PRIMARY_SERVICE(BT_UUID_FBS), //attr[0]

	// /* Create and add the TEMPERATURE characteristic and its CCCD  */ 
	// BT_GATT_CHARACTERISTIC(BT_UUID_FBS_TEMPERATURE, BT_GATT_CHRC_NOTIFY | BT_GATT_CHRC_READ, BT_GATT_PERM_READ_AUTHEN, NULL, NULL, NULL), // 2 attr[6], att[7] (uuid,r,w,userdata)
    // BT_GATT_CUD("Temperature",BT_GATT_PERM_READ_AUTHEN | BT_GATT_PERM_WRITE_AUTHEN), // attr[8]
	// BT_GATT_CCC(myfbsbc_ccc_temperature_cfg_changed, BT_GATT_PERM_READ | BT_GATT_PERM_WRITE), // attr[9]
	
	// /* Create and add the DUTYCYCLE characteristic and its CCCD  */
	// BT_GATT_CHARACTERISTIC(BT_UUID_FBS_DUTYCYCLE, BT_GATT_CHRC_WRITE | BT_GATT_CHRC_READ | BT_GATT_CHRC_NOTIFY, 
	// 		BT_GATT_PERM_WRITE_AUTHEN | BT_GATT_PERM_READ_AUTHEN, getDutyCycle, setDutyCycle, &dutyCycle_value),// attr[10], attr[11]
    // BT_GATT_CUD("DutyCycle",BT_GATT_PERM_READ_AUTHEN | BT_GATT_PERM_WRITE_AUTHEN),
	// BT_GATT_CCC(myfbsbc_ccc_dutycycle_cfg_changed, BT_GATT_PERM_READ | BT_GATT_PERM_WRITE),

	// 	/* Create and add the Threshold characteristic and its CCCD  */
	// BT_GATT_CHARACTERISTIC(BT_UUID_FBS_TEMP_THR, BT_GATT_CHRC_WRITE | BT_GATT_CHRC_READ, 
	// 		BT_GATT_PERM_WRITE_AUTHEN | BT_GATT_PERM_READ_AUTHEN, readThreshold, setThreshold, &threshold_value),
    // BT_GATT_CUD("Threshold",BT_GATT_PERM_READ_AUTHEN | BT_GATT_PERM_WRITE_AUTHEN),
	
	// /* Create and add the fan2 characteristic and its CCCD  */
	// BT_GATT_CHARACTERISTIC(BT_UUID_FBS_ENABLE_FAN2, BT_GATT_CHRC_WRITE | BT_GATT_CHRC_READ, 
	// 		BT_GATT_PERM_WRITE_AUTHEN | BT_GATT_PERM_READ_AUTHEN, readFan2, setFan2, &fan2_state),
    // BT_GATT_CUD("Fan2 state",BT_GATT_PERM_READ_AUTHEN | BT_GATT_PERM_WRITE_AUTHEN),

	// /* Create and add the poweron characteristic and its CCCD  */
	// BT_GATT_CHARACTERISTIC(BT_UUID_FBS_POWER_ON, BT_GATT_CHRC_WRITE | BT_GATT_CHRC_READ, 
	// 		BT_GATT_PERM_WRITE_AUTHEN | BT_GATT_PERM_READ_AUTHEN, readPower, setPower, &power_state),
    // BT_GATT_CUD("Power state",BT_GATT_PERM_READ_AUTHEN | BT_GATT_PERM_WRITE_AUTHEN),
	
	/* Create and add the TEMPERATURE characteristic and its CCCD  */ 
	BT_GATT_CHARACTERISTIC(BT_UUID_FBS_TEMPERATURE, BT_GATT_CHRC_NOTIFY | BT_GATT_CHRC_READ, 
			BT_GATT_PERM_READ_AUTHEN , readTemperature, NULL, &temperature_value), // 2 attr[1], att[2] (uuid,r,w,userdata)
	BT_GATT_CUD("Temperature",BT_GATT_PERM_READ_AUTHEN ), // attr[3]
	BT_GATT_CCC(myfbsbc_ccc_temperature_cfg_changed, BT_GATT_PERM_READ | BT_GATT_PERM_WRITE),// attr[4]
	
	/* Create and add the DUTYCYCLE characteristic and its CCCD  */
	BT_GATT_CHARACTERISTIC(BT_UUID_FBS_DUTYCYCLE, BT_GATT_CHRC_WRITE | BT_GATT_CHRC_READ | BT_GATT_CHRC_NOTIFY, 
			BT_GATT_PERM_WRITE_AUTHEN  | BT_GATT_PERM_READ_AUTHEN , getDutyCycle, setDutyCycle, &dutyCycle_value),// attr[5], attr[6]
    BT_GATT_CUD("DutyCycle",BT_GATT_PERM_READ_AUTHEN ), // attr[7]
	BT_GATT_CCC(myfbsbc_ccc_dutycycle_cfg_changed, BT_GATT_PERM_READ | BT_GATT_PERM_WRITE),// attr[8]

		/* Create and add the Threshold characteristic and its CCCD  */
	BT_GATT_CHARACTERISTIC(BT_UUID_FBS_TEMP_THR, BT_GATT_CHRC_WRITE | BT_GATT_CHRC_READ, 
			BT_GATT_PERM_WRITE_AUTHEN  | BT_GATT_PERM_READ_AUTHEN , readThreshold, setThreshold, &threshold_value),// attr[9], attr[10]
    BT_GATT_CUD("Threshold",BT_GATT_PERM_READ_AUTHEN ),// attr[11]
	
	/* Create and add the fan2 characteristic and its CCCD  */
	BT_GATT_CHARACTERISTIC(BT_UUID_FBS_ENABLE_FAN2, BT_GATT_CHRC_WRITE | BT_GATT_CHRC_READ, 
			BT_GATT_PERM_WRITE_AUTHEN  | BT_GATT_PERM_READ_AUTHEN , readFan2, setFan2, &fan2_state),// attr[12], attr[12]
    BT_GATT_CUD("Fan2 state",BT_GATT_PERM_READ_AUTHEN ),// attr[13]

	/* Create and add the poweron characteristic and its CCCD  */
	BT_GATT_CHARACTERISTIC(BT_UUID_FBS_POWER_ON, BT_GATT_CHRC_WRITE | BT_GATT_CHRC_READ, 
			BT_GATT_PERM_WRITE_AUTHEN  | BT_GATT_PERM_READ_AUTHEN , readPower, setPower, &power_state),// attr[14], attr[15]
    BT_GATT_CUD("Power state",BT_GATT_PERM_READ_AUTHEN ),// attr[16]

		/* Create and add the poweron characteristic and its CCCD  */
	BT_GATT_CHARACTERISTIC(BT_UUID_FBS_LEDS_ON, BT_GATT_CHRC_WRITE | BT_GATT_CHRC_READ, 
			BT_GATT_PERM_WRITE_AUTHEN  | BT_GATT_PERM_READ_AUTHEN , readLeds, setLeds, &leds_state),// attr[17], attr[18]
    BT_GATT_CUD("LEDs state",BT_GATT_PERM_READ_AUTHEN ),// attr[19]

			/* Create and add the poweron characteristic and its CCCD  */
	BT_GATT_CHARACTERISTIC(BT_UUID_FBS_PSW_SET, BT_GATT_CHRC_WRITE, 
			BT_GATT_PERM_WRITE_AUTHEN , NULL, setPsw, &pswVal),// attr[20], attr[21]
    BT_GATT_CUD("Internal data state",BT_GATT_PERM_READ_AUTHEN ),// attr[22]
);

/* A function to register application callbacks for the LED and Button characteristics  */
int my_fbs_init(struct my_fbs_cb *callbacks)
{
	if (callbacks) {
		fbs_cb.getDuty_cb = callbacks->getDuty_cb;
		fbs_cb.getFan2_cb = callbacks->getFan2_cb;
		fbs_cb.getPowerOn_cb = callbacks->getPowerOn_cb;
		fbs_cb.getThreshold_cb = callbacks->getThreshold_cb;
		fbs_cb.setDuty_cb = callbacks->setDuty_cb;
		fbs_cb.setFan2_cb = callbacks->setFan2_cb;
		fbs_cb.setPowerOn_cb = callbacks->setPowerOn_cb;
		fbs_cb.setThreshold_cb = callbacks->setThreshold_cb;
		fbs_cb.temperature_cb = callbacks->temperature_cb;

		fbs_cb.getLedsOn_cb = callbacks->getLedsOn_cb;
		fbs_cb.setLedsOn_cb = callbacks->setLedsOn_cb;
		fbs_cb.setPsw_cb = callbacks->setPsw_cb;
	}

	return 0;
}

/* Define the function to send notifications for the TEMPERATURE characteristic */
int my_fbs_send_temperature_notify(uint32_t temperature_valueArg)
{
	if (!notify_temperature_enabled) {
		return -EACCES;
	}

	return bt_gatt_notify(NULL, &my_fbs_svc.attrs[2], &temperature_valueArg, sizeof(temperature_valueArg));
}

/* Define the function to send notifications for the TEMPERATURE characteristic */
int my_fbs_send_dutycycle_notify(uint32_t dutycycle_valueArg)
{
	if (!notify_dutycycle_enabled) {
		return -EACCES;
	}

	return bt_gatt_notify(NULL, &my_fbs_svc.attrs[6], &dutycycle_valueArg, sizeof(dutycycle_valueArg));
}