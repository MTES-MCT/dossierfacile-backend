package fr.dossierfacile.garbagecollector.controller;

import com.fasterxml.jackson.annotation.JsonView;
import fr.dossierfacile.garbagecollector.model.object.Object;
import fr.dossierfacile.garbagecollector.service.ScheduledDeleteService;
import fr.dossierfacile.garbagecollector.service.interfaces.MarkerService;
import fr.dossierfacile.garbagecollector.service.interfaces.ObjectService;
import fr.dossierfacile.garbagecollector.service.interfaces.OvhService;
import java.util.ArrayList;
import java.util.List;
import javax.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.jpa.datatables.mapping.DataTablesInput;
import org.springframework.data.jpa.datatables.mapping.DataTablesOutput;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Slf4j
@Controller
@RequiredArgsConstructor
public class ObjectController {

    private final ObjectService objectService;
    private final OvhService ovhService;
    private final MarkerService markerService;
    private final ScheduledDeleteService scheduledDeleteService;

    @GetMapping("/")
    public String index() {
        return "redirect:checker";
    }

    //main view
    @GetMapping("/checker")
    public String object(Model model) {
        model.addAttribute("total_objects_to_delete", objectService.countAllObjectsForDeletion());
        model.addAttribute("total_objects_scanned", objectService.countAllObjectsScanned());
        model.addAttribute("is_scanner_running", markerService.isRunning());
        model.addAttribute("is_delete_running", scheduledDeleteService.isActive());

        return "index";
    }

    //get objects
    @GetMapping("/start-stop-scanner")
    public String toggleScanner() {
        boolean isNowRunning = markerService.toggleScanner();
        if (isNowRunning) {
            markerService.startScanner();
        }
        return "redirect:/checker";
    }

    @GetMapping("/restart-scanner")
    public String restartScanner() {
        markerService.setRunningToFalse();
        markerService.cleanDatabaseOfScanner();
        markerService.setRunningToTrue();
        markerService.startScanner();
        return "redirect:/checker";
    }

    @GetMapping(value = "/update-scanner-info", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<String>> currentReadObjects() {
        List<String> result = new ArrayList<>();
        result.add(String.valueOf(objectService.countAllObjectsForDeletion()));
        result.add(String.valueOf(objectService.countAllObjectsScanned()));
        result.add(String.valueOf(markerService.isRunning()));
        return ResponseEntity.ok(result);
    }

    @JsonView(DataTablesOutput.View.class)
    @GetMapping(value = "/objects")
    public ResponseEntity<DataTablesOutput<Object>> getObjects(@Valid DataTablesInput input) {
        return ResponseEntity.ok(objectService.getAllObjectsForDeletion(input));
    }

    //delete one object
    @GetMapping("/delete/{path}")
    public String deleteObject(@PathVariable("path") String path) {
        ovhService.delete(path);
        objectService.deleteObjectByPath(path);
        return "redirect:/checker";
    }

    @GetMapping("/delete/toggle")
    public String toggleDelete() {
        scheduledDeleteService.setIsActive(!scheduledDeleteService.isActive());
        return "redirect:/checker";
    }
}
