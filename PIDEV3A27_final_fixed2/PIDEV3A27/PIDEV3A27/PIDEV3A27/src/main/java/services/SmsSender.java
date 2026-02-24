package services;

import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;

public class SmsSender {

    // 🔴 Mets TES vraies valeurs ici
    public static final String ACCOUNT_SID = "";
    public static final String AUTH_TOKEN = "";

    public static void send(String to, String body) {

        Twilio.init(ACCOUNT_SID, AUTH_TOKEN);

        Message message = Message.creator(
                new PhoneNumber(to),               // Numéro patient
                new PhoneNumber("+12693903908"),   // TON numéro Twilio
                body
        ).create();

        System.out.println("Message sent! SID: " + message.getSid());
    }
}
