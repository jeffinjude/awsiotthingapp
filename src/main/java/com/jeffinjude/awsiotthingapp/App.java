package com.jeffinjude.awsiotthingapp;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Random;

import com.amazonaws.services.iot.client.AWSIotException;
import com.amazonaws.services.iot.client.AWSIotMessage;
import com.amazonaws.services.iot.client.AWSIotMqttClient;
import com.amazonaws.services.iot.client.AWSIotQos;
import com.amazonaws.services.iot.client.AWSIotTopic;
import com.jeffinjude.awsiotthingapp.SampleUtil.KeyStorePasswordPair;

/**
 * Hello world!
 *
 */
public class App {
	private static final String Topic = "jeffin_topics/iotthing";
	private static final AWSIotQos TopicQos = AWSIotQos.QOS0;

	private static AWSIotMqttClient awsIotClient;

	public static void setClient(AWSIotMqttClient client) {
		awsIotClient = client;
	}

	public static class NonBlockingPublisher implements Runnable {
		private final AWSIotMqttClient awsIotClient;

		public NonBlockingPublisher(AWSIotMqttClient awsIotClient) {
			this.awsIotClient = awsIotClient;
		}

		public void run() {
			long counter = 1;
			Random rn = new Random();
			int randomNum = rn.nextInt(100);
			String timeStamp = new SimpleDateFormat("yyyy.MM.dd.HH.mm.ss").format(new Date());

			while (true) {
				randomNum = rn.nextInt(100);
				timeStamp = new SimpleDateFormat("yyyy.MM.dd.HH.mm.ss").format(new Date());
				String payload = "{ \"temperature\":" + randomNum + ", \"timestamp\":\"" + timeStamp + "\"}";
				AWSIotMessage message = new NonBlockingPublishListener(Topic, TopicQos, payload);
				try {
					awsIotClient.publish(message);
				} catch (AWSIotException e) {
					System.out.println(System.currentTimeMillis() + ": publish failed for " + payload);
				}

				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					System.out.println(System.currentTimeMillis() + ": NonBlockingPublisher was interrupted");
					return;
				}
			}
		}
	}

	private static void initClient(CommandArguments arguments) {
        String clientEndpoint = arguments.getNotNull("clientEndpoint", SampleUtil.getConfig("clientEndpoint"));
        String clientId = arguments.getNotNull("clientId", SampleUtil.getConfig("clientId"));

        String certificateFile = arguments.get("certificateFile", SampleUtil.getConfig("certificateFile"));
        String privateKeyFile = arguments.get("privateKeyFile", SampleUtil.getConfig("privateKeyFile"));
        if (awsIotClient == null && certificateFile != null && privateKeyFile != null) {
            String algorithm = arguments.get("keyAlgorithm", SampleUtil.getConfig("keyAlgorithm"));

            KeyStorePasswordPair pair = SampleUtil.getKeyStorePasswordPair(certificateFile, privateKeyFile, algorithm);

            awsIotClient = new AWSIotMqttClient(clientEndpoint, clientId, pair.keyStore, pair.keyPassword);
        }

        if (awsIotClient == null) {
            String awsAccessKeyId = arguments.get("awsAccessKeyId", SampleUtil.getConfig("awsAccessKeyId"));
            String awsSecretAccessKey = arguments.get("awsSecretAccessKey", SampleUtil.getConfig("awsSecretAccessKey"));
            String sessionToken = arguments.get("sessionToken", SampleUtil.getConfig("sessionToken"));

            if (awsAccessKeyId != null && awsSecretAccessKey != null) {
                awsIotClient = new AWSIotMqttClient(clientEndpoint, clientId, awsAccessKeyId, awsSecretAccessKey,
                        sessionToken);
            }
        }

        if (awsIotClient == null) {
            throw new IllegalArgumentException("Failed to construct client due to missing certificate or credentials.");
        }
    }
	
	public static void main(String[] args) throws AWSIotException, InterruptedException {
		CommandArguments arguments = CommandArguments.parse(args);
        initClient(arguments);

        awsIotClient.connect();

        AWSIotTopic topic = new TopicListener(Topic, TopicQos);
        awsIotClient.subscribe(topic, true);
        
        Thread nonBlockingPublishThread = new Thread(new NonBlockingPublisher(awsIotClient));
        
        nonBlockingPublishThread.start();
        
        nonBlockingPublishThread.join();
	}
}
