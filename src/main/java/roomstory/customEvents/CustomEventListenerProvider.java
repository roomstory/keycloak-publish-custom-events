package roomstory.customEvents;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Properties;

import org.jboss.logging.Logger;
import org.keycloak.email.DefaultEmailSenderProvider;
import org.keycloak.email.EmailException;
import org.keycloak.events.Event;
import org.keycloak.events.EventListenerProvider;
import org.keycloak.events.EventType;
import org.keycloak.events.admin.AdminEvent;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RealmProvider;
import org.keycloak.models.UserModel;

public class CustomEventListenerProvider implements EventListenerProvider {


    private static final Logger log = Logger.getLogger(CustomEventListenerProvider.class);
    
    private final KeycloakSession session;
    private final RealmProvider model;
    private final Properties properties;
    private final String nsqHost;
    private final String nsqPort;
    private final String operationType;
    private final String topicName;

    public CustomEventListenerProvider(KeycloakSession session) {
        this.session = session;
        this.model = session.realms();
        this.properties = loadProperties("/config.properties");

        // Initialize other variables
        this.nsqHost = properties.getProperty("NSQ_HOST");
        this.nsqPort = properties.getProperty("NSQ_PORT");
        this.operationType = properties.getProperty("OPERATION_TYPE");
        this.topicName = properties.getProperty("TOPIC_NAME");
    }
     

    @Override
    public void onEvent(Event event) {
        if (EventType.REGISTER.equals(event.getType()) || EventType.LOGIN.equals(event.getType()) || EventType.UPDATE_PROFILE.equals(event.getType()) || EventType.REGISTER_ERROR.equals(event.getType())) {
            publishEventToTopic(event);
        }
    }

    private void publishEventToTopic(Event event) {
        log.infof("## NEW %s EVENT", event.getType());
        log.info("-----------------------------------------------------------");

        RealmModel realm = this.model.getRealm(event.getRealmId());
        UserModel newRegisteredUser = this.session.users().getUserById(realm, event.getUserId());

        String emailPlainContent = event.getType()+ "\n" +
                "Email: " + newRegisteredUser.getEmail() + "\n" +
                "Username: " + newRegisteredUser.getUsername() + "\n" +
                "Client: " + event.getClientId();

        // String emailHtmlContent = "<h1>New user registration</h1>" +
        //         "<ul>" +
        //         "<li>Email: " + newRegisteredUser.getEmail() + "</li>" +
        //         "<li>Username: " + newRegisteredUser.getUsername() + "</li>" +
        //         "<li>Client: " + event.getClientId() + "</li>" +
        //         "</ul>";

         // DefaultEmailSenderProvider senderProvider = new DefaultEmailSenderProvider(session);

        //String url = "http://host.docker.internal:4151/pub?topic=test-topic";
        String url = "http://" + nsqHost + ":" + nsqPort+ "/" +operationType + "?topic=" + topicName;
      
        try {
        URL nsqUrl = new URL(url);
        HttpURLConnection conn = (HttpURLConnection) nsqUrl.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "text/plain");
        conn.setDoOutput(true);

        OutputStream os = conn.getOutputStream();
        os.write(emailPlainContent.getBytes());
        os.flush();

        int responseCode = conn.getResponseCode();
        System.out.println("Response Code: " + responseCode);

        conn.disconnect();
      } catch (Exception e) {
        e.printStackTrace();
      }
        log.info("-----------------------------------------------------------");
    }

    private static Properties loadProperties(String configFile) {
        Properties properties = new Properties();
        try (InputStream input = CustomEventListenerProvider.class.getResourceAsStream(configFile)) {
            if (input != null) {
                properties.load(input);
            } else {
                System.err.println("Unable to find config.properties file on the classpath.");
            }
        } catch (IOException e) {
            System.err.println("Error loading config.properties file: " + e.getMessage());
            e.printStackTrace();
        }
        return properties;
    }


    @Override
    public void onEvent(AdminEvent adminEvent, boolean b) {

    }

    @Override
    public void close() {

    }
}
