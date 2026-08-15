package com.velocity.velocity_management.teammonthlycalculation.controller;

import com.velocity.velocity_management.teammonthlycalculation.dto.request.CreateTeamVelocityRequest;
import com.velocity.velocity_management.teammonthlycalculation.dto.response.TeamVelocityDetailResponse;
import com.velocity.velocity_management.teammonthlycalculation.dto.response.TeamVelocityResponse;
import com.velocity.velocity_management.teammonthlycalculation.service.TeamVelocityService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/team-velocities")
public class TeamVelocityController {

    private final TeamVelocityService teamVelocityService;

    public TeamVelocityController(TeamVelocityService teamVelocityService) {
        this.teamVelocityService = teamVelocityService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TeamVelocityResponse createTeamVelocity(
            @Valid @RequestBody CreateTeamVelocityRequest request) {

        return teamVelocityService.createTeamVelocity(request);
    }

    @GetMapping
    public List<TeamVelocityResponse> getAllTeamVelocities() {
        return teamVelocityService.getAllTeamVelocities();
    }

    @GetMapping("/{id}")
    public TeamVelocityResponse getTeamVelocityById(@PathVariable Long id) {
        return teamVelocityService.getTeamVelocityById(id);
    }

    @GetMapping("/{id}/details")
    public TeamVelocityDetailResponse getTeamVelocityDetails(@PathVariable Long id) {
        return teamVelocityService.getTeamVelocityDetails(id);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteTeamVelocity(@PathVariable Long id) {
        teamVelocityService.deleteTeamVelocity(id);
    }
}
