package com.example.todolist.TodoRepository;

import com.example.todolist.TodoModel.Task;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TaskRepo extends MongoRepository<Task, String> {
    //Đối với phần này , đây là phâần interface
    //Interface - chỉ khai báo, KHÔNG CẦN viết implementation
    //Extends MongoRepository<Task, String>:
    //Task → Entity class (đối tượng làm việc)
    //String → Kiểu dữ liệu của ID
    //Spring tự động TẠO implementation class ở runtime

    //Chú ý
//    Spring quét thấy TaskRepo extends MongoRepository
//    Spring tự động tạo 1 class implement TaskRepo (bạn không thấy code)
//    Class này có sẵn các methods:
//    save(Task) - Insert hoặc update
//    findById(String) - Tìm theo ID
//    findAll() - Lấy tất cả
//    deleteById(String) - Xóa theo ID
//    count() - Đếm
//    Spring tạo bean taskRepo và đưa vào ApplicationContext

    /**
     * Tìm tasks theo trạng thái completed
     * Spring tự động tạo query: db.task.find({completed: true/false})
     */
//    List<Task> findByCompleted(boolean completed);
//
//    /**
//     * Tìm tasks có title chứa keyword (không phân biệt hoa thường)
//     * Spring tự động tạo query: db.task.find({title: /keyword/i})
//     */
//    List<Task> findByTitleContainingIgnoreCase(String keyword);
}
