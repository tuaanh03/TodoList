package com.example.todolist.TodoService;

import com.example.todolist.TodoModel.Task;
import com.example.todolist.TodoRepository.TaskRepo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * TaskServiceImpl - Implementation của TaskService interface
 *
 * @Service - Đánh dấu đây là service bean, Spring sẽ tự động tạo và quản lý
 * @RequiredArgsConstructor - Lombok tự động tạo constructor cho final fields
 * @Slf4j - Lombok tự động tạo logger
 *
 * IMPLEMENT TaskService - Class này thực thi contract từ interface
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TaskServiceImpl implements TaskService {

    /**
     * Dependency: TaskRepo - Inject qua constructor
     * Spring tự động inject TaskRepo bean vào đây
     */
    private final TaskRepo taskRepo;

    /**
     * Tạo task mới
     *
     * LUỒNG:
     * 1. Controller nhận JSON từ client
     * 2. Spring tự động chuyển JSON → Task object (đã khởi tạo sẵn)
     * 3. Service nhận object Task, validate và save
     *
     * Business logic:
     * - Validation: title không được rỗng
     * - Task mới luôn có completed = false (đã set mặc định trong Model)
     * - Logging để trace
     */
    @Override
    public Task createTask(Task task) {
        log.info("Creating new task with title: {}", task.getTitle());

        // Validation: Title bắt buộc
        if (task.getTitle() == null || task.getTitle().trim().isEmpty()) {
            log.error("Task title cannot be empty");
            throw new IllegalArgumentException("Task title cannot be empty");
        }

        // Business rule: Task mới luôn chưa hoàn thành
        // Nếu client gửi completed = true, reset về false
        if (task.isCompleted()) {
            log.warn("New task cannot be completed, resetting to false");
            task.setCompleted(false);   // Đặt lại completed = false, và đóng gói lại, isCompleted() sẽ trả về false
        }

        // Save vào MongoDB
        Task savedTask = taskRepo.save(task);

        log.info("Task created successfully with ID: {}", savedTask.getId());
        return savedTask;
    }

    /**
     * Lấy task theo ID
     */
    @Override
    public Optional<Task> getTaskById(String id) {
        log.info("Fetching task with ID: {}", id);
        return taskRepo.findById(id);
    }

    /**
     * Lấy tất cả tasks
     */
    @Override
    public List<Task> getAllTasks() {
        log.info("Fetching all tasks");
        return taskRepo.findAll();
    }

    /**
     * Lấy tasks theo status
     */
//    @Override
//    public List<Task> getTasksByStatus(boolean completed) {
//        log.info("Fetching tasks with completed status: {}", completed);
//        return taskRepo.findByCompleted(completed);
//    }

    /**
     * Tìm kiếm tasks
     */
//    @Override
//    public List<Task> searchTasks(String keyword) {
//        log.info("Searching tasks with keyword: {}", keyword);
//        return taskRepo.findByTitleContainingIgnoreCase(keyword);
//    }

    /**
     * Update task
     */
    @Override
    public Optional<Task> updateTask(String id, Task task) {
        log.info("Updating task with ID: {}", id);

        return taskRepo.findById(id)
                .map(existingTask -> {
                    // Update fields
                    existingTask.setTitle(task.getTitle());
                    existingTask.setDescription(task.getDescription());
                    existingTask.setCompleted(task.isCompleted());

                    // Save updated task
                    Task updatedTask = taskRepo.save(existingTask);

                    log.info("Task updated successfully: {}", id);
                    return updatedTask;
                });
    }

    /**
     * Xóa task
     */
    @Override
    public boolean deleteTask(String id) {
        log.info("Deleting task with ID: {}", id);

        if (taskRepo.existsById(id)) {
            taskRepo.deleteById(id);
            log.info("Task deleted successfully: {}", id);
            return true;
        }

        log.warn("Task not found with ID: {}", id);
        return false;
    }

    /**
     * Xóa tất cả tasks
     */
    @Override
    public void deleteAllTasks() {
        log.info("Deleting all tasks");
        taskRepo.deleteAll();
        log.info("All tasks deleted successfully");
    }

    /**
     * Đếm tasks
     */
    @Override
    public long countTasks() {
        long count = taskRepo.count();
        log.info("Total tasks count: {}", count);
        return count;
    }
}

