package pro.sky.telegrambot.listener;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.UpdatesListener;
import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.request.SendMessage;
import com.pengrad.telegrambot.response.SendResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.List;

@Service
public class TelegramBotUpdatesListener implements UpdatesListener {

    private final Logger logger = LoggerFactory.getLogger(TelegramBotUpdatesListener.class);

    @Autowired
    private TelegramBot telegramBot;

    @PostConstruct
    public void init() {
        telegramBot.setUpdatesListener(this);
    }

    @Override
    public int process(List<Update> updates) {
        updates.forEach(update -> {
            logger.info("Processing update: {}", update);
            if (update.message().text().equals("/start")) {
                logger.info("Sending the greeting message");
                long chatId = update.message().chat().id();
                SendMessage message = new SendMessage(chatId,
                        "Hi! I`m Bob the bot");
                SendResponse response = telegramBot.execute(message);
                if (!response.isOk()) {
                    logger.error("Could not send the greeting message! " +
                            "Error code: {}", response.errorCode());
                }
            }
        });
        return UpdatesListener.CONFIRMED_UPDATES_ALL;
    }
}
