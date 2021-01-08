package com.timatifey.springbotanonymouschat.service;

import com.timatifey.springbotanonymouschat.repository.Chat;
import com.timatifey.springbotanonymouschat.repository.ChatsRepository;
import com.timatifey.springbotanonymouschat.repository.User;
import com.timatifey.springbotanonymouschat.repository.UsersRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class UsersService {
    private final UsersRepository usersRepository;
    private final ChatsRepository chatsRepository;

    @Autowired
    UsersService(UsersRepository usersRepository, ChatsRepository chatsRepository) {
        this.usersRepository = usersRepository;
        this.chatsRepository = chatsRepository;
    }

    public List<User> getUsersByChatId(long chatId) {
        return usersRepository.findAll().stream()
                .filter(user -> user.isInChat() && user.getChat().getId() == chatId)
                .collect(Collectors.toList());
    }

    public boolean userIsExist(long userId) {
        return usersRepository.findAll().stream()
                .anyMatch(user -> user.getUserId() == userId);
    }

    public User createUser(User user) {
        return usersRepository.save(user);
    }

    public User updateUser(long userId, User newUser) {
        User user = findUserById(userId);
        if (user == null) {
            throw new ResourceNotFoundException("User not found with id " + userId);
        }
        user.setName(newUser.getName());
        user.setChat(newUser.getChat());
        return usersRepository.save(user);
    }

    public ResponseEntity<?> deleteUser(long userId) {
        User user = findUserById(userId);
        if (user == null) {
            throw new ResourceNotFoundException("User not found with id " + userId);
        }
        usersRepository.delete(user);
        return ResponseEntity.ok().build();
    }

    public User findUserById(long userId) {
        Optional<User> res = usersRepository.findAll().stream()
                .filter(user -> user.getUserId() == userId)
                .findFirst();
        return res.orElse(null);
    }
}
