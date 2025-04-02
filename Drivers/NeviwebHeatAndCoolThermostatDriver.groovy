import groovy.json.JsonSlurper
import groovy.json.JsonOutput
import groovy.transform.Field

// Supported Climate Features
/*@Field static final int SUPPORT_FLAGS = (
    ClimateEntityFeature.TARGET_TEMPERATURE |
    ClimateEntityFeature.PRESET_MODE |
    ClimateEntityFeature.TURN_OFF |
    ClimateEntityFeature.TURN_ON
)

@Field static final int SUPPORT_AUX_FLAGS = SUPPORT_FLAGS

@Field static final int SUPPORT_HP_FLAGS = (
    ClimateEntityFeature.TARGET_TEMPERATURE |
    ClimateEntityFeature.PRESET_MODE |
    ClimateEntityFeature.FAN_MODE |
    ClimateEntityFeature.SWING_HORIZONTAL_MODE |
    ClimateEntityFeature.SWING_MODE |
    ClimateEntityFeature.TURN_OFF |
    ClimateEntityFeature.TURN_ON
)

@Field static final int SUPPORT_HC_FLAGS = (
    ClimateEntityFeature.TARGET_TEMPERATURE |
    ClimateEntityFeature.PRESET_MODE |
    ClimateEntityFeature.FAN_MODE |
    ClimateEntityFeature.TURN_OFF |
    ClimateEntityFeature.TURN_ON
)*/

// Default Names
@Field static final String DEFAULT_NAME = "neviweb130 climate"
@Field static final String DEFAULT_NAME_2 = "neviweb130 climate 2"
@Field static final String DEFAULT_NAME_3 = "neviweb130 climate 3"

// Constants
@Field static final int SNOOZE_TIME = 1200

// Dynamic Attributes Using getConstant()
def getUpdateAttributes() {
    return [
        parent.getConstant("ATTR_DRSETPOINT"),
        parent.getConstant("ATTR_DRSTATUS"),
        parent.getConstant("ATTR_OUTPUT_PERCENT_DISPLAY"),
        parent.getConstant("ATTR_ROOM_SETPOINT"),
        parent.getConstant("ATTR_ROOM_SETPOINT_MAX"),
        parent.getConstant("ATTR_ROOM_SETPOINT_MIN"),
        parent.getConstant("ATTR_ROOM_TEMPERATURE"),
        parent.getConstant("ATTR_TEMP"),
        parent.getConstant("ATTR_TIME")
    ]
}

def getUpdateHpAttributes() {
    return [
        parent.getConstant("ATTR_ROOM_SETPOINT"),
        parent.getConstant("ATTR_ROOM_SETPOINT_MAX"),
        parent.getConstant("ATTR_ROOM_SETPOINT_MIN"),
        parent.getConstant("ATTR_COOL_SETPOINT_MIN"),
        parent.getConstant("ATTR_COOL_SETPOINT_MAX"),
        parent.getConstant("ATTR_ROOM_TEMPERATURE"),
        parent.getConstant("ATTR_TEMP")
    ]
}

def getUpdateHeatCoolAttributes() {
    return [
        parent.getConstant("ATTR_OUTPUT_PERCENT_DISPLAY"),
        parent.getConstant("ATTR_ROOM_SETPOINT"),
        parent.getConstant("ATTR_COOL_SETPOINT"),
        parent.getConstant("ATTR_ROOM_SETPOINT_MAX"),
        parent.getConstant("ATTR_ROOM_SETPOINT_MIN"),
        parent.getConstant("ATTR_COOL_SETPOINT_MAX"),
        parent.getConstant("ATTR_COOL_SETPOINT_MIN"),
        parent.getConstant("ATTR_ROOM_TEMP_DISPLAY"),
        parent.getConstant("ATTR_ROOM_TEMPERATURE"),
        parent.getConstant("ATTR_TEMP"),
        parent.getConstant("ATTR_TIME")
    ]
}

// HVAC Modes
/*@Field static final List<String> SUPPORTED_HVAC_WIFI_MODES = [
    HVACMode.AUTO,
    HVACMode.HEAT,
    HVACMode.OFF
]

@Field static final List<String> SUPPORTED_HVAC_MODES = [
    HVACMode.HEAT,
    HVACMode.OFF
]

@Field static final List<String> SUPPORTED_HVAC_HC_MODES = [
    HVACMode.COOL,
    HVACMode.HEAT,
    HVACMode.OFF
]

@Field static final List<String> SUPPORTED_HVAC_HP_MODES = [
    HVACMode.COOL,
    HVACMode.DRY,
    HVACMode.FAN_ONLY,
    HVACMode.HEAT,
    HVACMode.OFF
]

@Field static final List<String> SUPPORTED_HVAC_HEAT_MODES = [
    HVACMode.FAN_ONLY,
    HVACMode.HEAT,
    HVACMode.OFF
]

@Field static final List<String> SUPPORTED_HVAC_COOL_MODES = [
    HVACMode.COOL,
    HVACMode.DRY,
    HVACMode.FAN_ONLY,
    HVACMode.OFF
]

// Preset Modes
@Field static final List<String> PRESET_WIFI_MODES = [
    PRESET_AWAY,
    PRESET_HOME,
    PRESET_NONE
]

@Field static final List<String> PRESET_MODES = [
    PRESET_AWAY,
    PRESET_NONE
]

@Field static final List<String> PRESET_HP_MODES = [
    PRESET_AWAY,
    PRESET_HOME,
    PRESET_NONE
]

@Field static final List<String> PRESET_HC_MODES = [
    PRESET_AWAY,
    PRESET_NONE
]

// Device Models
@Field static final List<Integer> DEVICE_MODEL_LOW = [7372]
@Field static final List<Integer> DEVICE_MODEL_LOW_WIFI = [739]
@Field static final List<Integer> DEVICE_MODEL_FLOOR = [737]
@Field static final List<Integer> DEVICE_MODEL_WIFI_FLOOR = [738]
@Field static final List<Integer> DEVICE_MODEL_WIFI = [1510, 742]
@Field static final List<Integer> DEVICE_MODEL_HEAT = [1123, 1124]
@Field static final List<Integer> DEVICE_MODEL_DOUBLE = [7373]
@Field static final List<Integer> DEVICE_MODEL_HEAT_G2 = [300]
@Field static final List<Integer> DEVICE_MODEL_HC = [1512]
@Field static final List<Integer> DEVICE_MODEL_HEAT_PUMP = [6810, 6811, 6812]
@Field static final List<Integer> DEVICE_MODEL_HEAT_COOL = [6727, 6730]

@Field static final List<Integer> IMPLEMENTED_DEVICE_MODEL = 
    DEVICE_MODEL_HEAT + DEVICE_MODEL_FLOOR + DEVICE_MODEL_LOW + 
    DEVICE_MODEL_WIFI_FLOOR + DEVICE_MODEL_WIFI + DEVICE_MODEL_LOW_WIFI + 
    DEVICE_MODEL_HEAT_G2 + DEVICE_MODEL_HC + DEVICE_MODEL_DOUBLE + 
    DEVICE_MODEL_HEAT_PUMP + DEVICE_MODEL_HEAT_COOL

// HA to Neviweb Period Mapping
@Field static final Map<String, Integer> HA_TO_NEVIWEB_PERIOD = [
    "15 sec": 15,
    "5 min": 300,
    "10 min": 600,
    "15 min": 900,
    "20 min": 1200,
    "25 min": 1500,
    "30 min": 1800
]*/

@Field Utils = Utils_create();
@Field List<String> LOG_LEVELS = ["error", "warn", "info", "debug", "trace"]
@Field String DEFAULT_LOG_LEVEL = LOG_LEVELS[1]
def driverVer() { return "0.1" }

metadata {
    definition(name: "Neviweb Heat and Cool Thermostat Driver", namespace: "rferrazguimaraes", author: "Rangner Ferraz Guimaraes") {
        capability "Thermostat"
        capability "TemperatureMeasurement"
        capability "Refresh"
        capability "Actuator"
        capability "Sensor"

        command "deviceLog", [[name: "Level*", type:"STRING", description: "Level of the message"], 
                              [name: "Message*", type:"STRING", description: "Message"]] 

        command "setHeatingSetpoint", ["NUMBER"]
        command "setCoolingSetpoint", ["NUMBER"]
        command "setThermostatMode", ["ENUM"]
        command "setThermostatFanMode", ["ENUM"]
        command "setPresetMode", ["ENUM"]
        command "setMinTemperature", ["NUMBER"]
        command "setMaxTemperature", ["NUMBER"]

        attribute "heatingSetpoint", "NUMBER"
        attribute "coolingSetpoint", "NUMBER"
        attribute "thermostatMode", "STRING"
        attribute "thermostatFanMode", "STRING"
        attribute "thermostatOperatingState", "STRING"
        attribute "currentTemperature", "NUMBER"
        attribute "targetTemperature", "NUMBER"
        attribute "presetMode", "STRING"
        attribute "supportedHvacModes", "JSON_OBJECT"
        attribute "supportedPresetModes", "JSON_OBJECT"
        attribute "wattage", "NUMBER"        
        attribute "DriverVersion", "string"
    }

    preferences {
        input name: "pollInterval", type: "enum", title: "Polling Interval (minutes)",
              options: ["1", "5", "10", "15"], defaultValue: "5", required: true
        input name: "logLevel", title: "Log Level", type: "enum", options: LOG_LEVELS, defaultValue: DEFAULT_LOG_LEVEL, required: true
    }
}

def installed() {
    Utils.toLogger("debug", "Installed Neviweb Heat and Cool Thermostat Driver")
    initialize()
}

def updated() {
    Utils.toLogger("debug", "Updated Neviweb Heat and Cool Thermostat Driver")
    unschedule()
    initialize()
}

def initialize() {
    Utils.toLogger("debug", "Initializing Neviweb Heat and Cool Thermostat Driver")
    
    updateInfo()
    
    // Set unused default values (for Google Home Integration)
    setSupportedThermostatModes(JsonOutput.toJson(["heat", "idle"]))
    setSupportedThermostatFanModes(JsonOutput.toJson([""]))
    sendEvent(name: "thermostatMode", value: "heat", isStateChange: true)
    sendEvent(name: "thermostatOperatingState", value: "idle", isStateChange: true)
    sendEvent(name: "thermostatFanMode", value: "auto", isStateChange: true)
    sendEvent(name: "coolingSetpoint", value: state.maxTemp, isStateChange: true)
    sendEvent(name: "thermostatSetpoint", value: state.targetTemp, isStateChange: true)
    sendEvent(name: "heatingSetpoint", value: state.minTemp, isStateChange: true)
    
    schedule("0 */${settings.pollInterval} * * * ?", refresh)
}

def refresh() {
    Utils.toLogger("debug", "Refreshing thermostat data...")
    updateInfo()
}

def setSupportedThermostatFanModes(fanModes) {
	Utils.toLogger("debug", "setSupportedThermostatFanModes(${fanModes}) was called")
	// (auto, circulate, on)
	sendEvent(name: "supportedThermostatFanModes", value: fanModes)
}

def setSupportedThermostatModes(modes) {
	Utils.toLogger("debug", "setSupportedThermostatModes(${modes}) was called")
	// (auto, cool, emergency heat, heat, off)
	sendEvent(name: "supportedThermostatModes", value: modes)
}

def setDeviceInfo(deviceInfo) {    
    state.deviceInfo = deviceInfo
    Utils.toLogger("debug", "DeviceInfo: ${state.deviceInfo}")
    state.sku = deviceInfo.sku
    Utils.toLogger("debug", "SKU: ${state.sku}")
    state.firmware = "${deviceInfo.signature.softVersion.major}.${deviceInfo.signature.softVersion.middle}.${deviceInfo.signature.softVersion.minor}"
    Utils.toLogger("debug", "Firmware: ${state.firmware}")
}

def updateInfo() {
    def HEAT_ATTRIBUTES = [
        parent.getConstant("ATTR_WATTAGE"), 
        parent.getConstant("ATTR_KEYPAD"), 
        parent.getConstant("ATTR_BACKLIGHT"), 
        parent.getConstant("ATTR_SYSTEM_MODE"), 
        parent.getConstant("ATTR_CYCLE"), 
        parent.getConstant("ATTR_DISPLAY2"), 
        parent.getConstant("ATTR_RSSI")
    ]
    def firmwareSpecial = (state.firmware == "0.6.4" || state.firmware == "0.6.0") ? [] : [parent.getConstant("ATTR_ROOM_TEMP_DISPLAY")]

    Utils.toLogger("debug", "Requesting attributes from parent app for ${device.label}: ${getUpdateAttributes() + HEAT_ATTRIBUTES + firmwareSpecial}")

    def start = now()

    try {
        def deviceData = parent.getDeviceAttributes(device.deviceNetworkId, getUpdateAttributes() + HEAT_ATTRIBUTES + firmwareSpecial)

        if (deviceData == null) {
            Utils.toLogger("error", "Failed to update device ${device.label}: deviceData is null. Possible network issue or API failure.")
            return
        }

        def elapsed = (now() - start) / 1000
        Utils.toLogger("debug", "Received data for ${device.label} (${elapsed} sec): ${deviceData}")

        if (!deviceData.containsKey("error")) {
            if (!deviceData.containsKey("errorCode")) {
                if (deviceData.containsKey(parent.getConstant("ATTR_ROOM_TEMP_DISPLAY"))) {
                    state.tempDisplayValue = deviceData[parent.getConstant("ATTR_ROOM_TEMP_DISPLAY")]
                }

                state.curTempBefore = state.curTemp ?: state.tempDisplayValue
                state.curTemp = deviceData[parent.getConstant("ATTR_ROOM_TEMPERATURE")]?.value ?: state.curTempBefore
                state.targetTemp = deviceData[parent.getConstant("ATTR_ROOM_SETPOINT")]
                state.minTemp = deviceData[parent.getConstant("ATTR_ROOM_SETPOINT_MIN")]
                state.maxTemp = deviceData[parent.getConstant("ATTR_ROOM_SETPOINT_MAX")]
                state.temperatureFormat = deviceData[parent.getConstant("ATTR_TEMP")]
                state.timeFormat = deviceData[parent.getConstant("ATTR_TIME")]

                state.display2 = deviceData[parent.getConstant("ATTR_DISPLAY2")]
                state.heatLevel = deviceData[parent.getConstant("ATTR_OUTPUT_PERCENT_DISPLAY")]
                state.keypad = deviceData[parent.getConstant("ATTR_KEYPAD")]
                state.backlight = deviceData[parent.getConstant("ATTR_BACKLIGHT")]

                if (deviceData.containsKey(parent.getConstant("ATTR_CYCLE"))) {
                    state.cycleLength = deviceData[parent.getConstant("ATTR_CYCLE")]
                }

                if (deviceData.containsKey(parent.getConstant("ATTR_RSSI"))) {
                    state.rssi = deviceData[parent.getConstant("ATTR_RSSI")]
                }

                state.operationMode = deviceData[parent.getConstant("ATTR_SYSTEM_MODE")]

                if (!state.isLowVoltage) {
                    state.wattage = deviceData[parent.getConstant("ATTR_WATTAGE")]
                }

                if (deviceData.containsKey(parent.getConstant("ATTR_DRSETPOINT"))) {
                    state.drSetpointStatus = deviceData[parent.getConstant("ATTR_DRSETPOINT")]["status"]
                    state.drSetpointValue = deviceData[parent.getConstant("ATTR_DRSETPOINT")]["value"] ?: 0
                }

                if (deviceData.containsKey(parent.getConstant("ATTR_DRSTATUS"))) {
                    state.drStatusActive = deviceData[parent.getConstant("ATTR_DRSTATUS")]["drActive"]
                    state.drStatusOptout = deviceData[parent.getConstant("ATTR_DRSTATUS")]["optOut"]
                    state.drStatusSetpoint = deviceData[parent.getConstant("ATTR_DRSTATUS")]["setpoint"]
                    state.drStatusAbs = deviceData[parent.getConstant("ATTR_DRSTATUS")]["powerAbsolute"]
                    state.drStatusRel = deviceData[parent.getConstant("ATTR_DRSTATUS")]["powerRelative"]
                }

                sendEvent(name: "currentTemperature", value: state.tempDisplayValue ?: state.curTemp)
                sendEvent(name: "targetTemperature", value: state.targetTemp)
                sendEvent(name: "heatingSetpoint", value: state.minTemp)
                sendEvent(name: "coolingSetpoint", value: state.maxTemp)
			    sendEvent(name: "thermostatSetpoint", value: state.targetTemp)
                sendEvent(name: "thermostatMode", value: state.operationMode)
                sendEvent(name: "wattage", value: state.wattage)
                sendEvent(name: "DriverVersion", value: driverVer())

            } else if (deviceData.errorCode == "ReadTimeout") {
                Utils.toLogger("warn", "Timeout occurred while updating device ${device.label}. Check your network. (${deviceData})")
            } else {
                Utils.toLogger("warn", "Error updating device ${device.label}: (${deviceData})")
            }
        } else {
            Utils.toLogger("error", "Device error: ${deviceData['error']['code']}")
        }
    } catch (Exception e) {
        Utils.toLogger("error", "Error during update: ${e.message}")
    }
}

def updateAttributes(data) {
    Utils.toLogger("debug", "Updated attributes: ${data}")
    try {
        updateInfo()
    } catch (Exception e) {
        Utils.toLogger("error", "Error updating attributes: ${e.message}")
    }
}

def setHeatingSetpoint(temp) {
    Utils.toLogger("debug", "Setting heating setpoint to ${temp}°C...")
    try {
        parent.setTemperature(device.deviceNetworkId, temp)
        sendEvent(name: "heatingSetpoint", value: temp)
    } catch (Exception e) {
        Utils.toLogger("error", "Error setting heating setpoint: ${e.message}")
    }
}

def setCoolingSetpoint(temp) {
    Utils.toLogger("debug", "Setting cooling setpoint to ${temp}°C...")
    try {
        parent.setDeviceAttributes(device.deviceNetworkId, ["cooling_setpoint": temp])
        sendEvent(name: "coolingSetpoint", value: temp)
    } catch (Exception e) {
        Utils.toLogger("error", "Error setting cooling setpoint: ${e.message}")
    }
}

def setThermostatMode(mode) {
    Utils.toLogger("debug", "Setting thermostat mode to ${mode}...")
    try {
        if (!["off", "heat", "cool"].contains(mode)) {
            Utils.toLogger("warn", "Unsupported thermostat mode: ${mode}")
            return
        }
        parent.setDeviceAttributes(device.deviceNetworkId, ["system_mode": mode])
        sendEvent(name: "thermostatMode", value: mode)
    } catch (Exception e) {
        Utils.toLogger("error", "Error setting thermostat mode: ${e.message}")
    }
}

def setThermostatFanMode(mode) {
    Utils.toLogger("debug", "Setting thermostat fan mode to ${mode}...")
    try {
        parent.setDeviceAttributes(device.deviceNetworkId, ["thermostat_fan_mode": mode])
        sendEvent(name: "thermostatFanMode", value: mode)
    } catch (Exception e) {
        Utils.toLogger("error", "Error setting thermostat fan mode: ${e.message}")
    }
}

def setPresetMode(preset) {
    Utils.toLogger("debug", "Setting preset mode to ${preset}...")
    try {
        if (!["home", "away", "none"].contains(preset)) {
            Utils.toLogger("warn", "Unsupported preset mode: ${preset}")
            return
        }
        parent.setDeviceAttributes(device.deviceNetworkId, ["occupancy_mode": preset])
        sendEvent(name: "presetMode", value: preset)
    } catch (Exception e) {
        Utils.toLogger("error", "Error setting preset mode: ${e.message}")
    }
}

/**
 * Simple utilities for manipulation
 */

def Utils_create() {
    def instance = [:];
    
    instance.toLogger = { level, msg ->
        if (level && msg) {
            Integer levelIdx = LOG_LEVELS.indexOf(level);
            Integer setLevelIdx = LOG_LEVELS.indexOf(logLevel);
            if (setLevelIdx < 0) {
                setLevelIdx = LOG_LEVELS.indexOf(DEFAULT_LOG_LEVEL);
            }
            if (levelIdx <= setLevelIdx) {
                log."${level}" "${device.displayName} ${msg}";
            }
        }
    }

    return instance;
}
