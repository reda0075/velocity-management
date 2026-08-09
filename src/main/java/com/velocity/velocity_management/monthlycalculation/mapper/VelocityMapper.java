package com.velocity.velocity_management.monthlycalculation.mapper;

import com.velocity.velocity_management.monthlycalculation.dto.response.VelocityResponse;
import com.velocity.velocity_management.monthlycalculation.dto.response.VelocityRitualResponse;
import com.velocity.velocity_management.monthlycalculation.entity.Velocity;
import com.velocity.velocity_management.monthlycalculation.entity.VelocityRitual;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class VelocityMapper {

    public VelocityResponse toResponse(
            Velocity velocity,
            Double totalRitualTimeHours,
            Double ritualTimeDays,
            Double effectiveWorkingDays,
            Double velocityRatio){


        VelocityResponse response = new VelocityResponse();

        response.setId(velocity.getId());
        response.setCollaboratorId(velocity.getCollaborator().getId());
        response.setYear(velocity.getYear());
        response.setMonth(velocity.getMonth());
        response.setWorkingDays(velocity.getWorkingDays());
        response.setVelocity(velocity.getVelocity());

        response.setRituals(
                velocity.getRituals()
                        .stream()
                        .map(this::toRitualResponse)
                        .toList()
        );

        response.setTotalRitualTimeHours(totalRitualTimeHours);
        response.setRitualTimeDays(ritualTimeDays);
        response.setEffectiveWorkingDays(effectiveWorkingDays);
        response.setVelocityRatio(velocityRatio);

        response.setCreatedAt(velocity.getCreatedAt());
        response.setUpdatedAt(velocity.getUpdatedAt());

        return response;
    }

    private VelocityRitualResponse toRitualResponse( VelocityRitual velocityRitual) {


        VelocityRitualResponse response = new VelocityRitualResponse();

        response.setRitualId(velocityRitual.getRitual().getId());
        response.setRitualName(velocityRitual.getRitual().getName());
        response.setDurationHours(
                velocityRitual.getRitual().getDurationHours()
        );
        response.setOccurrences(velocityRitual.getOccurrences());

        response.setTotalTimeHours(
                velocityRitual.getRitual().getDurationHours()
                        * velocityRitual.getOccurrences()
        );

        return response;
    }
}
