package com.velocity.velocity_management.collaborator.controller;

import com.velocity.velocity_management.collaborator.dto.request.CreateCollaboratorRequest;
import com.velocity.velocity_management.collaborator.dto.request.UpdateCollaboratorRequest;
import com.velocity.velocity_management.collaborator.dto.response.CollaboratorResponse;
import com.velocity.velocity_management.collaborator.service.CollaboratorService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/collaborators")
public class CollaboratorController {

    private final CollaboratorService collaboratorService;

    public CollaboratorController(CollaboratorService collaboratorService) {
        this.collaboratorService = collaboratorService;
    }
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CollaboratorResponse createCollaborator(
            @Valid @RequestBody CreateCollaboratorRequest request) {

        return collaboratorService.createCollaborator(request);
    }

    @GetMapping
    public List<CollaboratorResponse> getAllCollaborators() {
        return collaboratorService.getAllCollaborators();
    }

    @GetMapping("/{id}")
    public CollaboratorResponse getCollaboratorById(@PathVariable Long id) {

        return collaboratorService.getCollaboratorById(id);

    }

    @PutMapping("/{id}")
    public CollaboratorResponse updateCollaborator(
            @PathVariable Long id,
            @Valid @RequestBody UpdateCollaboratorRequest request) {

        return collaboratorService.updateCollaborator(id, request);
    }

    @PatchMapping("/{id}/activate")
    public CollaboratorResponse activateCollaborator(
            @PathVariable Long id) {

        return collaboratorService.activateCollaborator(id);
    }

    @PatchMapping("/{id}/deactivate")
    public CollaboratorResponse deactivateCollaborator(
            @PathVariable Long id) {

        return collaboratorService.deactivateCollaborator(id);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteCollaborator(@PathVariable Long id) {

        collaboratorService.deleteCollaborator(id);
    }
}
