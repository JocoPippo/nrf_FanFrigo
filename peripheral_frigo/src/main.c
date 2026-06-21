/*
 * Copyright (c) 2023 Nordic Semiconductor ASA
 *
 * SPDX-License-Identifier: LicenseRef-Nordic-5-Clause
 */

#include <zephyr/kernel.h>
#include <zephyr/logging/log.h>
#include <zephyr/bluetooth/gap.h>
#include <zephyr/bluetooth/uuid.h>
#include <zephyr/bluetooth/conn.h>
#include <zephyr/posix/netinet/in.h>
#include <zephyr/drivers/gpio.h>
#include <zephyr/drivers/sensor.h>
#include <zephyr/drivers/pwm.h>
#include <zephyr/bluetooth/bluetooth.h>
#include <zephyr/pm/device.h>

#include <zephyr/net/net_ip.h>
#include <zephyr/posix/arpa/inet.h>

#include <zephyr/bluetooth/hci_vs.h>
#include <zephyr/settings/settings.h>

//#include <zephyr/settings/settings.h> // for bonding and flash access
#include "my_fbs.h"

LOG_MODULE_REGISTER(Fan_Frigo_Camper, 1);

#define DEVICE_NAME CONFIG_BT_DEVICE_NAME
#define DEVICE_NAME_LEN (sizeof(DEVICE_NAME) - 1)

#define RUN_STATUS_LED DK_LED1
#define CON_STATUS_LED DK_LED2

#define STACKSIZE 1024
#define PRIORITY 7

#define RUN_LED_BLINK_INTERVAL 2000

#define MAX_PULSE_NS 50000
#define MAX_PULSE_2FAN2 35000
#define MIN_PULSE_WIDTH	7500
//#define MIN_PULSE_WIDTH	250

#define GETMASK(size)  ((1ULL<<(((size)*8)-1))-1)

#define INT2FLOAT_BITS 16
#define HI_INT2FLOAT_MASK  0xFFFF0000U
#define LOW_INT2FLOAT_MASK 0x0000FFFFU

#define MAX_TEMPERATURE 90U

#define STORAGE_SETTINGS_ROOT "up"
#define STORAGE_SETTINGS_KEY  "key"
#define STORAGE_SETTINGS_TEMP  "temp"


struct stateAndValueU32 {
 uint32_t value;
 bool forced;
};

struct stateAndValueBool {
 bool value;
 bool forced;
};

static int fan_settings_set(const char *name, size_t len,
                            settings_read_cb read_cb, void *cb_arg);
static int fan_settings_export(int (*storage_func)(const char *name, const void *value, size_t val_len));
struct settings_handler fan_conf = {
    .name = STORAGE_SETTINGS_ROOT,
    .h_set = fan_settings_set,
    .h_export = fan_settings_export
};

/* Define the data you want to stream over Bluetooth LE */
// stores the used dutycycle 
static uint32_t local_dutycycle_value = 0;
// stores the remote required dutycycle
static struct stateAndValueU32 app_dutycycle = {0,false};
static bool local_fan2State = false;
static struct stateAndValueBool app_fan2State = {false,false};
static bool app_poweron_state = true;
static uint32_t app_temperature_value = 10<<INT2FLOAT_BITS;
static struct stateAndValueU32 app_threshold ={42<<INT2FLOAT_BITS, false};// = 42<<INT2FLOAT_BITS;
static bool app_leds_state = true;
static bool connection_state = false;
static bool pwmOff = false;
static uint32_t dutyPulse;


static const struct gpio_dt_spec fanGpio_spec = GPIO_DT_SPEC_GET(DT_ALIAS(fan2), gpios);
static const struct gpio_dt_spec powerGpio_spec = GPIO_DT_SPEC_GET(DT_ALIAS(poweron), gpios);
static const struct pwm_dt_spec pwm_fan1 = PWM_DT_SPEC_GET(DT_NODELABEL(fanpwm1));
static const struct pwm_dt_spec pwm_fan2 = PWM_DT_SPEC_GET(DT_NODELABEL(fanpwm2));


static const struct gpio_dt_spec led0_spec = GPIO_DT_SPEC_GET(DT_ALIAS(led0), gpios);
static const struct gpio_dt_spec led1_spec = GPIO_DT_SPEC_GET(DT_ALIAS(led1), gpios);
static const struct gpio_dt_spec led2_spec = GPIO_DT_SPEC_GET(DT_NODELABEL(led2), gpios);

static const struct  device *uart_dev = DEVICE_DT_GET(DT_NODELABEL(uart0));

static const struct gpio_dt_spec btn0_spec = GPIO_DT_SPEC_GET(DT_NODELABEL(button0), gpios);
static const struct gpio_dt_spec btn1_spec = GPIO_DT_SPEC_GET(DT_NODELABEL(button1), gpios);

static const struct bt_data ad[] = {
	BT_DATA_BYTES(BT_DATA_FLAGS, (BT_LE_AD_GENERAL | BT_LE_AD_NO_BREDR)),
	BT_DATA(BT_DATA_NAME_COMPLETE, DEVICE_NAME, DEVICE_NAME_LEN),

};

static const struct bt_le_adv_param *adv_param = BT_LE_ADV_PARAM(
	(BT_LE_ADV_OPT_CONN | BT_LE_ADV_OPT_USE_IDENTITY), /* Connectable advertising and use identity address */
	3400, /* Min Advertising Interval 3000ms (4800*0.625ms) */
	4200, /* Max Advertising Interval 3375ms (5400*0.625ms) */
	NULL); /* Set to NULL for undirected advertising */

static struct k_work adv_work;


static const struct bt_data sd[] = {
	BT_DATA_BYTES(BT_DATA_UUID128_ALL, BT_UUID_FBS_VAL),
};

struct k_sem dutycycle_sem;
bool dutyToBeUpdate = false;

// for FAN2 managements
bool fan2ToBeUpdate = false;
static bool fan2ToBeStarted;

const struct device *tempDev;
unsigned int passkey = 123456;

float getFloatTemp4Int(uint32_t T_Arg);
static void app_setLeds_cb(bool ledState);

static void advertising_start(void)
{
	k_work_submit(&adv_work);
}

static void adv_work_handler(struct k_work *work)
{
	int err = bt_le_adv_start(adv_param, ad, ARRAY_SIZE(ad), sd, ARRAY_SIZE(sd));

	if (err) {
		LOG_DBG("Advertising failed to start (err %d)\n", err);
		return;
	}

	LOG_DBG("Advertising successfully started\n");
}

static void display_passkey(struct bt_conn *conn, unsigned int passkey)
{
	char peer_addr[BT_ADDR_LE_STR_LEN];
	bt_addr_le_to_str(bt_conn_get_dst(conn), peer_addr, sizeof(peer_addr));

	LOG_INF("Enter passkey on %s: %06u", peer_addr, passkey);
}

static void cancel_authentication(struct bt_conn *conn)
{
	char peer_addr[BT_ADDR_LE_STR_LEN];
	bt_addr_le_to_str(bt_conn_get_dst(conn), peer_addr, sizeof(peer_addr));

	LOG_INF("Pairing canceled by remote: %s", peer_addr);
}

/*
 * Get a device structure from a devicetree node with compatible
 * "maxim,ds18b20". (If there are multiple, just pick one.)
 */

static const struct device *get_ds18b20_device(void)
{
	const struct device *const dev = DEVICE_DT_GET_ANY(maxim_ds18b20);

	if (dev == NULL) {
		// No such node, or the node does not have status "okay". //
		LOG_DBG("\nError: no device found.\n");
		return NULL;
	}

	if (!device_is_ready(dev)) {
		LOG_DBG("\nError: Device \"%s\" is not ready; "
		       "check the driver initialization logs for errors.\n",
		       dev->name);
		return NULL;
	}

	LOG_DBG("Found device \"%s\", getting sensor data\n", dev->name);
	return dev;
}

/*
static void pairing_confirm(struct bt_conn *conn)
{
	char addr[BT_ADDR_LE_STR_LEN];

	bt_addr_le_to_str(bt_conn_get_dst(conn), addr, sizeof(addr));

	bt_conn_auth_pairing_confirm(conn);

	LOG_DBG("Pairing confirmed: %s\n", addr);
}

static void pairing_complete(struct bt_conn *conn, bool bonded)
{
	char addr[BT_ADDR_LE_STR_LEN];

	bt_addr_le_to_str(bt_conn_get_dst(conn), addr, sizeof(addr));

	LOG_DBG("Pairing completed: %s, bonded: %d\n", addr, bonded);
}

static void pairing_failed(struct bt_conn *conn, enum bt_security_err reason)
{
	char addr[BT_ADDR_LE_STR_LEN];

	bt_addr_le_to_str(bt_conn_get_dst(conn), addr, sizeof(addr));

	LOG_DBG("Pairing failed conn: %s, reason %d\n", addr, reason);
}
*/

static uint32_t uintFromFloat(float data) {
	uint32_t result;

	result = (uint32_t) data;
	data -= result;
	result <<= INT2FLOAT_BITS;
	return result | (int32_t)(data *100.f);
}

static uint32_t pulse2Uint(uint32_t pulse) {

	uint32_t result;
	float tmp;
	tmp = ((float)pulse/(float)MAX_PULSE_NS)* 100.0f;
	result = uintFromFloat(tmp);
	return result;
}

static uint32_t percentage2Uint(uint32_t percentage) {

	uint32_t result;
	float tmp = getFloatTemp4Int(percentage); // percentage in float rappresentation
	result = (tmp * (float)MAX_PULSE_NS/100.0f);
	return result;
}


static void recycled_cb(void)
{
	LOG_DBG("Connection object available from previous conn. Disconnect is complete!\n");
	advertising_start();
}

static uint32_t app_getDutyCycle_cb(void)
{
	uint32_t result;
	if(app_dutycycle.forced) {
		result = pulse2Uint(app_dutycycle.value);
		result |= app_dutycycle.forced<<(sizeof(app_dutycycle.value)*8-1);
	}
	else {
		result = pulse2Uint(local_dutycycle_value);
	}
	return result;
}

static uint8_t app_getFan2_cb(void)
{
	if(app_fan2State.forced)
		return app_fan2State.value | app_fan2State.forced<<(sizeof(app_fan2State.value)*8-1);
	else
		return local_fan2State;

}

static bool app_getPoweron_cb(void)
{
	return app_poweron_state;
}

static int32_t app_getTemperature_cb(void)
{
	return app_temperature_value;
}

static uint32_t app_getThreshold_cb(void)
{
	uint32_t result;
	result = app_threshold.value;
	if(app_threshold.forced) {
		result |= app_threshold.forced<<(sizeof(app_threshold.value)*8-1);
	}
	return result;
}

static bool app_getLeds_cb(void)
{
	return app_leds_state;
}

/**
 * callback used to set the dutycycle value
 * @param dutyCycleValue represents the new value of dutycycle to set
 * 					a negative value indicates the value has to be forced
 * 					a postive value indicates to use the calculated value
 */
static void app_setDutyCycle_cb(uint32_t dutyCycleValue)
{
	dutyCycleValue = htonl(dutyCycleValue); 

	if(dutyCycleValue & 0x80000000U) {
		app_dutycycle.forced = true;
			//LPP due the received value is set as percentage of pulse it must be re-calibrate in 
		// order to obtain the pulse period
		app_dutycycle.value = percentage2Uint(GETMASK(sizeof(app_dutycycle.value)) & dutyCycleValue);
		app_dutycycle.value = app_dutycycle.value>MAX_PULSE_NS?MAX_PULSE_NS:app_dutycycle.value;
		app_dutycycle.value = app_dutycycle.value<MIN_PULSE_WIDTH?0:app_dutycycle.value;

	}
	else {
		app_dutycycle.forced = false;
		app_dutycycle.value = local_dutycycle_value;
/* 		{
			k_sem_take(&dutycycle_sem, K_FOREVER);
			fan2ToBeUpdate=true;
			k_sem_give(&dutycycle_sem);
		} */
	}

	k_sem_take(&dutycycle_sem, K_FOREVER);
	dutyToBeUpdate = true;
	k_sem_give(&dutycycle_sem);
}

/**
 * callback used to set the Fan2 state
 * @param fan2_state indicates if the Fan2 must be set or not
 * 					a negative value indicates the state has to be forced
 * 					a postive value indicates to use the automatic (calculated) state
 */
static void app_setFan2_cb(uint8_t fan2_state)
{
	int size = sizeof(app_fan2State.value);
	//LPP TODO VERIFY HOW HANDLE THE IO in case on not forced -> shall be the stored previous state???
	if(fan2_state & (1<<(size*8-1))) {
		app_fan2State.forced = true;
		fan2_state &= GETMASK(size);
		app_fan2State.value = fan2_state; // evaluate the action
	}
	else {
		app_fan2State.forced = false;
		//status and pwm counter will be set in the next iteration of temperature acquisition
	}

/* 	k_sem_take(&fan_sem, K_FOREVER);
	fan2ToBeUpdate=true;
	k_sem_give(&fan_sem);
 */
	k_sem_take(&dutycycle_sem, K_FOREVER);
	fan2ToBeUpdate=true;
	dutyToBeUpdate = true;
	k_sem_give(&dutycycle_sem);
}


/**
 * The function `set_tx_power` is used to set the transmit power level for a specific Bluetooth handle.
 * 
 * @param handle_type The `handle_type` parameter in the `set_tx_power` function is used to specify the
 * type of handle being used. It is of type `uint8_t` and is passed as an argument to the function. The
 * handle type typically indicates whether the handle refers to a connection handle, advertising handle
 * @param handle The `handle` parameter in the `set_tx_power` function is used to specify the
 * connection handle for which the transmit power level needs to be set. It is of type `uint16_t`,
 * which means it is a 16-bit unsigned integer value. This handle uniquely identifies the connection
 * between the
 * @param tx_pwr_lvl The `tx_pwr_lvl` parameter in the `set_tx_power` function represents the transmit
 * power level that you want to set for a specific Bluetooth device. It is of type `int8_t`, which
 * means it is an 8-bit signed integer representing the power level in decibels (
 * 
 * @return The function `set_tx_power` returns void, which means it does not return any value.
 */
static void set_tx_power(uint8_t handle_type, uint16_t handle, int8_t tx_pwr_lvl)
{
	struct bt_hci_cp_vs_write_tx_power_level *cp;
	struct bt_hci_rp_vs_write_tx_power_level *rp;
	struct net_buf *buf, *rsp = NULL;
	int err;
	buf = bt_hci_cmd_alloc(K_TIMEOUT_ABS_MS(1)); 
	//buf = bt_hci_cmd_create(BT_HCI_OP_VS_WRITE_TX_POWER_LEVEL,
	//			sizeof(*cp));
	if (!buf) {
		printk("Unable to allocate command buffer\n");
		return;
	}

	cp = net_buf_add(buf, sizeof(*cp));
	cp->handle = sys_cpu_to_le16(handle);
	cp->handle_type = handle_type;
	cp->tx_power_level = tx_pwr_lvl;

	err = bt_hci_cmd_send_sync(BT_HCI_OP_VS_WRITE_TX_POWER_LEVEL,
				   buf, &rsp);
	if (err) {
		uint8_t reason = rsp ?
			((struct bt_hci_rp_vs_write_tx_power_level *)
			  rsp->data)->status : 0;
		printk("Set Tx power err: %d reason 0x%02x\n", err, reason);
		return;
	}

	rp = (void *)rsp->data;

	net_buf_unref(rsp);
}

/**
 * set Uart power state
 */
static void setUartPowerOnState(bool enabled) {
	int rc;
	if(!enabled) {
		rc = pm_device_action_run(uart_dev , PM_DEVICE_ACTION_SUSPEND);
		if (rc < 0) {
			LOG_ERR("Could not suspend uart0 (%d)\n" , rc);
		}
	}
	else {
		rc = pm_device_action_run(uart_dev , PM_DEVICE_ACTION_RESUME);
		if (rc < 0) {
			LOG_ERR("Could not resume uart0 (%d)\n" , rc);
		}
	}
}

/**
 * set leds and PWM power state
 */
static void setDeviceEnableState(bool enabled) {
	int rc;
	if(!enabled) {
		// turn off leds
		bool prevLedState = app_leds_state;
		app_setLeds_cb(false);
		app_leds_state = prevLedState;

		rc = pm_device_action_run(pwm_fan1.dev , PM_DEVICE_ACTION_SUSPEND);
		if (rc < 0) {
			LOG_ERR("Could not suspend fan1 (%d)\n" , rc);
		}
		rc = pm_device_action_run(pwm_fan2.dev , PM_DEVICE_ACTION_SUSPEND);
		if (rc < 0) {
			LOG_ERR("Could not suspend fan2 (%d)\n" , rc);
		}

	}
	else {
		//turn on leds
		if(app_leds_state)
			app_setLeds_cb(true);

		rc = pm_device_action_run(pwm_fan1.dev , PM_DEVICE_ACTION_RESUME);
		if (rc < 0) {
			LOG_ERR("Could not resume fan1 (%d)\n" , rc);
		}
		rc = pm_device_action_run(pwm_fan2.dev , PM_DEVICE_ACTION_RESUME);
		if (rc < 0) {
			LOG_ERR("Could not resume fan2 (%d)\n" , rc);
		}
		if(pwm_set_pulse_dt(&pwm_fan1, 0) !=0 ) {
    		LOG_ERR("Error: setting 0 pulse to device %s", pwm_fan1.dev->name);
    		return -1;
		}
		if(pwm_set_pulse_dt(&pwm_fan2, 0) !=0 ) {
    		LOG_ERR("Error: setting 0 pulse to device %s", pwm_fan2.dev->name);
    		return -1;
		}
	}
}



static void app_setPoweron_cb(bool poweron) {
 
	if (app_poweron_state && !poweron) {
		setUartPowerOnState(false);
		setDeviceEnableState(false);
		pwmOff = true;
	}
	if (!app_poweron_state && poweron) {
		setUartPowerOnState(true);
		setDeviceEnableState(true);
		// to avoid restart fan to maximum when powered off and on
		k_sem_take(&dutycycle_sem, K_NO_WAIT);
		//to upadate the dutycycle value
		dutyToBeUpdate = true;
		//and force the change 
		fan2ToBeUpdate=true;
		k_sem_give(&dutycycle_sem);		
		pwmOff = false;
	}
	app_poweron_state = poweron; // evaluate the action
}

static void app_setThreshold_cb(uint32_t thresholdValue)
{	
	thresholdValue = htonl(thresholdValue); 
	
	if(thresholdValue & 0x80000000U) {
		app_threshold.forced = true;
		app_threshold.value = GETMASK(sizeof(app_threshold.value)) & thresholdValue;
		uint32_t maxTmp= (MAX_TEMPERATURE << INT2FLOAT_BITS);
		app_threshold.value = app_threshold.value>maxTmp?maxTmp:app_threshold.value;
	}
	else {
		app_threshold.forced = false;
	}
}


static void app_setLeds_cb(bool ledState) {
int rc;
	if (app_leds_state && !ledState) {
		gpio_pin_configure_dt(&led0_spec , GPIO_INPUT | GPIO_PULL_DOWN);
		gpio_pin_configure_dt(&led1_spec , GPIO_INPUT | GPIO_PULL_DOWN);
		rc = pm_device_action_run(led0_spec.port , PM_DEVICE_ACTION_SUSPEND);
		if (rc < 0) { 
			LOG_ERR("Could not suspend led0 (%d)\n" , rc);
		}
		rc = pm_device_action_run(led1_spec.port , PM_DEVICE_ACTION_SUSPEND);
		if (rc < 0) {
			LOG_ERR("Could not suspend led1 (%d)\n" , rc);
		} 
	}
	if (!app_leds_state && ledState) {
 		rc = pm_device_action_run(led0_spec.port , PM_DEVICE_ACTION_RESUME);
		if (rc < 0) {
			LOG_ERR("Could not resume led0 (%d)\n" , rc);
		}
		rc = pm_device_action_run(led1_spec.port , PM_DEVICE_ACTION_RESUME);
		if (rc < 0) {
			LOG_ERR("Could not resume led1 (%d)\n" , rc);
		} 
		gpio_pin_configure_dt(&led0_spec , GPIO_OUTPUT_LOW);
		gpio_pin_configure_dt(&led1_spec , GPIO_OUTPUT_LOW);
		//if connected then lights up the led
		gpio_pin_set_dt(&led1_spec, connection_state);
	}

	app_leds_state = ledState; // evaluate the action
}


static void app_setPsw_cb(uint32_t pswValue)
{
	pswValue = htonl(pswValue); 

	passkey = pswValue;
	bt_passkey_set(passkey);
	//LPP TODO TODO
	//execute a disconnetion in order to use the new password
}

/* STEP 18.1 - Define the thread function  */
/*
void send_data_thread(void)
{
	while (1) {
		// Simulate data //
		simulate_data();
		// Send notification, the function sends notifications only if a client is subscribed //
		my_fbs_send_temperature_notify(app_temperature_value);
		k_sleep(K_MSEC(NOTIFY_INTERVAL));
	}
}
*/

/* STEP 18.1 - Define the thread function  */
/*
void send_duty_thread(void)
{
	while (1) {
		// Simulate data //
		simulate_dutycycle();
		// Send notification, the function sends notifications only if a client is subscribed //
		my_fbs_send_dutycycle_notify(app_dutycycle_value);
		k_sleep(K_MSEC(NOTIFY_INTERVAL));
	}
}
*/

static int init_pwm(void) {

	if (!pwm_is_ready_dt(&pwm_fan1)) {
    	LOG_ERR("Error: PWM device %s is not ready", pwm_fan1.dev->name);
    	return -1;
	}

	if (!pwm_is_ready_dt(&pwm_fan2)) {
    	LOG_ERR("Error: PWM device %s is not ready", pwm_fan2.dev->name);
    	return -1;
	}
	if(pwm_set_pulse_dt(&pwm_fan1, 0) !=0 ) {
    	LOG_ERR("Error: setting 0 pulse to device %s", pwm_fan1.dev->name);
    	return -1;
	}
	if(pwm_set_pulse_dt(&pwm_fan2, 0) !=0 ) {
    	LOG_ERR("Error: setting 0 pulse to device %s", pwm_fan2.dev->name);
    	return -1;
	}
	return 0;
}

static void on_connected(struct bt_conn *conn, uint8_t err)
{
	if (err) {
		LOG_DBG("Connection failed (err %u)\n", err);
		return;
	}

	LOG_DBG("Connected\n");
	gpio_pin_set_dt(&led1_spec, 1);
	connection_state=true;
}

static void on_disconnected(struct bt_conn *conn, uint8_t reason)
{
	LOG_DBG("Disconnected (reason %u)\n", reason);
	gpio_pin_set_dt(&led1_spec, 0);
	connection_state=false;
}


static struct my_fbs_cb app_callbacks = {
	.temperature_cb =app_getTemperature_cb, 
	.getDuty_cb = app_getDutyCycle_cb,
	.getFan2_cb = app_getFan2_cb,
	.getPowerOn_cb = app_getPoweron_cb,
	.getThreshold_cb = app_getThreshold_cb,
	.setDuty_cb = app_setDutyCycle_cb,
	.setFan2_cb = app_setFan2_cb,
	.setPowerOn_cb = app_setPoweron_cb,
	.setThreshold_cb = app_setThreshold_cb, 
	.setLedsOn_cb = app_setLeds_cb,
	.getLedsOn_cb = app_getLeds_cb,
	.setPsw_cb = app_setPsw_cb,
};

static const struct bt_conn_auth_cb auth_callbacks = {
	.passkey_display = display_passkey,
	.cancel = cancel_authentication,
	.pairing_confirm = NULL,
//	.pairing_complete = pairing_complete,
//	.pairing_failed = pairing_failed
};

struct bt_conn_cb connection_callbacks = {
	.connected = on_connected,
	.disconnected = on_disconnected,
	.recycled = recycled_cb,
};


float getFloatTemp4Int(uint32_t T_Arg) {
	return (float)((T_Arg & HI_INT2FLOAT_MASK)>>INT2FLOAT_BITS) + (float)(T_Arg & LOW_INT2FLOAT_MASK)/100.0f;
}

/**
 * 
 */
int32_t setPwmFan2(bool enable, uint32_t pulse) {
/*
	// forced        enabled		0  //set 0
	//not forced     enabled		0  //set required value
	// forced 		 disabled		0  //set 0
	//not forced 	 disabled		0  //set 0
	// forced        enabled		1  //set required value
	//not forced     enabled		1  //set required value
	// forced 		 disabled		1  //set required value
	//not forced 	 disabled		1  //set 0
	float pulseTmp=0;
	// linearization 
	if(pulse>=5000U && pulse<10000U) 
		pulseTmp = (float)(pulse - 5000U) * 0.0025f + 1430.0f;
	
	if(pulse>=10000U && pulse<15000U)
		pulseTmp = (float)(pulse - 10000U) * 0.0075f + 1443.0f;

	if(pulse>=15000U && pulse<20000U)
		pulseTmp = (float)(pulse - 15000U) * 0.0125f + 1480.0f;

	if(pulse>=20000U && pulse<30000U)
		pulseTmp = (float)(pulse - 20000U) * 0.0375f + 1543.0f;

	if(pulse>=30000U && pulse<35000U)
		pulseTmp = (float)(pulse - 30000U) * 0.15f + 1918.0f;

	if(pulse>=35000U && pulse<40000U)
		pulseTmp = (float)(pulse - 35000U) * 0.625f + 2668.0f;
		
	if(pulse>=40000U)
		pulseTmp = (float)(pulse - 40000U) * 0.6875f + 5663.0f;
	*/

	if((!app_fan2State.forced && enable) || // not forced and required to enable
		(app_fan2State.forced && app_fan2State.value)) { //forced and value is not 0
		
		if(fan2ToBeStarted){
			if(pwm_set_pulse_dt(&pwm_fan2, MAX_PULSE_2FAN2) !=0 ){
				LOG_ERR("Error setting pwm for dev %s ", pwm_fan2.dev->name);
				return -1;
			}
			fan2ToBeStarted=false;
			k_sleep(K_MSEC(500));
		}

//		if(pwm_set_pulse_dt(&pwm_fan2, pulseTmp) !=0 ){
		if(pwm_set_pulse_dt(&pwm_fan2, pulse) !=0 ){
			LOG_ERR("Error setting pwm for dev %s ", pwm_fan2.dev->name);
			return -1;
		}
	}
	else {
		// or disabled and not forced 
		//or forced and vale set to 0
		if(pwm_set_pulse_dt(&pwm_fan2, 0) !=0 ){
			LOG_ERR("Error setting pwm for dev %s ", pwm_fan2.dev->name);
			return -1;
		}
		fan2ToBeStarted=true;
	}
	return 0;
}

/**
 * Actually implementing only proportional thereisn't need to implement a complex one
 */
int32_t setPwmDuration(void) {

	//eval the dutycycle as proportional to the temeprature difference is it is not forced
	if(!app_dutycycle.forced) {
		float appTh = getFloatTemp4Int(app_threshold.value);
		float dT = getFloatTemp4Int(app_temperature_value) - appTh ;
		float TRange = MAX_TEMPERATURE - appTh;
		float dRange = MAX_PULSE_NS;
		if(dT>0)
			dutyPulse = (int32_t)(dT * dRange / TRange);
		else
			dutyPulse = 0;
	}
	else {
		dutyPulse = app_dutycycle.value;
	}


	if(dutyPulse<=(MAX_PULSE_2FAN2)) {
		// in case dutycycle is less than required and not already set then set to disable
		//DISABLE FAN2
		if(local_fan2State) {
			//FAN2 was automatically enable than stop it
			local_fan2State = false;
		}
	}
	else {
		// in case the dutycycle is heigh than enable the second fan if not already set
		//ENABLE FAN2
		if(!local_fan2State) {
			//isn't already required an activation
			local_fan2State = true;
		}
	}

	//acquire the fan2 update status
	k_sem_take(&dutycycle_sem, K_FOREVER);
	bool tmpFanUpdate=fan2ToBeUpdate;
	fan2ToBeUpdate=false;
	k_sem_give(&dutycycle_sem);

	//in case the fan2 is active then half the required dutycyle
	//this check shall be performed on every iteration 
	if(dutyPulse>(MAX_PULSE_2FAN2) && !local_fan2State) {
		//Enable FAN2 and reduce the pulse
		//implicit in the if condition
//		local_fan2State = true;
		dutyPulse >>= 1; 
	}

//	pulse<<=1;
//	LOG_ERR("Thresh=%.3f, dT=%.3f, pulse=%d ", appTh, dT, pulse);
	
	if(dutyPulse< MIN_PULSE_WIDTH) {
		//if fan active or dutycycle is forced than stop the fans

		//TO BE VERIFIED not sure the check on forced is required
		  // only ones                  // every loop
		//if(local_dutycycle_value !=0 || app_dutycycle.forced) {
		if(local_dutycycle_value !=0 || tmpFanUpdate) { //update in case changes or due to a request
			//already active -> require to disable the all fans 
			if(pwm_set_pulse_dt(&pwm_fan1, 0) !=0 ){
				LOG_ERR("Error setting pwm for dev %s ", pwm_fan1.dev->name);
				return -1;
			}
			setPwmFan2(local_fan2State, 0U);
		}
		//store as disabled
		local_dutycycle_value = 0;
		k_sem_take(&dutycycle_sem, K_FOREVER);
		dutyToBeUpdate = true;
		k_sem_give(&dutycycle_sem);
		// due the fans are disabled can enter in power safe if not already done
		//go in suspend mode
		
		if(!pwmOff && // the pwm is not shutdown 
			app_poweron_state) { // not in power off
			setDeviceEnableState(false);
			pwmOff = true;
		}
		return 0;
	}
	// dutycycle has changed
	if(dutyPulse != local_dutycycle_value || tmpFanUpdate) {
		//disable suspended mode
		if( pwmOff && // the pwm is shutdown
			app_poweron_state) { // power is on
			setDeviceEnableState(true);
			pwmOff = false;
		}

		if(pwm_set_pulse_dt(&pwm_fan1, dutyPulse) !=0 ){
			LOG_ERR("Error setting pwm for dev %s ", pwm_fan1.dev->name);
			return -1;
		}

		setPwmFan2(local_fan2State, dutyPulse);
		local_dutycycle_value = dutyPulse;
	}
	
	//dutyToBeUpdate = true;
	k_sem_take(&dutycycle_sem, K_FOREVER);
	dutyToBeUpdate = true;
	k_sem_give(&dutycycle_sem);
	return 0;
}

///////////FLASH STORAGE HANDLING START //////////////////////
/// @brief 
/// @param name 
/// @param len 
/// @param read_cb 
/// @param cb_arg 
/// @return 
static int fan_settings_set(const char *name, size_t len, settings_read_cb read_cb, void *cb_arg) {
    const char *next;
    int rc;
	size_t next_len;

    if (settings_name_steq(name, STORAGE_SETTINGS_KEY, &next) && !next) {
        if (len != sizeof(passkey)) {
            return -EINVAL;
        }

        rc = read_cb(cb_arg, &passkey, sizeof(passkey));
        if (rc >= 0) {
            /* key-value pair was properly read.
             * rc contains value length.
             */
            return 0;
        }
        /* read-out error */
        return rc;
    }
	
	next_len = settings_name_next(name, &next);
	if (!next) {
		return -ENOENT;
	}

    if (settings_name_steq(name, STORAGE_SETTINGS_TEMP, &next) && !next) {
        if (len != sizeof(app_threshold.value)) {
            return -EINVAL;
        }

        rc = read_cb(cb_arg, &(app_threshold.value), sizeof(app_threshold.value));
        if (rc >= 0) {
            /* key-value pair was properly read.
             * rc contains value length.
             */
            return 0;
        }
        /* read-out error */
        return rc;
    }

    return -ENOENT;
}

/// @brief 
/// @param storage_func 
/// @return 
static int fan_settings_export(int (*storage_func)(const char *name, const void *value, size_t val_len)) {
	return storage_func(STORAGE_SETTINGS_ROOT"/"STORAGE_SETTINGS_TEMP, &(app_threshold.value), sizeof(app_threshold.value));
}

int read_settings(void)
{
	return settings_load();
}

int fan_settings_init(void)
{
	int err = settings_subsys_init();
	if (err) {
		LOG_INF("Can't init settings subsys (%d)\n", err);
		return err;
	}

/* 	err = settings_load();
	if (err) {
		LOG_INF("Can't load settings (%d)\n", err);
		return err;
		
	}  */
	err = settings_register(&fan_conf);
	if (err) {
		LOG_INF("Can't register fan settings handler (%d)\n", err);
		return err;
	}
	/* Load settings from persistent storage */
	return read_settings();
}
///////////FLASH STORAGE HANDLING END //////////////////////

int main(void)
{
	int blink_status = 0;
	int err;
	k_sem_init(&dutycycle_sem, 1, 1);

	LOG_INF("Starting acquiring temp sensor \n");
	tempDev = get_ds18b20_device();
	
	if (tempDev == NULL) {
		return 0;
	}
	
	gpio_pin_configure_dt(&led0_spec, GPIO_OUTPUT);
	gpio_pin_configure_dt(&led1_spec, GPIO_OUTPUT);
	gpio_pin_configure_dt(&led2_spec, GPIO_OUTPUT);
	gpio_pin_set_dt(&led0_spec, 0);
	gpio_pin_set_dt(&led1_spec, 0);
	gpio_pin_set_dt(&led2_spec, 0);

	
	err = init_pwm();
	if (err) {
		LOG_ERR("PWM init failed (err %d)\n", err);
		return -1;
	}
	
	err = bt_enable(NULL);
	if (err) {
		LOG_ERR("Bluetooth init failed (err %d)\n", err);
		return -1;
	}
	//UNUSED so configure as input to reduce power
	gpio_pin_configure_dt(&btn0_spec, GPIO_INPUT | GPIO_PULL_DOWN);
	gpio_pin_configure_dt(&btn1_spec, GPIO_INPUT | GPIO_PULL_DOWN);
	err = pm_device_action_run(btn0_spec.port, PM_DEVICE_ACTION_SUSPEND);
	if (err < 0) {
			LOG_ERR("Could not suspend button0 (%d)\n", err);
	}
	err = pm_device_action_run(btn1_spec.port, PM_DEVICE_ACTION_SUSPEND);
	if (err < 0) {
			LOG_ERR("Could not suspend button1 (%d)\n", err);
	}
	gpio_pin_configure_dt(&led2_spec, GPIO_INPUT | GPIO_PULL_DOWN);
	err = pm_device_action_run(led2_spec.port, PM_DEVICE_ACTION_SUSPEND);
	if (err < 0) {
			LOG_ERR("Could not suspend led2 (%d)\n", err);
	}
	// no more used
	gpio_pin_configure_dt(&fanGpio_spec , GPIO_INPUT | GPIO_PULL_DOWN);
	err = pm_device_action_run(fanGpio_spec.port, PM_DEVICE_ACTION_SUSPEND);
	if (err < 0) {
			LOG_ERR("Could not suspend fanGpio (%d)\n", err);
	}
	
	gpio_pin_configure_dt(&powerGpio_spec , GPIO_INPUT | GPIO_PULL_DOWN);
	err = pm_device_action_run(powerGpio_spec.port, PM_DEVICE_ACTION_SUSPEND);
	if (err < 0) {
			LOG_ERR("Could not suspend powerGpio (%d)\n", err);
	}
	app_setPoweron_cb(true);

	//LPP TO BE REVIEW
	//initialize and load settings from internal flash
	//fan_settings_init();
	//	settings_save_one(STORAGE_SETTINGS_ROOT"/"STORAGE_SETTINGS_KEY, &passkey, sizeof(passkey));
	//	settings_save_one(STORAGE_SETTINGS_ROOT"/"STORAGE_SETTINGS_TEMP, &(app_threshold.value), sizeof(app_threshold.value));
	

	// unsigned int passkey = 181181;
	bt_passkey_set(passkey);
	bt_conn_auth_cb_register(&auth_callbacks);
	//NRF_FICR->DEVICEID[0] ^ NRF_FICR->DEVICEID[1]
	//NRF_FICR->DEVICEADDR[0] ^ NRF_FICR->DEVICEADDR[1];
	
	bt_conn_cb_register(&connection_callbacks);

	err = my_fbs_init(&app_callbacks);
	if (err) {
		LOG_DBG("Failed to init LBS (err:%d)\n", err);
		return -1;
	}
	LOG_INF("Bluetooth initialized\n");
	k_work_init(&adv_work, adv_work_handler);
	advertising_start();

	app_setLeds_cb(false);
	setDeviceEnableState(false);
	pwmOff = true;
	fan2ToBeStarted=true;
	//set_tx_power(BT_HCI_VS_LL_HANDLE_TYPE_ADV, 0, -20);

	struct sensor_value temp;
	uint32_t tmpTemp=0;
	// take in account how long can be the sleep
	uint32_t counterLoop=1;

	for (;;) {

		while(!app_poweron_state) {
			k_sleep(K_MSEC(RUN_LED_BLINK_INTERVAL<<2));
		}

		setUartPowerOnState(true);		
 		//err = sensor_sample_fetch(tempDev);
		err = sensor_sample_fetch_chan(tempDev,SENSOR_CHAN_AMBIENT_TEMP);
		if (err != 0) {
			LOG_DBG("sample_fetch() failed: %d\n", err);
			return err;
		}

		err = sensor_channel_get(tempDev, SENSOR_CHAN_AMBIENT_TEMP, &temp);
		if (err != 0) {
			LOG_DBG("channel_get() failed: %d\n", err);
			return err;
		}
		
		setUartPowerOnState(false);

		app_temperature_value = temp.val1<<16 | (temp.val2/10000);
		if(tmpTemp != app_temperature_value) {
			my_fbs_send_temperature_notify(app_temperature_value);
			tmpTemp = app_temperature_value;
			//dutyToBeUpdate=true;
			k_sem_take(&dutycycle_sem, K_NO_WAIT);
			dutyToBeUpdate = true;
			k_sem_give(&dutycycle_sem);			
		}

		// the temperature acquisition 
		counterLoop = 8;
		while(counterLoop>0) {
			//LPP TODO send notificaton also in case a setPwmDuration is called
			if(dutyToBeUpdate) {
				setPwmDuration();
				my_fbs_send_dutycycle_notify(app_getDutyCycle_cb());
				//dutyToBeUpdate = false;
				k_sem_take(&dutycycle_sem, K_FOREVER);
				dutyToBeUpdate = false;
				k_sem_give(&dutycycle_sem);
			}
			if(app_leds_state)
				gpio_pin_set_dt(&led0_spec, (++blink_status) % 2);
			k_sleep(K_MSEC(RUN_LED_BLINK_INTERVAL));
			counterLoop--;
		}
	}
}
