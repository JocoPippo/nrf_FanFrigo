/*
 * Copyright (c) 2018 Nordic Semiconductor ASA
 *
 * SPDX-License-Identifier: LicenseRef-Nordic-5-Clause
 */

#ifndef BT_LBS_H_
#define BT_LBS_H_

/**@file
 * @defgroup bt_lbs LED Button Service API
 * @{
 * @brief API for the LED Button Service (LBS).
 */

#ifdef __cplusplus
extern "C" {
#endif

#include <zephyr/types.h>
#undef __CDT_PARSER__
#undef __JETBRAINS_IDE__
/** @brief FBS Service UUID. */

#define BT_UUID_FBS_VAL BT_UUID_128_ENCODE(0x9b046cc7, 0xdec2, 0x438f, 0xbde5, 0xb093f4b5511c)

/* Assign a UUID to the TEMPERATURE characteristic */
/** @brief TEMPERATURE Characteristic UUID. */
#define BT_UUID_FBS_TEMPERATURE_VAL                                                                   \
	BT_UUID_128_ENCODE(0x9b046cc8, 0xdec2, 0x438f, 0xbde5, 0xb093f4b5511c)

/* Assign a UUID to the DUTYCYCLE characteristic */
/** @brief DUTYCYCLE Characteristic UUID. */
#define BT_UUID_FBS_DUTYCYCLE_VAL                                                                   \
	BT_UUID_128_ENCODE(0x9b046cc9, 0xdec2, 0x438f, 0xbde5, 0xb093f4b5511c)

/** @brief EnableFan2 Characteristic UUID. */
#define BT_UUID_FBS_ENABLE_FAN2_VAL                                                                   \
	BT_UUID_128_ENCODE(0x9b046cca, 0xdec2, 0x438f, 0xbde5, 0xb093f4b5511c)

/** @brief Set Temperature Threshold Characteristic UUID. */
#define BT_UUID_FBS_TEMP_THRESHOLD_VAL                                                                   \
	BT_UUID_128_ENCODE(0x9b046ccb, 0xdec2, 0x438f, 0xbde5, 0xb093f4b5511c)

/** @brief Set PowerOn Characteristic UUID. */
#define BT_UUID_FBS_POWER_ON_VAL                                                                   \
	BT_UUID_128_ENCODE(0x9b046ccc, 0xdec2, 0x438f, 0xbde5, 0xb093f4b5511c)

#define BT_UUID_FBS_LEDS_ON_VAL                                                                   \
	BT_UUID_128_ENCODE(0x9b046ccd, 0xdec2, 0x438f, 0xbde5, 0xb093f4b5511c)

#define BT_UUID_FBS_PSW_SET_VAL                                                                   \
	BT_UUID_128_ENCODE(0x9b046cce, 0xdec2, 0x438f, 0xbde5, 0xb093f4b5511c)

#define BT_UUID_FBS BT_UUID_DECLARE_128(BT_UUID_FBS_VAL)
/* Convert the array to a generic UUID */
#define BT_UUID_FBS_TEMPERATURE BT_UUID_DECLARE_128(BT_UUID_FBS_TEMPERATURE_VAL)
#define BT_UUID_FBS_DUTYCYCLE BT_UUID_DECLARE_128(BT_UUID_FBS_DUTYCYCLE_VAL)
#define BT_UUID_FBS_ENABLE_FAN2 BT_UUID_DECLARE_128(BT_UUID_FBS_ENABLE_FAN2_VAL)
#define BT_UUID_FBS_TEMP_THR BT_UUID_DECLARE_128(BT_UUID_FBS_TEMP_THRESHOLD_VAL)
#define BT_UUID_FBS_POWER_ON BT_UUID_DECLARE_128(BT_UUID_FBS_POWER_ON_VAL)
#define BT_UUID_FBS_LEDS_ON BT_UUID_DECLARE_128(BT_UUID_FBS_LEDS_ON_VAL)
#define BT_UUID_FBS_PSW_SET BT_UUID_DECLARE_128(BT_UUID_FBS_PSW_SET_VAL)
/** @brief Callback type for when an TEMPERATURE read request is received. */
typedef int32_t (*get_Temperature_cb_t)(void);

/** @brief Callback type for when an DUTYCYCLE read request is received. */
typedef uint32_t (*get_DutyCycle_cb_t)(void);

/** @brief Callback type for when an DUTYCYCLE state change is received. */
typedef void (*set_DutyCycle_cb_t)(const uint32_t dutycycle_value);

/** @brief Callback type for when an FAN2 read request is received. */
typedef uint8_t (*get_Fan2_cb_t)(void);

/** @brief Callback type for when an FAN2 state change is received. */
typedef void (*set_Fan2_cb_t)(const uint8_t fan2_state);

/** @brief Callback type for when an TEMPERATURE CHANGE state change is received. */
typedef uint32_t (*get_threshold_cb_t)(void);

/** @brief Callback type for when an TEMPERATURE CHANGE state change is received. */
typedef void (*set_threshold_cb_t)(const uint32_t threshold_value);

/** @brief Callback type for when an POWERON state change is received. */
typedef bool (*get_poweron_cb_t)(void);

/** @brief Callback type for when an POWERON state change is received. */
typedef void (*set_poweron_cb_t)(const bool power_state);

/** @brief Callback type for when an LEDSON state change is received. */
typedef bool (*get_ledson_cb_t)(void);

/** @brief Callback type for when an LEDSON state change is received. */
typedef void (*set_ledson_cb_t)(const bool leds_state);

/** @brief Callback type for when an PSW state change is received. */
typedef void (*set_psw_cb_t)(const uint32_t psw_val);
/** @brief Callback struct used by the LBS Service. */
struct my_fbs_cb {
	get_Temperature_cb_t temperature_cb; 
	get_DutyCycle_cb_t getDuty_cb;
	set_DutyCycle_cb_t setDuty_cb;
	get_Fan2_cb_t getFan2_cb;
	set_Fan2_cb_t setFan2_cb;
	get_threshold_cb_t getThreshold_cb;
	set_threshold_cb_t setThreshold_cb;
	get_poweron_cb_t getPowerOn_cb;
	set_poweron_cb_t setPowerOn_cb;
	get_ledson_cb_t getLedsOn_cb;
	set_ledson_cb_t setLedsOn_cb;
	set_psw_cb_t setPsw_cb;
};

/** @brief Initialize the LBS Service.
 *
 * This function registers application callback functions with the My LBS
 * Service
 *
 * @param[in] callbacks Struct containing pointers to callback functions
 *			used by the service. This pointer can be NULL
 *			if no callback functions are defined.
 *
 *
 * @retval 0 If the operation was successful.
 *           Otherwise, a (negative) error code is returned.
 */
int my_fbs_init(struct my_fbs_cb *callbacks);


/** @brief Send the sensor value as notification.
 *
 * This function sends an uint32_t  value, typically the value
 * of a simulated sensor to all connected peers.
 *
 * @param[in] temperature_value The value of the simulated sensor.
 *
 * @retval 0 If the operation was successful.
 *           Otherwise, a (negative) error code is returned.
 */
int my_fbs_send_temperature_notify(uint32_t temperature_value);

/** @brief Send the sensor value as notification.
 *
 * This function sends an uint32_t  value, typically the value
 * of a simulated sensor to all connected peers.
 *
 * @param[in] dutycycle_value The value of the simulated sensor.
 *
 * @retval 0 If the operation was successful.
 *           Otherwise, a (negative) error code is returned.
 */
int my_fbs_send_dutycycle_notify(uint32_t dutycycle_value);

#ifdef __cplusplus
}
#endif

/**
 * @}
 */

#endif /* BT_LBS_H_ */
