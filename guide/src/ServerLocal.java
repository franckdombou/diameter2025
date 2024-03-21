package src;

import java.util.Map;

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
import org.jdiameter.api.InternalException;
import org.jdiameter.api.Message;
import org.jdiameter.api.MetaData;
import org.jdiameter.api.Network;
import org.jdiameter.api.NetworkReqListener;
import org.jdiameter.api.Request;
import org.jdiameter.api.Session;
import org.jdiameter.api.SessionFactory;
import org.jdiameter.api.Stack;
import org.jdiameter.api.StackType;
import org.jdiameter.server.impl.StackImpl;
import org.jdiameter.server.impl.helpers.XMLConfiguration;
import org.mobicents.diameter.dictionary.AvpDictionary;
import org.mobicents.diameter.dictionary.AvpRepresentation;


// Importez les classes nécessaires pour la géolocalisation

/**
 * @author baranowb
 *
 */
public class ServerLocal implements NetworkReqListener {
    private static final Logger log = Logger.getLogger(ServerLocal.class);
    static{
        configLog4j();

    }

    private static void configLog4j() {
        InputStream inStreamLog4j = ServerLocal.class.getResourceAsStream("log4j.properties");
        Properties propertiesLog4j = new Properties();
        try {
            propertiesLog4j.load(inStreamLog4j);
            PropertyConfigurator.configure(propertiesLog4j);
        } catch (Exception e) {
            e.printStackTrace();
        }

        log.debug("log4j configured");

    }
    private static final String configFile = "resources/org/example/server/server-jdiameter-config-local.xml";
    private static final String dictionaryFile = "resources/org/example/server/dictionary.xml";
    // djou private static final String realmName = "exchange.example.org";
    private static final String realmName = "epc.mnc001.mcc624.3gppnetwork.org";


    private static final long SLh_applicationID = 16777291;
    private static final long SLg_applicationID = 16777255;
    private static final long Cx_applicationID = 16777216;
    private static final long S6a_applicationID = 16777251;

    private ApplicationId SLh_authAppId = ApplicationId.createByAuthAppId(SLh_applicationID);
    private ApplicationId SLg_authAppId = ApplicationId.createByAuthAppId(SLg_applicationID);
    private ApplicationId Cx_authAppId = ApplicationId.createByAuthAppId(Cx_applicationID);
    private ApplicationId S6a_authAppId = ApplicationId.createByAuthAppId(S6a_applicationID);

    // Defs for our app
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

    private static final String[] TO_RECEIVE = new String[] { "I want to get 3 answers", "This is second message", "Bye bye" };
    private AvpDictionary dictionary = AvpDictionary.INSTANCE;
    private Stack stack;
    private SessionFactory factory;

    // ////////////////////////////////////////
    // Objects which will be used in action //
    // ////////////////////////////////////////
    private Session session;
    private int toReceiveIndex = 0;
    private boolean finished = false;

    private void initStack() {
        if (log.isInfoEnabled()) {
            log.info("Initializing Stack...");
        }
        InputStream is = null;
        try {
            dictionary.parseDictionary(this.getClass().getClassLoader().getResourceAsStream(dictionaryFile));
            log.info("AVP Dictionary successfully parsed.");
            this.stack = new StackImpl();

            is = this.getClass().getClassLoader().getResourceAsStream(configFile);

            Configuration config = new XMLConfiguration(is);
            factory = stack.init(config);
            if (log.isInfoEnabled()) {
                log.info("Stack Configuration successfully loaded.");
            }

            Set<org.jdiameter.api.ApplicationId> appIds = stack.getMetaData().getLocalPeer().getCommonApplications();

            log.info("Diameter Stack  :: Supporting " + appIds.size() + " applications.");
            for (org.jdiameter.api.ApplicationId x : appIds) {
                log.info("Diameter Stack  :: Common :: " + x);
            }
            is.close();
            Network network = stack.unwrap(Network.class);
            // network.addNetworkReqListener(this, this.authAppId);
            network.addNetworkReqListener(this, this.authAppId, SLh_authAppId, SLg_authAppId, Cx_authAppId, S6a_authAppId);

        } catch (Exception e) {
            e.printStackTrace();
            if (this.stack != null) {
                this.stack.destroy();
            }

            if (is != null) {
                try {
                    is.close();
                } catch (IOException e1) {
                    // TODO Auto-generated catch block
                    e1.printStackTrace();
                }
            }
            return;
        }

        MetaData metaData = stack.getMetaData();
        if (metaData.getStackType() != StackType.TYPE_SERVER || metaData.getMinorVersion() <= 0) {
            stack.destroy();
            if (log.isEnabledFor(org.apache.log4j.Level.ERROR)) {
                log.error("Incorrect driver");
            }
            return;
        }

        try {
            if (log.isInfoEnabled()) {
                log.info("Starting stack");
            }
            stack.start();
            if (log.isInfoEnabled()) {
                log.info("Stack is running.");
            }
        } catch (Exception e) {
            e.printStackTrace();
            stack.destroy();
            return;
        }
        if (log.isInfoEnabled()) {
            log.info("Stack initialization successfully completed.");
        }
    }

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




    /**
     * @return
     */
    private boolean finished() {
        return this.finished;
    }

    public static void main(String[] args) {
        ServerLocal es = new ServerLocal();
        es.initStack();

        while (!es.finished()) {
            try {
                Thread.currentThread().sleep(5000);
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }



  //  @Override
    public Answer processRequest1(Request request) {
        System.out.println("METHODE PROCESSREQUEST");
        dumpMessage(request,false);

        Answer answer = request.createAnswer(2001);
        AvpSet answerAvps = answer.getAvps();
        //add origin, its required by duplicate detection
        answerAvps.addAvp(Avp.ORIGIN_HOST, stack.getMetaData().getLocalPeer().getUri().getFQDN(), true, false, true);
        answerAvps.addAvp(Avp.ORIGIN_REALM, stack.getMetaData().getLocalPeer().getRealmName(), true, false, true);
        dumpMessage(answer,true);

        return answer;
    }


    @Override
    public Answer processRequest(Request request) {
        dumpMessage(request, false);

        // Traitez la demande de géolocalisation
        Map<Integer, String> locationInfo = extractLocationInfo(request);

        // Créez une réponse basée sur les informations de géolocalisation
        Answer answer = createLocationAnswer(request, locationInfo);

        // Envoyez la réponse
        dumpMessage(answer, true);
        return answer;
    }

    // Méthode pour extraire les informations de géolocalisation de la demande
    private Map<Integer, String> extractLocationInfo(Request request) {
        // Implémentez la logique pour extraire les informations de géolocalisation de la demande
        // Vous pouvez accéder aux AVPs de la demande à l'aide de request.getAvps()
        // et extraire les informations pertinentes
        return null; // Remplacez ceci par votre logique d'extraction des informations de géolocalisation
    }

    // Méthode pour créer une réponse basée sur les informations de géolocalisation
    private Answer createLocationAnswer(Request request, Map<Integer, String> locationInfo) {
        Answer answer = request.createAnswer(2001);
        AvpSet answerAvps = answer.getAvps();

        // Ajoutez les AVPs pour les informations de géolocalisation à la réponse
        if (locationInfo != null) {
            for (Map.Entry<Integer, String> entry : locationInfo.entrySet()) {
                answerAvps.addAvp(entry.getKey(), entry.getValue().getBytes(), true, false);
            }
        }

        // Ajoutez d'autres AVPs si nécessaire

        // Assurez-vous d'ajouter les AVPs ORIGIN
        answerAvps.addAvp(Avp.ORIGIN_HOST, stack.getMetaData().getLocalPeer().getUri().getFQDN(), true, false, true);
        answerAvps.addAvp(Avp.ORIGIN_REALM, stack.getMetaData().getLocalPeer().getRealmName(), true, false, true);

        return answer;
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * org.jdiameter.api.NetworkReqListener#processRequest(org.jdiameter.api
     * .Request)
     */
    // @Override
    public Answer processRequest2(Request request) { //Ancien code
        dumpMessage(request,false);
        if (request.getCommandCode() != commandCode) {
            log.error("Received bad answer: " + request.getCommandCode());
            return null;
        }
        AvpSet requestAvpSet = request.getAvps();

        Avp exchangeTypeAvp = requestAvpSet.getAvp(exchangeTypeCode, vendorID);
        Avp exchangeDataAvp = requestAvpSet.getAvp(exchangeDataCode, vendorID);
        if (exchangeTypeAvp == null) {
            log.error("Request does not have Exchange-Type");

            Answer answer = createAnswer(request, 5004, EXCHANGE_TYPE_TERMINATING);
            dumpMessage(answer,true);
            return answer; // set
            // exchange
            // type
            // to
            // terminating
        }
        if (exchangeDataAvp == null) {
            log.error("Request does not have Exchange-Data");
            Answer answer = createAnswer(request, 5004, EXCHANGE_TYPE_TERMINATING);
            dumpMessage(answer,true);
            return answer; // set
            // exchange
            // type
            // to
            // terminating
        }
        // cast back to int(Enumerated is Unsigned32, and API represents it as
        // long so its easier
        // to manipulate
        try {
            switch ((int) exchangeTypeAvp.getUnsigned32()) {
                case EXCHANGE_TYPE_INITIAL:
                    // JIC check;
                    String data = exchangeDataAvp.getUTF8String();
                    this.session = this.factory.getNewSession(request.getSessionId());
                    if (data.equals(TO_RECEIVE[toReceiveIndex])) {
                        // create session;

                        Answer answer = createAnswer(request, 2001, EXCHANGE_TYPE_INITIAL); // set
                        // exchange
                        // type
                        // to
                        // terminating
                        toReceiveIndex++;
                        dumpMessage(answer,true);
                        return answer;
                    } else {
                        log.error("Received wrong Exchange-Data: " + data);
                        Answer answer = request.createAnswer(6000);
                    }
                    break;
                case EXCHANGE_TYPE_INTERMEDIATE:
                    // JIC check;
                    data = exchangeDataAvp.getUTF8String();
                    if (data.equals(TO_RECEIVE[toReceiveIndex])) {

                        Answer answer = createAnswer(request, 2001, EXCHANGE_TYPE_INTERMEDIATE); // set
                        // exchange
                        // type
                        // to
                        // terminating
                        toReceiveIndex++;
                        dumpMessage(answer,true);
                        return answer;
                    } else {
                        log.error("Received wrong Exchange-Data: " + data);
                    }
                    break;
                case EXCHANGE_TYPE_TERMINATING:
                    data = exchangeDataAvp.getUTF8String();
                    if (data.equals(TO_RECEIVE[toReceiveIndex])) {
                        // good, we reached end of FSM.
                        finished = true;
                        // release session and its resources.
                        Answer answer = createAnswer(request, 2001, EXCHANGE_TYPE_TERMINATING); // set
                        // exchange
                        // type
                        // to
                        // terminating
                        toReceiveIndex++;
                        this.session.release();
                        finished = true;
                        this.session = null;
                        dumpMessage(answer,true);
                        return answer;

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
        }
        //error, something bad happened.
        finished = true;
        return null;
    }

    private Answer createAnswer(Request r, int resultCode, int enumType) {
        Answer answer = r.createAnswer(resultCode);
        AvpSet answerAvps = answer.getAvps();
        // code , value , vendor, mandatory,protected,isUnsigned32
        // (Enumerated)
        Avp exchangeType = answerAvps.addAvp(exchangeTypeCode, (long) enumType, vendorID, true, false, true); // value
        // is
        // set
        // on
        // creation
        // code , value , vendor, mandatory,protected, isOctetString
        Avp exchengeData = answerAvps.addAvp(exchangeDataCode, TO_RECEIVE[toReceiveIndex], vendorID, true, false, false); // value
        // is
        // set
        // on
        // creation


        //add origin, its required by duplicate detection
        answerAvps.addAvp(Avp.ORIGIN_HOST, stack.getMetaData().getLocalPeer().getUri().getFQDN(), true, false, true);
        answerAvps.addAvp(Avp.ORIGIN_REALM, stack.getMetaData().getLocalPeer().getRealmName(), true, false, true);
        return answer;
    }
}
