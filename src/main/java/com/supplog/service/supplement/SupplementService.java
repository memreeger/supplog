package com.supplog.service.supplement;

import com.supplog.dto.supplement.CreateSupplementRequestDto;
import com.supplog.entity.Supplement;
import org.springframework.stereotype.Service;

@Service
public interface SupplementService {
    void addSupplement(CreateSupplementRequestDto supplementRequestDto);

    //CRUD SoftDelete
}
