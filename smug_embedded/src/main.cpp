#include "Arduino.h"

#define LED_BUILTIN 2

void setup() {
  // Initialize Serial communication
  Serial.begin(115200);
  Serial.println("ESP32 Blink Test");
  
  // Initialize LED digital pin as an output
  pinMode(LED_BUILTIN, OUTPUT);
}

void loop() {
  Serial.println("LED ON");
  digitalWrite(LED_BUILTIN, HIGH);
  delay(1000);
  
  Serial.println("LED OFF");
  digitalWrite(LED_BUILTIN, LOW);
  delay(1000);
}
