package com.velocity.velocity_management.team.controller;

import com.velocity.velocity_management.team.dto.request.CreateTeamRequest;
import com.velocity.velocity_management.team.dto.request.UpdateTeamRequest;
import com.velocity.velocity_management.team.dto.response.TeamResponse;
import com.velocity.velocity_management.team.service.TeamService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/teams")
public class TeamController {

    private final TeamService teamService;

    public TeamController(TeamService teamService) {
        this.teamService = teamService;
    }

    @PostMapping
    public ResponseEntity<TeamResponse> createTeam(
            @Valid @RequestBody CreateTeamRequest request) {

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(teamService.createTeam(request));
    }

    @GetMapping
    public ResponseEntity<List<TeamResponse>> getAllTeams() {

        return ResponseEntity.ok(
                teamService.getAllTeams()
        );
    }

    @GetMapping("/{id}")
    public ResponseEntity<TeamResponse> getTeamById(
            @PathVariable Long id) {

        return ResponseEntity.ok(
                teamService.getTeamById(id)
        );
    }

    @PutMapping("/{id}")
    public ResponseEntity<TeamResponse> updateTeam(
            @PathVariable Long id,
            @Valid @RequestBody UpdateTeamRequest request) {

        return ResponseEntity.ok(
                teamService.updateTeam(id, request)
        );
    }

    @PatchMapping("/{id}/activate")
    public ResponseEntity<TeamResponse> activateTeam(
            @PathVariable Long id) {

        return ResponseEntity.ok(
                teamService.activateTeam(id)
        );
    }

    @PatchMapping("/{id}/deactivate")
    public ResponseEntity<TeamResponse> deactivateTeam(
            @PathVariable Long id) {

        return ResponseEntity.ok(
                teamService.deactivateTeam(id)
        );
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTeam(
            @PathVariable Long id) {

        teamService.deleteTeam(id);

        return ResponseEntity.noContent().build();
    }
}