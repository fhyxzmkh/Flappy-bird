package org.backend.service;

import org.backend.model.User;
import org.backend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    public void printAllUsers() {
        List<User> users = (List<User>) userRepository.findAll();
        for (User user : users) {
            System.out.println(user);
        }
    }
}