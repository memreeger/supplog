package com.supplog.service.supplement.impl;

import com.supplog.dto.supplement.CreateSupplementRequestDto;
import com.supplog.entity.Supplement;
import com.supplog.repository.SupplementRepository;
import com.supplog.repository.UserRepository;
import com.supplog.service.supplement.SupplementService;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

@Service
public class SupplementServiceImpl implements SupplementService {
    private final SupplementRepository supplementRepository;
    private final ModelMapper mapper;

    public SupplementServiceImpl(SupplementRepository supplementRepository, ModelMapper mapper) {
        this.supplementRepository = supplementRepository;
        this.mapper = mapper;
    }

    @Override
    public void addSupplement(CreateSupplementRequestDto requestDto) {
        Supplement supplement = new Supplement();
        mapper.map(requestDto, supplement);

        supplementRepository.save(supplement);
    }
}
