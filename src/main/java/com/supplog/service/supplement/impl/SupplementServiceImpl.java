package com.supplog.service.supplement.impl;

import com.supplog.dto.supplement.CreateSupplementRequestDto;
import com.supplog.dto.supplement.SupplementResponseDto;
import com.supplog.dto.supplement.UpdateSupplementDosageRequestDto;
import com.supplog.dto.supplement.UpdateSupplementRequestDto;
import com.supplog.entity.Supplement;
import com.supplog.entity.User;
import com.supplog.repository.SupplementRepository;
import com.supplog.repository.UserRepository;
import com.supplog.service.supplement.SupplementService;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class SupplementServiceImpl implements SupplementService {
    private final SupplementRepository supplementRepository;
    private final UserRepository userRepository;
    private final ModelMapper mapper;

    public SupplementServiceImpl(SupplementRepository supplementRepository, UserRepository userRepository, ModelMapper mapper) {
        this.supplementRepository = supplementRepository;
        this.userRepository = userRepository;
        this.mapper = mapper;
    }

    @Override
    public void addSupplement(CreateSupplementRequestDto requestDto) {

        User user = userRepository.findById(requestDto.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        Supplement supplement = new Supplement();

        supplement.setName(requestDto.getName());
        supplement.setSuppDosage(requestDto.getSuppDosage());
        supplement.setExpireDate(requestDto.getExpireDate());
        supplement.setType(requestDto.getType());
        supplement.setInsertedByUser(user);

        supplementRepository.save(supplement);
    }

    @Override
    public SupplementResponseDto getById(Long id) {
        Supplement supplement = supplementRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException(""));

        SupplementResponseDto dto = mapper.map(supplement, SupplementResponseDto.class);
        return dto;
    }

    @Override
    public List<SupplementResponseDto> getAll() {
        List<Supplement> allSupplements = supplementRepository.findAll();
        List<SupplementResponseDto> allSupplementDtos = new ArrayList<>();
        for (Supplement supplement : allSupplements) {
            allSupplementDtos.add(mapper.map(supplement, SupplementResponseDto.class));
        }
        return allSupplementDtos;
    }

    @Override
    public List<SupplementResponseDto> getAllByUserIdIsDeletedFalse(Long userId) {
        List<Supplement> supplements =
                supplementRepository.findAllByInsertedByUserIdAndIsDeletedFalse(userId);

        List<SupplementResponseDto> dtos = new ArrayList<>();
        for (Supplement supplement : supplements) {
            dtos.add(mapper.map(supplement, SupplementResponseDto.class));
        }

        return dtos;
    }

    public List<SupplementResponseDto> findAllByInsertedByUserId(Long userId){
        List<Supplement> supplements =
                supplementRepository.findAllByInsertedByUserId(userId);
        List<SupplementResponseDto> dtos = new ArrayList<>();

        for(Supplement supplement : supplements){
            dtos.add(mapper.map(supplement,SupplementResponseDto.class));
        }
        return dtos;

    }

    @Override
    public void updateSupplement(String name, UpdateSupplementRequestDto requestDto) {
        Supplement supplement = findByName(name);
        supplement.setName(requestDto.getName());
        supplement.setSuppDosage(requestDto.getSuppDosage());
        supplement.setExpireDate(requestDto.getExpireDate());
        supplement.setType(requestDto.getType());

        supplementRepository.save(supplement);
    }

    @Override
    public void updateSupplementDosage(String name, UpdateSupplementDosageRequestDto requestDto) {
        Supplement supplement = findByName(name);
        supplement.setSuppDosage(requestDto.getDosage());

        supplementRepository.save(supplement);
    }

    @Override
    public void deleteSupplement(String name) {
        Supplement supplement = findByName(name);
        supplement.setDeleted(true);
        supplementRepository.save(supplement);
    }

    //Helper methods
    public Supplement findByName(String name) {
        return supplementRepository.findSupplementByName(name)
                .orElseThrow(() -> new IllegalArgumentException("Supplement not found"));
    }
}
