package com.mycompany.restpostclient;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.util.Properties;
import javax.ws.rs.client.Client;
import org.glassfish.jersey.client.oauth1.AccessToken;
import org.glassfish.jersey.client.oauth1.ConsumerCredentials;
import org.glassfish.jersey.client.oauth1.OAuth1AuthorizationFlow;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Feature;
import javax.ws.rs.core.Form;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.glassfish.jersey.client.oauth1.OAuth1ClientSupport;
import org.glassfish.jersey.jackson.JacksonFeature;

public class TwitterPost {

    private static final BufferedReader IN = new BufferedReader(new InputStreamReader(System.in, Charset.forName("UTF-8")));
    private static final String FRIENDS_TIMELINE_URI = "https://api.twitter.com/1.1/statuses/home_timeline.json";
    private static final Properties PROPERTIES = new Properties();
    private static final String PROPERTIES_FILE_NAME = "twitterclient.properties";
    private static final String PROPERTY_CONSUMER_KEY = "";
    private static final String PROPERTY_CONSUMER_SECRET = "";
    private static final String PROPERTY_TOKEN = "token";
    private static final String PROPERTY_TOKEN_SECRET = "tokenSecret";

    public static void main(String[] args) {
        final ConsumerCredentials consumerCredentials = new ConsumerCredentials(PROPERTY_CONSUMER_KEY, PROPERTY_CONSUMER_SECRET);

        final Feature filterFeature;

        if (PROPERTIES.getProperty(PROPERTY_TOKEN) == null) {
            final OAuth1AuthorizationFlow authFlow = OAuth1ClientSupport.builder(consumerCredentials)
                    .authorizationFlow(
                            "https://api.twitter.com/oauth/request_token",
                            "https://api.twitter.com/oauth/access_token",
                            "https://api.twitter.com/oauth/authorize")
                    .build();
            final String authorizationUri = authFlow.start();

            System.out.println("Enter the following URI into a web browser and authorize me:");
            System.out.println(authorizationUri);
            System.out.print("Enter the authorization code: ");
            final String verifier;
            try {
                verifier = IN.readLine();
            } catch (final IOException ex) {
                throw new RuntimeException(ex);
            }
            final AccessToken accessToken = authFlow.finish(verifier);

            // store access token for next application execution
            PROPERTIES.setProperty(PROPERTY_TOKEN, accessToken.getToken());
            PROPERTIES.setProperty(PROPERTY_TOKEN_SECRET, accessToken.getAccessTokenSecret());

            // get the feature that will configure the client with consumer credentials and
            // received access token
            filterFeature = authFlow.getOAuth1Feature();
        } else {
            final AccessToken storedToken = new AccessToken(PROPERTIES.getProperty(PROPERTY_TOKEN),
                    PROPERTIES.getProperty(PROPERTY_TOKEN_SECRET));
            // build a new feature from the stored consumer credentials and access token
            filterFeature = OAuth1ClientSupport.builder(consumerCredentials).feature()
                    .accessToken(storedToken).build();
        }

        // create a new Jersey client and register filter feature that will add OAuth signatures and
        // JacksonFeature that will process returned JSON data.
        final Client client = (Client) ClientBuilder.newBuilder()
                .register(filterFeature)
                .register(JacksonFeature.class)
                .build();

        try {

            String tweet = "Im living breathing proof #arest";
            
            WebTarget webTarget
                    = client.target("https://api.twitter.com/1.1/statuses/update.json?status=" + URLEncoder.encode(tweet, "UTF-8"));

            Invocation.Builder invocationBuilder
                    = webTarget.request(MediaType.APPLICATION_JSON);

            Form form = new Form();
            form.param("key", "status");
            form.param("value", "I'm Living Breathing proof");

            String input = "{\"status\":\"Im living breathing proof\"}";

            Response response
                    = invocationBuilder
                            .post(Entity.entity(form, MediaType.APPLICATION_JSON));

            if (response.getStatus() != 201) {
                throw new RuntimeException("NEED API KEY...Failed : HTTP error code : "
                        + response.getStatus() + "" + response.getStatusInfo());
            }

            System.out.println("Output from Server .... \n");
            //   String output = response.getEntity(String.class);
            // System.out.println(output);

        } catch (Exception e) {

            e.printStackTrace();

        }

    }
}
