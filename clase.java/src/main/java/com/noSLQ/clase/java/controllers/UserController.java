package com.noSLQ.clase.java.controllers;


import com.noSLQ.clase.java.dto.UserRequest;
import com.noSLQ.clase.java.entity.User;
import com.noSLQ.clase.java.service.UserService;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/User")
@AllArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/{id}")
    public User getUser(@PathVariable String id) {
        return userService.getById(id);
    }

    @GetMapping("/email/{email}")
    public User getUserByEmail(@PathVariable String email) {
        return userService.getByEmail(email);
    }

    @GetMapping("/search")
    public List<User> searchUsersByName(@RequestParam String name) {
        return userService.getByName(name);
    }

    @GetMapping("/buscar")
    public List<User> buscarUsuarios(@RequestParam String nombre) {
        return userService.getByName(nombre);
    }

    @PostMapping
    public User saveUser(@RequestBody UserRequest user){
        return userService.createUser(user);
    }

    @GetMapping("all")
    public List<User> getAllUsers(){
        return userService.getAll();
    }

    @PatchMapping("/{id}")
    public User updateUser(@PathVariable String id, @RequestBody UserRequest user){
        return userService.updateUser(id, user);
    }

    @DeleteMapping("/{id}")
    public String deleteUser(@PathVariable String id){
        userService.deleteUser(id);
        return "User deleted with id: " + id;
    }
}
