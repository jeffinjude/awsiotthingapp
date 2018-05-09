# awsiotthingapp

The application that needs to run in the iot hardware. This application logic just returns a random temperature between 0 and 100.

1. Keep the certificate, public and private keys of the iot thing in the same folder as that of this application's jar.
2. Also keep the root ca certificate also in the same folder.
3. Now run the awsiotthingapp jar using the command "java -jar awsiotthingapp.jar -clientEndpoint <arn of your iot thing> -clientId <some client id> -certificateFile <iot thing certificate file> -privateKeyFile <iot thing private key>"
4. In AWS MQTT client subscribe to topic "jeffin_topics/iotthing" to see the json messages sent from the iot hardware.
