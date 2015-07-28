/*
 created 29 Jun. 2015
 by Ivan Marchand

 This example code is in the public domain.

 */

//#include <math.h>
#include <Bridge.h>
#include <YunServer.h>
#include <YunClient.h>

#include <IRremote.h>

#include <DHT.h>
#define DHTPIN 4     // what pin we're connected to
#define DHTTYPE DHT11   // DHT 11


DHT dht(DHTPIN, DHTTYPE);
YunServer server;
IRsend irsend;


// These constants won't change.  They're used to give names
// to the pins used:
//const int analogInPin = A0;  // Analog input pin that the potentiometer is attached to


/*double Thermistor(int RawADC) {
 double Temp;
 Temp = 50000.0*(1024.0/RawADC-1);
 Temp = log(Temp/50000.0);
 Temp = 1 / (3.354016E-03 + 2.460382E-04 * Temp + 3.405377E-06 * Temp * Temp +  1.034240E-07 * Temp * Temp * Temp);
 Temp = Temp - 273.15;            // Convert Kelvin to Celcius
 Temp = (Temp * 9.0)/ 5.0 + 34.0; // Convert Celcius to Fahrenheit
 return Temp;
}*/

void setup() {
  // initialize serial communications at 9600 bps:
  Serial.begin(9600);
  // Initialize Bridge
  Bridge.begin();
  // Initialize Server
  server.listenOnLocalhost();
  server.begin();
}

void sendIRRaw(YunClient client) {
  unsigned int data[200] = {0};
  unsigned int i = 0;
  char last = '\0';
  for ( ; i < 200 ; i++)
  {
    data[i] = client.parseInt() * 10;
    last = client.read();
    if (last != ',')
    {
      break;
    }
  }
  int hz = 38;
  if (last == '/')
  {
    hz = client.parseInt();
  }
  client.print(",\"size\":\"");
  client.print(i+1);
  client.print("\"");
  client.print(",\"khz\":\"");
  client.print(hz);
  client.print("\"");
  irsend.sendRaw(data, i+1, hz);
}

void sendIRCommand(YunClient client) {
  String protocol = client.readStringUntil('/');
  String valueHex = client.readStringUntil('/');
  unsigned long value;
  sscanf(valueHex.c_str(), "%lx", &value); // if hex string
  int nbits = client.parseInt();
  // Option frequency param
  int hz = 38;
  if (client.read() == '/')
  {
    hz = client.parseInt();
  }
  client.print(",\"value\":\"");
  client.print(value);
  client.print("\"");
  client.print(",\"khz\":\"");
  client.print(hz);
  client.print("\"");

  if (protocol == "SAMSUNG") {
    irsend.sendSAMSUNG(value, nbits);
  }
  else if (protocol == "NEC") {
    irsend.sendNEC(value, nbits, hz);
  }
  else if (protocol == "AC") {
    irsend.sendAC(value, nbits, hz);
  }
  else if (protocol == "SONY") {
    irsend.sendSony(value, nbits);
  }
  else if (protocol == "RC5") {
    irsend.sendRC5(value, nbits);
  }
  else if (protocol == "RC6") {
    irsend.sendRC6(value, nbits);
  }
  else if (protocol == "DISH") {
    irsend.sendDISH(value, nbits);
  }
  //  else if (protocol == "SHARP") {
  //    irsend.sendNEC(value, nbits);
  //  }
  else if (protocol == "PANASONIC") {
    irsend.sendPanasonic(value, nbits);
  }
  else if (protocol == "JVC") {
    irsend.sendJVC(value, nbits, 1);
  }
  else if (protocol == "WHYNTER") {
    irsend.sendWhynter(value, nbits);
  }
  else {
    client.print(",\"error\":\"Unknown protocol\"");
  }
}

void getTemperature(YunClient client) {
  String tempUnit = client.readStringUntil('\r');
  //int sensorValue = analogRead(analogInPin);
  //double temp = Thermistor(sensorValue);
  double temp = dht.readTemperature(tempUnit == "F");
  double humidity = dht.readHumidity();

  // print the results to the serial monitor:
  client.print(",\"temperature\":\"" );
  client.print(temp);
  client.print("\"" );
  client.print(",\"humidity\":\"" );
  client.print(humidity);
  client.print("\"" );

}

void process(YunClient client) {
  String command = client.readStringUntil('/');
  client.print("{\"command\":\"");
  client.print(command);
  client.print("\"" );
  Serial.println(command);

  if (command == "getTemp") {
    getTemperature(client);
  }
  else if (command == "sendIRCommand") {
    sendIRCommand(client);
  }
  else if (command == "sendIRRaw") {
    sendIRRaw(client);
  }
  else {
    client.print(",\"error\":\"Unknown Command\"");
  }
  client.print("}" );
}

void loop() {
  YunClient client = server.accept();

  if (client) {
    process(client);
    client.stop();
  }

  delay(50);
}
