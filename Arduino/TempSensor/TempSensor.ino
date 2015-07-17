/*

 */
#include <math.h>
#include <Bridge.h>
#include <DHT.h>

#define DHTPIN 4     // what pin we're connected to
#define DHTTYPE DHT11   // DHT 11

DHT dht(DHTPIN, DHTTYPE);

// These constants won't change.  They're used to give names
// to the pins used:
const int analogInPin = A0;  // Analog input pin that the potentiometer is attached to


double Thermistor(int RawADC) {
 double Temp;
 Temp = 50000.0*(1024.0/RawADC-1);
 Temp = log(Temp/50000.0);
 Temp = 1 / (3.354016E-03 + 2.460382E-04 * Temp + 3.405377E-06 * Temp * Temp +  1.034240E-07 * Temp * Temp * Temp);
 Temp = Temp - 273.15;            // Convert Kelvin to Celcius
 Temp = (Temp * 9.0)/ 5.0 + 34.0; // Convert Celcius to Fahrenheit
 return Temp;
}

void setup() {
  // initialize serial communications at 9600 bps:
  Serial.begin(9600);
  // Initialize Bridge
  Bridge.begin();
}

void loop() {
  int sensorValue = analogRead(analogInPin);
  //double temp = Thermistor(sensorValue);
  double temp = dht.readTemperature(true);
  double humidity = dht.readHumidity();

  // print the results to the serial monitor:
  Serial.print("temp = " );
  Serial.print(temp);
  Serial.print("\n" );
  Serial.print("humidity = " );
  Serial.print(humidity);
  Serial.print("\n" );

  Bridge.put("temp", String(temp));
  Bridge.put("humidity", String(humidity));

  // Wait 10s
  delay(10000);
}
