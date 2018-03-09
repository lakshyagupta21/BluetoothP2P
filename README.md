# Wifi Hotspot

An app to transfer forms from Collect app to supervisor app via `Wifi Hotspot`.

Tranfer includes all forms files i.e, `images`, `xml files`

To work on the same code a seperate branch is created under the `collect` repo. [link to branch](https://github.com/lakshyagupta21/collect/tree/COLLECT-SUPERVISOR-HOTSPOT)

How to use

Make sure your wifi is enabled in client device.

[Collect app](https://github.com/opendatakit/collect)
1. Go to  `Send Finalized form`
2. Select any number of forms
3. Click on `Send Selected`
4. Click on 'Start Listening' (This will listen for all incoming connection )
5. This will create a hotspot configuration specifically for this app so that client app can detect it.
5. If device is connected it will start sending the data to multiple devices connected to it.

Supervisor app
1. Open the app
2. Tap on 'Wifi'
3. If wifi is enabled it will connect to it automatically if not enabled then tap on 'Scan for devices' and tap back and repeat from step 2
3. When wifi is connected tap on 'Scan for devices'
4. Select "NewSSID" from the wifi list
3. Whenever collect app send the data it starts receiving the files and saves it under "Downloads" folder

Client -> Server


![alt text](https://github.com/lakshyagupta21/BluetoothP2P/blob/wifi/screenshots/wifi.gif)
