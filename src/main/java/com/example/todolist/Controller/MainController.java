package com.example.todolist.Controller;


import com.example.todolist.Model.Task;
import com.example.todolist.Repository.TaskRepo;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class MainController {

    private final TaskRepo taskRepo;

    // Constructor injection (Spring will autowire this single constructor)
    public MainController(TaskRepo taskRepo) {
        this.taskRepo = taskRepo;
    }

    @PostMapping("/api/addTask")
    public ResponseEntity<Task> addTask(@RequestBody Task task) {
        Task saved = taskRepo.save(task);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }
}
