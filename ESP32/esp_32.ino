#include <Arduino.h>
#include <WiFi.h>
#include <Firebase_ESP_Client.h>
#include <DHT.h>
#include <OneWire.h>
#include <DallasTemperature.h>
#include "addons/TokenHelper.h"

// WiFi Credentials
#define WIFI_SSID "BLACKHOLE"
#define WIFI_PASSWORD "723338364"

// Firebase Configuration
#define FIREBASE_DATABASE_URL "https://cheerpark-450f1-default-rtdb.firebaseio.com/"
#define FIREBASE_API_KEY "AIzaSyDwxQv8fI77W0sV8_c02qr7Y4Yf2lUG0dA"
#define USER_EMAIL "mursalin7842@gmail.com"
#define USER_PASSWORD "1234567890"

// Sensor Pins
#define DHT_AGRO_PIN 4
#define DHT_CATTLE_PIN 15
#define DHT_POULTRY_PIN 18
#define MOISTURE_PIN 21
#define FISH_TEMP_PIN 5

// Firebase objects
FirebaseData fbdo;
FirebaseConfig config;
FirebaseAuth auth;

// Sensors
DHT dhtAgro(DHT_AGRO_PIN, DHT11);
DHT dhtCattle(DHT_CATTLE_PIN, DHT11);
DHT dhtPoultry(DHT_POULTRY_PIN, DHT11);
OneWire oneWire(FISH_TEMP_PIN);
DallasTemperature fishTempSensor(&oneWire);

unsigned long sendDataPrevMillis = 0;

// Function prototypes
void sendData(const String& path, float temp, float humidity, int moisture = -1);
void sendFishData(float temp);

void setupWiFi() {
  WiFi.begin(WIFI_SSID, WIFI_PASSWORD);
  Serial.print("Connecting to WiFi");
  while (WiFi.status() != WL_CONNECTED) {
    delay(500);
    Serial.print(".");
  }
  Serial.print("\nConnected! IP: ");
  Serial.println(WiFi.localIP());
}

void setupFirebase() {
  config.host = FIREBASE_DATABASE_URL;
  config.api_key = FIREBASE_API_KEY;
  auth.user.email = USER_EMAIL;
  auth.user.password = USER_PASSWORD;

  config.token_status_callback = tokenStatusCallback;
  
  Firebase.reconnectWiFi(true);
  fbdo.setResponseSize(4096);
  Firebase.begin(&config, &auth);
}

void setup() {
  Serial.begin(115200);
  
  // Initialize sensors
  dhtAgro.begin();
  dhtCattle.begin();
  dhtPoultry.begin();
  fishTempSensor.begin();
  pinMode(MOISTURE_PIN, INPUT);

  setupWiFi();
  setupFirebase();

  while (!Firebase.ready()) {
    Serial.println("Waiting for Firebase...");
    delay(500);
  }
}

void loop() {
  if (Firebase.ready() && (millis() - sendDataPrevMillis > 10000)) {
    sendDataPrevMillis = millis();

    // Read sensors with proper error checking
    float agroTemp = dhtAgro.readTemperature();
    float agroHumidity = dhtAgro.readHumidity();
    int agroMoisture = analogRead(MOISTURE_PIN);
    
    float cattleTemp = dhtCattle.readTemperature();
    float cattleHumidity = dhtCattle.readHumidity();
    
    float poultryTemp = dhtPoultry.readTemperature();
    float poultryHumidity = dhtPoultry.readHumidity();
    
    fishTempSensor.requestTemperatures();
    float fishTemp = fishTempSensor.getTempCByIndex(0);

    // Send data with proper condition checks
    if(!isnan(agroTemp)) {
      sendData("/AgroFarm", agroTemp, agroHumidity, agroMoisture);
    }
    if(!isnan(cattleTemp)) {
      sendData("/CattleFarm", cattleTemp, cattleHumidity);
    }
    if(!isnan(poultryTemp)) {
      sendData("/PoultryFarm", poultryTemp, poultryHumidity);
    }
    if(fishTemp != DEVICE_DISCONNECTED_C) {
      sendFishData(fishTemp);
    }
  }
}

void sendData(const String& path, float temp, float humidity, int moisture) {
  FirebaseJson json;
  json.set("temperature", temp);
  json.set("humidity", humidity);
  if(moisture != -1) json.set("moisture", moisture);
  json.set("timestamp", millis());

  Serial.print("Sending to ");
  Serial.print(path);
  
  if (Firebase.RTDB.pushJSON(&fbdo, path.c_str(), &json)) {
    Serial.println(" - Success");
  } else {
    Serial.println(" - Failed: " + fbdo.errorReason());
  }
}

void sendFishData(float temp) {
  FirebaseJson json;
  json.set("temperature", temp);
  json.set("ph", 6.0 + random(0, 35)/10.0);
  json.set("turbidity", random(2, 15));
  json.set("timestamp", millis());

  Serial.print("Sending fish data...");
  
  if (Firebase.RTDB.pushJSON(&fbdo, "/FishFarm", &json)) {
    Serial.println(" - Success");
  } else {
    Serial.println(" - Failed: " + fbdo.errorReason());
  }
}
