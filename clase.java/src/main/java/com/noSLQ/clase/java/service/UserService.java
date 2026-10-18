package com.noSLQ.clase.java.service;

import com.noSLQ.clase.java.config.CacheNames;
import com.noSLQ.clase.java.dto.UserRequest;
import com.noSLQ.clase.java.entity.User;
import com.noSLQ.clase.java.repository.LoanRepository;
import com.noSLQ.clase.java.repository.UserRepository;
import lombok.AllArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;


@AllArgsConstructor
@Service
public class UserService {

    private static final String STATUS_ACTIVE = "ACTIVE";

    private final UserRepository userRepository;
    private final LoanRepository loanRepository;
    private final ConsistencyService consistencyService;

    @Cacheable(cacheNames = CacheNames.USERS)
    public List<User> getAll(){
        return userRepository.findAll();
    }

    @Cacheable(cacheNames = CacheNames.USERS, key = "'id:' + #id")
    public User getById(String id){
        return userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + id));
    }

    @Cacheable(cacheNames = CacheNames.USERS, key = "'email:' + #email")
    public User getByEmail(String email){
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found with email: " + email));
    }

    @Cacheable(cacheNames = CacheNames.USERS, key = "'name:' + #name")
    public List<User> getByName(String name){
        return userRepository.findByNameContainingIgnoreCase(name);
    }

    @CacheEvict(cacheNames = {CacheNames.USERS, CacheNames.LOANS, CacheNames.REVIEWS, CacheNames.FAVORITES}, allEntries = true)
    public User createUser(UserRequest request){
        validateUser(request, null);

        User user = new User();
        user.setName(request.getName());
        user.setEmail(request.getEmail());
        user.setPhone(request.getPhone());
        user.setAddress(request.getAddress());
        user.setRegistrationDate(request.getRegistrationDate() != null
                ? request.getRegistrationDate()
                : LocalDateTime.now());

        return userRepository.save(user);
    }

    @CacheEvict(cacheNames = {CacheNames.USERS, CacheNames.LOANS, CacheNames.REVIEWS, CacheNames.FAVORITES}, allEntries = true)
    public User updateUser(String id, UserRequest request){
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + id));

        validateUser(request, id);

        user.setName(request.getName());
        user.setEmail(request.getEmail());
        user.setPhone(request.getPhone());
        user.setAddress(request.getAddress());
        if (request.getRegistrationDate() != null) {
            user.setRegistrationDate(request.getRegistrationDate());
        }

        User saved = userRepository.save(user);

        consistencyService.syncUser(saved);

        return saved;
    }

    @CacheEvict(cacheNames = {CacheNames.USERS, CacheNames.LOANS, CacheNames.REVIEWS, CacheNames.FAVORITES}, allEntries = true)
    public void deleteUser(String id){
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + id));

        if (!loanRepository.findByUserIdAndStatus(id, STATUS_ACTIVE).isEmpty()) {
            throw new RuntimeException("Cannot delete a user with active loans with id: " + id);
        }

        consistencyService.removeUserReferences(id);
        userRepository.delete(user);
    }

    private void validateUser(UserRequest request, String currentUserId){
        if (request.getName() == null || request.getName().isBlank()) {
            throw new RuntimeException("User name is required");
        }
        if (request.getEmail() == null || request.getEmail().isBlank()) {
            throw new RuntimeException("User email is required");
        }

        userRepository.findByEmail(request.getEmail())
                .filter(existing -> currentUserId == null || !currentUserId.equals(existing.getId()))
                .ifPresent(existing -> {
                    throw new RuntimeException("User already exists with email: " + request.getEmail());
                });
    }

}
