package com.example.todolist.TodoController;

import com.example.todolist.TodoModel.Task;
import com.example.todolist.TodoService.TaskService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * MainController - REST API Controller
 *
 * ═══════════════════════════════════════════════════════════════════
 * QUY TẮC VÀNG #1: INTERFACE LÀ VUA
 * ═══════════════════════════════════════════════════════════════════
 *
 * ✅ Phụ thuộc vào TaskService (INTERFACE), KHÔNG phải TaskServiceImpl
 * ✅ Controller chỉ gọi service, không gọi trực tiếp repository
 * ✅ Dễ test - có thể mock TaskService interface
 * ✅ Dễ thay đổi implementation mà không sửa controller
 *
 * ═══════════════════════════════════════════════════════════════════
 */
@RestController
@RequestMapping("/api/tasks")
@RequiredArgsConstructor  // Lombok tự tạo constructor cho final fields
public class MainController {

    /**
     * Dependency: TaskService INTERFACE (không phải TaskServiceImpl!)
     * Spring tự động inject TaskServiceImpl bean vào đây
     */
    private final TaskService taskService;

    // ═══════════════════════════════════════════════════════════════
    // CREATE - Tạo task mới
    // ═══════════════════════════════════════════════════════════════
    @PostMapping
    public ResponseEntity<Task> createTask(@RequestBody Task task) {
        Task createdTask = taskService.createTask(task);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdTask);
    }

    // ═══════════════════════════════════════════════════════════════
    // READ - Lấy tất cả tasks
    // ═══════════════════════════════════════════════════════════════
    @GetMapping
    public ResponseEntity<List<Task>> getAllTasks() {
        List<Task> tasks = taskService.getAllTasks();
        return ResponseEntity.ok(tasks);
    }

    // ═══════════════════════════════════════════════════════════════
    // READ - Lấy task theo ID
    // ═══════════════════════════════════════════════════════════════
    @GetMapping("/{id}")
    public ResponseEntity<Task> getTaskById(@PathVariable String id) {
        return taskService.getTaskById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // ═══════════════════════════════════════════════════════════════
    // READ - Tìm kiếm tasks
    // ═══════════════════════════════════════════════════════════════
//    @GetMapping("/search")
//    public ResponseEntity<List<Task>> searchTasks(@RequestParam String keyword) {
//        List<Task> tasks = taskService.searchTasks(keyword);
//        return ResponseEntity.ok(tasks);
//    }

    // ═══════════════════════════════════════════════════════════════
    // READ - Lấy tasks theo status
    // ═══════════════════════════════════════════════════════════════
//    @GetMapping("/completed/{status}")
//    public ResponseEntity<List<Task>> getTasksByStatus(@PathVariable boolean status) {
//        List<Task> tasks = taskService.getTasksByStatus(status);
//        return ResponseEntity.ok(tasks);
//    }

    // ═══════════════════════════════════════════════════════════════
    // UPDATE - Cập nhật task
    // ═══════════════════════════════════════════════════════════════
    @PutMapping("/{id}")
    public ResponseEntity<Task> updateTask(
            @PathVariable String id,
            @RequestBody Task task) {
        return taskService.updateTask(id, task)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // ═══════════════════════════════════════════════════════════════
    // DELETE - Xóa task
    // ═══════════════════════════════════════════════════════════════
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTask(@PathVariable String id) {
        boolean deleted = taskService.deleteTask(id);
        return deleted
            ? ResponseEntity.noContent().build()
            : ResponseEntity.notFound().build();
    }

    // ═══════════════════════════════════════════════════════════════
    // DELETE - Xóa tất cả tasks
    // ═══════════════════════════════════════════════════════════════
    @DeleteMapping
    public ResponseEntity<Void> deleteAllTasks() {
        taskService.deleteAllTasks();
        return ResponseEntity.noContent().build();
    }

    // ═══════════════════════════════════════════════════════════════
    // COUNT - Đếm tasks
    // ═══════════════════════════════════════════════════════════════
    @GetMapping("/count")
    public ResponseEntity<Long> countTasks() {
        long count = taskService.countTasks();
        return ResponseEntity.ok(count);
    }
}
