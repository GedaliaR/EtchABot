#include <AccelStepper.h> //stepper library
#include <SoftwareSerial.h> //bluetooth library

#define horzMotorPin1  9     // IN1 on the ULN2003 driver
#define horzMotorPin2  8     // IN2 on the ULN2003 driver
#define horzMotorPin3  7     // IN3 on the ULN2003 driver
#define horzMotorPin4  6     // IN4 on the ULN2003 driver
#define vertMotorPin1  13      // IN1 on the ULN2003 driver
#define vertMotorPin2  12      // IN2 on the ULN2003 driver
#define vertMotorPin3  11      // IN3 on the ULN2003 driver
#define vertMotorPin4  10      // IN4 on the ULN2003 driver

#define MotorInterfaceType 8 //4-wire motor in half step mode

AccelStepper horzMotor = AccelStepper(MotorInterfaceType, horzMotorPin1, horzMotorPin3, horzMotorPin2, horzMotorPin4);
AccelStepper vertMotor = AccelStepper(MotorInterfaceType, vertMotorPin1, vertMotorPin3, vertMotorPin2, vertMotorPin4);

SoftwareSerial BT(4, 5); //RX, TX

uint8_t _prevHorzDir;
uint8_t _prevVertDir;

const int STEP_DELAY = 3;

const int _xMin = 0;
int _xMax = 16000;
const int _yMin = 0;
int _yMax = 16000;

int _hBacklash;
int _vBacklash;

void setup() {

  BT.begin(9600);
  BT.println("#start up");
  BT.println("#OK");

  // Seem to need delay here before starting to draw with motors
  delay(4000);

  //speed is in steps per second. 4076-4096 steps/revolution in half-step mode ~= 14-15rpm. Don't run faster!
  horzMotor.setMaxSpeed(1000);
  vertMotor.setMaxSpeed(1000);

  horzMotor.setAcceleration(300);
  vertMotor.setAcceleration(300);  
}

// True if we've changed directions horizontally
boolean horzDirChange(uint8_t dir) {
  if (_prevHorzDir && dir != _prevHorzDir) {
    _prevHorzDir = dir;
    return true;
  } else if (!_prevHorzDir) {
    _prevHorzDir = dir;
  }
  return false;
}

// True if we've changed directions vertically
boolean vertDirChange(uint8_t dir) {
  if (_prevVertDir && dir != _prevVertDir) {
    _prevVertDir = dir;
    return true;
  } else if (!_prevVertDir) {
    _prevVertDir = dir;
  }
  return false;
}

/*
   Consider replacing the algorithmic drawline with a simpler drawline that takes an angle (x/y)
   and power (speed) to create a vector ([x/y]*speed). May be easier to integrate w/app.
   Keep this for testing with computer.

   Draws a line from the current position to the end position.  If we are at the end position, does nothing.
   TODO: Got the motors to spin simultaneously (yay!) now get them to spin so that they stop spinning at
   the same time. Consider implementing the original algorithmic way, but with the AccelStepper.step() method.
*/
void drawLine(int targetX, int targetY) {

  //  BT.print("Target(x,y) = ");
  //  BT.print(targetX);
  //  BT.print(", ");
  //  BT.println(targetY);

  // Boundary check
  if (targetX < _xMin) targetX = _xMin;
  if (targetX > _xMax) targetX = _xMax;
  if (targetY < _yMin) targetY = _yMin;
  if (targetY > _yMax) targetY = _yMax;

  boolean dx = (abs(targetX - horzMotor.currentPosition()) != 0);     //true if there is a difference between target and current; false if we're already there.
  int sx = horzMotor.currentPosition() < targetX ? 1 : -1;            //direction coefficient
  boolean dy = (abs(targetY - vertMotor.currentPosition()) != 0);
  int sy = vertMotor.currentPosition() < targetY ? 1 : -1;

  moveXBacklash(dx, sx);
  moveYBacklash(dy, sy);

  while (horzMotor.distanceToGo() != 0 || vertMotor.distanceToGo() != 0) {
    horzMotor.run();
    vertMotor.run();
  }

//  BT.println("moving to target location");
  horzMotor.moveTo(targetX);
  vertMotor.moveTo(targetY);

  while (horzMotor.distanceToGo() != 0 || vertMotor.distanceToGo() != 0) {
    horzMotor.run();
    vertMotor.run();
  }
}

/**
 * Moves a certain amount of steps relative to current position.
 */
void move(int xRelTarget, int yRelTarget) {

  xRelTarget = validateXTarget(xRelTarget);
  yRelTarget = validateYTarget(yRelTarget);

  boolean dx = xRelTarget != 0;
  int sx = xRelTarget > 0 ? 1 : -1;
  boolean dy = yRelTarget != 0;
  int sy = yRelTarget > 0 ? 1 : -1;

  moveXBacklash(dx, sx);
  moveYBacklash(dy, sy);

  horzMotor.move(xRelTarget);
  vertMotor.move(yRelTarget);

  while (horzMotor.distanceToGo() != 0 || vertMotor.distanceToGo() != 0) {
    horzMotor.run();
    vertMotor.run();
  }
  
  BT.print("Current position is " );
    BT.print(horzMotor.currentPosition());
    BT.print(", ");
    BT.println(vertMotor.currentPosition());

}

int validateXTarget(int xTarget) {
  if ((xTarget > 0 && horzMotor.currentPosition() >= _xMax) || (xTarget < 0 && horzMotor.currentPosition() <= _xMin)) {
    return 0;
  }
  else return xTarget;
}

int validateYTarget(int yTarget) {
  if ((yTarget > 0 && vertMotor.currentPosition() >= _yMax) || (yTarget < 0 && vertMotor.currentPosition() <= _yMin)) {
    return 0;
  }
  else return yTarget;
}

void moveXBacklash(boolean dx, int sx) {
  if (dx && horzDirChange(sx)) {
    horzMotor.move(_hBacklash * sx);
    delay(STEP_DELAY);
  }
}

void moveYBacklash(boolean dy, int sy) {
  if (dy && vertDirChange(sy)) {
    vertMotor.move(_vBacklash * sy);
    delay(STEP_DELAY);
  }
}


// Get the string out of the input.  Assumes each string has format: 'c x y', where
// 'c' is a character command and x,y are intergers
void extractAndExecuteCmd(String &inputString) {
  // First character had better be the command
  char cmd = inputString[0];
  int x = 0, y = 0;
  int space1, space2, len;

  switch (cmd) {
    case 'B': // Set backlash to supplied values
      space1 = inputString.indexOf(' ');
      space2 = inputString.lastIndexOf(' ');
      _hBacklash = inputString.substring(space1 + 1, space2).toInt();
      _vBacklash = inputString.substring(space1 + 1, space2).toInt();
      BT.print("#B ");
      BT.print(_hBacklash);
      BT.print(", ");
      BT.println(_vBacklash);
      break;
    case 'b': // Return backlash values
      BT.print("#b ");
      BT.print(_hBacklash);
      BT.print(", ");
      BT.println(_vBacklash);
      break;
    case 'D':
    case 'd':  // Return screen dimensions
      BT.print("#D ");
      BT.print(_xMax);
      BT.print(" ");
      BT.println(_yMax);
      break;
    case 'L':
    cmd = 'L';
      space1 = inputString.indexOf(' ');
      space2 = inputString.lastIndexOf(' ');
      x = inputString.substring(space1 + 1, space2).toInt();
      y = inputString.substring(space2 + 1).toInt();
      drawLine(x, y);
      break;
    case 'M':
      cmd = 'M';
      space1 = inputString.indexOf(' ');
      space2 = inputString.lastIndexOf(' ');
      x = inputString.substring(space1 + 1, space2).toInt();
      y = inputString.substring(space2 + 1).toInt();
      move(x, y);
      break;
    default:
      BT.print("#unknown command: ");
      BT.println(cmd);
      BT.println("OK");
      break;
  }
}

String inputString = "";
boolean stringComplete = false;
void loop() {

#ifndef MODE_CALIBRATION
  // Fill the command buffer with commands
  // TBD - make sure we don't hang waiting for a new command
  while (BT.available()) {
    // Get the new byte
    char inChar = (char) BT.read();
    // add it to inputString
    if (inChar != '\n' && inChar != '\r') {
      inputString += inChar;
      if (inChar == ';') {
        stringComplete = true;
      }
    }
    // Parse the string for instructions
    if (stringComplete) {
      //delay(50);
      //Serial.print("Got String: ");
      //Serial.println(inputString.c_str());
      extractAndExecuteCmd(inputString);
      inputString = "";
      stringComplete = false;
    }
  }
#endif
}
