# EtchABot App

<p>
The purpose of this app is to function as a master to the Arduino slave. Therefore, it is entirely dependent on a Bluetooth 
connection. Instead of handling the Bluetooth connection in the app itself, I used a library called Ardutooth (to which I 
actually made a code contribution) to handle all the bluetooth stuff (turning on Bluetooth, pairing, connecting, sockets, etc.)
externally. Here's a link to that <a href="https://github.com/giuseppebrb/Ardutooth">project</a>.

As of this version, the app has four activities:

<ol>
<li>The Main Activity</li>
<li>The Spirograph Activity</li>
<li>The Microstep Activity</li>
<li>The About Activity</li>
</ol>

</p>

## Main Activity

<p>
The Main Activity is the first activity that is launched upon the app starting.

It has several elements:
<ul>
<li>A Joystick in the center</li>
<li>Two TextViews positioned over the Joystick</li>
<li>A Menu bar that has the buttons to the other Activites</li>
</ul>

 The Joystick View in the center was created by Damien Brun - <a href="https://github.com/controlwear/virtual-joystick-android">Link to his project</a>. 
The Joystick serves as the main control for the Etch-A-Sketch. If a user taps anywhere on the Joystick, or slides a finger
across it, the event handler takes the angle, and the distance from the center and creates a vector that can be sent to 
the Arduino over Bluetooth.

The TextViews show the angle and power of the current input via the Joystick.

It has two layouts, one for portrait, one for landscape:
</p>

![](../images/Main_Activity_portrait.png) ![](../images/Main_Activity_land.png)

## Spirograph Activity

<p>
In the Spirograph Activity, a user can enter two numbers in the fields in the center of the page and press the Start button, and 
the app will use those two parameters in an algorithm. This algorithm generates a set of coordinates and sends the coordinates,
one-by-one to the Arduino, drawing a spirograph. The algorith and communication takes place in a background Service on a seperate
thread, so the user can start the spirograph and switch to a different app on the Android. The Service will stay alive as long 
as the app is, or until the user presses the Stop button. The user can also leave the two fields blank, and the app will randomly 
generate two parameters.

The algorithm was taken from <a href="https://github.com/geekmomprojects/EtchABot/blob/master/examples/EtchABotPatterns/EtchABotPatterns.ino">this</a> 
part of Debra Ansell's project.

Here's a screenshot of the activity:
</p>

![](../images/Spirograph.png)

<p>And here's a gif of the spirograph drawing:</p>

![](../images/Spirograph.gif)

## Microstep Activity
