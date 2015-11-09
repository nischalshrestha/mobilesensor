##Mobile Sensor

Uses the Aware Framework: https://github.com/denzilferreira/aware-client

<h3>Features</h3>

The mobile sensor project aims to do provide an android service that does the following:

1. Detects the current mode of transportation (still/walking/running/biking/driving). Based on the confidence in each activity it gives a notification to the user about the most probable activity. 

2. Determines whether there is any multitasking performed by the user on the phone. For example if the user switches back and forth between two apps or more within 30s.

3. Using sound recording, it attempts to imitate the function of an actual sound meter. It converts the audio input into decibels and further, it classifies the audio in the environment as noisy or silent.

4. Detects whether the user has just sent or received a text messaging. If user uses a popular chat app, it will also be detected as texting.

5. Detects whether the user was just on the phone.

6. Detects whether the user has an important event coming up on their calendar. If the event has a reminder, it is treated as important.

7. Detects whether the user has been using an email app.

8. Keeps track of all app installs 

9. Upon detection of one of these sensors, it will ask the user to affirm a false positive, and input a stressfulness rating as well.
For the Noise sensor, it will ask the user to rate the loudness (false negative test). For the mode of transportation sensor, it will
ask whether they are on foot, bicycle, still, or in vehicle.

Limitations:

1. The Mode of Transportation is not always reliable, but it does provide the confidence levels.
2. The Ambient Noise is based on the setting for what counts as silence (in Db) and this may vary its classification of "noisy" and "silent" environment.

<h3>Installation/Uninstallation</h3>

Steps to run the app:

1. Install AWARE client
2. Install Mobile Sensor via QR Code (api.awareframework.com, under Researcher->SoS Mobile Sensor)
3. Turn on both the Mobile Sensor and AWARE service in the Accessibility settings
5. To stop the service after the service is started, disable it in the Aware client under Plugins

Steps to uninstall:

1. Uninstall Mobile Sensor
2. Uninstall Google Activity Recognition and Ambient Noise
3. Uninstall Aware Client
4. Delete AWARE Folder in phone storage

<h3>Instructions for Experiment</h3>

Server dashboard: api.awareframework.com

- Select the study (under 'Researcher'), Join Study by clicking on the QRcode to display QR code
- On Client app, scan QR code and joine study

<h3>Common Issues</h3>

Downloading files: 

There's been an issue with some devices with downloading files.

If this happens: 

- Try having them open AWARE and hit Sign Up again, or (if our Study Info dialog isn't there) scan the QR code again. 
- This takes a few tries sometimes

<h3>Relevant Databases</h3>

plugin_sos_mobile_sensor

- Recordings of all sensor firings

esms

- Recordings of all user answers to false positive and stress rating questions

