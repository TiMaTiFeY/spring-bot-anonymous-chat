package com.timatifey.springbotanonymouschat.repository;

import lombok.Data;
import lombok.ToString;

import javax.persistence.*;

@Entity
@Table(name = "users")
@Data
@ToString
public class User {
    @Id
    @Column(name = "id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @Column(name = "user_id")
    private long userId;

    @Column(name = "name")
    private String name;

    @Column(name = "in_chat")
    private boolean inChat;

    @ManyToOne(cascade=CascadeType.ALL)
    @JoinColumn(name = "chat_id")
    private Chat chat;

    public User(long id) {
        this.userId = id;
        this.name = "Anonymous";
        this.inChat = false;
    }

    public User(long id, String name) {
        this.userId = id;
        this.name = name;
    }

    public User(long id, Chat chat) {
        this.userId = id;
        this.name = "Anonymous";
        this.chat = chat;
        inChat = true;
    }

    public User(long id, String name, Chat chat) {
        this.userId = id;
        this.name = name;
        this.chat = chat;
        inChat = true;
    }

    public User() {
        userId = -1;
        name = "Anonymous";
        inChat = false;
    }

    public User cloneUser() {
        if (this.chat != null) {
            return new User(this.userId, this.name, this.chat);
        }
        return new User(this.userId, this.name);
    }
}
