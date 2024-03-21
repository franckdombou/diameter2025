package src;
/**
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Properties;
import java.util.Set;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.jdiameter.api.Answer;
import org.jdiameter.api.ApplicationId;
import org.jdiameter.api.Avp;
import org.jdiameter.api.AvpDataException;
import org.jdiameter.api.AvpSet;
import org.jdiameter.api.Configuration;
import org.jdiameter.api.EventListener;
import org.jdiameter.api.IllegalDiameterStateException;
import org.jdiameter.api.InternalException;
import org.jdiameter.api.Message;
import org.jdiameter.api.MetaData;
import org.jdiameter.api.Network;
import org.jdiameter.api.NetworkReqListener;
import org.jdiameter.api.OverloadException;
import org.jdiameter.api.Request;
import org.jdiameter.api.RouteException;
import org.jdiameter.api.Session;
import org.jdiameter.api.SessionFactory;
import org.jdiameter.api.Stack;
import org.jdiameter.api.StackType;
import org.jdiameter.server.impl.StackImpl;
import org.jdiameter.server.impl.helpers.XMLConfiguration;
import org.mobicents.diameter.dictionary.AvpDictionary;
import org.mobicents.diameter.dictionary.AvpRepresentation;

public class WClientRIR {


    //Initialisation du Stack Diameter et envoi de la demande RIR (Routing-Info-Request) :
    private void start() {
        try {
            // Attendre la connexion au serveur Diameter
            try {
                Thread.currentThread().sleep(5000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            // Créer une nouvelle session
            this.session = this.factory.getNewSession("BadCustomSessionId;YesWeCanPassId;" + System.currentTimeMillis());

            // Envoyer la demande RIR au serveur
            sendRIR();
        } catch (InternalException | IllegalDiameterStateException | RouteException | OverloadException e) {
            e.printStackTrace();
        }
    }

    //Gérer la réponse du serveur et envoyer les demandes suivantes en fonction du type de réponse :
    @Override
    public void receivedSuccessMessage(Request request, Answer answer) {
        dumpMessage(answer, false);

        try {
            // Si la réponse est un RIR (Routing-Info-Request)
            if (answer.getCommandCode() == RIR_commandCode) {
                // Envoyer la demande PLR (Provide-Location-Request)
                sendPLR();
            } else if (answer.getCommandCode() == PLR_commandCode) {
                // Envoyer la demande LIR (Location-Info-Request)
                sendLIR();
            } else if (answer.getCommandCode() == LIR_commandCode) {
                // Envoyer la demande IDR (Insert-Subscriber-Data)
                sendIDR();
            } else if (answer.getCommandCode() == IDR_commandCode) {
                // Envoyer la demande ULR (Update-Location-Request)
                sendULR();
            } else if (answer.getCommandCode() == ULR_commandCode) {
                log.info("FIN DU PROGRAMME");
            } else {
                log.error("Réponse incorrecte reçue : " + answer.getCommandCode());
                return;
            }
        } catch (InternalException | IllegalDiameterStateException | RouteException | OverloadException e) {
            e.printStackTrace();
        }
    }


}
**/