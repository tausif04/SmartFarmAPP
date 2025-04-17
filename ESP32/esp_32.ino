#include <WiFi.h>
#include <HTTPClient.h>
#include <WebServer.h>
#include <DHT.h>

#define DHT_PIN 4       // Pin for the DHT11 sensor

const char* ssid = "Blackberry";
const char* password = "0@14368@murovi";
const char* tempServer = "http://127.0.0.1:8000/api/agriculture/";
//const char* humServer = "http://192.168.18.192:8000/tur/data/";

WebServer server(80);
WiFiClient client;
unsigned long previousDataTransmissionTime = 0;
const unsigned long dataTransmissionInterval = 15000; // 30 seconds

DHT dht(DHT_PIN, DHT11);

void setup() {
  Serial.begin(9600);
  dht.begin();
  connectToWiFi();

  server.begin();
}

void loop() {
  unsigned long currentMillis = millis();

  if (currentMillis - previousDataTransmissionTime >= dataTransmissionInterval) {
    float temperature = readTemperature();
    float humidity = readHumidity();

    sendData(tempServer, "Temperature", temperature);
    //sendData(humServer, "Humidity", humidity);

    previousDataTransmissionTime = currentMillis;
  }

  server.handleClient(); // No delay for handling client requests
}

void connectToWiFi() {
  WiFi.begin(ssid, password);
  while (WiFi.status() != WL_CONNECTED) {
    delay(1000);
    Serial.println("Connecting to WiFi...");
  }
  Serial.println("Connected to WiFi");
  Serial.print("ESP32 IP address: ");
  Serial.println(WiFi.localIP());
}

void sendData(const char* server, const char* dataType, float dataValue) {
  if (WiFi.status() == WL_CONNECTED) {
    HTTPClient http;
    http.begin(server);
    http.addHeader("Content-Type", "application/json");

    String postData = "{\"value\":" + String(dataValue, 2) + "}";
    int httpResponseCode = http.POST(postData);

    if (httpResponseCode > 0) {
      Serial.print(dataType);
      Serial.print(" HTTP Response code: ");
      Serial.println(httpResponseCode);
      String response = http.getString();
      Serial.print("Server response: ");
      Serial.println(response);
    } else {
      Serial.print("Error sending ");
      Serial.print(dataType);
      Serial.println(" data to the server.");
    }

    http.end();
  }
}

float readTemperature() {
  float temperature = dht.readTemperature();
  Serial.print("Temperature: ");
  Serial.println(temperature);
  return temperature;
}

float readHumidity() {
  float humidity = dht.readHumidity();
  Serial.print("Humidity: ");
  Serial.println(humidity);
  return humidity;
}

// void handleLEDControl() {
//   server.sendHeader("Access-Control-Allow-Origin", "*"); // CORS header

//   String status = server.arg("status");
//   if (status == "on") {
//     digitalWrite(LED_PIN, HIGH);
//   } else if (status == "off") {
//     digitalWrite(LED_PIN, LOW);
//   }

//   server.send(200, "text/plain", "LED is now " + status);
// }
