import groovy.json.JsonSlurper
import groovy.json.JsonOutput
import groovy.transform.Field

@Field Utils = Utils_create();
@Field List<String> LOG_LEVELS = ["error", "warn", "info", "debug", "trace"]
@Field String DEFAULT_LOG_LEVEL = LOG_LEVELS[1]
def driverVer() { return "0.1" }

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

metadata {
    definition(name: "Neviweb Floor Thermostat Driver", namespace: "rferrazguimaraes", author: "Rangner Ferraz Guimaraes") {
        capability "Thermostat"
        capability "TemperatureMeasurement"
        capability "Refresh"
        capability "Actuator"
        capability "Sensor"

        command "setHeatingSetpoint", ["NUMBER"]
        command "setCoolingSetpoint", ["NUMBER"]
        command "setThermostatMode", ["ENUM"]
        command "setThermostatFanMode", ["ENUM"]
        command "setPresetMode", ["ENUM"]
        command "setMinTemperature", ["NUMBER"]
        command "setMaxTemperature", ["NUMBER"]
        command "setBacklightOn"
		command "setBacklightOff"

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
        input name: "backlightPreference", type: "enum", title: "Backlight Mode", description: "Select backlight mode", options: ["on", "bedroom", "auto"], defaultValue: "auto", required: true
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
    sendEvent(name: "coolingSetpoint", value: state.minTemp, isStateChange: true)
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

    def FLOOR_ATTRIBUTES = [
        parent.getConstant("ATTR_ROOM_TEMP_DISPLAY"), parent.getConstant("ATTR_WATTAGE"), 
        parent.getConstant("ATTR_GFCI_STATUS"), parent.getConstant("ATTR_GFCI_ALERT"), 
        parent.getConstant("ATTR_FLOOR_MODE"), parent.getConstant("ATTR_FLOOR_AUX"), 
        parent.getConstant("ATTR_FLOOR_OUTPUT2"), parent.getConstant("ATTR_FLOOR_AIR_LIMIT"), 
        parent.getConstant("ATTR_FLOOR_SENSOR"), parent.getConstant("ATTR_FLOOR_MAX"), 
        parent.getConstant("ATTR_FLOOR_MIN"), parent.getConstant("ATTR_KEYPAD"), 
        parent.getConstant("ATTR_BACKLIGHT"), parent.getConstant("ATTR_SYSTEM_MODE"), 
        parent.getConstant("ATTR_CYCLE"), parent.getConstant("ATTR_DISPLAY2"), 
        parent.getConstant("ATTR_RSSI")
    ]

    Utils.toLogger("debug", "Requesting attributes from parent app for ${device.label}: ${getUpdateAttributes() + FLOOR_ATTRIBUTES}")

    def start = now()

    try {
        def deviceData = parent.getDeviceAttributes(device.deviceNetworkId, getUpdateAttributes() + FLOOR_ATTRIBUTES)

        if (deviceData == null) {
            Utils.toLogger("error", "Failed to update device ${device.label}: deviceData is null. Possible network issue or API failure.")
            return
        }

        def elapsed = (now() - start) / 1000
        Utils.toLogger("debug", "Updating ${device.label} (${elapsed} sec): ${deviceData}")

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
                state.tempDisplayValue = deviceData[parent.getConstant("ATTR_ROOM_TEMP_DISPLAY")]
                state.display2 = deviceData[parent.getConstant("ATTR_DISPLAY2")]
                state.heatLevel = deviceData[parent.getConstant("ATTR_OUTPUT_PERCENT_DISPLAY")]
                state.keypad = deviceData[parent.getConstant("ATTR_KEYPAD")]
                state.backlight = deviceData[parent.getConstant("ATTR_BACKLIGHT")]
                state.operationMode = deviceData[parent.getConstant("ATTR_SYSTEM_MODE")]
                state.wattage = deviceData[parent.getConstant("ATTR_WATTAGE")]
                state.gfciStatus = deviceData[parent.getConstant("ATTR_GFCI_STATUS")]
                state.floorMode = deviceData[parent.getConstant("ATTR_FLOOR_MODE")]
                state.emHeat = deviceData[parent.getConstant("ATTR_FLOOR_AUX")]
                
                if (deviceData.containsKey(parent.getConstant("ATTR_CYCLE"))) {
                    state.cycleLength = deviceData[parent.getConstant("ATTR_CYCLE")]
                }
                if (deviceData.containsKey(parent.getConstant("ATTR_RSSI"))) {
                    state.rssi = deviceData[parent.getConstant("ATTR_RSSI")]
                }
                if (deviceData.containsKey(parent.getConstant("ATTR_FLOOR_AIR_LIMIT"))) {
                    state.floorAirLimit = deviceData[parent.getConstant("ATTR_FLOOR_AIR_LIMIT")]["value"]
                    state.floorAirLimitStatus = deviceData[parent.getConstant("ATTR_FLOOR_AIR_LIMIT")]["status"]
                }
                if (deviceData.containsKey(parent.getConstant("ATTR_FLOOR_MAX"))) {
                    state.floorMax = deviceData[parent.getConstant("ATTR_FLOOR_MAX")]["value"]
                    state.floorMaxStatus = deviceData[parent.getConstant("ATTR_FLOOR_MAX")]["status"]
                }
                if (deviceData.containsKey(parent.getConstant("ATTR_FLOOR_MIN"))) {
                    state.floorMin = deviceData[parent.getConstant("ATTR_FLOOR_MIN")]["value"]
                    state.floorMinStatus = deviceData[parent.getConstant("ATTR_FLOOR_MIN")]["status"]
                }
                if (deviceData.containsKey(parent.getConstant("ATTR_FLOOR_OUTPUT2"))) {
                    state.load2Status = deviceData[parent.getConstant("ATTR_FLOOR_OUTPUT2")]["status"]
                    state.load2 = (state.load2Status == "on") ? deviceData[parent.getConstant("ATTR_FLOOR_OUTPUT2")]["value"] : 0
                }
                state.gfciAlert = deviceData[parent.getConstant("ATTR_GFCI_ALERT")]
                
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

    //if (state.sku != "FLP55" && state.sku != "True Comfort") {
        //doStat(start)
    //}
    //getSensorErrorCode(start)
}
    
def extraStateAttributes() {
    def data = [:]
    data << [
        "wattage": state.wattage,
        "gfci_status": state[parent.getConstant("ATTR_GFCI_STATUS")],
        "gfci_alert": state[parent.getConstant("ATTR_GFCI_ALERT")],
        "sensor_mode": state[parent.getConstant("ATTR_FLOOR_MODE")],
        "auxiliary_heat": state[parent.getConstant("ATTR_FLOOR_AUX")],
        "auxiliary_status": state[parent.getConstant("ATTR_FLOOR_OUTPUT2")]["status"],
        "auxiliary_load": state[parent.getConstant("ATTR_FLOOR_OUTPUT2")]["value"],
        "floor_setpoint_max": state[parent.getConstant("ATTR_FLOOR_MAX")],
        "floor_setpoint_low": state[parent.getConstant("ATTR_FLOOR_MIN")],
        "floor_air_limit": state[parent.getConstant("ATTR_FLOOR_AIR_LIMIT")],
        "floor_sensor_type": state[parent.getConstant("ATTR_FLOOR_SENSOR")],
        "load_watt": state.wattage,
        "error_code": state.errorCode,
        "heat_level": state.heatLevel,
        "pi_heating_demand": state.heatLevel,
        "cycle_length": state[parent.getConstant("ATTR_CYCLE")],
        "temp_display_value": state[parent.getConstant("ATTR_ROOM_TEMP_DISPLAY")],
        "second_display": state[parent.getConstant("ATTR_DISPLAY2")],
        "keypad": state[parent.getConstant("ATTR_KEYPAD")],
        "backlight": state[parent.getConstant("ATTR_BACKLIGHT")],
        "time_format": state.timeFormat,
        "temperature_format": state.temperatureFormat,
        "setpoint_max": state.maxTemp,
        "setpoint_min": state.minTemp,
        "eco_status": state.drStatusActive,
        "eco_optOut": state.drStatusOptout,
        "eco_setpoint": state.drStatusSetpoint,
        "eco_power_relative": state.drStatusRel,
        "eco_power_absolute": state.drStatusAbs,
        "eco_setpoint_status": state.drSetpointStatus,
        "eco_setpoint_delta": state.drSetpointValue,
        "hourly_kwh_count": state.hourEnergyKwhCount,
        "daily_kwh_count": state.todayEnergyKwhCount,
        "monthly_kwh_count": state.monthEnergyKwhCount,
        "hourly_kwh": state.hourKwh,
        "daily_kwh": state.todayKwh,
        "monthly_kwh": state.monthKwh,
        "rssi": state[parent.getConstant("ATTR_RSSI")],
        "sku": state.sku,
        "device_model": state.deviceModel.toString(),
        "device_model_cfg": state.deviceModelCfg,
        "firmware": state.firmware,
        "activation": state.activ,
        "id": device.getDeviceNetworkId()
    ]
    return data
}

def updateAttributes(data) {
    Utils.toLogger("debug", "Updated attributes: ${data}")
    try {
        updateInfo()
    } catch (Exception e) {
        Utils.toLogger("error", "Error updating attributes: ${e.message}")
    }
}

def setBacklightOn() {
    def deviceId = device.deviceNetworkId
    def deviceType = device.getDataValue("deviceType") ?: "zigbee"
    setBacklight(deviceId, "on", deviceType)
    Utils.toLogger("debug", "Backlight set to ON for device type ${deviceType}")
}

def setBacklightOff() {
    def deviceId = device.deviceNetworkId
    def deviceType = device.getDataValue("deviceType") ?: "zigbee"
    setBacklight(deviceId, "auto", deviceType)
    Utils.toLogger("debug", "Backlight set to OFF (Auto) for device type ${deviceType}")
}

def applyBacklightPreference() {
    def deviceId = device.deviceNetworkId
    def deviceType = device.getDataValue("deviceType") ?: "zigbee"
    def level = settings.backlightPreference ?: "auto"
    
    setBacklight(deviceId, level, deviceType)
    Utils.toLogger("debug", "Applied backlight preference: ${level} for device type ${deviceType}")
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

def setSecondDisplay(deviceId, display) {
    def displayName = (display == "outsideTemperature") ? "Outside" : "Setpoint"
    def data = [(parent.getConstant("ATTR_DISPLAY2")): display]
    Utils.toLogger("debug", "display.data = ${data}")
    parent.setDeviceAttributes(deviceId, data)
}

def setBacklight(deviceId, level, deviceType) {
    def levelCommand
    def levelName
    
    if (level == "on") {
        levelCommand = (deviceType == "wifi") ? "alwaysOn" : "always"
        levelName = "On"
    } else if (level == "bedroom") {
        levelCommand = "bedroom"
        levelName = "bedroom"
    } else {
        levelCommand = (deviceType == "wifi") ? "onUserAction" : "onActive"
        levelName = "Auto"
    }
    
    def data = [(parent.getConstant("ATTR_BACKLIGHT")): levelCommand]
    Utils.toLogger("debug", "backlight.data = ${data}")
    parent.setDeviceAttributes(deviceId, data)
}

def setKeypadLock(deviceId, lock, isWifi) {
    def lockCommand = lock
    
    if (lock == "locked" && isWifi) {
        lockCommand = "lock"
    } else if (lock == "partiallyLocked" && isWifi) {
        lockCommand = "partialLock"
    } else if (isWifi) {
        lockCommand = "unlock"
    }
    
    def data = [(parent.getConstant("ATTR_KEYPAD")): lockCommand]
    Utils.toLogger("debug", "lock.data = ${data}")
    parent.setDeviceAttributes(deviceId, data)
}

def setTimeFormat(deviceId, timeFormat) {
    def formatCommand = (timeFormat == 12) ? "12h" : "24h"
    def data = [(parent.getConstant("ATTR_TIME")): formatCommand]
    Utils.toLogger("debug", "time.data = ${data}")
    parent.setDeviceAttributes(deviceId, data)
}

def setTemperatureFormat(deviceId, tempFormat) {
    def data = [(parent.getConstant("ATTR_TEMP")): tempFormat]
    Utils.toLogger("debug", "temperature.data = ${data}")
    parent.setDeviceAttributes(deviceId, data)
}

def setAirFloorMode(deviceId, mode) {
    def data = [(parent.getConstant("ATTR_FLOOR_MODE")): mode]
    Utils.toLogger("debug", "floor_mode.data = ${data}")
    parent.setDeviceAttributes(deviceId, data)
}

def setSetpointMax(deviceId, temp) {
    def data = [(parent.getConstant("ATTR_ROOM_SETPOINT_MAX")): temp]
    Utils.toLogger("debug", "setpointMax.data = ${data}")
    parent.setDeviceAttributes(deviceId, data)
}

def setSetpointMin(deviceId, temp) {
    def data = [(parent.getConstant("ATTR_ROOM_SETPOINT_MIN")): temp]
    Utils.toLogger("debug", "setpointMin.data = ${data}")
    parent.setDeviceAttributes(deviceId, data)
}

def setCoolSetpointMax(deviceId, temp) {
    def data = [(parent.getConstant("ATTR_COOL_SETPOINT_MAX")): temp]
    Utils.toLogger("debug", "coolSetpointMax.data = ${data}")
    parent.setDeviceAttributes(deviceId, data)
}

def setCoolSetpointMin(deviceId, temp) {
    def data = [(parent.getConstant("ATTR_COOL_SETPOINT_MIN")): temp]
    Utils.toLogger("debug", "coolSetpointMin.data = ${data}")
    parent.setDeviceAttributes(deviceId, data)
}

def setFloorAirLimit(deviceId, temp) {
    def status = (temp == 0) ? "off" : "on"
    def data = [(parent.getConstant("ATTR_FLOOR_AIR_LIMIT")): ["status": status, "value": temp]]
    Utils.toLogger("debug", "floor_air_limit.data = ${data}")
    parent.setDeviceAttributes(deviceId, data)
}

def setEarlyStart(deviceId, start) {
    def data = [(parent.getConstant("ATTR_EARLY_START")): start]
    Utils.toLogger("debug", "early_start.data = ${data}")
    parent.setDeviceAttributes(deviceId, data)
}

def setHvacDrOptions(deviceId, dr, optout, setpoint) {
    def data = [(parent.getConstant("ATTR_DRSTATUS")): ["drActive": dr, "optOut": optout, "setpoint": setpoint]]
    Utils.toLogger("debug", "hvac.DR.options = ${data}")
    parent.setDeviceAttributes(deviceId, data)
}

def setHvacDrSetpoint(deviceId, status, val) {
    def data = [(parent.getConstant("ATTR_DRSETPOINT")): ["status": status, "value": val]]
    Utils.toLogger("debug", "hvac.DR.setpoint = ${data}")
    parent.setDeviceAttributes(deviceId, data)
}

def setHvacMode(deviceId, hvacMode) {
    def data = [:]
    
    if (hvacMode == "off") {
        data[parent.getConstant("ATTR_SYSTEM_MODE")] = "off"
    } else if (hvacMode in ["heat", "manual"]) {
        data[parent.getConstant("ATTR_SYSTEM_MODE")] = hvacMode
    } else if (hvacMode == "auto") {
        data[parent.getConstant("ATTR_SYSTEM_MODE")] = "auto"
    } else if (hvacMode == "autoBypass") {
        data[parent.getConstant("ATTR_SYSTEM_MODE")] = "autoBypass"
    } else {
        Utils.toLogger("error", "Unable to set HVAC mode: ${hvacMode}")
        return
    }
    
    Utils.toLogger("debug", "hvacMode.data = ${data}")
    parent.setDeviceAttributes(deviceId, data)
}

def setPresetMode(deviceId, presetMode) {
    def data = [:]
    
    if (presetMode == "away") {
        data[parent.getConstant("ATTR_OCCUPANCY")] = "away"
    } else if (presetMode == "home") {
        data[parent.getConstant("ATTR_OCCUPANCY")] = "home"
    } else if (presetMode == "none") {
        setHvacMode(deviceId, getCurrentHvacMode())
        return
    } else {
        Utils.toLogger("error", "Unable to set preset mode: ${presetMode}")
        return
    }
    
    Utils.toLogger("debug", "presetMode.data = ${data}")
    parent.setDeviceAttributes(deviceId, data)
}

def turnEmHeatOn(deviceId, type, cycleLength) {
    def data = [:]
    
    if (type == "lowVoltage") {
        data[parent.getConstant("ATTR_CYCLE_OUTPUT2")] = ["status": "on", "value": cycleLength]
    } else if (type == "wifi") {
        data[parent.getConstant("ATTR_AUX_CYCLE")] = cycleLength
    } else {
        data[parent.getConstant("ATTR_FLOOR_AUX")] = "slave"
    }
    
    Utils.toLogger("debug", "emHeatOn.data = ${data}")
    parent.setDeviceAttributes(deviceId, data)
}

def turnEmHeatOff(deviceId, type) {
    def data = [:]
    
    if (type == "lowVoltage") {
        data[parent.getConstant("ATTR_CYCLE_OUTPUT2")] = ["status": "off"]
    } else if (type == "wifi") {
        data[parent.getConstant("ATTR_AUX_CYCLE")] = 0
    } else {
        data[parent.getConstant("ATTR_FLOOR_AUX")] = "off"
    }
    
    Utils.toLogger("debug", "emHeatOff.data = ${data}")
    parent.setDeviceAttributes(deviceId, data)
}

def setAuxiliaryLoad(deviceId, status, load) {
    def data = [(parent.getConstant("ATTR_FLOOR_OUTPUT2")): ["status": status, "value": load]]
    Utils.toLogger("debug", "auxiliaryLoad.data = ${data}")
    parent.setDeviceAttributes(deviceId, data)
}

def setCycleOutput(deviceId, cycleLength) {
    def data = [(parent.getConstant("ATTR_CYCLE")): cycleLength]
    Utils.toLogger("debug", "cycleOutput.data = ${data}")
    parent.setDeviceAttributes(deviceId, data)
}

def setPumpProtection(deviceId, status) {
    def data = [(parent.getConstant("ATTR_PUMP_PROTEC")): status]
    Utils.toLogger("debug", "pumpProtection.data = ${data}")
    parent.setDeviceAttributes(deviceId, data)
}

def setSensorType(deviceId, sensorType) {
    def data = [(parent.getConstant("ATTR_FLOOR_SENSOR")): sensorType]
    Utils.toLogger("debug", "sensorType.data = ${data}")
    parent.setDeviceAttributes(deviceId, data)
}

def setFloorLimit(deviceId, temp, limitType, isWifi) {
    def adjustedTemp = temp
    if (limitType == "low" && temp > 0 && temp < 5) {
        adjustedTemp = 5
    } else if (limitType == "high" && temp > 0 && temp < 7) {
        adjustedTemp = 7
    }
    
    def data = [(parent.getConstant(limitType == "low" ? "ATTR_FLOOR_MIN" : "ATTR_FLOOR_MAX")): ["value": adjustedTemp, "status": "on"]]
    Utils.toLogger("debug", "floorLimit.data = ${data}")
    parent.setDeviceAttributes(deviceId, data)
}

def setHeatPumpOperationLimit(deviceId, temp) {
    def data = [(parent.getConstant("ATTR_BALANCE_PT")): temp]
    Utils.toLogger("debug", "heatPumpOperationLimit.data = ${data}")
    parent.setDeviceAttributes(deviceId, data)
}

def setHeatLockoutTemperature(deviceId, temp) {
    def data = [(parent.getConstant("ATTR_HEAT_LOCK_TEMP")): temp]
    Utils.toLogger("debug", "heatLockoutTemperature.data = ${data}")
    parent.setDeviceAttributes(deviceId, data)
}

def setCoolLockoutTemperature(deviceId, temp) {
    def data = [(parent.getConstant("ATTR_COOL_LOCK_TEMP")): temp]
    Utils.toLogger("debug", "coolLockoutTemperature.data = ${data}")
    parent.setDeviceAttributes(deviceId, data)
}

def setDisplayConfig(deviceId, display) {
    def data = [(parent.getConstant("ATTR_DISPLAY_CONF")): display]
    Utils.toLogger("debug", "displayConfig.data = ${data}")
    parent.setDeviceAttributes(deviceId, data)
}

def setSoundConfig(deviceId, sound) {
    def data = [(parent.getConstant("ATTR_SOUND_CONF")): sound]
    Utils.toLogger("debug", "soundConfig.data = ${data}")
    parent.setDeviceAttributes(deviceId, data)
}

def setHcSecondDisplay(deviceId, display) {
    def data = [(parent.getConstant("ATTR_DISPLAY2")): display]
    Utils.toLogger("debug", "hcSecondDisplay.data = ${data}")
    parent.setDeviceAttributes(deviceId, data)
}

def setLanguage(deviceId, lang) {
    def data = [(parent.getConstant("ATTR_LANGUAGE")): lang]
    Utils.toLogger("debug", "language.data = ${data}")
    parent.setDeviceAttributes(deviceId, data)
}

def setAuxHeatMinTimeOn(deviceId, time) {
    def data = [(parent.getConstant("ATTR_AUX_HEAT_TIMEON")): time]
    Utils.toLogger("debug", "auxHeatMinTimeOn.data = ${data}")
    parent.setDeviceAttributes(deviceId, data)
}

def setCoolMinTime(deviceId, time, state) {
    def key = state == "on" ? parent.getConstant("ATTR_COOL_MIN_TIME_ON") : parent.getConstant("ATTR_COOL_MIN_TIME_OFF")
    def data = [(key): time]
    Utils.toLogger("debug", "coolMinTime.data = ${data}")
    parent.setDeviceAttributes(deviceId, data)
}

/*def setHvacMode(deviceId, hvacMode) {
    def data = [:]
    
    if (hvacMode == "off") {
        data[parent.getConstant("ATTR_SYSTEM_MODE")] = "off"
    } else if (hvacMode in ["heat", "manual"]) {
        data[parent.getConstant("ATTR_SYSTEM_MODE")] = hvacMode
    } else if (hvacMode == "auto") {
        data[parent.getConstant("ATTR_SYSTEM_MODE")] = "auto"
    } else if (hvacMode == "autoBypass") {
        data[parent.getConstant("ATTR_SYSTEM_MODE")] = "autoBypass"
    } else {
        Utils.toLogger("error", "Unable to set HVAC mode: ${hvacMode}"
        return
    }
    
    Utils.toLogger("debug", "hvacMode.data = ${data}")
    parent.setDeviceAttributes(deviceId, data)
}

def setPresetMode(deviceId, presetMode) {
    def data = [:]
    
    if (presetMode == "away") {
        data[parent.getConstant("ATTR_OCCUPANCY")] = "away"
    } else if (presetMode == "home") {
        data[parent.getConstant("ATTR_OCCUPANCY")] = "home"
    } else if (presetMode == "none") {
        setHvacMode(deviceId, getCurrentHvacMode())
        return
    } else {
        Utils.toLogger("error", "Unable to set preset mode: ${presetMode}")
        return
    }
    
    Utils.toLogger("debug", "presetMode.data = ${data}")
    parent.setDeviceAttributes(deviceId, data)
}*/

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
