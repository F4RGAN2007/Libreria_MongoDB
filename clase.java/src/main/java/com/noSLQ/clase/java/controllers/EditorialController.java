package com.noSLQ.clase.java.controllers;


import com.noSLQ.clase.java.dto.EditorialRequest;
import com.noSLQ.clase.java.entity.Editorial;
import com.noSLQ.clase.java.service.EditorialService;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/Editorial")
@AllArgsConstructor
public class EditorialController {

    private final EditorialService editorialService;

    @GetMapping("/{id}")
    public Editorial getEditorial(@PathVariable String id) {
        return editorialService.getById(id);
    }

    @PostMapping
    public Editorial saveEditorial(@RequestBody EditorialRequest editorial){
        return editorialService.createEditorial(editorial);
    }

    @GetMapping("all")
    public List<Editorial> getAllEditorials(){
        return editorialService.getAll();
    }

    @PatchMapping("/{id}")
    public Editorial updateEditorial(@PathVariable String id, @RequestBody EditorialRequest editorial){
        return editorialService.updateEditorial(id, editorial);
    }

    @DeleteMapping("/{id}")
    public String deleteEditorial(@PathVariable String id){
        editorialService.deleteEditorial(id);
        return "Editorial deleted with id: " + id;
    }
}
