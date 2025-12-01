# Luồng Data từ JSON → Database (Chi tiết từng bước)

## 🎯 Mục tiêu
Giải thích **CHI TIẾT TỪNG BƯỚC** object `Task` được tạo như thế nào và đi qua các layer nào.

---

## 📋 Bước 1: Client gửi JSON

```bash
# Postman hoặc curl
POST http://localhost:8080/api/tasks
Content-Type: application/json

{
  "title": "Learn Spring Boot",
  "description": "Study annotations",
  "completed": false
}
```

**Điều gì xảy ra:**
- HTTP request được gửi đến server
- Request body chứa chuỗi JSON (kiểu String)

---

## 📋 Bước 2: Spring Boot nhận request

```
┌────────────────────────────────────────────────┐
│     Spring Boot DispatcherServlet              │
│                                                 │
│  - Nhận HTTP request                           │
│  - Tìm Controller xử lý: MainController        │
│  - Tìm method xử lý: addTask()                 │
│  - Đọc annotation @RequestBody                 │
└────────────────────────────────────────────────┘
                    │
                    ↓
        "Ồ, cần chuyển JSON → Object Task!"
```

---

## 📋 Bước 3: Jackson parse JSON → Object

```
┌────────────────────────────────────────────────┐
│         Jackson ObjectMapper                    │
│                                                 │
│  1. Đọc JSON string:                           │
│     {                                           │
│       "title": "Learn Spring Boot",            │
│       "description": "Study annotations",      │
│       "completed": false                       │
│     }                                           │
│                                                 │
│  2. Tạo object mới:                            │
│     Task task = new Task();                    │
│                                                 │
│  3. Gọi setters (Lombok đã tạo):               │
│     task.setTitle("Learn Spring Boot");        │
│     task.setDescription("Study annotations");  │
│     task.setCompleted(false);                  │
│                                                 │
│  4. Trả object hoàn chỉnh                      │
└────────────────────────────────────────────────┘
                    │
                    ↓
            Object Task đã sẵn sàng!
```

**Lưu ý quan trọng:**
- Vì có `@NoArgsConstructor` → Jackson gọi `new Task()`
- Vì có `@Data` → Lombok tạo các setter → Jackson gọi `setTitle()`, `setDescription()`, `setCompleted()`
- Field `id` không có trong JSON → giữ giá trị `null`

---

## 📋 Bước 4: Spring inject object vào Controller

```java
// MainController.java
@RestController
@RequestMapping("/api/tasks")
public class MainController {
    
    @PostMapping
    public ResponseEntity<Task> addTask(
        @RequestBody Task task  ← OBJECT ĐÃ TỒN TẠI Ở ĐÂY!
    ) {
        // Lúc này:
        // task.getTitle() = "Learn Spring Boot"
        // task.getDescription() = "Study annotations"
        // task.isCompleted() = false
        // task.getId() = null (chưa lưu DB)
        
        Task savedTask = taskService.createTask(task);
        return ResponseEntity.ok(savedTask);
    }
}
```

**Câu hỏi thường gặp:**
- **Q:** Tại sao không cần `new Task()` trong Controller?
- **A:** Spring + Jackson đã làm việc đó rồi (bước 3)!

---

## 📋 Bước 5: Controller gọi Service

```
┌────────────────────────────────────────────────┐
│          MainController                         │
└────────────────────┬───────────────────────────┘
                     │
                     │ taskService.createTask(task)
                     │
                     │ Truyền object:
                     │ {
                     │   id: null,
                     │   title: "Learn Spring Boot",
                     │   description: "Study annotations",
                     │   completed: false
                     │ }
                     ↓
┌────────────────────────────────────────────────┐
│         TaskServiceImpl                         │
│                                                 │
│  public Task createTask(Task task) {           │
│      // task ĐÃ TỒN TẠI, có đầy đủ data       │
│      // KHÔNG CẦN new Task() nữa!             │
│  }                                              │
└────────────────────────────────────────────────┘
```

---

## 📋 Bước 6: Service validate & xử lý logic

```java
@Override
public Task createTask(Task task) {
    log.info("Creating task: {}", task.getTitle());
    
    // ✅ VALIDATION
    if (task.getTitle() == null || task.getTitle().trim().isEmpty()) {
        throw new IllegalArgumentException("Task title cannot be empty");
    }
    
    // ✅ BUSINESS LOGIC
    // Rule: Task mới luôn chưa hoàn thành
    if (task.isCompleted()) {
        log.warn("Client gửi completed=true, reset về false");
        task.setCompleted(false);  ← SỬA GIÁ TRỊ (không tạo object mới)
    }
    
    // ✅ SAVE
    Task savedTask = taskRepo.save(task);
    
    return savedTask;
}
```

**Điều gì xảy ra:**
1. Kiểm tra title có rỗng không
2. Nếu client cố tình gửi `completed = true`, reset về `false`
3. Gọi Repository để lưu vào MongoDB

---

## 📋 Bước 7: Repository lưu vào MongoDB

```
┌────────────────────────────────────────────────┐
│          TaskRepo (MongoRepository)             │
│                                                 │
│  save(task) được gọi                           │
│                                                 │
│  Spring Data MongoDB tự động:                  │
│  1. Kiểm tra task.getId() → null               │
│  2. Gọi MongoDB insert (không phải update)     │
│  3. MongoDB tự sinh _id mới                    │
│  4. Trả về object có _id                       │
└────────────────────────────────────────────────┘
                    │
                    ↓
┌────────────────────────────────────────────────┐
│          MongoDB Database                       │
│                                                 │
│  Collection: tasks                             │
│  {                                              │
│    "_id": ObjectId("674b8a1f..."),  ← TỰ SINH │
│    "title": "Learn Spring Boot",               │
│    "description": "Study annotations",         │
│    "completed": false                          │
│  }                                              │
└────────────────────────────────────────────────┘
```

**Lưu ý:**
- MongoDB tự động generate `_id` nếu không có
- Spring Data map `_id` → `task.id`

---

## 📋 Bước 8: Trả response về Client

```
┌────────────────────────────────────────────────┐
│          MongoDB                                │
└────────────────────┬───────────────────────────┘
                     │
                     │ Trả object có _id
                     ↓
┌────────────────────────────────────────────────┐
│          TaskServiceImpl                        │
│                                                 │
│  Task savedTask = taskRepo.save(task);         │
│  return savedTask;                             │
│  // savedTask.getId() = "674b8a1f..."          │
└────────────────────┬───────────────────────────┘
                     │
                     │ Trả về Controller
                     ↓
┌────────────────────────────────────────────────┐
│          MainController                         │
│                                                 │
│  return ResponseEntity.ok(savedTask);          │
│  // HTTP 200 + JSON body                       │
└────────────────────┬───────────────────────────┘
                     │
                     │ Spring chuyển Object → JSON
                     ↓
┌────────────────────────────────────────────────┐
│          Jackson ObjectMapper                   │
│                                                 │
│  savedTask.getId()          → "_id"            │
│  savedTask.getTitle()       → "title"          │
│  savedTask.getDescription() → "description"    │
│  savedTask.isCompleted()    → "completed"      │
│                                                 │
│  Tạo JSON:                                     │
│  {                                              │
│    "id": "674b8a1f...",                        │
│    "title": "Learn Spring Boot",               │
│    "description": "Study annotations",         │
│    "completed": false                          │
│  }                                              │
└────────────────────┬───────────────────────────┘
                     │
                     │ HTTP Response
                     ↓
┌────────────────────────────────────────────────┐
│          Client (Postman)                       │
│                                                 │
│  HTTP/1.1 200 OK                               │
│  Content-Type: application/json                │
│                                                 │
│  {                                              │
│    "id": "674b8a1f...",                        │
│    "title": "Learn Spring Boot",               │
│    "description": "Study annotations",         │
│    "completed": false                          │
│  }                                              │
└────────────────────────────────────────────────┘
```

---

## 🎯 Tóm tắt toàn bộ luồng

| Bước | Nơi xử lý | Object Task tồn tại? | Ai tạo object? |
|------|----------|---------------------|----------------|
| 1 | Client | ❌ (chỉ có JSON string) | - |
| 2 | DispatcherServlet | ❌ | - |
| 3 | Jackson | ✅ **TẠO Ở ĐÂY!** | **Jackson ObjectMapper** |
| 4 | Controller | ✅ (nhận từ Jackson) | - |
| 5 | Service | ✅ (nhận từ Controller) | - |
| 6 | Repository | ✅ | - |
| 7 | MongoDB | ✅ (lưu vào DB) | - |
| 8 | Response | ✅ (chuyển về JSON) | - |

---

## 💡 Giải đáp thắc mắc

### Q1: Tại sao Service không tạo `new Task()`?
**A:** Vì Jackson đã tạo ở bước 3 rồi! Service chỉ nhận object có sẵn để validate + xử lý logic.

### Q2: Nếu muốn Service tự tạo object thì sao?
**A:** Dùng DTO pattern:
```java
// Controller nhận DTO
@PostMapping
public ResponseEntity<Task> addTask(@RequestBody TaskCreateDTO dto) {
    Task saved = taskService.createTask(dto);  // Service nhận DTO
    return ResponseEntity.ok(saved);
}

// Service tự tạo Entity
public Task createTask(TaskCreateDTO dto) {
    Task task = new Task();  ← TẠO MỚI
    task.setTitle(dto.getTitle());
    task.setDescription(dto.getDescription());
    task.setCompleted(false);
    return taskRepo.save(task);
}
```

### Q3: `@NoArgsConstructor` và `@AllArgsConstructor` để làm gì?
**A:**
- `@NoArgsConstructor` → Jackson cần constructor không tham số để tạo object
- `@AllArgsConstructor` → Tiện cho testing: `new Task(null, "title", "desc", false)`

### Q4: Nếu xóa `@Data` thì sao?
**A:** Jackson không có setter → không map được JSON → lỗi!

### Q5: Field `id` null có sao không?
**A:** Không sao! MongoDB sẽ tự sinh `_id` khi insert.

---

## 🧪 Thực hành

### Thử nghiệm 1: Log object ở mỗi layer

**Controller:**
```java
@PostMapping
public ResponseEntity<Task> addTask(@RequestBody Task task) {
    System.out.println("Controller nhận: " + task);
    Task saved = taskService.createTask(task);
    System.out.println("Controller trả về: " + saved);
    return ResponseEntity.ok(saved);
}
```

**Service:**
```java
public Task createTask(Task task) {
    System.out.println("Service nhận: " + task);
    Task saved = taskRepo.save(task);
    System.out.println("Service trả về: " + saved);
    return saved;
}
```

**Kết quả:**
```
Controller nhận: Task(id=null, title=Learn Spring Boot, description=Study annotations, completed=false)
Service nhận: Task(id=null, title=Learn Spring Boot, description=Study annotations, completed=false)
Service trả về: Task(id=674b8a1f..., title=Learn Spring Boot, description=Study annotations, completed=false)
Controller trả về: Task(id=674b8a1f..., title=Learn Spring Boot, description=Study annotations, completed=false)
```

**Nhận xét:**
- Object đã tồn tại từ Controller
- Sau khi save, `id` được MongoDB sinh ra

---

### Thử nghiệm 2: Client gửi id

**Request:**
```json
{
  "id": "fake-id-123",
  "title": "Test",
  "completed": true
}
```

**Kết quả:**
- MongoDB **KHÔNG SỬ DỤNG** `id` từ client
- MongoDB tự sinh `_id` mới
- Field `completed` bị reset về `false` (business rule trong Service)

---

## 📚 Kết luận

1. **Object được tạo ở Controller layer** (Jackson tự động)
2. **Service không cần `new Task()`** vì object đã có sẵn
3. **Service chỉ validate + business logic + save**
4. **MongoDB tự sinh `_id`** nếu không có

**Best Practice:**
- Dùng DTO để tách biệt API và Database structure
- Validate ở Service layer (không tin tưởng client)
- Set giá trị mặc định trong Model (`completed = false`)

---

**Ngày tạo:** 2025-12-01  
**Liên quan:** [VI_SAO_KHONG_KHOI_TAO_TASK_TRONG_SERVICE.md](./VI_SAO_KHONG_KHOI_TAO_TASK_TRONG_SERVICE.md)

