package fr.dossierfacile.garbagecollector.controller;

import fr.dossierfacile.garbagecollector.dto.PathDTO;
import fr.dossierfacile.garbagecollector.model.object.Object;

import fr.dossierfacile.garbagecollector.service.ScheduledDeleteService;
import fr.dossierfacile.garbagecollector.service.interfaces.MarkerService;
import fr.dossierfacile.garbagecollector.service.interfaces.ObjectService;
import fr.dossierfacile.garbagecollector.service.interfaces.OvhService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Controller
@RequiredArgsConstructor
public class ObjectController {

    private final ObjectService objectService;
    private final OvhService ovhService;
    private final MarkerService markerServiceImp;

    private static final String SCHEDULED_TASKS = "scheduledTasks";
    private final ScheduledDeleteService deleteSchedule;

    @GetMapping("/")
    public String index() {
        return "redirect:checker";
    }

    //get objects
    @GetMapping("/path/all")
    public String getAllPath(Model model) throws IOException {
        markerServiceImp.getObjectFromOvhAndProcess();
        PathDTO path = new PathDTO();
        model.addAttribute("path", path);
        return "redirect:/checker";
    }

    //main view
    @GetMapping("/checker")
    public String object(Model model) {

        List<Object> objectList = objectService.getAllObjectInTrue();
        PathDTO path = new PathDTO();
        model.addAttribute("path", path);

        if (!objectService.getAll().isEmpty()) {
            model.addAttribute("objectList", objectList);
        } else {
            model.addAttribute("objectList", new ArrayList<>());
        }
        model.addAttribute("to_process", objectService.getAll().size());
        model.addAttribute("is_delete_running",deleteSchedule.IsActive());
        model.addAttribute("is_importing_running",markerServiceImp.isRunning());
        model.addAttribute("read_count",markerServiceImp.getReadCount());
        if (!objectList.isEmpty()) {
            model.addAttribute("candidates", objectService.getObjectListToDeleteTrue());
        }
        return "index";
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
    @GetMapping("/update/all")
    public String updateAllTo_DeleteObjects() {
        objectService.updateAllToDeleteObjects();
        return "redirect:/checker";
    }

    @GetMapping("/delete/toggle")
    public String toggleDelete() {
        deleteSchedule.setIsActive(!deleteSchedule.IsActive());
        return "redirect:/checker";
    }
}
