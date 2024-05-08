package roomstory.customEvents;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

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

    public CustomEventListenerProvider(KeycloakSession session) {
        this.session = session;
        this.model = session.realms();
    }

    @Override
    public void onEvent(Event event) {

        if (EventType.REGISTER.equals(event.getType())) {
            publishEventToTopic(event);
        }
        else if(EventType.LOGIN.equals(event.getType())) {
            publishEventToTopic(event);        
        }
        else if(EventType.UPDATE_PROFILE.equals(event.getType())){
            publishEventToTopic(event);
        }

    }

    private void publishEventToTopic(Event event) {
        log.infof("## NEW %s EVENT", event.getType());
        log.info("-----------------------------------------------------------");

        RealmModel realm = this.model.getRealm(event.getRealmId());
        UserModel newRegisteredUser = this.session.users().getUserById(realm, event.getUserId());

        String emailPlainContent = "New user registration\n\n" +
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

        String url = "http://host.docker.internal:4151/pub?topic=test-topic";
      
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

    @Override
    public void onEvent(AdminEvent adminEvent, boolean b) {

    }

    @Override
    public void close() {

    }
}
