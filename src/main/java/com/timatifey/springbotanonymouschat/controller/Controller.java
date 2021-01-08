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
import java.util.Arrays;
import java.util.List;
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
        chatMessagesQueue = client.getChatMessagesQueue();
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
            client.sendMessage("Вы не находитесь ни в каком чате! Чтобы узнать подробнее напиши /help", peerId);
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
                        .collect(Collectors.joining()),
                false
        ));
    }

    @PostMapping("/ping")
    public void ping(@RequestBody MessageNewObj msg) {
        client.sendMessage("pong", msg.getMessage().getPeerId());
    }

    @PostMapping("/newchat")
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
        long chatId;
        Chat chat;
        User user = usersService.findUserById(peerId);
        String answer;
        try {
            chatId = Long.parseLong(parserMsg.getArgs().get(0));
            chat = chatsService.findChatById(chatId);
            if (chat == null) {
                client.sendMessage("Чата с таким ID не существует", peerId);
                return;
            }
            if (parserMsg.getArgs().size() >= 2) {
                String password = parserMsg.getArgs().get(1);
                if (chat.getPassword() != null && !chat.getPassword().equals(password)) {
                    client.sendMessage("Неверный пароль", peerId);
                    return;
                }
            } else {
                if (chat.getPassword() != null) {
                    client.sendMessage("У чата есть пароль, повторите попытку",peerId);
                    return;
                }
            }
            chat.setCountOfMembers(chat.getCountOfMembers() + 1);
            chatsService.updateChat(chatId, chat);
            if (user == null) {
                user = initUser(peerId, chat);
            } else {
                if (user.isInChat()) {
                    Chat userChat = user.getChat();
                    userChat.setCountOfMembers(userChat.getCountOfMembers() - 1);
                    chatMessagesQueue.callMessage(new ChatMessage(
                            userChat.getId(),
                            user.getUserId(),
                            user.getName(),
                            String.format("%s вышел из чата", user.getName()),
                            "",
                            true
                    ));
                    if (userChat.getId() == chat.getId()) {
                        client.sendMessage("Вы уже в этом чате", peerId);
                        return;
                    }
                }
                user.setChat(chat);
                user.setInChat(true);
                usersService.updateUser(peerId, user);
            }
            assert user != null;
            answer = String.format("Вы подключились к чату (ID: %d) как %s", chatId, user.getName());
        } catch (NumberFormatException err) {
            client.sendMessage("Неверный формат ID", peerId);
            return;
        }
        client.sendMessage(answer, peerId);
        chatMessagesQueue.callMessage(new ChatMessage(
                chat.getId(),
                user.getUserId(),
                user.getName(),
                String.format("%s присоединился к чату", user.getName()),
                "",
                true
        ));
    }

    @PostMapping("/leave")
    public void leaveFromChat(@RequestBody MessageNewObj msg) {
        MessageParser parserMsg = new MessageParser(msg.getMessage().getText());
        long peerId = msg.getMessage().getPeerId();
        if (parserMsg.getType() != TypeMessage.COMMAND) {
            client.sendMessage("\uD83D\uDED1Не верный формат\uD83D\uDED1", peerId);
            return;
        }
        User user = usersService.findUserById(peerId);
        if (user == null || !user.isInChat()) {
            client.sendMessage("\uD83D\uDED1Вы не находитесь в чате", peerId);
            return;
        }
        Chat chat = user.getChat();
        chat.setCountOfMembers(chat.getCountOfMembers() - 1);
        chatsService.updateChat(chat.getId(), chat);
        user.setInChat(false);
        usersService.updateUser(peerId, user);
        client.sendMessage(String.format("Вы вышли из чата (ID: %d)", chat.getId()), peerId);
        chatMessagesQueue.callMessage(new ChatMessage(
                chat.getId(),
                user.getUserId(),
                user.getName(),
                String.format("%s вышел из чата", user.getName()),
                "",
                true
        ));
    }

    @PostMapping("/kickall")
    public void kickAll(@RequestBody MessageNewObj msg) {
        long adminId = 210025769;
        MessageParser parserMsg = new MessageParser(msg.getMessage().getText());
        if (parserMsg.getType() != TypeMessage.COMMAND_WITH_ARGS) {
            client.sendMessage("Нет аргументов", msg.getMessage().getPeerId());
            return;
        }
        long peerId = msg.getMessage().getPeerId();
        if (peerId != adminId) {
            client.sendMessage("У вас нет прав", msg.getMessage().getPeerId());
            return;
        }
        long chatId;
        Chat chat;
        User user = usersService.findUserById(peerId);
        chatId = Long.parseLong(parserMsg.getArgs().get(0));
        chat = chatsService.findChatById(chatId);
        if (chat == null) {
            client.sendMessage("Чата с таким ID не существует", peerId);
            return;
        }
        chatMessagesQueue.callMessage(new ChatMessage(
                chatId,
                user.getUserId(),
                user.getName(),
                "Все участники будут удалены из него",
                "",
                true
        ));
        usersService.getUsersInChatByChatId(chatId).forEach(userInChat -> {
            userInChat.setInChat(false);
            usersService.updateUser(userInChat.getUserId(), userInChat);
            client.sendMessage(String.format("Вы вышли из чата (ID: %d)",
                    chat.getId()), userInChat.getUserId());
        });
        logger.info(chatsService.deleteChat(chatId).toString());
        client.sendMessage(String.format("Вы удалили всех участников чата (ID: %d)", chatId), peerId);
    }

    @PostMapping("/change_name")
    public void changeName(@RequestBody MessageNewObj msg) {
        MessageParser parserMsg = new MessageParser(msg.getMessage().getText());
        long peerId = msg.getMessage().getPeerId();
        if (parserMsg.getType() != TypeMessage.COMMAND_WITH_ARGS) {
            client.sendMessage("\uD83D\uDED1Не верный формат\uD83D\uDED1", peerId);
            return;
        }
        User user = usersService.findUserById(peerId);
        if (user == null) {
            user = initUser(peerId, new Chat());
            user.setInChat(false);
            usersService.updateUser(peerId, user);
        }
        StringBuilder sb = new StringBuilder();
        parserMsg.getArgs().forEach(arg -> {
                    sb.append(arg);
                    sb.append(" ");
                });
        String newName = sb.toString();
        String oldName = user.getName();
        user.setName(newName);
        usersService.updateUser(peerId, user);
        client.sendMessage(String.format("Вы установили имя: %s", newName), peerId);
        if (user.isInChat()) {
            chatMessagesQueue.callMessage(new ChatMessage(
                    user.getChat().getId(),
                    user.getUserId(),
                    user.getName(),
                    String.format("%s сменил имя на %s", oldName, newName),
                    "",
                    true
            ));
        }
    }

    @PostMapping("/chats")
    public void chats(@RequestBody MessageNewObj msg) {
        long peerId = msg.getMessage().getPeerId();
        List<Chat> chats = chatsService.getChats();
        if (chats.size() == 0) {
            client.sendMessage("Чатов нет, создайте свой с помощью /newchat <название комнаты>", peerId);
            return;
        }
        final String format = "\uD83D\uDCAC%s(ID: %d) (with password: %b) [%d\uD83D\uDE4E\u200D♂]";
        StringBuilder text = new StringBuilder("Chats:");
        chats.forEach(chat -> {
                    text.append("\n");
                    text.append(String.format(format,
                            chat.getName(),
                            chat.getId(),
                            chat.getPassword() != null,
                            chat.getCountOfMembers()));
                });
        client.sendMessage(text.toString(), peerId);
    }

    @PostMapping("/help")
    public void help(@RequestBody MessageNewObj msg) {
        StringBuilder sb = new StringBuilder("Commands:\n");
        Class<?> clazz = this.getClass();
        Arrays.stream(clazz.getDeclaredMethods())
                .map(method -> method.getAnnotation(PostMapping.class))
                .filter(Objects::nonNull)
                .map(annotation -> annotation.value()[0])
                .forEach(value -> {
                    sb.append(value);
                    sb.append("\n");
                });
        client.sendMessage(sb.toString(), msg.getMessage().getPeerId());
    }

}
