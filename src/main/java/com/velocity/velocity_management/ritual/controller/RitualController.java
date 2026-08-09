package com.velocity.velocity_management.ritual.controller;


import com.velocity.velocity_management.collaborator.dto.response.CollaboratorResponse;
import com.velocity.velocity_management.ritual.dto.request.CreateRitualRequest;
import com.velocity.velocity_management.ritual.dto.request.UpdateRitualRequest;
import com.velocity.velocity_management.ritual.dto.response.RitualResponse;
import com.velocity.velocity_management.ritual.service.RitualService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/rituals")
public class RitualController {

    private final RitualService ritualService ;

    public RitualController(RitualService ritualService) {
        this.ritualService = ritualService;
    }


    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public RitualResponse createRitual(
            @Valid @RequestBody CreateRitualRequest request) {

        return ritualService.createRitual(request);
    }


    @GetMapping
    public List<RitualResponse> getAllRituals() {
        return ritualService.getAllRituals();
    }

    @GetMapping("/{id}")
    public RitualResponse getRitualById(@PathVariable Long id) {
        return ritualService.getRitualById(id);
    }

    @PutMapping("/{id}")
    public RitualResponse updateRitual(
            @PathVariable Long id,
            @Valid @RequestBody UpdateRitualRequest request) {

        return ritualService.updateRitual(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteRitual(@PathVariable Long id) {
        ritualService.deleteRitual(id);
    }



}
