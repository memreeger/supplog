package com.supplog.controller;

import com.supplog.dto.supplement.CreateSupplementRequestDto;
import com.supplog.dto.supplement.SupplementResponseDto;
import com.supplog.dto.supplement.UpdateSupplementDosageRequestDto;
import com.supplog.dto.supplement.UpdateSupplementRequestDto;
import com.supplog.service.supplement.SupplementService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/supplements")
public class SupplementController {
    private final SupplementService supplementService;

    public SupplementController(SupplementService supplementService) {
        this.supplementService = supplementService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public void addSupplement(@RequestBody CreateSupplementRequestDto supplementRequestDto) {
        supplementService.addSupplement(supplementRequestDto);
    }

    @GetMapping
    public List<SupplementResponseDto> getAll() {
        return supplementService.getAll();
    }


    @GetMapping("/supplement/{id}")
    public SupplementResponseDto getById(@PathVariable Long id) {
        return supplementService.getById(id);
    }

    @GetMapping("/userId/{userId}")
    public List<SupplementResponseDto> getAllByUserId(@PathVariable Long userId) {
        return supplementService.getAllByUserIdIsDeletedFalse(userId);
    }

    @GetMapping("userId/{userId}/allSupplements")
    public List<SupplementResponseDto> findAllByInsertedByUserId(@PathVariable Long userId) {
        return supplementService.findAllByInsertedByUserId(userId);
    }


    @PutMapping("/supplementName/{name}/updateSupplement")
    public void updateSupplement(@PathVariable String name,
                                 @RequestBody UpdateSupplementRequestDto requestDto) {
        supplementService.updateSupplement(name, requestDto);
    }

    @PutMapping("supplementName/{name}/updateDosage")
    public void updateSupplementDosage(@PathVariable String name,
                                       @RequestBody UpdateSupplementDosageRequestDto requestDto) {
        supplementService.updateSupplementDosage(name, requestDto);
    }

    @DeleteMapping("/supplementName/{name}")
    public void deleteSupplement(@PathVariable String name) {
        supplementService.deleteSupplement(name);
    }

}
