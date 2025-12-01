package com.example.todolist.TodoService;

import com.example.todolist.TodoModel.Task;

import java.util.List;
import java.util.Optional;

/**
 * TaskService Interface - Contract cho business logic layer
 *
 * QUY TẮC VÀNG #1: INTERFACE LÀ VUA
 * - Controller phụ thuộc vào interface này, KHÔNG phải implementation
 * - Có thể có nhiều implementation khác nhau
 * - Dễ dàng test bằng cách mock interface
 */
public interface TaskService {

    /**
     * Tạo task mới
     * @param task Thông tin task
     * @return Task đã được lưu (có id)
     */
    Task createTask(Task task);

    /**
     * Lấy task theo ID
     * @param id ID của task
     * @return Optional chứa task nếu tìm thấy
     */
    Optional<Task> getTaskById(String id);

    /**
     * Lấy tất cả tasks
     * @return Danh sách tất cả tasks
     */
    List<Task> getAllTasks();

    /**
     * Lấy tasks theo trạng thái completed
     * @param completed Trạng thái cần lọc
     * @return Danh sách tasks
     */
//    List<Task> getTasksByStatus(boolean completed);

    /**
     * Tìm kiếm tasks theo từ khóa trong title
     * @param keyword Từ khóa tìm kiếm
     * @return Danh sách tasks phù hợp
     */
//    List<Task> searchTasks(String keyword);

    /**
     * Update task
     * @param id ID của task cần update
     * @param task Thông tin mới
     * @return Optional chứa task đã update
     */
    Optional<Task> updateTask(String id, Task task);

    /**
     * Xóa task theo ID
     * @param id ID của task cần xóa
     * @return true nếu xóa thành công, false nếu không tìm thấy
     */
    boolean deleteTask(String id);

    /**
     * Xóa tất cả tasks
     */
    void deleteAllTasks();

    /**
     * Đếm tổng số tasks
     * @return Số lượng tasks
     */
    long countTasks();
}

