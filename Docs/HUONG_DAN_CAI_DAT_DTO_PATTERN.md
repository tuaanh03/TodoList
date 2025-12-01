# Hướng dẫn cài đặt DTO Pattern vào Project TodoList

## 📌 Mục tiêu
Hướng dẫn chi tiết cách triển khai **DTO (Data Transfer Object) Pattern** vào project TodoList hiện tại, bao gồm:
- Cấu trúc thư mục
- Các loại DTO cần tạo
- Có nên dùng Abstract Class không?
- Có nên dùng MapStruct không?
- Cách map giữa Entity và DTO

---

## 🎯 Tại sao cần DTO?

### ❌ Vấn đề hiện tại (không dùng DTO)

```java
// Controller hiện tại
@PostMapping
public ResponseEntity<Task> createTask(@RequestBody Task task) {
    // Client có thể gửi:
    // {
    //   "id": "fake-id-123",        ← HACK: Client tự set ID!
    //   "title": "Hacked",
    //   "completed": true            ← HACK: Task mới đã hoàn thành!
    // }
    Task created = taskService.createTask(task);
    return ResponseEntity.status(HttpStatus.CREATED).body(created);
}
```

**Vấn đề:**
1. ❌ Client có thể gửi `id` để hack hệ thống
2. ❌ Client có thể set `completed = true` cho task mới
3. ❌ Expose cấu trúc database ra ngoài (nếu thêm field `createdAt`, `updatedAt`, client sẽ thấy)
4. ❌ Khó thay đổi database structure mà không ảnh hưởng API

---

### ✅ Giải pháp: Dùng DTO

```
┌─────────────────────────────────────────────────────────┐
│                    CLIENT                                │
│  Gửi JSON đơn giản, chỉ có data cần thiết              │
│  { "title": "...", "description": "..." }               │
└────────────────────┬────────────────────────────────────┘
                     │
                     │ JSON
                     ↓
┌─────────────────────────────────────────────────────────┐
│                 CONTROLLER                               │
│  @PostMapping                                            │
│  createTask(@RequestBody TaskCreateDTO dto) ← DTO       │
└────────────────────┬────────────────────────────────────┘
                     │
                     │ DTO object
                     ↓
┌─────────────────────────────────────────────────────────┐
│                  SERVICE                                 │
│  createTask(TaskCreateDTO dto) {                        │
│      Task task = new Task();  ← Convert DTO → Entity   │
│      task.setTitle(dto.getTitle());                     │
│      task.setCompleted(false);  ← Set giá trị mặc định │
│      return taskRepo.save(task);                        │
│  }                                                       │
└────────────────────┬────────────────────────────────────┘
                     │
                     │ Entity
                     ↓
┌─────────────────────────────────────────────────────────┐
│                 REPOSITORY                               │
│  save(Task entity) → MongoDB                            │
└────────────────────┬────────────────────────────────────┘
                     │
                     │ Entity (có ID từ MongoDB)
                     ↓
┌─────────────────────────────────────────────────────────┐
│                  SERVICE                                 │
│  TaskResponseDTO dto = mapToDTO(task); ← Entity → DTO  │
└────────────────────┬────────────────────────────────────┘
                     │
                     │ DTO
                     ↓
┌─────────────────────────────────────────────────────────┐
│                 CONTROLLER                               │
│  return ResponseEntity.ok(dto);                         │
└────────────────────┬────────────────────────────────────┘
                     │
                     │ JSON
                     ↓
┌─────────────────────────────────────────────────────────┐
│                    CLIENT                                │
│  Nhận JSON đã được "clean":                             │
│  { "id": "...", "title": "...", "completed": false }    │
└─────────────────────────────────────────────────────────┘
```

---

## 📁 Cấu trúc thư mục đề xuất

```
src/main/java/com/example/todolist/
├── TodoModel/                    # Entity (Database models)
│   └── Task.java                 # Entity cho MongoDB
│
├── TodoDTO/                      # ★ THÊM MỚI: Data Transfer Objects
│   ├── request/                  # DTOs cho REQUEST (Client → Server)
│   │   ├── TaskCreateDTO.java   # Tạo task mới
│   │   └── TaskUpdateDTO.java   # Cập nhật task
│   │
│   ├── response/                 # DTOs cho RESPONSE (Server → Client)
│   │   ├── TaskResponseDTO.java # Response đầy đủ
│   │   └── TaskSummaryDTO.java  # Response tóm tắt (cho list)
│   │
│   └── mapper/                   # ★ Mapper: Convert Entity ↔ DTO
│       └── TaskMapper.java       # Manual mapping hoặc MapStruct
│
├── TodoController/
│   └── MainController.java       # Sửa để dùng DTO
│
├── TodoService/
│   ├── TaskService.java          # Sửa interface để dùng DTO
│   └── TaskServiceImpl.java      # Sửa implementation
│
├── TodoRepository/
│   └── TaskRepo.java             # Không đổi (vẫn dùng Entity)
│
└── TodolistApplication.java
```

---

## 🔨 Các bước triển khai

### Bước 1: Tạo package TodoDTO

```bash
# Trong IntelliJ hoặc terminal:
mkdir -p src/main/java/com/example/todolist/TodoDTO/request
mkdir -p src/main/java/com/example/todolist/TodoDTO/response
mkdir -p src/main/java/com/example/todolist/TodoDTO/mapper
```

---

### Bước 2: Tạo Request DTOs

#### 📄 TaskCreateDTO.java
**Mục đích:** Client gửi khi tạo task mới

**Nên có:**
- `title` (bắt buộc)
- `description` (optional)

**KHÔNG có:**
- ❌ `id` (MongoDB tự sinh)
- ❌ `completed` (mặc định = false)
- ❌ `createdAt`, `updatedAt` (nếu có thì tự động set)

**Code mẫu:**
```java
package com.example.todolist.TodoDTO.request;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TaskCreateDTO {
    
    // Validation annotations (optional, nhưng nên có)
    // @NotBlank(message = "Title is required")
    // @Size(min = 3, max = 100, message = "Title must be between 3 and 100 characters")
    private String title;
    
    // @Size(max = 500, message = "Description must not exceed 500 characters")
    private String description;
    
    // KHÔNG CÓ: id, completed, createdAt, updatedAt
}
```

**Lưu ý:**
- Nếu muốn validation, thêm dependency `spring-boot-starter-validation` vào `pom.xml`
- Annotations: `@NotBlank`, `@Size`, `@Email`, `@Pattern`, etc.

---

#### 📄 TaskUpdateDTO.java
**Mục đích:** Client gửi khi cập nhật task

**Nên có:**
- `title` (có thể thay đổi)
- `description` (có thể thay đổi)
- `completed` (có thể toggle)

**KHÔNG có:**
- ❌ `id` (lấy từ path variable)
- ❌ `createdAt` (không được sửa)

**Code mẫu:**
```java
package com.example.todolist.TodoDTO.request;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TaskUpdateDTO {
    
    // Client có thể sửa title
    private String title;
    
    // Client có thể sửa description
    private String description;
    
    // Client có thể toggle completed
    private Boolean completed;  // Dùng Boolean (nullable) thay vì boolean
    
    // KHÔNG CÓ: id, createdAt, updatedAt
}
```

**Lưu ý:**
- Dùng `Boolean` (object) thay vì `boolean` (primitive) để phân biệt:
  - `null` = không cập nhật field này
  - `true`/`false` = cập nhật giá trị

---

### Bước 3: Tạo Response DTOs

#### 📄 TaskResponseDTO.java
**Mục đích:** Server trả về client (đầy đủ thông tin)

**Nên có:**
- `id` (để client biết ID)
- `title`
- `description`
- `completed`
- `createdAt`, `updatedAt` (nếu có trong Entity)

**Code mẫu:**
```java
package com.example.todolist.TodoDTO.response;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder  // Pattern Builder để dễ tạo object
public class TaskResponseDTO {
    
    private String id;
    
    private String title;
    
    private String description;
    
    private boolean completed;
    
    // Nếu Entity có thêm fields, thêm vào đây:
    // private LocalDateTime createdAt;
    // private LocalDateTime updatedAt;
}
```

---

#### 📄 TaskSummaryDTO.java (Optional)
**Mục đích:** Response tóm tắt cho danh sách (tiết kiệm bandwidth)

**Nên có:**
- `id`
- `title`
- `completed`

**KHÔNG có:**
- ❌ `description` (dài, không cần trong list)

**Code mẫu:**
```java
package com.example.todolist.TodoDTO.response;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TaskSummaryDTO {
    
    private String id;
    
    private String title;
    
    private boolean completed;
    
    // Không có description (cho list ngắn gọn hơn)
}
```

**Khi nào dùng:**
- `GET /api/tasks` → List<TaskSummaryDTO> (tóm tắt)
- `GET /api/tasks/{id}` → TaskResponseDTO (chi tiết)

---

### Bước 4: Tạo Mapper (Entity ↔ DTO)

#### 📄 TaskMapper.java (Manual mapping)

**Code mẫu:**
```java
package com.example.todolist.TodoDTO.mapper;

import com.example.todolist.TodoModel.Task;
import com.example.todolist.TodoDTO.request.TaskCreateDTO;
import com.example.todolist.TodoDTO.request.TaskUpdateDTO;
import com.example.todolist.TodoDTO.response.TaskResponseDTO;
import com.example.todolist.TodoDTO.response.TaskSummaryDTO;

import java.util.List;
import java.util.stream.Collectors;

/**
 * TaskMapper - Chuyển đổi giữa Entity và DTO
 * 
 * MANUAL MAPPING (không dùng MapStruct)
 * - Đơn giản, dễ hiểu
 * - Không cần thêm dependency
 * - Phù hợp với project nhỏ
 */
public class TaskMapper {
    
    // ═══════════════════════════════════════════════════════════
    // DTO → ENTITY (Create)
    // ═══════════════════════════════════════════════════════════
    
    /**
     * Convert TaskCreateDTO → Task Entity
     * Dùng khi tạo task mới
     */
    public static Task toEntity(TaskCreateDTO dto) {
        Task task = new Task();
        task.setTitle(dto.getTitle());
        task.setDescription(dto.getDescription());
        task.setCompleted(false);  // Mặc định false
        // ID sẽ do MongoDB tự sinh
        return task;
    }
    
    // ═══════════════════════════════════════════════════════════
    // DTO → ENTITY (Update)
    // ═══════════════════════════════════════════════════════════
    
    /**
     * Update Task Entity từ TaskUpdateDTO
     * CHỈ update các fields NOT NULL
     */
    public static void updateEntity(Task task, TaskUpdateDTO dto) {
        if (dto.getTitle() != null) {
            task.setTitle(dto.getTitle());
        }
        if (dto.getDescription() != null) {
            task.setDescription(dto.getDescription());
        }
        if (dto.getCompleted() != null) {
            task.setCompleted(dto.getCompleted());
        }
    }
    
    // ═══════════════════════════════════════════════════════════
    // ENTITY → DTO (Response)
    // ═══════════════════════════════════════════════════════════
    
    /**
     * Convert Task Entity → TaskResponseDTO
     * Dùng khi trả về chi tiết 1 task
     */
    public static TaskResponseDTO toResponseDTO(Task task) {
        return TaskResponseDTO.builder()
                .id(task.getId())
                .title(task.getTitle())
                .description(task.getDescription())
                .completed(task.isCompleted())
                // .createdAt(task.getCreatedAt())
                // .updatedAt(task.getUpdatedAt())
                .build();
    }
    
    /**
     * Convert Task Entity → TaskSummaryDTO
     * Dùng cho list (tóm tắt)
     */
    public static TaskSummaryDTO toSummaryDTO(Task task) {
        return TaskSummaryDTO.builder()
                .id(task.getId())
                .title(task.getTitle())
                .completed(task.isCompleted())
                .build();
    }
    
    /**
     * Convert List<Task> → List<TaskSummaryDTO>
     */
    public static List<TaskSummaryDTO> toSummaryDTOList(List<Task> tasks) {
        return tasks.stream()
                .map(TaskMapper::toSummaryDTO)
                .collect(Collectors.toList());
    }
    
    /**
     * Convert List<Task> → List<TaskResponseDTO>
     */
    public static List<TaskResponseDTO> toResponseDTOList(List<Task> tasks) {
        return tasks.stream()
                .map(TaskMapper::toResponseDTO)
                .collect(Collectors.toList());
    }
}
```

**Ưu điểm manual mapping:**
- ✅ Không cần dependency thêm
- ✅ Đơn giản, dễ debug
- ✅ Tùy chỉnh logic linh hoạt
- ✅ Phù hợp project nhỏ/vừa

**Nhược điểm:**
- ❌ Phải viết code nhiều (boilerplate)
- ❌ Nếu Entity có 20 fields → rất mất công
- ❌ Dễ quên update khi thêm field mới

---

### Bước 5: Sửa TaskService Interface

**File:** `TodoService/TaskService.java`

**Thay đổi:**
```java
package com.example.todolist.TodoService;

import com.example.todolist.TodoDTO.request.TaskCreateDTO;
import com.example.todolist.TodoDTO.request.TaskUpdateDTO;
import com.example.todolist.TodoDTO.response.TaskResponseDTO;
import com.example.todolist.TodoDTO.response.TaskSummaryDTO;

import java.util.List;
import java.util.Optional;

public interface TaskService {

    /**
     * Tạo task mới
     * @param dto TaskCreateDTO (chỉ có title, description)
     * @return TaskResponseDTO (có id)
     */
    TaskResponseDTO createTask(TaskCreateDTO dto);

    /**
     * Lấy task theo ID
     * @param id ID của task
     * @return Optional chứa TaskResponseDTO
     */
    Optional<TaskResponseDTO> getTaskById(String id);

    /**
     * Lấy tất cả tasks (tóm tắt)
     * @return List<TaskSummaryDTO>
     */
    List<TaskSummaryDTO> getAllTasks();
    
    /**
     * Cập nhật task
     * @param id ID của task
     * @param dto TaskUpdateDTO
     * @return Optional chứa TaskResponseDTO
     */
    Optional<TaskResponseDTO> updateTask(String id, TaskUpdateDTO dto);
    
    /**
     * Xóa task
     * @param id ID của task
     * @return true nếu xóa thành công
     */
    boolean deleteTask(String id);
    
    // ... các methods khác
}
```

**Lưu ý:**
- ❌ KHÔNG dùng Entity (`Task`) trong signature
- ✅ CHỈ dùng DTO (`TaskCreateDTO`, `TaskResponseDTO`, ...)
- Repository vẫn dùng Entity

---

### Bước 6: Sửa TaskServiceImpl

**File:** `TodoService/TaskServiceImpl.java`

**Thay đổi:**
```java
package com.example.todolist.TodoService;

import com.example.todolist.TodoModel.Task;
import com.example.todolist.TodoRepository.TaskRepo;
import com.example.todolist.TodoDTO.request.TaskCreateDTO;
import com.example.todolist.TodoDTO.request.TaskUpdateDTO;
import com.example.todolist.TodoDTO.response.TaskResponseDTO;
import com.example.todolist.TodoDTO.response.TaskSummaryDTO;
import com.example.todolist.TodoDTO.mapper.TaskMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class TaskServiceImpl implements TaskService {
    
    private final TaskRepo taskRepo;
    
    @Override
    public TaskResponseDTO createTask(TaskCreateDTO dto) {
        log.info("Creating task with title: {}", dto.getTitle());
        
        // 1. Validate
        if (dto.getTitle() == null || dto.getTitle().trim().isEmpty()) {
            throw new IllegalArgumentException("Task title cannot be empty");
        }
        
        // 2. DTO → Entity (dùng Mapper)
        Task task = TaskMapper.toEntity(dto);
        
        // 3. Save Entity
        Task savedTask = taskRepo.save(task);
        
        // 4. Entity → DTO (trả về cho Controller)
        log.info("Task created with ID: {}", savedTask.getId());
        return TaskMapper.toResponseDTO(savedTask);
    }
    
    @Override
    public Optional<TaskResponseDTO> getTaskById(String id) {
        return taskRepo.findById(id)
                .map(TaskMapper::toResponseDTO);  // Entity → DTO
    }
    
    @Override
    public List<TaskSummaryDTO> getAllTasks() {
        List<Task> tasks = taskRepo.findAll();
        return TaskMapper.toSummaryDTOList(tasks);  // List<Entity> → List<DTO>
    }
    
    @Override
    public Optional<TaskResponseDTO> updateTask(String id, TaskUpdateDTO dto) {
        return taskRepo.findById(id)
                .map(task -> {
                    // Update Entity từ DTO
                    TaskMapper.updateEntity(task, dto);
                    
                    // Save
                    Task updated = taskRepo.save(task);
                    
                    // Entity → DTO
                    return TaskMapper.toResponseDTO(updated);
                });
    }
    
    @Override
    public boolean deleteTask(String id) {
        if (taskRepo.existsById(id)) {
            taskRepo.deleteById(id);
            return true;
        }
        return false;
    }
    
    // ... các methods khác
}
```

**Điểm quan trọng:**
1. Service layer làm việc với **cả Entity và DTO**
2. Nhận DTO từ Controller → Convert sang Entity → Save
3. Lấy Entity từ Repository → Convert sang DTO → Trả về Controller
4. **Repository chỉ biết Entity, không biết DTO**

---

### Bước 7: Sửa MainController

**File:** `TodoController/MainController.java`

**Thay đổi:**
```java
package com.example.todolist.TodoController;

import com.example.todolist.TodoDTO.request.TaskCreateDTO;
import com.example.todolist.TodoDTO.request.TaskUpdateDTO;
import com.example.todolist.TodoDTO.response.TaskResponseDTO;
import com.example.todolist.TodoDTO.response.TaskSummaryDTO;
import com.example.todolist.TodoService.TaskService;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tasks")
@RequiredArgsConstructor
public class MainController {
    
    private final TaskService taskService;
    
    // ═══════════════════════════════════════════════════════════
    // CREATE
    // ═══════════════════════════════════════════════════════════
    @PostMapping
    public ResponseEntity<TaskResponseDTO> createTask(
            @RequestBody TaskCreateDTO dto  // ← Nhận DTO, không phải Entity
    ) {
        TaskResponseDTO created = taskService.createTask(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }
    
    // ═══════════════════════════════════════════════════════════
    // READ - Get all (summary)
    // ═══════════════════════════════════════════════════════════
    @GetMapping
    public ResponseEntity<List<TaskSummaryDTO>> getAllTasks() {
        List<TaskSummaryDTO> tasks = taskService.getAllTasks();
        return ResponseEntity.ok(tasks);
    }
    
    // ═══════════════════════════════════════════════════════════
    // READ - Get by ID (detail)
    // ═══════════════════════════════════════════════════════════
    @GetMapping("/{id}")
    public ResponseEntity<TaskResponseDTO> getTaskById(@PathVariable String id) {
        return taskService.getTaskById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
    
    // ═══════════════════════════════════════════════════════════
    // UPDATE
    // ═══════════════════════════════════════════════════════════
    @PutMapping("/{id}")
    public ResponseEntity<TaskResponseDTO> updateTask(
            @PathVariable String id,
            @RequestBody TaskUpdateDTO dto  // ← Nhận DTO
    ) {
        return taskService.updateTask(id, dto)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
    
    // ═══════════════════════════════════════════════════════════
    // DELETE
    // ═══════════════════════════════════════════════════════════
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTask(@PathVariable String id) {
        boolean deleted = taskService.deleteTask(id);
        return deleted 
                ? ResponseEntity.noContent().build()
                : ResponseEntity.notFound().build();
    }
}
```

**Điểm quan trọng:**
- Controller **KHÔNG BAO GIỜ** biết về Entity (`Task`)
- Controller chỉ làm việc với DTO
- Layer separation rõ ràng

---

## 🤔 Có nên dùng Abstract Class không?

### Trường hợp 1: Abstract DTO Class

**Câu hỏi:** Có nên tạo `AbstractTaskDTO` chứa các fields chung?

```java
// AbstractTaskDTO.java
public abstract class AbstractTaskDTO {
    private String title;
    private String description;
    // Getter/Setter
}

// TaskCreateDTO extends AbstractTaskDTO
// TaskUpdateDTO extends AbstractTaskDTO
```

### ✅ Ưu điểm:
- Tránh duplicate code (title, description xuất hiện nhiều nơi)
- Dễ maintain (sửa 1 chỗ → ảnh hưởng tất cả)

### ❌ Nhược điểm:
- Phức tạp hóa code (thêm 1 layer inheritance)
- Khó hiểu với developer mới
- Nếu các DTO khác nhau nhiều → abstract class không giúp được gì

### 💡 Kết luận:
**KHÔNG NÊN** dùng Abstract DTO Class trong project này vì:
1. Chỉ có 2-3 DTOs, không có quá nhiều duplicate
2. Lombok `@Data` đã giảm boilerplate rồi
3. Inheritance làm phức tạp không cần thiết

**Nên dùng khi nào:**
- Có 10+ DTOs với nhiều fields chung
- Cần share business logic (không chỉ fields)
- Có hierarchy phức tạp (ví dụ: User → AdminUser, CustomerUser)

---

### Trường hợp 2: Abstract Mapper Class

**Câu hỏi:** Có nên tạo `AbstractMapper<E, D>` cho tất cả Mappers?

```java
public abstract class AbstractMapper<E, D> {
    public abstract D toDTO(E entity);
    public abstract E toEntity(D dto);
    
    public List<D> toDTOList(List<E> entities) {
        return entities.stream()
            .map(this::toDTO)
            .collect(Collectors.toList());
    }
}

// TaskMapper extends AbstractMapper<Task, TaskResponseDTO>
```

### ✅ Ưu điểm:
- Tái sử dụng logic chung (như `toDTOList`)
- Đồng nhất cách làm việc với mappers

### ❌ Nhược điểm:
- Over-engineering cho project nhỏ
- Mỗi Entity có nhiều DTOs (Create, Update, Response) → khó generic

### 💡 Kết luận:
**KHÔNG CẦN** Abstract Mapper trong project này vì:
1. Chỉ có 1 Entity (Task) → không có pattern lặp lại
2. Static methods đơn giản hơn
3. Dễ hiểu, dễ maintain

**Nên dùng khi nào:**
- Có 5+ Entities (Task, User, Project, Comment, ...)
- Tất cả đều có pattern giống nhau
- Team lớn, cần standardize

---

## 🤔 Có nên dùng MapStruct không?

### MapStruct là gì?

**MapStruct** là thư viện **code generation** tự động tạo implementation cho interface Mapper.

**Ví dụ:**
```java
// Bạn viết interface
@Mapper(componentModel = "spring")
public interface TaskMapper {
    TaskResponseDTO toResponseDTO(Task task);
    Task toEntity(TaskCreateDTO dto);
}

// MapStruct tự động generate implementation (compile time)
```

---

### ✅ Ưu điểm MapStruct:

1. **Giảm boilerplate code**
   - Không cần viết `task.setTitle(dto.getTitle())` từng dòng
   - Tự động map fields có tên giống nhau

2. **Nhanh hơn reflection**
   - Generate code ở compile time → nhanh như viết tay
   - Không dùng reflection (như ModelMapper, Dozer)

3. **Type-safe**
   - Lỗi phát hiện ở compile time
   - Nếu thêm field mới mà quên map → compile error

4. **Tích hợp tốt với Spring**
   - `@Mapper(componentModel = "spring")` → auto bean

---

### ❌ Nhược điểm MapStruct:

1. **Phức tạp setup**
   - Phải config `pom.xml` thêm annotation processor
   - Phải rebuild project để generate code

2. **Khó debug**
   - Generated code ở `target/generated-sources`
   - Nếu có lỗi → khó tìm nguyên nhân

3. **Learning curve**
   - Phải học annotations: `@Mapping`, `@Mappings`, `@AfterMapping`, ...
   - Custom logic phức tạp → phải viết thêm methods

4. **Overkill cho project nhỏ**
   - Chỉ có 1-2 Entities → không cần thiết

---

### 💡 Kết luận: Có nên dùng MapStruct không?

| Tiêu chí | Manual Mapping | MapStruct |
|---------|---------------|-----------|
| **Số lượng Entities** | 1-3 Entities | 5+ Entities |
| **Độ phức tạp** | Đơn giản | Nhiều fields, nhiều DTOs |
| **Team size** | 1-2 người | 3+ người |
| **Maintenance** | Phải update thủ công | Tự động update |
| **Learning curve** | Dễ | Trung bình |
| **Performance** | Tốt | Rất tốt (nhưng không đáng kể với app nhỏ) |

---

### 🎯 Khuyến nghị cho project TodoList:

#### ✅ Dùng MANUAL MAPPING nếu:
- Chỉ có 1-3 Entities (như hiện tại: chỉ có Task)
- Team nhỏ (1-2 người)
- Muốn code đơn giản, dễ hiểu
- Không muốn setup phức tạp

#### ✅ Dùng MAPSTRUCT nếu:
- Có 5+ Entities
- Mỗi Entity có 10+ fields
- Team lớn, cần standardize
- Dự án lâu dài, cần maintain

---

### 📦 Cách thêm MapStruct (nếu muốn thử)

#### Bước 1: Thêm dependency vào `pom.xml`

```xml
<properties>
    <java.version>17</java.version>
    <mapstruct.version>1.5.5.Final</mapstruct.version>
</properties>

<dependencies>
    <!-- MapStruct -->
    <dependency>
        <groupId>org.mapstruct</groupId>
        <artifactId>mapstruct</artifactId>
        <version>${mapstruct.version}</version>
    </dependency>
</dependencies>

<build>
    <plugins>
        <plugin>
            <groupId>org.apache.maven.plugins</groupId>
            <artifactId>maven-compiler-plugin</artifactId>
            <configuration>
                <annotationProcessorPaths>
                    <!-- Lombok -->
                    <path>
                        <groupId>org.projectlombok</groupId>
                        <artifactId>lombok</artifactId>
                    </path>
                    <!-- MapStruct -->
                    <path>
                        <groupId>org.mapstruct</groupId>
                        <artifactId>mapstruct-processor</artifactId>
                        <version>${mapstruct.version}</version>
                    </path>
                    <!-- Lombok + MapStruct binding -->
                    <path>
                        <groupId>org.projectlombok</groupId>
                        <artifactId>lombok-mapstruct-binding</artifactId>
                        <version>0.2.0</version>
                    </path>
                </annotationProcessorPaths>
            </configuration>
        </plugin>
    </plugins>
</build>
```

---

#### Bước 2: Tạo Mapper interface

```java
package com.example.todolist.TodoDTO.mapper;

import com.example.todolist.TodoModel.Task;
import com.example.todolist.TodoDTO.request.TaskCreateDTO;
import com.example.todolist.TodoDTO.request.TaskUpdateDTO;
import com.example.todolist.TodoDTO.response.TaskResponseDTO;
import com.example.todolist.TodoDTO.response.TaskSummaryDTO;

import org.mapstruct.*;

import java.util.List;

@Mapper(componentModel = "spring")  // Tự động tạo Spring Bean
public interface TaskMapper {
    
    // DTO → Entity
    @Mapping(target = "id", ignore = true)  // Không map id
    @Mapping(target = "completed", constant = "false")  // Mặc định false
    Task toEntity(TaskCreateDTO dto);
    
    // Entity → Response DTO
    TaskResponseDTO toResponseDTO(Task task);
    
    // Entity → Summary DTO
    TaskSummaryDTO toSummaryDTO(Task task);
    
    // List mapping
    List<TaskSummaryDTO> toSummaryDTOList(List<Task> tasks);
    
    // Update entity from DTO (chỉ update fields NOT NULL)
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)  // Không update id
    void updateEntity(@MappingTarget Task task, TaskUpdateDTO dto);
}
```

---

#### Bước 3: Inject Mapper vào Service

```java
@Service
@RequiredArgsConstructor
public class TaskServiceImpl implements TaskService {
    
    private final TaskRepo taskRepo;
    private final TaskMapper taskMapper;  // Inject MapStruct mapper
    
    @Override
    public TaskResponseDTO createTask(TaskCreateDTO dto) {
        Task task = taskMapper.toEntity(dto);  // Dùng MapStruct
        Task saved = taskRepo.save(task);
        return taskMapper.toResponseDTO(saved);
    }
    
    // ... các methods khác tương tự
}
```

---

## 📊 So sánh tổng hợp

| Tiêu chí | Không dùng DTO | Manual Mapping | MapStruct |
|---------|---------------|----------------|-----------|
| **Bảo mật** | ❌ Kém | ✅ Tốt | ✅ Tốt |
| **Flexibility** | ⚠️ Thấp | ✅ Cao | ✅ Cao |
| **Code amount** | ✅ Ít nhất | ⚠️ Trung bình | ✅ Ít |
| **Complexity** | ✅ Đơn giản | ✅ Trung bình | ⚠️ Cao (setup) |
| **Performance** | ✅ Tốt | ✅ Tốt | ✅ Rất tốt |
| **Maintainability** | ❌ Khó | ✅ Tốt | ✅ Rất tốt |
| **Learning curve** | ✅ Dễ | ✅ Dễ | ⚠️ Trung bình |
| **Best for** | Demo/Prototype | Small/Medium project | Large project |

---

## 🎯 Khuyến nghị cuối cùng cho TodoList Project

### ✅ KHUYẾN NGHỊ: Dùng Manual Mapping (không dùng MapStruct)

**Lý do:**
1. ✅ Project nhỏ, chỉ có 1 Entity (Task)
2. ✅ Đơn giản, dễ hiểu, dễ debug
3. ✅ Không cần setup phức tạp
4. ✅ Dễ customize logic (validation, default values)
5. ✅ Đủ nhanh cho hầu hết use cases

**Không dùng Abstract Class vì:**
- Project nhỏ, không có pattern lặp lại nhiều
- Inheritance làm phức tạp không cần thiết

**Nâng cấp sau này:**
- Nếu thêm 3-5 Entities nữa (User, Project, Comment, ...) → cân nhắc MapStruct
- Nếu team mở rộng → cân nhắc standardize với MapStruct

---

## 🧪 Test API sau khi triển khai DTO

### Test 1: Tạo task mới

**Request:**
```bash
POST http://localhost:8080/api/tasks
Content-Type: application/json

{
  "title": "Learn DTO Pattern",
  "description": "Study Data Transfer Object"
}
```

**Expected Response:**
```json
{
  "id": "674c1234abcd...",
  "title": "Learn DTO Pattern",
  "description": "Study Data Transfer Object",
  "completed": false
}
```

**Test security:**
```bash
# Client cố gắng hack
{
  "id": "fake-id-123",      # Bị ignore
  "title": "Hacked",
  "completed": true          # Bị force = false
}
```

**Expected:** ID do MongoDB sinh, completed vẫn là false

---

### Test 2: Update task

**Request:**
```bash
PUT http://localhost:8080/api/tasks/674c1234abcd...
Content-Type: application/json

{
  "completed": true
}
```

**Expected:** Chỉ update `completed`, không đổi `title` và `description`

---

### Test 3: Get all tasks

**Request:**
```bash
GET http://localhost:8080/api/tasks
```

**Expected Response:**
```json
[
  {
    "id": "674c1234...",
    "title": "Task 1",
    "completed": false
  },
  {
    "id": "674c5678...",
    "title": "Task 2",
    "completed": true
  }
]
```

**Lưu ý:** Không có `description` (dùng TaskSummaryDTO)

---

### Test 4: Get task by ID

**Request:**
```bash
GET http://localhost:8080/api/tasks/674c1234...
```

**Expected Response:**
```json
{
  "id": "674c1234...",
  "title": "Learn DTO Pattern",
  "description": "Study Data Transfer Object",
  "completed": false
}
```

**Lưu ý:** Có đầy đủ `description` (dùng TaskResponseDTO)

---

## 📚 Tóm tắt

### ✅ Nên làm:
1. ✅ Triển khai DTO Pattern (tách Request/Response DTOs)
2. ✅ Dùng Manual Mapping với static methods
3. ✅ Tổ chức code rõ ràng: `request/`, `response/`, `mapper/`
4. ✅ Validate ở Service layer
5. ✅ Dùng Builder pattern cho Response DTOs

### ❌ Không nên:
1. ❌ Dùng Abstract Class (overkill cho project nhỏ)
2. ❌ Dùng MapStruct lúc này (chưa cần thiết)
3. ❌ Expose Entity ra ngoài API
4. ❌ Tin tưởng data từ client (luôn validate)

### 🔮 Nâng cấp sau này:
- Khi có 5+ Entities → cân nhắc MapStruct
- Thêm Validation annotations (`@NotBlank`, `@Size`, ...)
- Thêm Exception handling toàn cục (`@ControllerAdvice`)
- Thêm Pagination cho `GET /api/tasks`
- Thêm DTO cho error responses

---

## 🔗 Tài liệu liên quan

- [VI_SAO_KHONG_KHOI_TAO_TASK_TRONG_SERVICE.md](./VI_SAO_KHONG_KHOI_TAO_TASK_TRONG_SERVICE.md)
- [LUONG_DATA_TU_JSON_DEN_DATABASE.md](./LUONG_DATA_TU_JSON_DEN_DATABASE.md)
- [Cách sử dụng interface, implementation và DTO](./Cách%20sử%20dụng%20interface,%20implementation%20và%20DTO)

---

**Ngày tạo:** 2025-12-01  
**Tác giả:** GitHub Copilot  
**Project:** TodoList Spring Boot + MongoDB

