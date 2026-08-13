package com.velocity.velocity_management.monthlycalculation.controller;

import com.velocity.velocity_management.monthlycalculation.dto.request.CreateVelocityRequest;
import com.velocity.velocity_management.monthlycalculation.dto.response.VelocityResponse;
import com.velocity.velocity_management.monthlycalculation.service.VelocityService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/velocities")
public class VelocityController {

    private final VelocityService velocityService;

    public VelocityController(VelocityService velocityService) {
        this.velocityService = velocityService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public VelocityResponse createVelocity(
            @Valid @RequestBody CreateVelocityRequest request) {

        return velocityService.createVelocity(request);
    }

    @GetMapping
    public List<VelocityResponse> getAllVelocities() {
        return velocityService.getAllVelocities();
    }

    @GetMapping("/{id}")
    public VelocityResponse getVelocityById(@PathVariable Long id) {
        return velocityService.getVelocityById(id);
    }

    @PutMapping("/{id}")
    public VelocityResponse updateVelocity(
            @PathVariable Long id,
            @Valid @RequestBody CreateVelocityRequest request) {

        return velocityService.updateVelocity(id, request);
    }

    @PatchMapping("/{id}/validate")
    public VelocityResponse validateVelocity(@PathVariable Long id) {
        return velocityService.validateVelocity(id);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteVelocity(@PathVariable Long id) {
        velocityService.deleteVelocity(id);
    }
}