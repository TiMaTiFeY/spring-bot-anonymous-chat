package com.timatifey.springbotanonymouschat.service;

import com.timatifey.springbotanonymouschat.repository.Chat;
import com.timatifey.springbotanonymouschat.repository.ChatsRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class ChatsService {

    private final ChatsRepository chatsRepository;

    @Autowired
    ChatsService(ChatsRepository chatsRepository) {
        this.chatsRepository = chatsRepository;
    }

    public boolean chatIsExist(long chatId) {
        return chatsRepository.findById(chatId).isPresent();
    }

    public Chat findChatByName(String name) {
        for (Chat chat : chatsRepository.findAll()) {
            if (chat.getName().equals(name)) {
                return chat;
            }
        }
        return null;
    }

    public Chat findChatById(Long id) {
        Optional<Chat> res = chatsRepository.findById(id);
        return res.orElse(null);
    }

    public List<Chat> getChats() {
        return chatsRepository.findAll();
    }

    public Chat createChat(Chat chat) {
        return chatsRepository.save(chat);
    }

    public Chat updateChat(long chatId, Chat newChat) {
        return chatsRepository.findById(chatId)
                .map(chat -> {
                    chat.setName(newChat.getName());
                    chat.setPassword(newChat.getPassword());
                    chat.setCountOfMembers(newChat.getCountOfMembers());
                    return chatsRepository.save(chat);
                }).orElseThrow(() -> new ResourceNotFoundException("Chat not found with id " + chatId));
    }

    public ResponseEntity<?> deleteChat(long chatId) {
        return chatsRepository.findById(chatId)
                .map(chat -> {
                    chatsRepository.delete(chat);
                    return ResponseEntity.ok().build();
                }).orElseThrow(() -> new ResourceNotFoundException("Chat not found with id " + chatId));
    }

    public void clearChats() {
        chatsRepository.deleteAll();
    }

}
