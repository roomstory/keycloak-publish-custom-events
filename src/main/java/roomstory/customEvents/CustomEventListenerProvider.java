package roomstory.customEvents;

import org.jboss.logging.Logger;
import org.keycloak.events.Event;
import org.keycloak.events.EventListenerProvider;
import org.keycloak.events.admin.AdminEvent;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RealmProvider;
import org.keycloak.models.UserModel;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Arrays;
import java.util.List;

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

        String eventTypesStr = System.getenv("EVENT_TYPES");
        List<String> eventTypes = Arrays.asList(eventTypesStr.split(","));

        log.infof("## NEW %s EVENT", event.getType());
        log.info("-----------------------------------------------------------");
        System.out.println("EVENT TYPE ::" + event.getType().toString());
        if(eventTypes.contains(event.getType().toString())){
            publishEventToTopic(event);
        }
    }

    private void publishEventToTopic(Event event) {


        RealmModel realm = this.model.getRealm(event.getRealmId());
        UserModel newRegisteredUser = this.session.users().getUserById(realm, event.getUserId());

        String emailPlainContent = event.getType()+ "\n" +
                "Email: " + newRegisteredUser.getEmail() + "\n" +
                "Username: " + newRegisteredUser.getUsername() + "\n" +
                "Client: " + event.getClientId();

        String url = "http://" + System.getenv("NSQ_HOST") + ":" + System.getenv("NSQ_PORT") + "/" + System.getenv("OPERATION_TYPE") + "?topic=" + System.getenv("TOPIC_NAME");
        System.out.println("URL : " + url);

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
