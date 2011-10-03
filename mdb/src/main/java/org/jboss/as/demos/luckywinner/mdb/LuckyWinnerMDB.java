package org.jboss.as.demos.luckywinner.mdb;

import java.util.Random;
import java.util.logging.Logger;

import javax.ejb.ActivationConfigProperty;
import javax.ejb.MessageDriven;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.TextMessage;

@MessageDriven(activationConfig = {
        @ActivationConfigProperty(propertyName = "destinationType", propertyValue = "javax.jms.Queue"),
        @ActivationConfigProperty(propertyName = "destination", propertyValue = "java:/queue/demo")
})
public class LuckyWinnerMDB implements MessageListener {

    static final Logger log = Logger.getLogger(LuckyWinnerMDB.class.getName());

    private final Random random = new Random();

    public void onMessage(Message message) {
        TextMessage msg = (TextMessage) message;
        try {
            LuckyWinnerMDB.log.info("-----> Processing Lucky Winner drawing entry from: " + msg.getText());
            synchronized (this) {
                if (this.random.nextInt() % 3 == 0)
                    LuckyWinnerMDB.log.info(msg.getText() + " is a winner!");
            }
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }
}
