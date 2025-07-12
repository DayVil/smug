#include <Arduino.h>
#include <BLEDevice.h>
#include <BLEUtils.h>
#include <BLEServer.h>
#include "HX711.h"

#define SERVICE_UUID "4fafc201-1fb5-459e-8fcc-c5c9c331914b"
#define CHARACTERISTIC_UUID "beb5483e-36e1-4688-b7f5-ea07361b26a8"

// This works at least
#define LOADCELL_DOUT_PIN 16
#define LOADCELL_SCK_PIN 4

#define LOADCELL_FACTOR 916

BLECharacteristic *pCharacteristic = NULL;
BLEServer *pServer = NULL;
BLEService *pService = NULL;
BLEAdvertising *pAdvertising = NULL;
HX711 scale;

bool deviceConnected = false;
bool oldDeviceConnected = false;

class MyServerCallbacks : public BLEServerCallbacks
{
  void onConnect(BLEServer *pServer)
  {
    deviceConnected = true;
    Serial.println("Device connected");
  }

  void onDisconnect(BLEServer *pServer)
  {
    deviceConnected = false;
    Serial.println("Device disconnected");
  }
};

void startAdvertising()
{
  Serial.println("Starting advertising...");
  pAdvertising->start();
  Serial.println("Waiting for a client connection to notify...");
}

void setup()
{
  Serial.begin(115200);
  Serial.println("Starting BLE work!");

  BLEDevice::init("MyESP32");
  pServer = BLEDevice::createServer();
  pServer->setCallbacks(new MyServerCallbacks());

  pService = pServer->createService(SERVICE_UUID);
  pCharacteristic = pService->createCharacteristic(
      CHARACTERISTIC_UUID,
      BLECharacteristic::PROPERTY_READ |
          BLECharacteristic::PROPERTY_WRITE |
          BLECharacteristic::PROPERTY_NOTIFY);

  BLEDescriptor *pDescriptor = new BLEDescriptor(BLEUUID((uint16_t)0x2902));
  pCharacteristic->addDescriptor(pDescriptor);

  pCharacteristic->setValue("Scale setup");
  pService->start();

  pAdvertising = BLEDevice::getAdvertising();
  pAdvertising->addServiceUUID(SERVICE_UUID);
  pAdvertising->setScanResponse(true);
  pAdvertising->setMinPreferred(0x06);
  pAdvertising->setMinPreferred(0x12);

  startAdvertising();
  Serial.println("Characteristic defined! Now you can read it in your phone!");

  scale.begin(LOADCELL_DOUT_PIN, LOADCELL_SCK_PIN);
  scale.set_scale(LOADCELL_FACTOR);
  scale.tare();
}

void loop()
{
  // Handle reconnection logic
  if (!deviceConnected && oldDeviceConnected)
  {
    delay(500); // Give the bluetooth stack time to get ready
    startAdvertising();
    oldDeviceConnected = deviceConnected;
  }

  // Connecting
  if (deviceConnected && !oldDeviceConnected)
  {
    oldDeviceConnected = deviceConnected;
  }

  if (scale.is_ready())
  {
    long reading = scale.get_units();
    Serial.print("HX711 reading: ");
    Serial.println(reading);

    // Only notify if device is connected
    if (deviceConnected)
    {
      std::string strValue = std::to_string(reading);
      pCharacteristic->setValue(strValue);
      pCharacteristic->notify();
    }
  }
  else
  {
    Serial.println("HX711 not found.");
  }

  delay(2000);
}
