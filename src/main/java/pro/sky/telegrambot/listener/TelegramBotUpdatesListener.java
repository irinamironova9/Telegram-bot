package pro.sky.telegrambot.listener;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.UpdatesListener;
import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.request.SendMessage;
import com.pengrad.telegrambot.response.SendResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import pro.sky.telegrambot.model.NotificationTask;
import pro.sky.telegrambot.repository.NotificationTaskRepository;

import javax.annotation.PostConstruct;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class TelegramBotUpdatesListener implements UpdatesListener {

    private final Logger logger = LoggerFactory.getLogger(TelegramBotUpdatesListener.class);

    @Autowired
    private TelegramBot telegramBot;

    @Autowired
    private NotificationTaskRepository repository;

    @PostConstruct
    public void init() {
        telegramBot.setUpdatesListener(this);
    }

    @Scheduled(cron = "0 0/1 * * * *")
    public void sendNotifications() {
        Collection<NotificationTask> notifications = repository.findByDateTime(
                LocalDateTime.now().truncatedTo(ChronoUnit.MINUTES));
        notifications.parallelStream().forEach(n -> {
            SendMessage message = new SendMessage(n.getChatId(), n.getText());
            SendResponse response = telegramBot.execute(message);
            if (!response.isOk()) {
                logger.error("Could not send the scheduled notification! " +
                        "Error code: {}", response.errorCode());
            }
        });
    }

    @Override
    public int process(List<Update> updates) {
        updates.forEach(update -> {
            logger.info("Processing update: {}", update);

            if (update.message().text().equals("/start")) {
                sendGreetings(update);
            } else {
                extractNotificationTask(update);
            }
        });
        return UpdatesListener.CONFIRMED_UPDATES_ALL;
    }

    private void sendGreetings(Update update) {
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

    private void extractNotificationTask(Update update) {
        logger.info("Extracting the notification task");
        Pattern pattern = Pattern.compile("([0-9.:\\s]{16})(\\s)([\\W+]+)");
        Matcher matcher = pattern.matcher(update.message().text());
        if (matcher.matches()) {
            long chatId = update.message().chat().id();
            String text = matcher.group(3);
            String dateTime = matcher.group(1);
            LocalDateTime localDateTime = LocalDateTime.parse(
                    dateTime, DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"));

            NotificationTask notificationTask = new NotificationTask();
            notificationTask.setChatId(chatId);
            notificationTask.setText(text);
            notificationTask.setDateTime(localDateTime);
            NotificationTask savedNotification = repository.save(notificationTask);

            SendMessage message = new SendMessage(chatId,
                    "Notification is saved. Date: " + savedNotification.getDateTime()
                            + " Task: " + savedNotification.getText());
            SendResponse response = telegramBot.execute(message);
            if (!response.isOk()) {
                logger.error("Could not send the confirmation of saved notification task! " +
                        "Error code: {}", response.errorCode());
            }
        }
    }
}
