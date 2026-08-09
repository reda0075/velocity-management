package com.velocity.velocity_management.ritual.service;

import com.velocity.velocity_management.common.exception.ResourceNotFoundException;
import com.velocity.velocity_management.ritual.dto.request.CreateRitualRequest;
import com.velocity.velocity_management.ritual.dto.request.UpdateRitualRequest;
import com.velocity.velocity_management.ritual.dto.response.RitualResponse;
import com.velocity.velocity_management.ritual.entity.Ritual;
import com.velocity.velocity_management.ritual.mapper.RitualMapper;
import com.velocity.velocity_management.ritual.repository.RitualRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class RitualService {

    private final RitualRepository ritualRepository ;
    private final RitualMapper ritualMapper ;

    public RitualService(RitualRepository ritualRepository, RitualMapper ritualMapper) {
        this.ritualRepository = ritualRepository;
        this.ritualMapper = ritualMapper;
    }


    public RitualResponse createRitual(CreateRitualRequest request) {

        if (ritualRepository.existsByName(request.getName())) {
            throw new IllegalArgumentException(
                    "A ritual with the name '" + request.getName() + "' already exists"
            );
        }

        Ritual ritual = ritualMapper.toEntity(request);

        LocalDateTime now = LocalDateTime.now();

        ritual.setCreatedAt(now);
        ritual.setUpdatedAt(now);

        ritual = ritualRepository.save(ritual);

        return ritualMapper.toResponse(ritual);
    }


    public List<RitualResponse> getAllRituals(){
        return ritualRepository.findAll()
                .stream()
                .map(ritualMapper::toResponse)
                .toList();
    }

    public RitualResponse getRitualById(Long id){

    Ritual ritual = ritualRepository.findById(id)
        .orElseThrow(() ->
            new ResourceNotFoundException("This Ritual ID " + id + " not found"));

        return ritualMapper.toResponse(ritual);

    }

    public RitualResponse updateRitual(Long id, UpdateRitualRequest request){

        Ritual ritual = ritualRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException("This Ritual ID " + id + " not found"));

        ritualMapper.updateEntity(ritual, request);

        ritual.setUpdatedAt(LocalDateTime.now());

        ritual = ritualRepository.save(ritual);

        return ritualMapper.toResponse(ritual);

    }


    public void deleteRitual(Long id){
        Ritual ritual = ritualRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException("This Ritual ID " + id + " not found"));

        ritualRepository.delete(ritual);

    }



}
