package com.velocity.velocity_management.team.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class UpdateTeamMembersRequest {

    @NotNull
    private List<Long> collaboratorIds;
}
