# Vì sao Service không khởi tạo `new Task()`?

## 📌 Câu hỏi
Tại sao trong `TaskServiceImpl.createTask()` không có dòng `Task task = new Task()` mà lại nhận parameter `Task task` có sẵn và dùng `task.setCompleted(false)`?

---

## 🔄 Luồng hoạt động từ Client → Database

```
┌─────────────────────────────────────────────────────────────────┐
│                          CLIENT (Postman)                        │
│                                                                   │
│  POST http://localhost:8080/api/tasks                            │
│  Content-Type: application/json                                  │
│                                                                   │
│  Body:                                                            │
│  {                                                                │
│    "title": "Learn Spring Boot",                                 │
│    "description": "Study @RestController",                       │
│    "completed": false                                            │
│  }                                                                │
└───────────────────────────────┬─────────────────────────────────┘
                                │
                                │ HTTP Request (JSON)
                                ↓
┌─────────────────────────────────────────────────────────────────┐
│                    CONTROLLER (MainController)                   │
│                                                                   │
│  @RestController                                                 │
│  @RequestMapping("/api/tasks")                                   │
│  public class MainController {                                   │
│                                                                   │
│      @PostMapping                                                │
│      public ResponseEntity<Task> addTask(                        │
│          @RequestBody Task task  ← OBJECT ĐÃ ĐƯỢC TẠO Ở ĐÂY!   │
│      ) {                                                          │
│          // Spring đã tự động:                                   │
│          // 1. Parse JSON                                        │
│          // 2. new Task()                                        │
│          // 3. task.setTitle("Learn Spring Boot")               │
│          // 4. task.setDescription("Study @RestController")     │
│          // 5. task.setCompleted(false)                         │
│                                                                   │
│          Task savedTask = taskService.createTask(task);          │
│          return ResponseEntity.ok(savedTask);                    │
│      }                                                            │
│  }                                                                │
└───────────────────────────────┬─────────────────────────────────┘
                                │
                                │ Truyền object Task (đã khởi tạo)
                                ↓
┌─────────────────────────────────────────────────────────────────┐
│                   SERVICE (TaskServiceImpl)                      │
│                                                                   │
│  @Service                                                        │
│  public class TaskServiceImpl implements TaskService {           │
│                                                                   │
│      @Override                                                   │
│      public Task createTask(Task task) {                         │
│          // Task đã là object hoàn chỉnh!                        │
│          // KHÔNG CẦN new Task() nữa                            │
│                                                                   │
│          // Chỉ cần:                                             │
│          // 1. Validate                                          │
│          if (task.getTitle() == null) {                          │
│              throw new IllegalArgumentException(...);            │
│          }                                                        │
│                                                                   │
│          // 2. Business logic                                    │
│          if (task.isCompleted()) {                               │
│              task.setCompleted(false);  ← SỬA GIÁ TRỊ          │
│          }                                                        │
│                                                                   │
│          // 3. Save                                              │
│          return taskRepo.save(task);                             │
│      }                                                            │
│  }                                                                │
└───────────────────────────────┬─────────────────────────────────┘
                                │
                                │ Truyền object Task
                                ↓
┌─────────────────────────────────────────────────────────────────┐
│                    REPOSITORY (TaskRepo)                         │
│                                                                   │
│  public interface TaskRepo                                       │
│      extends MongoRepository<Task, String> {                     │
│      // Spring Data tự động implement                           │
│      // save(), findById(), findAll()...                        │
│  }                                                                │
└───────────────────────────────┬─────────────────────────────────┘
                                │
                                │ MongoDB Driver thực thi query
                                ↓
┌─────────────────────────────────────────────────────────────────┐
│                      DATABASE (MongoDB)                          │
│                                                                   │
│  Collection: tasks                                               │
│  {                                                                │
│    "_id": "674b8a1f2e3d4c5f6g7h8i9j",  ← MongoDB tự sinh       │
│    "title": "Learn Spring Boot",                                 │
│    "description": "Study @RestController",                       │
│    "completed": false                                            │
│  }                                                                │
└─────────────────────────────────────────────────────────────────┘
```

---

## 🎯 Trách nhiệm của từng layer

| Layer | Trách nhiệm | Có tạo object không? |
|-------|------------|---------------------|
| **Controller** | - Nhận HTTP request<br>- Parse JSON → Object (Spring tự động)<br>- Gọi Service<br>- Trả HTTP response | ✅ **CÓ** (Spring tự động) |
| **Service** | - Validate data<br>- Xử lý business logic<br>- Transform/modify object<br>- Gọi Repository | ❌ **KHÔNG** (nhận object có sẵn) |
| **Repository** | - CRUD với database<br>- Chuyển object → DB query | ❌ **KHÔNG** |

---

## 💡 Khi nào Service CẦN tạo object mới?

### Trường hợp 1: Sử dụng DTO Pattern (Best Practice)

```java
// DTO (Data Transfer Object) - không có @Id, không có metadata
public class TaskCreateDTO {
    private String title;
    private String description;
    // Không có id, không có completed
}

// Service
@Override
public Task createTask(TaskCreateDTO dto) {
    // TẠO Entity mới từ DTO
    Task task = new Task();
    task.setTitle(dto.getTitle());
    task.setDescription(dto.getDescription());
    task.setCompleted(false);  // Set giá trị mặc định
    
    return taskRepo.save(task);
}
```

**Ưu điểm DTO:**
- Client không thể gửi `id` hoặc `completed = true` để hack
- Tách biệt cấu trúc API với cấu trúc Database
- Dễ thay đổi database mà không ảnh hưởng API

---

### Trường hợp 2: Tạo object phụ trợ

```java
@Override
public Task createTask(Task task) {
    // Validate
    validateTask(task);
    
    // Tạo audit log (object phụ trợ)
    AuditLog log = new AuditLog();  ← TẠO OBJECT MỚI
    log.setAction("CREATE_TASK");
    log.setUserId(currentUser.getId());
    log.setTimestamp(LocalDateTime.now());
    
    // Save task
    Task saved = taskRepo.save(task);
    
    // Save audit log
    auditLogRepo.save(log);
    
    return saved;
}
```

---

### Trường hợp 3: Clone/Copy object

```java
@Override
public Task duplicateTask(String taskId) {
    // Tìm task gốc
    Task original = taskRepo.findById(taskId)
        .orElseThrow(() -> new TaskNotFoundException(taskId));
    
    // TẠO bản copy
    Task duplicate = new Task();  ← TẠO OBJECT MỚI
    duplicate.setTitle(original.getTitle() + " (Copy)");
    duplicate.setDescription(original.getDescription());
    duplicate.setCompleted(false);
    
    return taskRepo.save(duplicate);
}
```

---

## 🔧 Giải thích về `task.setCompleted(false)`

### ❌ Cách cũ (Gây nhầm lẫn)

```java
// Task.java
public class Task {
    private boolean completed;
    
    // Lombok @Data đã tạo sẵn:
    public void setCompleted(boolean completed) {
        this.completed = completed;
    }
    
    // Bạn tạo thêm method trùng tên:
    public void setCompleted() {  ← GÂY NHẦM LẪN!
        this.completed = false;
    }
}

// TaskServiceImpl.java
task.setCompleted();  // Gọi method nào? Không rõ ràng!
```

**Vấn đề:**
- 2 methods cùng tên → overload → khó hiểu
- Developer khác đọc code sẽ bối rối

---

### ✅ Cách mới (Rõ ràng)

```java
// Task.java
@Data
public class Task {
    private String id;
    private String title;
    private String description;
    private boolean completed = false;  ← GIÁ TRỊ MẶC ĐỊNH
    
    // Lombok @Data tự tạo:
    // - getCompleted() / isCompleted()
    // - setCompleted(boolean completed)
}

// TaskServiceImpl.java
@Override
public Task createTask(Task task) {
    // Validate
    if (task.getTitle() == null || task.getTitle().trim().isEmpty()) {
        throw new IllegalArgumentException("Task title cannot be empty");
    }
    
    // Business rule: Task mới luôn chưa hoàn thành
    if (task.isCompleted()) {
        log.warn("Client gửi completed = true, tự động reset về false");
        task.setCompleted(false);  ← RÕ RÀNG: truyền false
    }
    
    return taskRepo.save(task);
}
```

---

## 📊 So sánh 2 cách

| Tiêu chí | Cách cũ (new Task()) | Cách hiện tại (nhận Task) |
|---------|---------------------|--------------------------|
| **Khởi tạo object** | Service tự tạo | Spring tự động tạo ở Controller |
| **Validate JSON** | Phải parse thủ công | Spring tự động validate |
| **Code duplication** | Controller + Service đều phải parse | Chỉ 1 nơi (Controller) |
| **Testability** | Khó test | Dễ test (mock object) |
| **Best practice** | ❌ Vi phạm Single Responsibility | ✅ Đúng nguyên tắc |

---

## 🎓 Bài tập thực hành

### Bài 1: Trace luồng data
Dùng Postman gửi request:
```json
POST http://localhost:8080/api/tasks
{
  "title": "Test Task",
  "completed": true
}
```

**Câu hỏi:**
1. Object `Task` được tạo ở đâu?
2. Giá trị `completed` cuối cùng là gì khi lưu vào DB? (true hay false?)
3. Vì sao?

**Đáp án:**
1. Object được tạo ở `MainController` bởi Spring (qua `@RequestBody`)
2. Giá trị là `false`
3. Vì `TaskServiceImpl.createTask()` kiểm tra nếu `completed = true` thì reset về `false`

---

### Bài 2: Thử nghiệm với DTO

Tạo DTO mới:
```java
public class TaskCreateDTO {
    private String title;
    private String description;
}
```

Sửa Controller:
```java
@PostMapping
public ResponseEntity<Task> addTask(@RequestBody TaskCreateDTO dto) {
    Task saved = taskService.createTask(dto);  // Lỗi: type không khớp
    return ResponseEntity.ok(saved);
}
```

**Câu hỏi:** Phải sửa Service như thế nào?

**Gợi ý:** Service cần tạo `Task` mới từ `TaskCreateDTO`.

---

## 📚 Tóm tắt

1. **Object được tạo ở Controller** (Spring tự động qua `@RequestBody`)
2. **Service nhận object có sẵn** → chỉ validate + xử lý logic + save
3. **Không cần `new Task()`** trừ khi:
   - Dùng DTO pattern
   - Tạo object phụ trợ
   - Clone/duplicate object
4. **Set giá trị mặc định** trong Model (`completed = false`) thay vì tạo method riêng

---

## 🔗 Xem thêm

- [Docs/LUONG_HOAT_DONG.md](./LUONG_HOAT_DONG.md) - Luồng hoạt động chi tiết
- [Docs/Cách sử dụng interface, implementation và DTO](./Cách%20sử%20dụng%20interface,%20implementation%20và%20DTO) - Pattern Best Practice
- [SERVICE_LAYER_DONE.md](../SERVICE_LAYER_DONE.md) - Tài liệu Service Layer

---

**Ngày tạo:** 2025-12-01  
**Tác giả:** GitHub Copilot  
**Project:** TodoList Spring Boot + MongoDB

