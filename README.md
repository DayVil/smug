# Smug

Smug, short for **Smart Mug**, is a system that combines hardware and software to help users monitor their liquid consumption and estimate nutrient intake through precise, weight-based measurements. The system is composed of two main components:

- **Smart Scale:** A Bluetooth-enabled ESP32 device with a weight sensor.
- **Android App:** A modern, Compose-based application for user interaction, data logging, and visualization.

These components are physically independent but communicate seamlessly over Bluetooth.

## Features

### Hardware (Smart Scale)
- **Real-time Weight Sensing:** Continuously measures the weight of the mug and its contents.
- **Bluetooth LE Connectivity:** Transmits weight data to the Android app.

### Android App
- **Bluetooth Device Discovery & Connection:** Automatically scans and connects to the Smart Scale.
- **Barcode Scanning:** Scan drink barcodes to fetch nutritional data from Open Food Facts.
- **Zeroing Function:** Allows users to tare the scale for accurate measurement.
- **Drink Logging:** Log consumed beverages with precise amounts and nutrient breakdowns.
- **Calory Estimation:** Calculates calories and sugar based on actual consumed volume.
- **Daily & Weekly Reports:** View consumption history, daily goals, and weekly summaries.
- **Charts:** Visualize intake by type, calories, and water consumption with pie, bar, and stacked bar charts.
- **Insights & Recommendations:** Get weekly insights based on your drinking patterns using Gemini API.
- **Editable Entries:** Edit or delete logged drinks, or clear your entire history.

## Getting Started

### Prerequisites

- **Hardware:**
-   ESP32 with a compatible weight sensor (e.g., HX711). See [here](https://randomnerdtutorials.com/esp32-load-cell-hx711/) for reference.
-   Android phone.

### Installation

1. **Hardware Setup**
    - Flash the ESP32 with the provided firmware in `smug_embedded` for BLE and weight sensing. 
    - Assemble the weight sensor and connect to the ESP32. See [here](https://randomnerdtutorials.com/esp32-load-cell-hx711/) for reference.
    - Power the device and ensure it advertises over BLE.

2. **App Setup**
    - Clone this repository.
    - Open in Android Studio.
    - Set your Gemini API key in ReportViewModel.kt (val apiKey = ...) 
    - Build and run on your Android device.

### Connecting the App to the Scale

- Launch the Smug app.
- The app will automatically scan for BLE devices and connect to the Smart Scale.
- The current weight is displayed in real time. Place the cup on the Smart Scale and press **Zero** to save the offset.

## Usage

### Logging a Drink

1. **Place your mug on the coaster without any liquid.**
2. Zero the scale.
3. Pour the liquid into the mug.
4. **Scan the barcode** of your beverage or enter a product name.
5. Select the correct product from the search results.
6. Confirm the detected weight. Nutrients are estimated based on the actual consumed amount.

### Reviewing Your Consumption

- Switch between **daily** and **weekly** views.
- Set a **daily liquid goal** and track your progress.
- View **charts** for volume, calories, and water intake.
- Tap on an entry to view or edit its details, or to see a nutrient breakdown.

### Insights

- Tap the **lightbulb icon** to receive personalized insights and tips based on your recent drinking habits.

## Demo

![Demo](https://youtube.com/shorts/c0TtVSi5ysk)

## Acknowledgments

- [Open Food Facts](https://world.openfoodfacts.org/) for product and nutrition data.

*smug, one sip at a time!*
