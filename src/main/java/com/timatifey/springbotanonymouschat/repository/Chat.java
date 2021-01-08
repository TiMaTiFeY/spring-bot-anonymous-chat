package com.timatifey.springbotanonymouschat.repository;

import lombok.Data;
import lombok.ToString;

import javax.persistence.*;

@Entity
@Table(name = "chats")
@Data
@ToString
public class Chat {
    @Id
    @Column(name = "id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @Column(name = "name")
    private String name;

    @Column(name = "password")
    private String password;

    @Column(name = "count")
    private int countOfMembers;

    public Chat(String name, String password, int countOfMembers) {
        this.name = name;
        this.password = password;
        this.countOfMembers = countOfMembers;
    }
    public Chat(String name, int countOfMembers) {
        this.name = name;
        this.password = password;
        this.countOfMembers = countOfMembers;
    }

    public Chat() {
        name = "test";
        countOfMembers = 0;
    }

    public Chat cloneChat() {
        if (password != null) {
            return new Chat(
                    this.name,
                    this.password,
                    this.countOfMembers
                    );
        }
        return new Chat(this.name, this.countOfMembers);
    }
}
