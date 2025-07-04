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

BLECharacteristic *pCharacteristic = NULL;
HX711 scale;

void setup()
{
    Serial.begin(115200);
    Serial.println("Starting BLE work!");

    BLEDevice::init("MyESP32");
    BLEServer *pServer = BLEDevice::createServer();
    BLEService *pService = pServer->createService(SERVICE_UUID);
    pCharacteristic = pService->createCharacteristic(
        CHARACTERISTIC_UUID,
        BLECharacteristic::PROPERTY_READ |
            BLECharacteristic::PROPERTY_WRITE |
            BLECharacteristic::PROPERTY_NOTIFY);

    BLEDescriptor *pDescriptor = new BLEDescriptor(BLEUUID((uint16_t)0x2902));
    pCharacteristic->addDescriptor(pDescriptor);

    pCharacteristic->setValue("Scale setup");
    pService->start();
    BLEAdvertising *pAdvertising = BLEDevice::getAdvertising();
    pAdvertising->addServiceUUID(SERVICE_UUID);
    pAdvertising->setScanResponse(true);
    pAdvertising->setMinPreferred(0x06); 
    pAdvertising->setMinPreferred(0x12);
    BLEDevice::startAdvertising();
    Serial.println("Characteristic defined! Now you can read it in your phone!");


    scale.begin(LOADCELL_DOUT_PIN, LOADCELL_SCK_PIN);
}

void loop()
{
    // static int value = 0;
    // std::string newValue = std::to_string(value++);

    if (scale.is_ready()) {
        long reading = scale.read();
        Serial.print("HX711 reading: ");
        Serial.println(reading);
        std::string strValue = std::to_string(reading);
        pCharacteristic->setValue(strValue);
        pCharacteristic->notify();
      } else {
        Serial.println("HX711 not found.");
      }

    delay(2000);
}
