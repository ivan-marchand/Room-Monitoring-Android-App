/*
  Analog input, analog output, serial output

 Reads an analog input pin, maps the result to a range from 0 to 255
 and uses the result to set the pulsewidth modulation (PWM) of an output pin.
 Also prints the results to the serial monitor.

 The circuit:
 * potentiometer connected to analog pin 0.
   Center pin of the potentiometer goes to the analog pin.
   side pins of the potentiometer go to +5V and ground
 * LED connected from digital pin 9 to ground

 created 29 Dec. 2008
 modified 9 Apr 2012
 by Tom Igoe

 This example code is in the public domain.

 */
#include <math.h>
#include <Bridge.h>

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
  double temp = Thermistor(sensorValue);

  // print the results to the serial monitor:
  Serial.print("temp = " );
  Serial.print(temp);
  Serial.print("\n" );

  Bridge.put("temp", String(temp));

  // Wait 10s
  delay(10000);
}
