package src;


import java.util.ArrayList;

public class Utils {

    // public static String decimalToBinary(String decimalString) {
    //     try {
    //         int decimal = Integer.parseInt(decimalString);
    //         return Integer.toBinaryString(decimal);
    //     } catch (NumberFormatException e) {
    //         return "Invalid input. Please provide a valid decimal number.";
    //     }
    // }

    public static String digitTo4Binary(String decimalDigit) {
        try {
            int decimal = Integer.parseInt(decimalDigit);
            String res = Integer.toBinaryString(decimal);
            while(res.length() < 4) res = "0" + res;
            return res;
        } catch (NumberFormatException e) {
            return "Invalid input. Please provide a valid decimal number.";
        }
    }

    // public static String MSISDNToOctetString(String MSISDN){ //Paramètre: MSISDN complet
    //     String res = "";
    //     String msisdn = MSISDN.charAt(0) == '+' ? MSISDN.substring(1) : MSISDN; //Pour enlever le + au début s'il y en a
    //     int len = msisdn.length();
    //     for(int i = 0; i < len; i++){
    //         res += Utils.digitTo4Binary(String.valueOf(msisdn.charAt(i)));
    //     }

    //     res = len % 2 == 0 ? res : "1111" + res;
    //     System.out.println("MSISDN = " + res);
    //     return res;
    // }

    public static byte[] MSISDNToOctetString(String MSISDN){ //Paramètre: MSISDN
        ArrayList<Integer> list = new ArrayList<Integer>();
        String msisdn = MSISDN.charAt(0) == '+' ? MSISDN.substring(1) : MSISDN; //Pour enlever le + au début s'il y en a
        int len = msisdn.length();
        for(int i = 0; i <= len - 2; i += 2){
            list.add(Character.digit(msisdn.charAt(i+1), 10) * 16 + Character.digit(msisdn.charAt(i), 10));
        }

        if(len % 2 == 1) list.add(15*16 + Character.digit(msisdn.charAt(len - 1), 10));

        byte[] res = new byte[list.size()];

        int val;
        for(int i = 0; i < list.size(); i++){
            val = list.get(i);
            res[i] = (byte)val;
        }
        return res;
    }

    public static byte[] MSISDNToOctetString(String TEL, String NDC){ //Paramètres: TEL (numéro de tel), NDC (indicatif pays, ex: +237)
        return Utils.MSISDNToOctetString(NDC + TEL);
    }

    public static byte[] PLMNToOctetString(String MCC, String MNC){//Paramètre: MCC et MNC de l'opérateur, ex: 624, 01 pour MTN Cameroun
        // String res = "";
        // res += Utils.digitTo4Binary(String.valueOf(MNC.charAt(MNC.length()-2))); //MNC digit 2
        // res += Utils.digitTo4Binary(String.valueOf(MNC.charAt(MNC.length()-1))); //MNC digit 1
        // res += MNC.length() < 3 ? "1111" : Utils.digitTo4Binary(String.valueOf(MNC.charAt(MNC.length()-3))); //MNC digit 3

        // res += Utils.digitTo4Binary(String.valueOf(MCC.charAt(MCC.length()-3))); //MCC digit 3
        // res += Utils.digitTo4Binary(String.valueOf(MCC.charAt(MCC.length()-2))); //MCC digit 2
        // res += Utils.digitTo4Binary(String.valueOf(MCC.charAt(MCC.length()-1))); //MCC digit 1

        // System.out.println("PLMN = " + res);
        // return res;
        byte[] res = new byte[3];
        int oct3 = Character.digit(MNC.charAt(1), 10); //MNC digit 2
        int oct3_ = Character.digit(MNC.charAt(0), 10); //MNC digit 1
        res[2] = (byte)(oct3 * 16 + oct3_);

        int oct2 = MNC.length() < 3 ? 15 : Character.digit(MNC.charAt(2), 10); //MNC digit 3
        int oct2_ = Character.digit(MCC.charAt(2), 10); //MCC digit 3
        res[1] = (byte)(oct2 * 16 + oct2_);

        int oct1 = Character.digit(MCC.charAt(1), 10); //MCC digit 2
        int oct1_ = Character.digit(MCC.charAt(0), 10); //MCC digit 1
        res[0] = (byte)(oct1 * 16 + oct1_);

        return res;
    }

}
