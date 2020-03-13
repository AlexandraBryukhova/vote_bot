import org.telegram.telegrambots.ApiContextInitializer;
import org.telegram.telegrambots.bots.DefaultBotOptions;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.ApiContext;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.apache.log4j.Logger;
import java.lang.String;

import java.io.*;
import java.util.*;

public class Vioce_bot extends TelegramLongPollingBot {

    static Map<Integer, String> playerName = new HashMap<>(); // ID / Name
    static Map<Integer, Integer> playerVotes = new HashMap<>(); // ID / Votes
    static Map<String, Integer> phone = new HashMap<>(); // Voter Name / ID for voted
    static final Logger logger = Logger.getLogger(Vioce_bot.class);
    static String BotToken, BotUsername, startMsg, voteMsg, wrongFormatMsg, wrongNumberMsg, statusMsg, revoteMsg;
    static String ruVarsMsg, ruVoteMsg, ruRevoteMsg, ruWrongNumberMsg, ruWrongFormatMsg, ruStartMsg, ruDoubleVoteMsg;

    public Vioce_bot(DefaultBotOptions botOptions)
    {
        super(botOptions);
    }

    public static void main(String[] args) {
        settings();
        ApiContextInitializer.init();
        TelegramBotsApi telegramBotsApi = new TelegramBotsApi();
        try {
            DefaultBotOptions botOptions = ApiContext.getInstance(DefaultBotOptions.class);
            botOptions.setProxyHost("96.113.166.133");
            botOptions.setProxyPort(1080);
            botOptions.setProxyType(DefaultBotOptions.ProxyType.SOCKS5);
            telegramBotsApi.registerBot(new Vioce_bot(botOptions));
        } catch (TelegramApiException e) {
            logger.error("ERROR TELEGRAM BOT TOKEN: " + e);
            System.exit(0);
        }
        logger.info("BOT WORKING");
    }

    public void onUpdateReceived(Update update) {
        Message message = update.getMessage();
        if (message != null && message.hasText()) {
            if (message.getText().equals("/start") && message.getFrom().getLanguageCode().equals("ru")) {
                sndMsg(message, ruStartMsg);
                logger.info("Posted /start message");
                sndMsg(message, ruVarsMsg);
            }
            else if (message.getText().equals("/start")) {
                sndMsg(message, startMsg);
                logger.info("Posted /start message");
                sndMsg(message, ruVarsMsg);
            }
            else if (message.getFrom().getLanguageCode().equals("ru")) {
                newVoteRu(message);
            }
            else {
                newVote(message);
            }
        } else if (message != null && !message.hasText()) {
            if (message.getFrom().getLanguageCode().equals("ru")) {
                sndMsg(message, ruWrongFormatMsg);
                logger.info("Posted RuWrongFormatMsg message");
            } else {
                sndMsg(message, wrongFormatMsg);
                logger.info("Posted WrongFormatMsg message");
            }
        }
    }

    public static void settings() {
        Properties properties = new Properties();
        FileInputStream in = null;
        try {
            in = new FileInputStream("src/main/resources/config.properties");
            properties.load(in);
        } catch (FileNotFoundException e) {
            logger.error("FILE NOT FOUND EXCEPTION: " + e);
        } catch (IOException e) {
            logger.error("IO EXCEPTION: " + e);
        }

        BotToken = properties.getProperty("BotToken");
        BotUsername = properties.getProperty("BotUsername", "Unknown Bot");

        // ADDING VARS
        String players = properties.getProperty("allPlayers");
        int id = 1;
        while (players.contains(",")) {
            playerName.put(id, players.substring(0, players.indexOf(",")));
            players = players.substring(players.indexOf(", ") + 2);
            id++;
        }
        playerName.put(id, players);

        ruVarsMsg = properties.getProperty("RuVoteMessage", "Список вариантов: 1 = var1, 2 = var2, 3 = var3, 4 = var4, 5 = var5, 6 = var6");
        ruStartMsg = properties.getProperty("RuStartMessage", "Привет! Я бот голосования! Напиши номер за который хочешь проголосовать.");
        ruVoteMsg = properties.getProperty("RuVoteMessage", "Вы проголосовали за %s");
        ruWrongFormatMsg = properties.getProperty("RuWrongFormatMessage", "Вы ввели неверный формат.");
        ruWrongNumberMsg = properties.getProperty("RuWrongNumberMessage", "Вы ввели неверное число.");
        ruRevoteMsg = properties.getProperty("RuReVoteMessage", "Вы изменили свой голос на %s");
        ruDoubleVoteMsg = properties.getProperty("RuDobVoteMessage", "Нельзя голосовать дважды");

        for (int i = 1; i < playerName.size() + 1; i++) {
            playerVotes.put(i, 0);
        }
    }

    public void newVote(Message message) {
        try {
            if (!phone.containsKey(message.getFrom().getUserName())) {     // message.getText().contains("1") &&
                if (!(Integer.parseInt(message.getText()) > playerName.size())) {
                    if (phone.get(message.getFrom().getUserName()) == null) {
                        addVote(Integer.parseInt(message.getText()));
                        sndMsg(message, String.format(voteMsg, getNameById(Integer.parseInt(message.getText())))); // "You voted for the player " + message.getText()
                        phone.put(message.getFrom().getUserName(), Integer.parseInt(message.getText()));
                        logger.info(String.format("A voice was given to the var %s from the user %s", message.getText(), message.getFrom().getUserName()));
                    } else if (phone.get(message.getFrom().getUserName()) != null) {
                        removeVote(phone.get(message.getFrom().getUserName()));
                        addVote(Integer.parseInt(message.getText()));
                        updateStatus();
                        sndMsg(message, String.format(revoteMsg, getNameById(Integer.parseInt(message.getText()))));
                        phone.put(message.getFrom().getUserName(), Integer.parseInt(message.getText()));
                        logger.info(String.format("A voice was given to the %s from the user %s (THAT WAS A REVOTE)", message.getText(), message.getFrom().getUserName()));
                    }
                }
                else {
                    logger.info(String.format("%s enter wrong number", message.getFrom().getUserName()));
                    sndMsg(message, wrongNumberMsg);
                }
            }
            else {
                sndMsg(message, String.format(ruDoubleVoteMsg, getNameById(Integer.parseInt(message.getText())))); // "You voted again!! " + message.getText()
                logger.info(String.format("You can't vote twice!", message.getText(), message.getFrom().getUserName()));
            }
        } catch (NumberFormatException e) {
            logger.info(String.format("%s enter invalid format", message.getFrom().getUserName()));
            sndMsg(message, wrongFormatMsg);
        } catch (IndexOutOfBoundsException e) {
            logger.info(String.format("%s enter wrong number", message.getFrom().getUserName()));
            sndMsg(message, wrongNumberMsg);
        }
    }

    public void newVoteRu(Message message) {
        try {
            if (!phone.containsKey(message.getFrom().getUserName())) {     // message.getText().contains("1") &&
                if (!(Integer.parseInt(message.getText()) > playerName.size())) {
                    if (phone.get(message.getFrom().getUserName()) == null) {
                        addVote(Integer.parseInt(message.getText()));
                        sndMsg(message, String.format(ruVoteMsg, getNameById(Integer.parseInt(message.getText())))); // "You voted for the player " + message.getText()
                        phone.put(message.getFrom().getUserName(), Integer.parseInt(message.getText()));
                        logger.info(String.format("A voice was given to the %s from the user %s", message.getText(), message.getFrom().getUserName()));
                    } else if (phone.get(message.getFrom().getUserName()) != null) {
                        removeVote(phone.get(message.getFrom().getUserName()));
                        addVote(Integer.parseInt(message.getText()));
                        updateStatus();
                        sndMsg(message, String.format(ruRevoteMsg, getNameById(Integer.parseInt(message.getText()))));
                        phone.put(message.getFrom().getUserName(), Integer.parseInt(message.getText()));
                        logger.info(String.format("A voice was given to the %s from the user %s (THAT WAS A REVOTE)", message.getText(), message.getFrom().getUserName()));
                    }
                }
                else {
                    logger.info(String.format("%s enter wrong number", message.getFrom().getUserName()));
                    sndMsg(message, ruWrongFormatMsg);
                }
            }
            else {
                sndMsg(message, String.format(ruDoubleVoteMsg, getNameById(Integer.parseInt(message.getText())))); // "You voted again!! " + message.getText()
                logger.info(String.format("You can't vote twice!", message.getText(), message.getFrom().getUserName()));
            }
        } catch (NumberFormatException e) {
            logger.info(String.format("%s enter invalid format", message.getFrom().getUserName()));
            sndMsg(message, ruWrongFormatMsg);
        } catch (IndexOutOfBoundsException e) {
            logger.info(String.format("%s enter wrong choice", message.getFrom().getUserName()));
            sndMsg(message, ruWrongNumberMsg);
        }
    }

    public void addVote(int id) {
        playerVotes.put(id, playerVotes.get(id) + 1);
    }

    public void removeVote(int id) {
        playerVotes.put(id, playerVotes.get(id) - 1);
    }

    public Integer getVoteById(int id) {
        return playerVotes.get(id);
    }

    public String getNameById(int id) {
        return playerName.get(id);
    }

    public void updateStatus() {
        try {
            Writer writer = new BufferedWriter(new FileWriter("vote status.txt", false));
            for (int i = 1; i < playerName.size() + 1; i++) {
                writer.write(String.format(statusMsg + "\n", getNameById(i), i, getVoteById(i)));
                writer.flush();
            }
            writer.close();
        } catch (IOException e) {
            logger.error("IO EXCEPTION: " + e);
        }

        logger.info("Status updated");
    }

    private void sndMsg(Message message, String s) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.enableMarkdown(true);
        sendMessage.setChatId(message.getChatId().toString());
        sendMessage.setText(s);
        try {
            execute(sendMessage);
        } catch (TelegramApiException e) {
            logger.error("TELEGRAM API EXCEPTION: " + e);
        }
    }

    @Override
    public String getBotUsername() {
        return BotUsername;
    }

    @Override
    public String getBotToken() {
        return BotToken;
    }
}

