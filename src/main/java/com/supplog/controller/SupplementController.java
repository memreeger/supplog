package com.supplog.controller;

import com.supplog.dto.supplement.CreateSupplementRequestDto;
import com.supplog.dto.supplement.SupplementResponseDto;
import com.supplog.dto.supplement.UpdateSupplementDosageRequestDto;
import com.supplog.dto.supplement.UpdateSupplementRequestDto;
import com.supplog.service.supplement.SupplementService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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

    @GetMapping("/all")
    public ResponseEntity <List<SupplementResponseDto>> getAll() {
        return ResponseEntity.ok(supplementService.getAll());
    }


    @GetMapping("/{id}")
    public ResponseEntity<SupplementResponseDto> getById(@PathVariable Long id) {
        SupplementResponseDto responseDto = supplementService.getById(id);
        return ResponseEntity.ok(responseDto);
    }

    @GetMapping("/users/{userId}")
    public List<SupplementResponseDto> getAllByUserId(@PathVariable Long userId) {
        return supplementService.getAllByUserIdIsDeletedFalse(userId);
    }


    @GetMapping("/users/{userId}/allSupplements")
    public List<SupplementResponseDto> findAllByInsertedByUserId(@PathVariable Long userId) {
        return supplementService.findAllByInsertedByUserId(userId);
    }



    @PutMapping("/{id}")
    public ResponseEntity<Void> updateSupplement(@PathVariable Long id,
                                 @RequestBody UpdateSupplementRequestDto requestDto) {
        supplementService.updateSupplement(id, requestDto);
       return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/dosage")
    public void updateSupplementDosage(@PathVariable Long id,
                                       @RequestBody UpdateSupplementDosageRequestDto requestDto) {
        supplementService.updateSupplementDosage(id, requestDto);
    }

    @DeleteMapping("/{id}")
    public void deleteSupplement(@PathVariable Long id) {
        supplementService.deleteSupplement(id);
    }

}
