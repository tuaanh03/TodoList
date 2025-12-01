package com.example.todolist.TodoModel;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "tasks")  // Đánh dấu class này là document trong MongoDB, collection name = "tasks"
@Data  // Lombok tự tạo getter/setter/toString/equals/hashCode
@NoArgsConstructor  // Tạo constructor không tham số (bắt buộc cho MongoDB)
@AllArgsConstructor  // Tạo constructor với tất cả fields

public class Task {
    @Id   // Đánh dấu field này là _id trong MongoDB (MongoDB tự sinh nếu null)
    private String id;

    private String title;

    private String description;

    // Giá trị mặc định = false cho task mới
    private boolean completed = false;
}

