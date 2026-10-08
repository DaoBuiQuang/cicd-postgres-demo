package com.example.cicddemo.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import com.example.cicddemo.entity.AppUser;
import com.example.cicddemo.repository.AppUserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Tag(name = "Users", description = "Quản lý người dùng")
@RestController
@RequestMapping("/api/users")
public class UserController {

    private final AppUserRepository repository;

    public UserController(AppUserRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    public List<AppUser> findAll() {
        return repository.findAll();
    }

    @GetMapping("/{id}")
    public AppUser findById(@PathVariable Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AppUser create(@RequestBody AppUser user) {
        return repository.save(user);
    }
}
