package com.noSLQ.clase.java.service;

import com.noSLQ.clase.java.config.CacheNames;
import com.noSLQ.clase.java.dto.EditorialRequest;
import com.noSLQ.clase.java.entity.Editorial;
import com.noSLQ.clase.java.repository.EditorialRepository;
import lombok.AllArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;


@AllArgsConstructor
@Service
public class EditorialService {

    private final EditorialRepository editorialRepository;

    @Cacheable(cacheNames = CacheNames.EDITORIALS)
    public List<Editorial> getAll(){
        return editorialRepository.findAll();
    }

    @Cacheable(cacheNames = CacheNames.EDITORIALS, key = "#id")
    public Editorial getById(String id){
        return editorialRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Editorial not found with id: " + id));
    }

    @CacheEvict(cacheNames = CacheNames.EDITORIALS, allEntries = true)
    public Editorial createEditorial(EditorialRequest request){
        Editorial editorial = new Editorial();
        editorial.setName(request.getName());
        editorial.setCountry(request.getCountry());
        editorial.setWebsite(request.getWebsite());

        return editorialRepository.save(editorial);
    }

    @CacheEvict(cacheNames = CacheNames.EDITORIALS, allEntries = true)
    public Editorial updateEditorial(String id, EditorialRequest request){
        Editorial editorial = editorialRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Editorial not found with id: " + id));

        editorial.setName(request.getName());
        editorial.setCountry(request.getCountry());
        editorial.setWebsite(request.getWebsite());

        return editorialRepository.save(editorial);
    }

    @CacheEvict(cacheNames = CacheNames.EDITORIALS, allEntries = true)
    public void deleteEditorial(String id){
        Editorial editorial = editorialRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Editorial not found with id: " + id));

        editorialRepository.delete(editorial);
    }

}
