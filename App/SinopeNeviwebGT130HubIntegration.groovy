import groovy.json.JsonSlurper
import groovy.transform.Field

/*Support for Neviweb thermostat connected to GT130 ZigBee.
model 1123 = thermostat TH1123ZB 3000W
model 300 = thermostat TH1123ZB-G2 3000W
model 1124 = thermostat TH1124ZB 4000W
model 300 = thermostat TH1124ZB-G2 4000W
model 737 = thermostat TH1300ZB 3600W (floor)
model 737 = thermostat TH1320ZB-04 (floor)
model 7373 = thermostat TH1500ZB double pole thermostat
model 7372 = thermostat TH1400ZB low voltage
model 7372 = thermostat TH1420ZB-01 Nordik low voltage radiant hydroponic floor thermostat
model 1124 = thermostat OTH4000-ZB Ouellet
model 737 = thermostat OTH3600-GA-ZB Ouellet
model 1512 = Thermostat TH1134ZB-HC for heating/cooling interlocking

Support for Neviweb wifi thermostats
model 1510 = thermostat TH1123WF 3000W (wifi)
model 1510 = thermostat TH1124WF 4000W (wifi)
model 1510 = thermostat TH1133WF 3000W (wifi)
model 1510 = thermostat TH1133CR Sinopé Evo 3000W (wifi)
model 738 = thermostat TH1300WF 3600W, TH1325WF, TH1310WF, SRM40, True Comfort (wifi floor)
model 739 = thermostat TH1400WF low voltage (wifi)
model 742 = thermostat TH1500WF double pole thermostat (wifi)
model 6727 = thermostat TH6500WF heat/cool (wifi)
model 6730 = thermostat TH6250WF heat/cool (wifi)

Support for Flextherm wifi thermostat
model 738 = Thermostat Flextherm concerto connect FLP55 (wifi floor), (sku: FLP55), no energy stats

Support for heat pump interfaces
model 6810 = HP6000ZB-GE for Ouellet heat pump with Gree connector
model 6811 = HP6000ZB-MA for Ouellet Convectair heat pump with Midea connector
model 6812 = HP6000ZB-HS for Hisense, Haxxair and Zephyr heat pump*/

// Global definition of device models
@Field static final def DEVICE_MODELS = [
    "low"         : [7372],
    "low_wifi"    : [739],
    "floor"       : [737],
    "wifi_floor"  : [738],
    "wifi"        : [1510, 742],
    "heat"        : [1123, 1124],
    "double"      : [7373],
    "heat_g2"     : [300],
    "hc"          : [1512],
    "heat_pump"   : [6810, 6811, 6812],
    "heat_cool"   : [6727]
]

@Field static final def DEVICE_MODEL_DRIVERS = [
    "low"         : "Neviweb Low Thermostat Driver",
    "low_wifi"    : "Neviweb Low WiFi Thermostat Driver",
    "floor"       : "Neviweb Floor Thermostat Driver",
    "wifi_floor"  : "Neviweb WiFi Floor Thermostat Driver",
    "wifi"        : "Neviweb WiFi Thermostat Driver",
    "heat"        : "Neviweb Heat Thermostat Driver",
    "double"      : "Neviweb Double Thermostat Driver",
    "heat_g2"     : "Neviweb G2 Heat Thermostat Driver",
    "hc"          : "Neviweb Heat and Cool Thermostat Driver",
    "heat_pump"   : "Neviweb Heat Pump Thermostat Driver",
    "heat_cool"   : "Neviweb Heat Cool Thermostat Driver"
]

@Field static final String DOMAIN = "neviweb130"

// Configuration
@Field static final String CONF_NETWORK = "network"
@Field static final String CONF_NETWORK2 = "network2"
@Field static final String CONF_NETWORK3 = "network3"
@Field static final String CONF_HOMEKIT_MODE = "homekit_mode"
@Field static final String CONF_STAT_INTERVAL = "stat_interval"
@Field static final String CONF_NOTIFY = "notify"

// Thermostat Attributes
@Field final float SINOPE_MIN_TEMPERATURE_CELSIUS = 5.0
@Field final float SINOPE_MAX_TEMPERATURE_CELSIUS = 36.0
@Field final float SINOPE_MIN_TEMPERATURE_FAHRENHEIT = 41.0
@Field final float SINOPE_MAX_TEMPERATURE_FAHRENHEIT = 86.0
@Field static final String ATTR_INTENSITY = "intensity"
@Field static final String ATTR_INTENSITY_MIN = "intensityMin"
@Field static final String ATTR_WATTAGE = "loadConnected"
@Field static final String ATTR_WATTAGE_INSTANT = "wattageInstant"
@Field static final String ATTR_ROOM_SETPOINT = "roomSetpoint"
@Field static final String ATTR_ROOM_SETPOINT_AWAY = "roomSetpointAway"
@Field static final String ATTR_ROOM_TEMPERATURE = "roomTemperature"
@Field static final String ATTR_ROOM_SETPOINT_MIN = "roomSetpointMin"
@Field static final String ATTR_ROOM_SETPOINT_MAX = "roomSetpointMax"
@Field static final String ATTR_OUTPUT_PERCENT_DISPLAY = "outputPercentDisplay"
@Field static final String ATTR_ROOM_TEMP_DISPLAY = "roomTemperatureDisplay"
@Field static final String ATTR_SYSTEM_MODE = "systemMode"
@Field static final String ATTR_BACKLIGHT = "backlightAdaptive"
@Field static final String ATTR_BACKLIGHT_AUTO_DIM = "backlightAutoDim"
@Field static final String ATTR_KEYPAD = "lockKeypad"
@Field static final String ATTR_RSSI = "rssi"
@Field static final String ATTR_DISPLAY2 = "config2ndDisplay"
@Field static final String ATTR_TIME = "timeFormat"
@Field static final String ATTR_TEMP = "temperatureFormat"
@Field static final String ATTR_DRSTATUS = "drStatus"
@Field static final String ATTR_DRSETPOINT = "drSetpoint"
@Field static final String ATTR_DRACTIVE = "drActive"
@Field static final String ATTR_OPTOUT = "optOut"
@Field static final String ATTR_SETPOINT = "setpoint"
@Field static final String ATTR_CYCLE = "cycleLength"
@Field static final String ATTR_GFCI_STATUS = "gfciStatus"
@Field static final String ATTR_GFCI_ALERT = "alertGfci"
@Field static final String ATTR_FLOOR_AUX = "auxHeatConfig"
@Field static final String ATTR_FLOOR_OUTPUT2 = "loadWattOutput2" //#status on/off, value=xx
@Field static final String ATTR_FLOOR_MODE = "airFloorMode"
@Field static final String ATTR_FLOOR_AIR_LIMIT = "floorAirLimit"
@Field static final String ATTR_FLOOR_SENSOR = "floorSensor"
@Field static final String ATTR_FLOOR_MAX = "floorMax"
@Field static final String ATTR_FLOOR_MIN = "floorMin"

// Fan & Cooling
@Field static final String ATTR_FAN_SPEED = "fanSpeed"
@Field static final String ATTR_FAN_SWING_VERT = "fanSwingVertical"
@Field static final String ATTR_FAN_SWING_HORIZ = "fanSwingHorizontal"
@Field static final String ATTR_COOL_SETPOINT = "coolSetpoint"
@Field static final String ATTR_COOL_SETPOINT_MIN = "coolSetpointMin"
@Field static final String ATTR_COOL_SETPOINT_MAX = "coolSetpointMax"
@Field static final String ATTR_COOL_MIN_TIME_ON = "coolMinTimeOn"
@Field static final String ATTR_COOL_MIN_TIME_OFF = "coolMinTimeOff"

// Heat & Protection
@Field static final String ATTR_HEAT_LOCK_TEMP = "heatLockoutTemperature"
@Field static final String ATTR_COOL_LOCK_TEMP = "coolLockoutTemperature"
@Field static final String ATTR_HEAT_INSTALL_TYPE = "HeatInstallationType"
@Field static final String ATTR_PUMP_PROTEC = "pumpProtection"
@Field static final String ATTR_PUMP_PROTEC_DURATION = "pumpProtectDuration"
@Field static final String ATTR_PUMP_PROTEC_PERIOD = "pumpProtectPeriod"
@Field static final String ATTR_AUX_HEAT_TIMEON = "auxHeatMinTimeOn"
@Field static final String ATTR_AUX_HEAT_START_DELAY = "auxHeatStartDelay"

// Modes
@Field static final String MODE_AUTO = "auto"
@Field static final String MODE_AUTO_BYPASS = "autoBypass"
@Field static final String MODE_MANUAL = "manual"
@Field static final String MODE_AWAY = "away"
@Field static final String MODE_HOME = "home"
@Field static final String MODE_OFF = "off"
@Field static final String MODE_EM_HEAT = "emergencyHeat"

@Field Utils = Utils_create();
@Field List<String> LOG_LEVELS = ["error", "warn", "info", "debug", "trace"]
@Field String DEFAULT_LOG_LEVEL = LOG_LEVELS[1]
def driverVer() { return "0.3" }

definition(
    name: 'Sinope Neviweb GT130 Hub Integration',
    namespace: 'rferrazguimaraes',
    author: 'Rangner Ferraz Guimaraes',
    description: 'Integrates with Sinope GT130 Hub',
    category: 'My Apps',
    iconUrl: "",
    iconX2Url: "",
    singleInstance: true
)

preferences {
    page(name: "mainPage")
}

def mainPage() {
    dynamicPage(name: "mainPage", title: "Neviweb Integration Setup", install: true, uninstall: true) {
        section("Neviweb Account") {
            input name: "username", type: "text", title: "Username", required: true
            input name: "password", type: "password", title: "Password", required: true
            input "logLevel", "enum", title: "Log Level", options: LOG_LEVELS, defaultValue: DEFAULT_LOG_LEVEL, required: false
        }
        /*section("Polling Interval") {
            input name: "pollInterval", type: "enum", title: "Polling Interval (minutes)", 
                  options: ["5", "10", "15", "30"], defaultValue: "5", required: true
        }*/
        section("Discovered Devices") {
            paragraph "Devices will be listed here after a successful login and polling."
            if (state.gatewayData) {
                state.gatewayData.each { device ->
                    paragraph "${device.name} (ID: ${device.id})"
                }
            } else if (state.gatewayData2) {
                state.gatewayData2.each { device ->
                    paragraph "${device.name} (ID: ${device.id})"
                }
            } else {
                paragraph "No devices discovered yet. Save settings to begin polling."
            }
        }
    }
}

def installed() {
    Utils.toLogger("debug", "Installed Neviweb Integration")
    initialize()
}

def updated() {
    Utils.toLogger("debug", "Updated Neviweb Integration")
    unschedule()
    initialize()
}

def initialize() {
    Utils.toLogger("debug", "Initializing Neviweb Integration")
    try {
        login()
        setupThermostats()
        //schedulePolling()
        pollDevices()
    } catch (Exception e) {
        Utils.toLogger("error", "Initialization failed: ${e.message}")
    }
}

def login() {
    Utils.toLogger("debug", "Attempting to log in...")
    def loginUrl = "https://neviweb.com/api/login"
    def postBody = [
        username: settings.username,
        password: settings.password,
        interface: "neviweb",
        stayConnected: 1
    ]

    try {
        httpPost([uri: loginUrl, body: postBody]) { resp ->
            if (resp.status == 200) {
                Utils.toLogger("debug", "Login successful. Data: ${resp.data}")
                state.sessionId = resp.data?.session?.toString()
                if (!state.sessionId) {
                    Utils.toLogger("error", "Session ID is null. Login first.")
                    return
                }

                state.accountId = resp.data?.account?.id?.toString()
                if (!state.accountId) {
                    Utils.toLogger("error", "Account ID is null. Login first.")
                    return
                }
                
                state.headers = [
                    "Session-Id": state.sessionId
                ]
                
                state.cookies = resp.headers['Set-Cookie']
                state.cookies1 = resp?.headers?.'Set-Cookie'?.toString()
                state.cookies2 = resp?.headers?.'Set-Cookie'?.split(';')?.getAt(0)
                Utils.toLogger("debug", "Login successful. Session ID: ${state.sessionId}, Account ID: ${state.accountId}")
                getNetwork()
                fetchGateways()
                getGatewayData()
                state.lastLoginTime = now()
            } else {
                Utils.toLogger("error", "Login failed. Status: ${resp.status}")
            }
        }
    } catch (Exception e) {
        Utils.toLogger("error", "Error during login: ${e.message}")
    }
}

def schedulePolling() {
    def interval = settings.pollInterval.toInteger()
    schedule("0 */${interval} * * * ?", pollDevices)
}

def getNetwork() {
    if (!state.accountId) {
        Utils.toLogger("error", "Account ID is null. Login first.")
        initialize()
        return
    }

    def locationsUrl = "https://neviweb.com/api/locations?account\$id=${state.accountId}"
    Utils.toLogger("debug", "Fetching networks from: ${locationsUrl}")

    try {
        httpGet([uri: locationsUrl, headers: state.headers]) { resp ->
            if (resp.status == 200) {
                def networks = resp.data
                Utils.toLogger("debug", "Found ${networks.size()} networks: ${networks}")

                // Default to first and second networks if user preferences are not provided
                if (!settings.primaryNetwork && !settings.secondaryNetwork) {
                    state.primaryNetworkId = networks[0]?.id
                    state.primaryNetworkName = networks[0]?.name
                    state.primaryOccupancyMode = networks[0]?.mode
                    Utils.toLogger("debug", "Using default primary network: ${state.primaryNetworkName}")

                    if (networks.size() > 1) {
                        state.secondaryNetworkId = networks[1]?.id
                        state.secondaryNetworkName = networks[1]?.name
                        Utils.toLogger("debug", "Using default secondary network: ${state.secondaryNetworkName}")
                    }
                } else {
                    // Match user-specified network names
                    networks.each { network ->
                        if (network.name == settings.primaryNetwork) {
                            state.primaryNetworkId = network.id
                            state.primaryNetworkName = network.name
                            state.primaryOccupancyMode = network.mode
                            Utils.toLogger("debug", "Matched primary network: ${state.primaryNetworkName}")
                        } else if (network.name == settings.secondaryNetwork) {
                            state.secondaryNetworkId = network.id
                            state.secondaryNetworkName = network.name
                            Utils.toLogger("debug", "Matched secondary network: ${state.secondaryNetworkName}")
                        }
                    }
                }

                // Log warnings if no matches found
                if (!state.primaryNetworkId) {
                    Utils.toLogger("warn", "Primary network '${settings.primaryNetwork}' not found. Using default if available.")
                }
                if (!state.secondaryNetworkId && settings.secondaryNetwork) {
                    Utils.toLogger("warn", "Secondary network '${settings.secondaryNetwork}' not found.")
                }
            } else {
                Utils.toLogger("error", "Failed to fetch networks. Status: ${resp.status}")
            }
        }
    } catch (Exception e) {
        Utils.toLogger("error", "Error fetching networks: ${e.message}")
    }
}

def fetchGateways() {
    if (!state.accountId) {
        Utils.toLogger("error", "Account ID is not set. Cannot fetch gateways.")
        return
    }

    def locationsUrl = "https://neviweb.com/api/locations?account\$id=${state.accountId}"
    Utils.toLogger("debug", "Fetching gateways with URL: ${locationsUrl}")

    try {
        httpGet([uri: locationsUrl, headers: state.headers]) { resp ->
            if (resp.status == 200) {
                def locations = resp.data
                if (locations?.size() > 0) {
                    // Set the primary gateway ID
                    state.gatewayId = locations[0]?.id
                    state.gatewayName = locations[0]?.name
                    Utils.toLogger("debug", "Primary Gateway: ${state.gatewayName}, ID: ${state.gatewayId}")

                    // Optionally, handle secondary gateways
                    if (locations.size() > 1) {
                        state.gatewayId2 = locations[1]?.id
                        state.gatewayName2 = locations[1]?.name
                        Utils.toLogger("debug", "Secondary Gateway: ${state.gatewayName2}, ID: ${state.gatewayId2}")
                    }
                } else {
                    Utils.toLogger("warn", "No gateways found for this account.")
                }
            } else {
                Utils.toLogger("error", "Failed to fetch gateways. Status: ${resp.status}")
            }
        }
    } catch (Exception e) {
        Utils.toLogger("error", "Error fetching gateways: ${e.message}")
    }
}

def getGatewayData() {
    Utils.toLogger("debug", "Fetching gateway data...")
    
    if (!state.sessionId || !state.gatewayId) {
        Utils.toLogger("error", "Session ID or Gateway ID is missing. Ensure login and gateway configuration are correct.")
        return
    }

    def gatewayUrl = "https://neviweb.com/api/devices?location\$id=${state.gatewayId}"
    Utils.toLogger("debug", "Gateway URL: ${gatewayUrl}")
    Utils.toLogger("debug", "Session ID: ${state.sessionId}")
    Utils.toLogger("debug", "Headers: ${state.headers}")
    try {
        httpGet([uri: gatewayUrl, headers: state.headers]) { resp ->
            if (resp.status == 200) {
                if (resp.data?.error?.code == "SVCINVREQ") {
                    Utils.toLogger("error", "Invalid request to the gateway. Check Gateway ID or Session ID.")
                    return
                }
                
                state.gatewayData = resp.data
                Utils.toLogger("debug", "Received gateway data: ${state.gatewayData}")
                // Process devices in the gateway data
                state.gatewayData.each { device ->
                    Utils.toLogger("debug", "gatewayData - Device ID: ${device.id}, Name: ${device.name}, Type: ${device.type}")
                    def attributes = getDeviceAttributes(device.id, ["signature"])
                    if (attributes?.signature) {
                        device.signature = attributes.signature
                        Utils.toLogger("debug", "gatewayData - Device ID: ${device.id}, Name: ${device.name}, Type: ${device.type}, Signature Type: ${device.signature.type}")
                    }
                }
            } else {
                Utils.toLogger("error", "Failed to fetch gateway data. Status: ${resp.status}")
            }
        }
    } catch (Exception e) {
        Utils.toLogger("error", "Error fetching gateway data: ${e.message}")
    }

    // If a secondary gateway exists, fetch its data
    if (state.gatewayId2) {
        Utils.toLogger("debug", "Fetching secondary gateway data...")
        def secondaryGatewayUrl = "https://neviweb.com/api/device/${state.gatewayId2}"
        try {
            httpGet([uri: secondaryGatewayUrl, headers: state.headers]) { resp ->
                if (resp.status == 200) {
                    state.gatewayData2 = resp.data
                    Utils.toLogger("debug", "Received secondary gateway data: ${state.gatewayData2}")

                    // Process devices in the secondary gateway data
                    state.gatewayData2.each { device ->
                        Utils.toLogger("debug", "gatewayData2 - Device ID: ${device.id}, Name: ${device.name}, Type: ${device.type}")
                        def attributes = getDeviceAttributes(device.id, ["signature"])
                        if (attributes?.signature) {
                            device.signature = attributes.signature
                            Utils.toLogger("debug", "gatewayData2 - Device ID: ${device.id}, Name: ${device.name}, Type: ${device.type}, Signature Type: ${device.signature.type}")
                        }
                    }
                } else {
                    Utils.toLogger("error", "Failed to fetch secondary gateway data. Status: ${resp.status}")
                }
            }
        } catch (Exception e) {
            Utils.toLogger("error", "Error fetching secondary gateway data: ${e.message}")
        }
    }
}

def getDriverForDevice(deviceId) {
    def deviceModel = getDeviceModelType(deviceId)
    return DEVICE_MODEL_DRIVERS[deviceModel] ?: "No driver found"
}

def getDeviceModelType(deviceId) {
    DEVICE_MODELS.find { type, ids ->
        ids.contains(deviceId)
    }?.key
}

def needsLogin() {
    def sessionExpiryTime = 3600 * 1000 // Example: 1 hour session expiration
    return !state.sessionId || (state.lastLoginTime && now() - state.lastLoginTime > sessionExpiryTime)
}

def pollDevices() {
    Utils.toLogger("debug", "Polling devices...")
    
    if (needsLogin()) { 
        Utils.toLogger("debug", "Session expired or missing. Logging in...")
        login()
    } else {
        Utils.toLogger("debug", "Session still valid. Skipping login.")
    }

    if (state.gatewayData) {
        state.gatewayData.each { device ->
            def childDevice = getChildDevice(device.id.toString())
            if (childDevice) {
                childDevice.updateAttributes(device)
            }
        }
    }

    if (state.gatewayData2) {
        state.gatewayData2.each { device ->
            def childDevice = getChildDevice(device.id.toString())
            if (childDevice) {
                childDevice.updateAttributes(device)
            }
        }
    }
}

def getNeviwebStatus(String location) {
    Utils.toLogger("debug", "Fetching occupancy mode status for location: ${location}")

    if (!state.sessionId) {
        Utils.toLogger("warn", "Session ID is missing. Re-authenticating...")
        login()
    }

    def statusUrl = "https://neviweb.com/api/location/${location}/notifications"
    try {
        def response
        httpGet([uri: statusUrl, headers: state.headers]) { resp ->
            if (resp.status == 200) {
                response = resp.data
                Utils.toLogger("debug", "Received occupancy mode status: ${response}")

                // Handle session expiration
                if (response?.error?.code == "USRSESSEXP") {
                    Utils.toLogger("error", "Session expired. Re-authenticating...")
                    login()
                    return null
                }

                return response
            } else {
                Utils.toLogger("error", "Failed to fetch occupancy mode status for location ${location}. Status: ${resp.status}")
                return null
            }
        }
    } catch (Exception e) {
        Utils.toLogger("error", "Error fetching occupancy mode status for location ${location}: ${e.message}")
        return null
    }
}

def getDeviceStatus(String deviceId) {
    Utils.toLogger("debug", "Fetching status for device ID: ${deviceId}")

    if (!state.sessionId) {
        Utils.toLogger("warn", "Session ID is missing. Re-authenticating...")
        login()
    }

    def statusUrl = "https://neviweb.com/api/device/${deviceId}/status"
    try {
        def response
        httpGet([uri: statusUrl, headers: state.headers]) { resp ->
            if (resp.status == 200) {
                response = resp.data
                Utils.toLogger("debug", "Received device status: ${response}")

                // Handle session expiration
                if (response?.error?.code == "USRSESSEXP") {
                    Utils.toLogger("error", "Session expired. Re-authenticating...")
                    login()
                    return null
                }

                return response
            } else {
                Utils.toLogger("error", "Failed to fetch status for device ${deviceId}. Status: ${resp.status}")
                return null
            }
        }
    } catch (Exception e) {
        Utils.toLogger("error", "Error fetching status for device ${deviceId}: ${e.message}")
        return null
    }
}

def setupThermostats() {
    Utils.toLogger("debug", "Setting up Neviweb thermostats...")

    // Process primary gateway data
    if (state.gatewayData?.size() > 0) {
        Utils.toLogger("debug", "Processing devices from primary gateway...")
        state.gatewayData.each { deviceInfo ->
            if (isSupportedDevice(deviceInfo)) {
                createThermostatChild(deviceInfo, "Gateway 1")
            } else {
                Utils.toLogger("warn", "Unsupported device model in primary gateway: ${deviceInfo.signature?.model}")
            }
        }
    } else {
        Utils.toLogger("warn", "No devices found in primary gateway.")
    }

    // Process secondary gateway data
    if (state.gatewayData2?.size() > 0) {
        Utils.toLogger("debug", "Processing devices from secondary gateway...")
        state.gatewayData2.each { deviceInfo ->
            if (isSupportedDevice(deviceInfo)) {
                createThermostatChild(deviceInfo, "Gateway 2")
            } else {
                Utils.toLogger("warn", "Unsupported device model in secondary gateway: ${deviceInfo.signature?.model}")
            }
        }
    } else if (state.gatewayId2) {
        Utils.toLogger("warn", "No devices found in secondary gateway.")
    }
}

def isSupportedDevice(deviceInfo) {
    def model = deviceInfo.signature?.model

    // Debug logs
    Utils.toLogger("debug", "Checking device. Model: ${model}, Device Info: ${deviceInfo}")    
    Utils.toLogger("debug", "Supported device models: ${DEVICE_MODELS}")

    if (model == null) {
        Utils.toLogger("error", "Device model is null. Device Info: ${deviceInfo}")
        return false
    }

    // Check if the model exists in DEVICE_MODELS\
     // Use any() method to check if model exists in any of the lists in DEVICE_MODELS
    def supported = DEVICE_MODELS.values().any { it.contains(model) }
    Utils.toLogger("debug", "Device model ${model} is supported: ${supported}")
    return supported
}

def createThermostatChild(deviceInfo, gatewayName) {
    Utils.toLogger("debug", "createThermostatChild - deviceInfo:${deviceInfo}")
    def deviceId = deviceInfo.signature.model
    def driverName = getDriverForDevice(deviceId)
    def deviceName = "Thermostat ${deviceInfo.name}"

    def device = getChildDevice(deviceInfo.id.toString())
    if (!device) {
        Utils.toLogger("debug", "Creating device: ${deviceName} with driver: ${driverName}")
        device = addChildDevice("rferrazguimaraes", driverName, deviceInfo.id.toString(), [
            label: deviceName,
            isComponent: true
        ])
    } else {
        Utils.toLogger("debug", "Device ${deviceName} with driver ${driverName} already exists.")
    }
    
    device.setDeviceInfo(deviceInfo)
}

def uninstalled() {
    Utils.toLogger("debug", "Uninstalling Neviweb Integration")
    deleteChildDevices(getChildDevices())
}

def deleteChildDevices(devices) {
    devices.each { device ->
        try {
            deleteChildDevice(device.deviceNetworkId)
        } catch (Exception e) {
            Utils.toLogger("error", "Error deleting child device ${device.deviceNetworkId}: ${e.message}")
        }
    }
}

def getDeviceAttributes(deviceId, attributes) {
    Utils.toLogger("debug", "Fetching attributes for device ID: ${deviceId}, Attributes: ${attributes}")

    if (!state.sessionId) {
        Utils.toLogger("warn", "Session ID is missing. Re-authenticating...")
        login()
    }

    def attributesUrl = "https://neviweb.com/api/device/${deviceId}/attribute?attributes=${attributes.join(',')}"
    Utils.toLogger("debug", "Fetching attributes for device ID: ${deviceId}, attributesUrl: ${attributesUrl}")
    try {
        def response
        httpGet([uri: attributesUrl, contentType: "application/json", headers: state.headers]) { resp ->
            if (resp.status == 200) {
                response = resp.data
                Utils.toLogger("debug", "Received attributes: ${response}")
                Utils.toLogger("debug", "state.headers = ${state.headers}")

                // Check for session expiration
                if (response?.error?.code == "USRSESSEXP") {
                    Utils.toLogger("error", "Session expired. Re-authenticating...")
                    login()
                    return null
                }

                return response
            } else {
                Utils.toLogger("error", "Failed to fetch device attributes. Status: ${resp.status}")
                return null
            }
        }
    } catch (Exception e) {
        Utils.toLogger("error", "Error fetching device attributes: ${e.message}")
        return null
    }
}

def setDeviceAttributes(deviceId, data) {
    Utils.toLogger("debug", "Setting device attributes for device ID: ${deviceId}, Data: ${data}")

    if (!state.sessionId) {
        Utils.toLogger("error", "Session ID is missing. Please log in again.")
        return false
    }

    def url = "https://neviweb.com/api/device/${deviceId}/attribute"
    def maxRetries = 3
    def attempt = 0
    def success = false

    while (attempt < maxRetries && !success) {
        attempt++
        try {
            Utils.toLogger("debug", "API Request Attempt ${attempt}: URL: ${url}, Data: ${data}, state.cookies: ${state.cookies}")
            
            def requestParams = [
                uri     : url,
                contentType: "application/json",
                requestContentType: "application/json",               
                headers : state.headers,
                body    : data,
            ]

            httpPut(requestParams) { resp ->
                Utils.toLogger("debug", "API Response Status: ${resp.status}")
                Utils.toLogger("debug", "API Response Data: ${resp.data}")          

                if (resp.status == 200) {
                    if (resp.data?.error) {
                        Utils.toLogger("warn", "Service error received: ${resp.data.error}. Retrying... (Attempt ${attempt + 1})")
                        pauseExecution(2000) // Wait 2 seconds before retrying
                    } else {
                        Utils.toLogger("debug", "Successfully set attributes for device ${deviceId}.")
                        success = true // ✅ Mark success
                    }
                } else {
                    Utils.toLogger("error", "HTTP error: ${resp.status}. Retrying... (Attempt ${attempt + 1})")
                    pauseExecution(2000)
                }
            }
        } catch (Exception e) {
            Utils.toLogger("error", "Error during API request: ${e.message}. Retrying... (Attempt ${attempt + 1})")
            pauseExecution(2000)
        }
    }

    if (success) {
        return true
    } else {
        Utils.toLogger("error", "Failed to set attributes for device ${deviceId} after ${maxRetries} attempts.")
        return false
    }
}
def setBrightness(deviceId, brightness) {
    def data = [(getConstant("ATTR_INTENSITY")): brightness]
    setDeviceAttributes(deviceId, data)
}

def setOnOff(deviceId, onoff) {
    def data = [(getConstant("ATTR_ONOFF")): onoff]
    setDeviceAttributes(deviceId, data)
}

def setLightOnOff(deviceId, onoff, brightness) {
    def data = [(getConstant("ATTR_ONOFF")): onoff, (getConstant("ATTR_INTENSITY")): brightness]
    setDeviceAttributes(deviceId, data)
}

def setValveOnOff(deviceId, onoff) {
    def data = [(getConstant("ATTR_MOTOR_TARGET")): onoff]
    setDeviceAttributes(deviceId, data)
}

def setMode(deviceId, mode) {
    def data = [(getConstant("ATTR_POWER_MODE")): mode]
    setDeviceAttributes(deviceId, data)
}

def setSetpointMode(deviceId, mode, wifi) {
    def data = wifi ? [(getConstant("ATTR_SETPOINT_MODE")): (mode in ["HEAT", "MANUAL"] ? "MANUAL" : mode)] : [(getConstant("ATTR_SYSTEM_MODE")): mode]
    setDeviceAttributes(deviceId, data)
}

def setTemperature(deviceId, temperature) {
    def data = [(getConstant("ATTR_ROOM_SETPOINT")): temperature]
    setDeviceAttributes(deviceId, data)
}

def setBacklight(deviceId, level, deviceType) {
    def data = deviceType == "wifi" ? [(getConstant("ATTR_BACKLIGHT_AUTO_DIM")): level] : [(getConstant("ATTR_BACKLIGHT")): level]
    Utils.toLogger("debug", "backlight.data = ${data}")
    setDeviceAttributes(deviceId, data)
}

def setKeypadLock(deviceId, lock, wifi) {
    def data = wifi ? [(getConstant("ATTR_WIFI_KEYPAD")): lock] : [(getConstant("ATTR_KEYPAD")): lock]
    Utils.toLogger("debug", "lock.data = ${data}")
    setDeviceAttributes(deviceId, data)
}

def setTimer(deviceId, time) {
    def data = [(getConstant("ATTR_TIMER")): time]
    Utils.toLogger("debug", "timer.data = ${data}")
    setDeviceAttributes(deviceId, data)
}

def setTimer2(deviceId, time) {
    def data = [(getConstant("ATTR_TIMER2")): time]
    Utils.toLogger("debug", "timer2.data = ${data}")
    setDeviceAttributes(deviceId, data)
}

def setLanguage(deviceId, lang) {
    def data = [(getConstant("ATTR_LANGUAGE")): lang]
    Utils.toLogger("debug", "language.data = ${data}")
    setDeviceAttributes(deviceId, data)
}

def setCoolSetpointMin(deviceId, temp) {
    def data = [(getConstant("ATTR_COOL_SETPOINT_MIN")): temp]
    Utils.toLogger("debug", "CoolsetpointMin.data = ${data}")
    setDeviceAttributes(deviceId, data)
}

def setCoolSetpointMax(deviceId, temp) {
    def data = [(getConstant("ATTR_COOL_SETPOINT_MAX")): temp]
    Utils.toLogger("debug", "CoolsetpointMax.data = ${data}")
    setDeviceAttributes(deviceId, data)
}

def setWattage(deviceId, watt) {
    def data = [(getConstant("ATTR_LIGHT_WATTAGE")): ["status": "on", "value": watt]]
    Utils.toLogger("debug", "wattage.data = ${data}")
    setDeviceAttributes(deviceId, data)
}

def setAuxiliaryLoad(deviceId, status, load) {
    def data = [(getConstant("ATTR_FLOOR_OUTPUT2")): ["status": status, "value": load]]
    Utils.toLogger("debug", "auxiliary_load.data = ${data}")
    setDeviceAttributes(deviceId, data)
}

def setBatteryType(deviceId, batt) {
    def data = [(getConstant("ATTR_BATTERY_TYPE")): batt]
    Utils.toLogger("debug", "battery_type.data = ${data}")
    setDeviceAttributes(deviceId, data)
}

def setTankHeight(deviceId, height) {
    def data = [(getConstant("ATTR_TANK_HEIGHT")): height]
    Utils.toLogger("debug", "tank_height.data = ${data}")
    setDeviceAttributes(deviceId, data)
}

def setTankType(deviceId, tank) {
    def data = [(getConstant("ATTR_TANK_TYPE")): tank]
    Utils.toLogger("debug", "tank_type.data = ${data}")
    setDeviceAttributes(deviceId, data)
}

def setGaugeType(deviceId, gauge) {
    def data = [(getConstant("ATTR_GAUGE_TYPE")): gauge]
    Utils.toLogger("debug", "gauge_type.data = ${data}")
    setDeviceAttributes(deviceId, data)
}

def setOccupancyMode(deviceId, mode, wifi) {
    def data = wifi ? [(getConstant("ATTR_OCCUPANCY")): mode] : [(getConstant("ATTR_SYSTEM_MODE")): mode]
    setDeviceAttributes(deviceId, data)
}

def setPhase(deviceId, phase) {
    def data = [(getConstant("ATTR_PHASE_CONTROL")): phase]
    Utils.toLogger("debug", "phase.data = ${data}")
    setDeviceAttributes(deviceId, data)
}

def setDoubleUp(deviceId, doubleUpFlag) {
    def data = [(getConstant("ATTR_KEY_DOUBLE_UP")): doubleUpFlag]
    Utils.toLogger("debug", "double_up.data = ${data}")
    setDeviceAttributes(deviceId, data)
}

def setEarlyStart(deviceId, start) {
    def data = [(getConstant("ATTR_EARLY_START")): start]
    Utils.toLogger("debug", "early_start.data = ${data}")
    setDeviceAttributes(deviceId, data)
}

def setSensorAlert(deviceId, leak, batt, temp, close) {
    def data = [
        (getConstant("ATTR_LEAK_ALERT")): leak,
        (getConstant("ATTR_BATT_ALERT")): batt,
        (getConstant("ATTR_TEMP_ALERT")): temp,
        (getConstant("ATTR_CONF_CLOSURE")): close
    ]
    Utils.toLogger("debug", "leak.data = ${data}")
    setDeviceAttributes(deviceId, data)
}

def setLoadDrOptions(deviceId, onoff, optout, dr) {
    def data = [(getConstant("ATTR_DRSTATUS")): ["drActive": dr, "optOut": optout, "onOff": onoff]]
    Utils.toLogger("debug", "Load.DR.options = ${data}")
    setDeviceAttributes(deviceId, data)
}

def setHvacDrOptions(deviceId, dr, optout, setpoint) {
    def data = [(getConstant("ATTR_DRSTATUS")): ["drActive": dr, "optOut": optout, "setpoint": setpoint]]
    Utils.toLogger("debug", "hvac.DR.options = ${data}")
    setDeviceAttributes(deviceId, data)
}

def setHvacDrSetpoint(deviceId, status, val) {
    def data = [(getConstant("ATTR_DRSETPOINT")): ["status": status, "value": val]]
    Utils.toLogger("debug", "hvac.DR.setpoint = ${data}")
    setDeviceAttributes(deviceId, data)
}

def setCoolTime(deviceId, time, state) {
    def data = state == "on" ? [(getConstant("ATTR_COOL_MIN_TIME_ON")): time] : [(getConstant("ATTR_COOL_MIN_TIME_OFF")): time]
    Utils.toLogger("debug", "HC cool_min_time_on/off.data = ${data}")
    setDeviceAttributes(deviceId, data)
}

def setFuelAlert(deviceId, fuel) {
    def data = [(getConstant("ATTR_FUEL_ALERT")): fuel]
    Utils.toLogger("debug", "fuel_alert.data = ${data}")
    setDeviceAttributes(deviceId, data)
}

def setBatteryAlert(deviceId, batt) {
    def data = [(getConstant("ATTR_BATT_ALERT")): batt]
    Utils.toLogger("debug", "battery_alert.data = ${data}")
    setDeviceAttributes(deviceId, data)
}

def setPowerSupply(deviceId, supply) {
    def data = [(getConstant("ATTR_POWER_SUPPLY")): supply]
    Utils.toLogger("debug", "power_supply.data = ${data}")
    setDeviceAttributes(deviceId, data)
}

def setOnOffInputDelay(deviceId, delay, onoff, inputNumber) {
    def data = [:]
    if (inputNumber == 1) {
        data = (onoff == "on") ? [(getConstant("ATTR_INPUT_1_ON_DELAY")): delay] : [(getConstant("ATTR_INPUT_1_OFF_DELAY")): delay]
    } else {
        data = (onoff == "on") ? [(getConstant("ATTR_INPUT_2_ON_DELAY")): delay] : [(getConstant("ATTR_INPUT_2_OFF_DELAY")): delay]
    }
    Utils.toLogger("debug", "input_delay.data = ${data}")
    setDeviceAttributes(deviceId, data)
}

def setHeatPumpLimit(deviceId, temp) {
    def data = [(getConstant("ATTR_BALANCE_PT")): temp]
    Utils.toLogger("debug", "Heat pump limit value.data = ${data}")
    setDeviceAttributes(deviceId, data)
}

def setHeatLockout(deviceId, temp) {
    def data = [(getConstant("ATTR_HEAT_LOCK_TEMP")): temp]
    Utils.toLogger("debug", "Heat lockout limit value.data = ${data}")
    setDeviceAttributes(deviceId, data)
}

def setCoolLockout(deviceId, temp) {
    def data = [(getConstant("ATTR_COOL_LOCK_TEMP")): temp]
    Utils.toLogger("debug", "Cool lockout limit value.data = ${data}")
    setDeviceAttributes(deviceId, data)
}

def setHpDisplay(deviceId, display) {
    def data = [(getConstant("ATTR_DISPLAY_CONF")): display]
    Utils.toLogger("debug", "Display config value.data = ${data}")
    setDeviceAttributes(deviceId, data)
}

def setHpSound(deviceId, sound) {
    def data = [(getConstant("ATTR_SOUND_CONF")): sound]
    Utils.toLogger("debug", "Sound config value.data = ${data}")
    setDeviceAttributes(deviceId, data)
}

def setSwingHorizontal(deviceId, swing) {
    def data = [(getConstant("ATTR_FAN_SWING_HORIZ")): swing]
    Utils.toLogger("debug", "Fan horizontal swing value.data = ${data}")
    setDeviceAttributes(deviceId, data)
}

def setSwingVertical(deviceId, swing) {
    def data = [(getConstant("ATTR_FAN_SWING_VERT")): swing]
    Utils.toLogger("debug", "Fan vertical swing value.data = ${data}")
    setDeviceAttributes(deviceId, data)
}

def setInputOutputNames(deviceId, in1, in2, out1, out2) {
    def data = [:]
    data[getConstant("ATTR_NAME_1")] = in1 ?: ""
    data[getConstant("ATTR_NAME_2")] = in2 ?: ""
    data[getConstant("ATTR_OUTPUT_NAME_1")] = out1 ?: ""
    data[getConstant("ATTR_OUTPUT_NAME_2")] = out2 ?: ""
    Utils.toLogger("debug", "in/out names.data = ${data}")
    setDeviceAttributes(deviceId, data)
}

def setAuxHeatTimeOn(deviceId, time) {
    def data = [(getConstant("ATTR_AUX_HEAT_TIMEON")): time]
    Utils.toLogger("debug", "HC aux_heat_time_on.data = ${data}")
    setDeviceAttributes(deviceId, data)
}

def getConstant(String name) {
    return this."${name}"
}

/**
 * Simple utilities for manipulation
 */
float formatTemperature(temperature) {
    formatTemperature(temperature, getTemperatureScale())
}

float formatTemperature(temperature, locationScale) {
    Utils.toLogger("debug", "FormatTemperature - temperature before conversion: ${temperature} - locationScale: ${locationScale}")

    def formattedTemperature = temperature as Float
    
    if(formattedTemperature > SINOPE_MAX_TEMPERATURE_CELSIUS) {
        if (locationScale == "C") { // convert to celsius
            formattedTemperature = fahrenheitToCelsius(formattedTemperature)
            Utils.toLogger("debug", "FormatTemperature - temperature converted to celsius: ${formattedTemperature}")
        } else {
            formattedTemperature = convertTemperatureIfNeeded(formattedTemperature, locationScale, 1).toBigDecimal()
        }
    } else {
        if (locationScale == "F") { // convert to fahrenheit
            formattedTemperature = celsiusToFahrenheit(formattedTemperature)
            Utils.toLogger("debug", "FormatTemperature - temperature converted to fahrenheit: ${formattedTemperature}")
        } else {
            formattedTemperature = convertTemperatureIfNeeded(formattedTemperature, locationScale, 1).toBigDecimal()
        }
    }
    
    if (locationScale == "C") { // celsius
        formattedTemperature = roundToHalf(formattedTemperature)
        Utils.toLogger("debug", "FormatTemperature - temperature roundToHalf celsius: ${formattedTemperature}")
        formattedTemperature = Math.min(SINOPE_MAX_TEMPERATURE_CELSIUS, Math.max(SINOPE_MIN_TEMPERATURE_CELSIUS, formattedTemperature))
    } else { // fahrenheit
        formattedTemperature = (Math.round(formattedTemperature)).toDouble().round(0)
        Utils.toLogger("debug", "FormatTemperature - temperature round fahrenheit: ${formattedTemperature}")
        formattedTemperature = Math.min(SINOPE_MAX_TEMPERATURE_FAHRENHEIT, Math.max(SINOPE_MIN_TEMPERATURE_FAHRENHEIT, formattedTemperature))
    }
    
    Utils.toLogger("debug", "FormatTemperature - temperature after conversion: ${formattedTemperature}")
    
    return formattedTemperature
}

float roundToHalf(float x) {
    return (float) (Math.round(x * 2) / 2)
}

def getTemperatureScale() {
    return "${location.temperatureScale}"
}

boolean isCelsius() {
    return getTemperatureScale() == "C"
}

boolean isFahrenheit() {
    return !isCelsius()
}

float getScaleStep() {
    def step
    
    if (isCelsius()) {
        step = 0.5
    } else {
        step = 1
    }
    
    return step
}

float getMaxTemperature() {
    if(isCelsius()) {
        return SINOPE_MAX_TEMPERATURE_CELSIUS
    } else {
        return SINOPE_MAX_TEMPERATURE_FAHRENHEIT
    }
}

float getMinTemperature() {
    if(isCelsius()) {
        return SINOPE_MIN_TEMPERATURE_CELSIUS
    } else {
        return SINOPE_MIN_TEMPERATURE_FAHRENHEIT
    }
}

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
                log."${level}" "${app.name} ${msg}";
            }
        }
    }
    
    // Converts seconds to time hh:mm:ss
    instance.convertSecondsToTime = { sec ->
                                     long millis = sec * 1000
                                     long hours = java.util.concurrent.TimeUnit.MILLISECONDS.toHours(millis)
                                     long minutes = java.util.concurrent.TimeUnit.MILLISECONDS.toMinutes(millis) % java.util.concurrent.TimeUnit.HOURS.toMinutes(1)
                                     String timeString = String.format("%02d:%02d", Math.abs(hours), Math.abs(minutes))
                                     return timeString
    }
    
    instance.extractInts = { String input ->
                            return input.findAll( /\d+/ )*.toInteger()
    }
    
    instance.convertErrorMessageTime = { String input ->
        Integer valueInteger = instance.extractInts(input).last()
        String valueStringConverted = instance.convertSecondsToTime(valueInteger)
        return input.replaceAll( valueInteger.toString() + " seconds", valueStringConverted )
    }        
    
    return instance;
}
