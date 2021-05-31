package fr.dossierfacile.api.front.controller;

import fr.dossierfacile.api.front.service.interfaces.GuarantorService;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@AllArgsConstructor
@RequestMapping("/api/guarantor")
public class GuarantorController {
    private final GuarantorService guarantorService;

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        guarantorService.delete(id);
        return ResponseEntity.ok().build();
    }

}
