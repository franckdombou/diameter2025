
package src;

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

import src.utils.Utils ;

public class ClientLocal implements EventListener<Request, Answer> {

    private static final Logger log = Logger.getLogger(ClientLocal.class);
    static{
        //configure logging.
        configLog4j();
    }

    //log4j  framework de journalisation populaire pour Java.
    //le processus d'enregistrement d'informations sur les événements qui se produisent dans un système informatique.
    private static void configLog4j() {
        InputStream inStreamLog4j = ClientLocal.class.getResourceAsStream("log4j.properties");
        Properties propertiesLog4j = new Properties();
        try {
            propertiesLog4j.load(inStreamLog4j);
            PropertyConfigurator.configure(propertiesLog4j);
        } catch (Exception e) {
            e.printStackTrace();
        }finally
        {
            if(inStreamLog4j!=null)
            {
                try {
                    inStreamLog4j.close();
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        }
        log.debug("log4j configured");
    }

    //configuration files
    private static final String configFile = "resources/org/example/client/client-jdiameter-config-local.xml";
    private static final String dictionaryFile = "resources/org/example/client/dictionary.xml";
    //our destination
   // djou private static final String serverHost = "127.0.0.1";
   //djou  private static final String serverHost = "dodsc01.epc.mnc001.mcc624.3gppnetwork.org";
    private static final String serverHost = "antic01.epc.mnc001.mcc624.3gppnetwork.org";
    private static final String serverPort = "3868";
    private static final String serverURI = "aaa://" + serverHost + ":" + serverPort;
    //our realm
   private static final String realmName = "exchange.example.org";
 //   private static final String realmName = "epc.mnc001.mcc624.3gppnetwork.org";

    // private static  int counter = 0;

    // definition of codes, IDs

    private static final int RIR_commandCode = 8388622; //Routing-Info-Request (SLh)
    private static final int PLR_commandCode = 8388620; //Provide-Location-Request (SLg)
    private static final int LRR_commandCode = 8388621; //Location-Report-Request (SLg)
    private static final int LIR_commandCode = 302; //Location-Info-Request (Cx)
    private static final int IDR_commandCode = 319; //Insert-Subscriber-Data (S6a)
    private static final int ULR_commandCode = 316; //Update-Location-Request (S6a)

    private static final long Vendor_Id_3GPP = 10415L;

    private static final long SLh_applicationID = 16777291;
    private static final long SLg_applicationID = 16777255;
    private static final long Cx_applicationID = 16777216;
    private static final long S6a_applicationID = 16777251;

    private ApplicationId SLh_authAppId = ApplicationId.createByAuthAppId(SLh_applicationID);
    private ApplicationId SLg_authAppId = ApplicationId.createByAuthAppId(SLg_applicationID);
    private ApplicationId Cx_authAppId = ApplicationId.createByAuthAppId(Cx_applicationID);
    private ApplicationId S6a_authAppId = ApplicationId.createByAuthAppId(S6a_applicationID);


    private static final int commandCode = 316;
    private static final long vendorID = 66666;
    private static final long applicationID = 16777251;
    private ApplicationId authAppId = ApplicationId.createByAuthAppId(applicationID);
    private static final int exchangeTypeCode = 888;
    private static final int exchangeDataCode = 999;
    // enum values for Exchange-Type AVP
    private static final int EXCHANGE_TYPE_INITIAL = 0;
    private static final int EXCHANGE_TYPE_INTERMEDIATE = 1;
    private static final int EXCHANGE_TYPE_TERMINATING = 2;
    //list of data we want to exchange.
    private static final String[] TO_SEND = new String[] { "I want to get 3 answers", "This is second message", "Bye bye" };
    //Dictionary, for informational purposes.
    private AvpDictionary dictionary = AvpDictionary.INSTANCE;
    //stack and session factory
    private Stack stack;
    private SessionFactory factory;

    // ////////////////////////////////////////
    // Objects which will be used in action //
    // ////////////////////////////////////////
    /*
    * une abstraction qui représente une communication Diameter et qui permet à
    * l'application d'échanger des messages et de gérer le contexte de la communication avec le serveur.
    * */
    private Session session;  // session used as handle for communication
    private int toSendIndex = 0;  //index in TO_SEND table
    private boolean finished = false;  //boolean telling if we finished our interaction

    /**
    *  cette fonction configure la couche de communication permettant
     *  à votre application d'interagir avec les serveurs Diameter.
     * */
    private void initStack() {
        if (log.isInfoEnabled()) {
            log.info("Initialisation de la pile..."); // Message d'information sur l'initialisation
        }

        InputStream is = null;
        try {
            // Analyse du dictionnaire (à but informatif)
            dictionary.parseDictionary(this.getClass().getClassLoader().getResourceAsStream(dictionaryFile));
            log.info("Dictionnaire AVP analysé avec succès."); // Message de confirmation d'analyse du dictionnaire

            this.stack = new StackImpl(); // Création d'un nouvel objet StackImpl (implémentation de la pile Diameter)

            // Analyse de la configuration de la pile à partir d'un fichier XML
            is = this.getClass().getClassLoader().getResourceAsStream(configFile);
            Configuration config = new XMLConfiguration(is);
            factory = stack.init(config); // Initialisation de la pile en utilisant la configuration
            if (log.isInfoEnabled()) {
                log.info("Configuration de la pile chargée avec succès."); // Message de confirmation de chargement de la configuration
            }

            // Affichage des informations sur les applications prises en charge
            Set<org.jdiameter.api.ApplicationId> appIds = stack.getMetaData().getLocalPeer().getCommonApplications();
            log.info("Pile Diameter :: Prise en charge de " + appIds.size() + " applications.");
            for (org.jdiameter.api.ApplicationId x : appIds) {
                log.info("Pile Diameter :: Commune :: " + x); // Message d'information sur chaque ID d'application pris en charge
            }
            is.close(); // Fermeture du flux d'entrée

            // Enregistrement de l'écouteur de requêtes réseau (même s'il n'est pas utilisé ici)
            Network network = stack.unwrap(Network.class);
            network.addNetworkReqListener(new NetworkReqListener() {

                @Override
                public Answer processRequest(Request request) {
                    // Cette méthode ne sera probablement pas appelée car configurée pour ne pas recevoir de requêtes
                    return null;
                }
            }, this.SLh_authAppId, this.SLg_authAppId, this.Cx_authAppId, this.S6a_authAppId); // Enregistrement de l'écouteur pour des ID d'application spécifiques

        } catch (Exception e) {
            e.printStackTrace(); // Impression de la pile d'appels en cas d'exceptions

            // Nettoyage en cas d'erreurs
            if (this.stack != null) {
                this.stack.destroy();
            }

            if (is != null) {
                try {
                    is.close();
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
            }
            return;
        }

        MetaData metaData = stack.getMetaData();
        // Ignorer pour l'instant (validation future potentielle)

        try {
            if (log.isInfoEnabled()) {
                log.info("Démarrage de la pile");
            }
            stack.start(); // Démarrage de la pile Diameter
            if (log.isInfoEnabled()) {
                log.info("La pile est en cours d'exécution.");
            }
        } catch (Exception e) {
            e.printStackTrace(); // Impression de la pile d'appels en cas d'exceptions
            stack.destroy(); // Destruction de la pile en cas d'échec du démarrage
            return;
        }
        if (log.isInfoEnabled()) {
            log.info("Initialisation de la pile terminée avec succès.");
        }
    }


    /**
     * @return
     * Elle sert à vérifier l'état de l'interaction de
     * l'application avec le serveur Diameter.
     */
    private boolean finished() {
        return this.finished;
    }

    /**
     *
     */
    //Initialisation du Stack Diameter et envoi de la demande RIR (Routing-Info-Request) :
    private void start() {
        try {
            //Attendre la connexion au serveur Diameter
            try {
                Thread.currentThread().sleep(5000);
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            //Créer une nouvelle session
           // djou this.session = this.factory.getNewSession("BadCustomSessionId;YesWeCanPassId;" + System.currentTimeMillis());
            this.session = this.factory.getNewSession("Session;" + System.currentTimeMillis());

            //Envoyer la demande RIR au serveur
            sendRIR(null,"23768219893");
           // sendPLR();
        } catch (InternalException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IllegalDiameterStateException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (RouteException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (OverloadException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

    }

    // réinitialise le canal de communication avec le serveur Diameter.

    public static byte[] hexStringToByteArray(String s) {
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                    + Character.digit(s.charAt(i+1), 16));
        }
        return data;
    }

    private void sendRIR1() throws InternalException, IllegalDiameterStateException, RouteException, OverloadException{
        Request r = this.session.createRequest(RIR_commandCode, SLh_authAppId, realmName, serverURI);
        AvpSet requestAvps = r.getAvps();
        // requestAvps.removeAvp(293); //Retire l'AVP 293 (Destination-Host)

        requestAvps.addAvp(1, "62401655479812", false); //IMSI
        // requestAvps.addAvp(701, "655479812", true); //MSISDN

        this.session.send(r, this);
        dumpMessage(r,true);
    }

    //Objectif: Déterminer le serveur Diameter responsable de la gestion d'un abonné spécifique.
    private void sendRIR(String IMSI, String MSISDN) throws InternalException, IllegalDiameterStateException, RouteException, OverloadException {
        Request r = this.session.createRequest(RIR_commandCode, SLh_authAppId, realmName, serverURI);

        /**
         * Permet à la requête d'être transmise par des proxies Diameter intermédiaires.
         * */
        r.setProxiable(true);


        AvpSet requestAvps = r.getAvps();

        /**
         * Supprime l'AVP Destination-Host,
         * car il sera déterminé automatiquement par la pile Diameter.
         * */
        requestAvps.removeAvp(293);

        // Ajouter d'autres AVPs nécessaires pour la géolocalisation

        // requestAvps.addAvp(1, "624017526407740", true, false, false); //IMSI
        /**
         * Ajoute l'AVP IMSI (1) et l'AVP MSISDN (701) à la requête si les valeurs correspondantes sont fournies,
         * en les convertissant au format OctetString pour l'AVP MSISDN.
         * */
        if(IMSI != null && !IMSI.isEmpty()){
            requestAvps.addAvp(1, IMSI, true, false, false); //IMSI
        }
        if(MSISDN != null && !MSISDN.isEmpty()){
            requestAvps.addAvp(701, Utils.MSISDNToOctetString(MSISDN), Vendor_Id_3GPP, true, false); //MSISDN (OctetString)
        }

        // Envoyer la requête au serveur Diameter
        this.session.send(r, this);
        dumpMessage(r, true);
    }


    //Objectif: Obtenir les coordonnées géographiques de l'abonné.
    public void sendPLR() throws InternalException, IllegalDiameterStateException, RouteException, OverloadException{
        Request r = this.session.createRequest(PLR_commandCode, SLg_authAppId, realmName, serverURI);
        r.setProxiable(true);
        AvpSet requestAvps = r.getAvps();
        requestAvps.removeAvp(293); //Retire l'AVP 293 (Destination-Host)

        // requestAvps.addAvp(1, "624017526407740", true, false, false); //IMSI
        requestAvps.addAvp(701, Utils.MSISDNToOctetString("698219893", "237"), Vendor_Id_3GPP, true, false); //MSISDN (OctetString)
        // requestAvps.addAvp(1402, "861538615386153", Vendor_Id_3GPP, true, false, false); //IMEI
        requestAvps.addAvp(2500, 1, Vendor_Id_3GPP, true, false); //SLg-Location-Type (Enumerated) CURRENT_OR_LAST_KNOWN_LOCATION (1)
        requestAvps.addAvp(1241, 3, Vendor_Id_3GPP, true, false); //LCS-Client-Type (Enumerated) 3 LAWFUL_INTERCEPT_SERVICES, 1 VALUE_ADDED_SERVICES

        AvpSet clientNameAvps = requestAvps.addGroupedAvp(2501, Vendor_Id_3GPP, true, false); //LCS-EPS-Client-Name (Grouped)
        clientNameAvps.addAvp(1238, "237698219893", Vendor_Id_3GPP, true, false, false); //LCS-Name-String
        clientNameAvps.addAvp(1237, 2, Vendor_Id_3GPP, true, false); //LCS-Format-Indicator (Enumerated),0 LOGICAL_NAME, 1 EMAIL_ADDRESS, 2 MSISDN, 3 URL, 4 SIP_URL

        this.session.send(r, this);
        dumpMessage(r,true);
    }

     private void sendLRR() throws InternalException, IllegalDiameterStateException, RouteException, OverloadException{
     	Request r = this.session.createRequest(LRR_commandCode, SLg_authAppId, realmName, serverURI);
     	AvpSet requestAvps = r.getAvps();
      requestAvps.removeAvp(293); //Retire l'AVP 293 (Destination-Host)

     	//requestAvps.addAvp(1, "62401655479812", false); //IMSI
          requestAvps.addAvp(701, "698219893", true); //MSISDN 701

     	this.session.send(r, this);
     	dumpMessage(r,true);
     }

    private void sendLIR() throws InternalException, IllegalDiameterStateException, RouteException, OverloadException{
        Request r = this.session.createRequest(LIR_commandCode, Cx_authAppId, realmName, serverURI);
        AvpSet requestAvps = r.getAvps();
        requestAvps.removeAvp(293); //Retire l'AVP 293 (Destination-Host)

        requestAvps.addAvp(601, "tel:+1-201-555-0123", false); //Public-Identity

        this.session.send(r, this);
        dumpMessage(r,true);
    }

    //Insert-Subscriber-Data
    private void sendIDR() throws InternalException, IllegalDiameterStateException, RouteException, OverloadException{
        Request r = this.session.createRequest(IDR_commandCode, S6a_authAppId, realmName, serverURI);
        AvpSet requestAvps = r.getAvps();
        requestAvps.removeAvp(293); //Retire l'AVP 293 (Destination-Host)

        requestAvps.addAvp(1, "62401655479812", false); //IMSI
        // requestAvps.addAvp(701, "655479812", true); //MSISDN

        this.session.send(r, this);
        dumpMessage(r,true);
    }

    private void sendULR() throws InternalException, IllegalDiameterStateException, RouteException, OverloadException{
        Request r = this.session.createRequest(ULR_commandCode, S6a_authAppId, realmName, serverURI);
        AvpSet requestAvps = r.getAvps();
        requestAvps.removeAvp(293); //Retire l'AVP 293 (Destination-Host)

        requestAvps.addAvp(1, "62401655479812", false); //IMSI
        // requestAvps.addAvp(701, "655479812", true); //MSISDN

        this.session.send(r, this);
        dumpMessage(r,true);
    }

    private void sendNextRequest(int enumType) throws InternalException, IllegalDiameterStateException, RouteException, OverloadException {
        Request r = this.session.createRequest(commandCode, this.authAppId, realmName, serverURI);
        // here we have all except our custom avps

        // example how to remove AVP
        AvpSet requestAvps = r.getAvps();
        requestAvps.removeAvp(293);

        // example how to add AVP IMSI
        byte[] b = hexStringToByteArray("31313131313131313131313131313131");
        requestAvps.addAvp(1, b, true, false);

        // code , value , vendor, mandatory,protected,isUnsigned32
        // (Enumerated)
        Avp exchangeType = requestAvps.addAvp(exchangeTypeCode, (long) enumType, vendorID, true, false, true); // value
        // is
        // set
        // on
        // creation
        // code , value , vendor, mandatory,protected, isOctetString
        Avp exchengeData = requestAvps.addAvp(exchangeDataCode, TO_SEND[toSendIndex++], vendorID, true, false, false); // value
        // is

        // set
        // on
        // creation
        // send
        this.session.send(r, this);
        dumpMessage(r,true); //dump info on console
    }


    @Override
    public void receivedSuccessMessage(Request request, Answer answer){
        dumpMessage(answer,false);

        try{
            // Si la réponse est un RIR (Routing-Info-Request)
            if (answer.getCommandCode() == RIR_commandCode) {
                log.info("ROUTING INFO ANSWER RECEIVED");
                System.out.println("## Une réponse \"RIA\" est arrivée mais on ne trouve pas l'opération correspondante ##");
                sendPLR();
            }
            // Envoyer la demande LIR (Location-Info-Request)
            else if (answer.getCommandCode() == PLR_commandCode) {
                sendLIR();
            }
            // Envoyer la demande IDR (Insert-Subscriber-Data)
            else if (answer.getCommandCode() == LIR_commandCode) {
                sendIDR();
            }
            // Envoyer la demande ULR (Update-Location-Request)
            else if (answer.getCommandCode() == IDR_commandCode) {
                sendULR();
            }
            else if (answer.getCommandCode() == ULR_commandCode) {
                log.info("END OF THE PROGRAM");
            }
            else {
                log.error("Received bad answer: " + answer.getCommandCode());
                return;
            }
        } catch (InternalException e) {
            e.printStackTrace();
        }
        catch (IllegalDiameterStateException e) {
            e.printStackTrace();
        }
        catch (RouteException e) {
            e.printStackTrace();
        }
        catch (OverloadException e) {
            e.printStackTrace();
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see org.jdiameter.api.EventListener#receivedSuccessMessage(org.jdiameter
     * .api.Message, org.jdiameter.api.Message)
     */
    //@Override
    public void receivedSuccessMessage2(Request request, Answer answer) { //Ancienne méthode
        dumpMessage(answer,false);
        if (answer.getCommandCode() != commandCode) {
            log.error("Received bad answer: " + answer.getCommandCode());
            return;
        }
        AvpSet answerAvpSet = answer.getAvps();

        Avp exchangeTypeAvp = answerAvpSet.getAvp(exchangeTypeCode, vendorID);
        Avp exchangeDataAvp = answerAvpSet.getAvp(exchangeDataCode, vendorID);
        Avp resultAvp = answer.getResultCode();


        try {
            //for bad formatted request.
            if (resultAvp.getUnsigned32() == 5005 || resultAvp.getUnsigned32() == 5004) {
                // missing || bad value of avp
                this.session.release();
                this.session = null;
                log.error("Something wrong happened at server side!");
                finished = true;
            }
            switch ((int) exchangeTypeAvp.getUnsigned32()) {
                case EXCHANGE_TYPE_INITIAL:
                    // JIC check;
                    String data = exchangeDataAvp.getUTF8String();
                    if (data.equals(TO_SEND[toSendIndex - 1])) {
                        // ok :) send next;
                        sendNextRequest(EXCHANGE_TYPE_INTERMEDIATE);
                    } else {
                        log.error("Received wrong Exchange-Data: " + data);
                    }
                    break;
                case EXCHANGE_TYPE_INTERMEDIATE:
                    // JIC check;
                    data = exchangeDataAvp.getUTF8String();
                    if (data.equals(TO_SEND[toSendIndex - 1])) {
                        // ok :) send next;
                        sendNextRequest(EXCHANGE_TYPE_TERMINATING);
                    } else {
                        log.error("Received wrong Exchange-Data: " + data);
                    }
                    break;
                case EXCHANGE_TYPE_TERMINATING:
                    data = exchangeDataAvp.getUTF8String();
                    if (data.equals(TO_SEND[toSendIndex - 1])) {
                        // good, we reached end of FSM.
                        finished = true;
                        // release session and its resources.
                        this.session.release();
                        this.session = null;
                    } else {
                        log.error("Received wrong Exchange-Data: " + data);
                    }
                    break;
                default:
                    log.error("Bad value of Exchange-Type avp: " + exchangeTypeAvp.getUnsigned32());
                    break;
            }
        } catch (AvpDataException e) {
            // thrown when interpretation of byte[] fails
            e.printStackTrace();
        } catch (InternalException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IllegalDiameterStateException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (RouteException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (OverloadException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

    }

    /*
     * (non-Javadoc)
     *
     * @see org.jdiameter.api.EventListener#timeoutExpired(org.jdiameter.api.
     * Message)
     */
    @Override
    public void timeoutExpired(Request request) {


    }

    //Imprime des informations sur un message Diameter dans un journal à des fins de débogage.
    private void dumpMessage(Message message, boolean sending) {
        if (log.isInfoEnabled()) {
            log.info((sending?"Sending ":"Received ") + (message.isRequest() ? "Request: " : "Answer: ") + message.getCommandCode() + "\nE2E:"
                    + message.getEndToEndIdentifier() + "\nHBH:" + message.getHopByHopIdentifier() + "\nAppID:" + message.getApplicationId());
            log.info("AVPS["+message.getAvps().size()+"]: \n");
            try {
                printAvps(message.getAvps());
            } catch (AvpDataException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }

    private void printAvps(AvpSet avpSet) throws AvpDataException {
        printAvpsAux(avpSet, 0);
    }

    /**
     * Prints the AVPs present in an AvpSet with a specified 'tab' level
     *
     * @param avpSet
     *            the AvpSet containing the AVPs to be printed
     * @param level
     *            an int representing the number of 'tabs' to make a pretty
     *            print
     * @throws AvpDataException
     */
    private void printAvpsAux(AvpSet avpSet, int level) throws AvpDataException {
        String prefix = "                      ".substring(0, level * 2);

        for (Avp avp : avpSet) {
            AvpRepresentation avpRep = AvpDictionary.INSTANCE.getAvp(avp.getCode(), avp.getVendorId());

            if (avpRep != null && avpRep.getType().equals("Grouped")) {
                log.info(prefix + "<avp name=\"" + avpRep.getName() + "\" code=\"" + avp.getCode() + "\" vendor=\"" + avp.getVendorId() + "\">");
                printAvpsAux(avp.getGrouped(), level + 1);
                log.info(prefix + "</avp>");
            } else if (avpRep != null) {
                String value = "";

                if (avpRep.getType().equals("Integer32"))
                    value = String.valueOf(avp.getInteger32());
                else if (avpRep.getType().equals("Integer64") || avpRep.getType().equals("Unsigned64"))
                    value = String.valueOf(avp.getInteger64());
                else if (avpRep.getType().equals("Unsigned32"))
                    value = String.valueOf(avp.getUnsigned32());
                else if (avpRep.getType().equals("Float32"))
                    value = String.valueOf(avp.getFloat32());
                else
                    //value = avp.getOctetString();
                    value = new String(avp.getOctetString(), StandardCharsets.UTF_8);

                log.info(prefix + "<avp name=\"" + avpRep.getName() + "\" code=\"" + avp.getCode() + "\" vendor=\"" + avp.getVendorId()
                        + "\" value=\"" + value + "\" />");
            }
        }
    }

    public static void main(String[] args) {
        ClientLocal ec = new ClientLocal();
        ec.initStack();
        ec.start();

        while (!ec.finished()) {
            try {
                Thread.currentThread().sleep(5000);
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }

}