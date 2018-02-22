An app to transfer forms from Collect app to supervisor app via `bluetooth`.

Tranfer includes all forms files i.e, `images`, `xml files`

To work on the same code a seperate branch is created under the `collect` repo. [Link to branch](https://github.com/lakshyagupta21/collect/tree/COLLECT-SUPERVISOR)

How to use

Make sure you use bluetooth enabled devices and bluetooth is started before opening of both the apps.
Devices communicating should be paired before using the app.

[Collect app](https://github.com/opendatakit/collect)
1. Go to  `Send Finalized form`
2. Select any number of forms
3. Click on `Send Selected`
4. Click on `Search` to search for bluetooth devices nearby.
5. Click on any device listed
6. If paired communiction will start.

Supervisor app
1. Open the app
2. If paired it will connect to requests coming from the collect app.
3. Whenever collect app send the data it starts receiving the files and saves it under "Downloads" folder

Client -> Server


![alt text](https://github.com/lakshyagupta21/BluetoothP2P/blob/master/screenshots/client-server.gif)
