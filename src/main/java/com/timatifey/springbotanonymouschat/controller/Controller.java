package com.timatifey.springbotanonymouschat.controller;

import com.timatifey.springbotanonymouschat.client.ChatMessage;
import com.timatifey.springbotanonymouschat.client.ChatMessagesQueue;
import com.timatifey.springbotanonymouschat.client.VKClient;
import com.timatifey.springbotanonymouschat.client.longpoll.MessageNewObj;
import com.timatifey.springbotanonymouschat.client.parser.MessageParser;
import com.timatifey.springbotanonymouschat.client.parser.TypeMessage;
import com.timatifey.springbotanonymouschat.repository.Chat;
import com.timatifey.springbotanonymouschat.repository.User;
import com.timatifey.springbotanonymouschat.service.ChatsService;
import com.timatifey.springbotanonymouschat.service.UsersService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.PostConstruct;
import java.util.Objects;
import java.util.stream.Collectors;

@RestController
public class Controller {
    private static final Logger logger = LoggerFactory.getLogger(Controller.class);

    private final VKClient client;
    private final UsersService usersService;
    private final ChatsService chatsService;

    private ChatMessagesQueue chatMessagesQueue;

    @Autowired
    Controller(VKClient client,
               UsersService usersService,
               ChatsService chatsService) {
        this.client = client;
        this.usersService = usersService;
        this.chatsService = chatsService;
    }

    @PostConstruct
    public void init() {
        chatMessagesQueue = new ChatMessagesQueue(client, usersService);
        new Thread(chatMessagesQueue).start();
    }

    public User initUser(long userId, Chat chat) {
        if (!usersService.userIsExist(userId)) {
            User newUser = new User(userId, chat);
            return usersService.createUser(newUser);
        }
        return null;
    }

    @PostMapping("/send")
    public void send(@RequestBody MessageNewObj msg) {
        long peerId = msg.getMessage().getPeerId();
        User user = usersService.findUserById(peerId);
        if (user == null || !user.isInChat()) {
            return;
        }
        chatMessagesQueue.callMessage(new ChatMessage(
                user.getChat().getId(),
                user.getUserId(),
                user.getName(),
                msg.getMessage().getText(),
                msg.getMessage().getAttachments().stream()
                        .filter(Objects::nonNull)
                        .map(Object::toString)
                        .collect(Collectors.joining())
        ));
    }

    @PostMapping("/ping")
    public void ping(@RequestBody MessageNewObj msg) {
        client.sendMessage("pong", msg.getMessage().getPeerId());
    }

    @PostMapping("/create_chat")
    public void createChat(@RequestBody MessageNewObj msg) {
        MessageParser parserMsg = new MessageParser(msg.getMessage().getText());
        if (parserMsg.getType() != TypeMessage.COMMAND_WITH_ARGS) {
            client.sendMessage("Нет аргументов", msg.getMessage().getPeerId());
            return;
        }
        Chat chat;
        String answer;
        String chatName = parserMsg.getArgs().get(0);
        if (parserMsg.getArgs().size() == 2) {
            String password = parserMsg.getArgs().get(1);
            chat = chatsService.createChat(new Chat(chatName, password, 0));
            answer = String.format("Создание комнаты %s(ID: %d) c паролем: %s", chatName, chat.getId(), password);
        } else {
            chat = chatsService.createChat(new Chat(chatName, 0));
            answer = String.format("Создание комнаты %s(ID: %d)", chatName, chat.getId());
        }
        long peerId = msg.getMessage().getPeerId();
        client.sendMessage(answer, peerId);
    }

    @PostMapping("/join")
    public void joinToChat(@RequestBody MessageNewObj msg) {
        MessageParser parserMsg = new MessageParser(msg.getMessage().getText());
        if (parserMsg.getType() != TypeMessage.COMMAND_WITH_ARGS) {
            client.sendMessage("Нет аргументов", msg.getMessage().getPeerId());
            return;
        }
        long peerId = msg.getMessage().getPeerId();
        if (parserMsg.getArgs().size() >= 2) {
            try {
                long chatId = Long.parseLong(parserMsg.getArgs().get(0));
                String password = parserMsg.getArgs().get(1);
                Chat chat = chatsService.findChatById(chatId);
                if (chat == null) {
                    client.sendMessage("Чата с таким ID не существует", peerId);
                    return;
                }
                if (chat.getPassword() != null && !chat.getPassword().equals(password)) {
                    client.sendMessage("Неверный пароль",peerId);
                    return;
                }
                chat.setCountOfMembers(chat.getCountOfMembers() + 1);
                chatsService.updateChat(chatId, chat);
                if (!usersService.userIsExist(peerId)) {
                    initUser(peerId, chat);
                } else {
                    User user = usersService.findUserById(peerId);
                    user.setChat(chat);
                    user.setInChat(true);
                    usersService.updateUser(peerId, user);
                }
                final String answer = String.format("Вы подключились к чату (ID: %d)", chatId);
                client.sendMessage(answer, peerId);
            } catch (NumberFormatException err) {
                client.sendMessage("Неверный формат ID", peerId);
                return;
            }
        } else {
            try {
                long chatId = Long.parseLong(parserMsg.getArgs().get(0));
                Chat chat = chatsService.findChatById(chatId);
                if (chat == null) {
                    client.sendMessage("Чата с таким ID не существует", peerId);
                    return;
                }
                if (chat.getPassword() != null) {
                    client.sendMessage("У чата есть пароль, повторите попытку",peerId);
                    return;
                }
                chat.setCountOfMembers(chat.getCountOfMembers() + 1);
                chatsService.updateChat(chatId, chat);
                if (!usersService.userIsExist(peerId)) {
                    initUser(peerId, chat);
                } else {
                    User user = usersService.findUserById(peerId);
                    user.setChat(chat);
                    user.setInChat(true);
                    usersService.updateUser(peerId, user);
                }
                final String answer = String.format("Вы подключились к чату (ID: %d)", chatId);
                client.sendMessage(answer, peerId);
            } catch (NumberFormatException err) {
                client.sendMessage("Неверный формат ID", peerId);
                return;
            }
        }
    }

    @PostMapping("/leave")
    public void leaveFromChat(@RequestBody MessageNewObj msg) {
        MessageParser parserMsg = new MessageParser(msg.getMessage().getText());
        long peerId = msg.getMessage().getPeerId();
        if (parserMsg.getType() != TypeMessage.COMMAND) {
            client.sendMessage("Не верный формат", peerId);
            return;
        }
        User user = usersService.findUserById(peerId);
        if (user == null || !user.isInChat()) {
            client.sendMessage("Вы не находитесь в чате", peerId);
            return;
        }
        Chat chat = user.getChat();
        chat.setCountOfMembers(chat.getCountOfMembers() - 1);
        chatsService.updateChat(chat.getId(), chat);
        user.setInChat(false);
        usersService.updateUser(peerId, user);
        client.sendMessage(String.format("Вы вышли из чата (ID: %d)", chat.getId()), peerId);
    }

    @PostMapping("/change_name")
    public void changeName(@RequestBody MessageNewObj msg) {
        MessageParser parserMsg = new MessageParser(msg.getMessage().getText());
        long peerId = msg.getMessage().getPeerId();
        if (parserMsg.getType() != TypeMessage.COMMAND_WITH_ARGS) {
            client.sendMessage("Не верный формат", peerId);
            return;
        }
        User user = usersService.findUserById(peerId);
        if (user == null) {
            user = initUser(peerId, new Chat());
            user.setInChat(false);
            usersService.updateUser(peerId, user);
        }
        String newName = parserMsg.getArgs().get(0);
        user.setName(newName);
        usersService.updateUser(peerId, user);
        client.sendMessage(String.format("Вы установили имя: %s", newName), peerId);
    }


}
