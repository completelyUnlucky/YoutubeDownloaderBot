package com.bot.main.bot.service;

import com.bot.main.bot.config.BotConfig;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.commands.SetMyCommands;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendVideo;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.commands.BotCommand;
import org.telegram.telegrambots.meta.api.objects.commands.scope.BotCommandScopeDefault;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@EqualsAndHashCode(callSuper = true)
public class BotService extends TelegramLongPollingBot {

    private final BotConfig botConfig;

    public BotService(BotConfig botConfig) {
        this.botConfig = botConfig;
        List<BotCommand> botCommandList = new ArrayList<>();
        botCommandList.add(new BotCommand("/start", "launch a bot"));
        botCommandList.add(new BotCommand("/help", "get a help"));
        botCommandList.add(new BotCommand("/author", "get author information"));
        try {
            this.execute(new SetMyCommands(botCommandList, new BotCommandScopeDefault(), "en"));
        } catch (TelegramApiException e) {
            log.error(e.getMessage());
        }
    }

    @Override
    public String getBotUsername() {
        return botConfig.getUsername();
    }

    @Override
    public String getBotToken() {
        return botConfig.getToken();
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()) {
            String messageText = update.getMessage().getText();
            long chatId = update.getMessage().getChatId();

            if (messageText.equals("/start")) {
                startCommand(chatId, update.getMessage().getChat().getFirstName());
                log.info("User: " + update.getMessage().getChat().getFirstName() + " " +
                                    update.getMessage().getChat().getLastName() +
                        ", press /start");
            } else if (messageText.equals("/help")) {
                helpCommand(chatId);
            } else if (messageText.equals("/author")) {
                authorCommand(chatId);
            } else {
                sendVideo(update.getMessage().getChatId(), messageText);

            }
        }
    }

    private void startCommand(long chatId, String firstName) {
        String answer = "Hi, " + firstName + "!\n" +
                "This is a bot that allows you to download videos from YouTube. " +
                "Just send a link to the video you want to download and I'll send you the file. Enjoy!";
        sendMessage(chatId, answer);
    }

    private void helpCommand(long chatId) {
        String answer = """
                The bot only accepts links from YouTube, in two formats:
                1. https://youtu.be/qwe123
                2. https://www.youtube.com/qwe123
                \n
                If you see this error message: "This is not YouTube URL. Please enter a valid URL.", it means that the bot processed your request, but did not find this video, enter another link.
                \n
                There should be no more questions.
                """;
        sendMessage(chatId, answer);
    }

    private void authorCommand(long chatId) {
        String answer = "The author of this bot is Boris Kriventsov, a student from Moscow.\n\n" +
                "Link to GitHub: https://github.com/completelyUnlucky\n" +
                "Link to this project repository: https://github.com/completelyUnlucky/YoutubeDownloaderBot";
        sendMessage(chatId, answer);
    }

    private void sendMessage(long chatId, String message) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(chatId);
        sendMessage.setText(message);
        try {
            execute(sendMessage);
        } catch (TelegramApiException e) {
            log.error(e.getMessage());
        }
    }
    private void sendVideo(long chatId, String videoURL) {
        try {
            VideoService downloadVideo = new VideoService();
            String videoId = null;

            if (videoURL.split("/")[2].equals("www.youtube.com")) {
                videoId = videoURL.substring(videoURL.indexOf("v")+2, videoURL.indexOf("&"));
            } else if (videoURL.split("/")[2].equals("youtu.be")) {
                videoId = videoURL.substring(videoURL.indexOf(".")+4, videoURL.indexOf("?"));
            }
            downloadVideo.download(videoId);

            InputFile videoFile = new InputFile(new File("/home/boris/IdeaProjects/bot/videos/video.mp4"),
                    "/home/boris/IdeaProjects/bot/videos/video.mp4");

            SendVideo sendVideo = new SendVideo();

            sendVideo.setChatId(chatId);
            sendVideo.setVideo(videoFile);

            execute(sendVideo);
            sendMessage(chatId, String.format("""
                            Please!
                            Video title: %s
                            Video author: %s""",
                    downloadVideo.getVideoTitle(videoId),
                    downloadVideo.getVideoChannel(videoId)));

            log.info(videoURL);
        } catch (TelegramApiException e) {
            log.error(e.getMessage());
        } catch (NullPointerException e) {
            sendMessage(chatId, "This is not YouTube URL. Please enter a valid URL.");
        }
    }
}
