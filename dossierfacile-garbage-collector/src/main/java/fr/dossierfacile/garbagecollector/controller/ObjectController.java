package fr.dossierfacile.garbagecollector.controller;

import fr.dossierfacile.garbagecollector.dto.PathDTO;
import fr.dossierfacile.garbagecollector.model.object.Object;
import fr.dossierfacile.garbagecollector.service.ScheduledDeleteService;
import fr.dossierfacile.garbagecollector.service.interfaces.MarkerService;
import fr.dossierfacile.garbagecollector.service.interfaces.ObjectService;
import fr.dossierfacile.garbagecollector.service.interfaces.OvhService;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

    private final ScheduledDeleteService deleteSchedule;

    @GetMapping("/")
    public String index() {
        return "redirect:checker";
    }

    //get objects
    @GetMapping("/toggle/scanner")
    public String toggleScanner() {
        boolean isNowRunning = markerService.toggleScanner();
        if (isNowRunning) {
            markerService.startScanner();
        }
        return "redirect:/checker";
    }

    @GetMapping("/restart/scanner")
    public String restartScanner() {
        markerService.setRunningToFalse();
        markerService.cleanDatabaseOfScanner();
        markerService.setRunningToTrue();
        markerService.startScanner();
        return "redirect:/checker";
    }

    //main view
    @GetMapping("/checker")
    public String object(Model model) {

        PathDTO path = new PathDTO();
        model.addAttribute("path", path);

        List<Object> objectsForDeletion = objectService.getAllObjectsForDeletion();
        if (!objectsForDeletion.isEmpty()) {
            model.addAttribute("objectsForDeletion", objectsForDeletion);
        } else {
            model.addAttribute("objectsForDeletion", new ArrayList<>());
        }

        model.addAttribute("total_objects_to_delete", objectService.countAllObjectsForDeletion());
        model.addAttribute("total_objects_scanned", objectService.countAllObjectsScanned());
        model.addAttribute("is_scanner_running", markerService.isRunning());
        model.addAttribute("is_delete_running", deleteSchedule.isActive());

        return "index";
    }

    @GetMapping(value = "/update-scanning-info", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<String>> currentReadObjects() {
        List<String> result = new ArrayList<>();
        result.add(String.valueOf(objectService.countAllObjectsForDeletion()));
        result.add(String.valueOf(objectService.countAllObjectsScanned()));
        result.add(String.valueOf(markerService.isRunning()));
        return ResponseEntity.ok(result);
    }

    //search object
    @GetMapping("/checker/searchPath")
    public String searchPath(Model model, PathDTO pathDTO) {

        Object path = objectService.findObjectByPath(pathDTO.getPath());
        if (path == null) {
            return "redirect:/checker";
        }
        model.addAttribute("path", path);
        return "search-object";
    }

    //delete one object
    @GetMapping("/delete/object/{path}")
    public String deleteDocument(@PathVariable("path") String path) {
        ovhService.delete(path);
        objectService.deleteObjectByPath(path);
        return "redirect:/checker";
    }

    //to_delete action
//    @GetMapping("/update/all")
//    public String updateAllTo_DeleteObjects() {
//        objectService.updateAllToDeleteObjects();
//        return "redirect:/checker";
//    }

    @GetMapping("/delete/toggle")
    public String toggleDelete() {
        deleteSchedule.setIsActive(!deleteSchedule.isActive());
        return "redirect:/checker";
    }
}
