package com.noSLQ.clase.java.controllers;


import com.noSLQ.clase.java.seed.DataSeeder;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/Seed")
@AllArgsConstructor
public class SeedController {

    private final DataSeeder dataSeeder;

    @PostMapping
    public Map<String, Object> seed(){
        return dataSeeder.seed();
    }

    @PostMapping("/reset")
    public Map<String, Object> resetAndSeed(){
        return dataSeeder.resetAndSeed();
    }

    @DeleteMapping
    public Map<String, Object> clear(){
        return dataSeeder.clear();
    }
}
