package fr.gouv.bo.controller;

import fr.gouv.bo.dto.ApartmentSharingDTO01;
import fr.gouv.bo.model.DataTable;
import fr.gouv.bo.service.ApartmentSharingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
@Slf4j
public class BORestController {

    private final ApartmentSharingService apartmentSharingService;

    @GetMapping("/bo/colocation/table")
    public ResponseEntity<DataTable<ApartmentSharingDTO01>> listAllTable(@RequestParam("draw") int draw,
                                                                         @RequestParam("start") int start,
                                                                         @RequestParam("length") int length) {
        int page = start / length; //Calculate page number

        Pageable pageable = PageRequest.of(
                page,
                length,
                Sort.by(Sort.Direction.DESC, "id")
        );
        Page<ApartmentSharingDTO01> responseData = apartmentSharingService.findAll(pageable);
        DataTable<ApartmentSharingDTO01> dataTable = new DataTable<>();
        dataTable.setData(responseData.getContent());
        dataTable.setRecordsTotal(responseData.getTotalElements());
        dataTable.setRecordsFiltered(responseData.getTotalElements());

        dataTable.setDraw(draw);
        dataTable.setStart(start);
        return ResponseEntity.ok(dataTable);

    }
}
