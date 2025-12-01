# 🔄 LUỒNG HOẠT ĐỘNG - Spring Data MongoDB

## 📊 SƠ ĐỒ TỔNG QUAN

```
┌─────────────────────────────────────────────────────────────────┐
│                    KHI ỨNG DỤNG KHỞI ĐỘNG                        │
└─────────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────────┐
│  1. Spring Boot đọc application.properties                      │
│     → spring.mongodb.uri = mongodb+srv://...                    │
└─────────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────────┐
│  2. Spring tự động tạo MongoClient Bean                         │
│     → Kết nối đến MongoDB Atlas                                 │
│     → Connection pool được tạo                                  │
└─────────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────────┐
│  3. Spring quét các class có @Document                          │
│     → Tìm thấy: Task.java                                       │
│     → Ánh xạ: Task class ↔ "task" collection trong MongoDB     │
└─────────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────────┐
│  4. Spring quét các interface extends MongoRepository           │
│     → Tìm thấy: TaskRepo                                        │
│     → TƯ ĐỘNG TẠO IMPLEMENTATION (không cần viết code!)         │
│     → Tạo Bean taskRepo trong ApplicationContext               │
└─────────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────────┐
│  5. Spring quét các class có @RestController                    │
│     → Tìm thấy: MainController                                  │
│     → Thấy constructor cần TaskRepo                             │
│     → TƯ ĐỘNG INJECT taskRepo bean vào constructor             │
└─────────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────────┐
│              ỨNG DỤNG KHỞI ĐỘNG XONG - SẴN SÀNG!                │
└─────────────────────────────────────────────────────────────────┘
```

---

## 🔥 LUỒNG XỬ LÝ KHI GỌI API: POST /api/addTask

```
  CLIENT (Postman)
       │
       │ POST http://localhost:8080/api/addTask
       │ Body: {"title": "Test", "description": "...", "completed": false}
       │
       ↓
┌──────────────────────────────────────────────────────────────────┐
│  BƯỚC 1: HTTP Request đến Spring Web (DispatcherServlet)         │
└──────────────────────────────────────────────────────────────────┘
       │
       │ Spring tìm @RestController có @PostMapping("/api/addTask")
       │
       ↓
┌──────────────────────────────────────────────────────────────────┐
│  BƯỚC 2: MainController.addTask() được gọi                       │
│                                                                   │
│  @RestController                                                 │
│  public class MainController {                                   │
│      private final TaskRepo taskRepo; ← Đã được inject sẵn      │
│                                                                   │
│      @PostMapping("/api/addTask")                                │
│      public void addTask(@RequestBody Task task) {               │
│          taskRepo.save(task); ← GỌI Ở ĐÂY                       │
│      }                                                            │
│  }                                                                │
└──────────────────────────────────────────────────────────────────┘
       │
       │ JSON trong body tự động convert thành Task object
       │ {"title": "Test"...} → Task{title="Test", description=...}
       │
       ↓
┌──────────────────────────────────────────────────────────────────┐
│  BƯỚC 3: taskRepo.save(task) được gọi                            │
│                                                                   │
│  TaskRepo extends MongoRepository<Task, String>                  │
│                                                                   │
│  → Spring đã TỰ ĐỘNG TẠO implementation của interface này!      │
│  → Method save() đã có sẵn (không cần viết code)                │
└──────────────────────────────────────────────────────────────────┘
       │
       │ Spring Data MongoDB xử lý
       │
       ↓
┌──────────────────────────────────────────────────────────────────┐
│  BƯỚC 4: Spring Data MongoDB xử lý save()                        │
│                                                                   │
│  1. Lấy Task object                                              │
│  2. Convert Task object → BSON Document                          │
│     Task{                            Document{                   │
│       id: null,                →       _id: ObjectId("..."),    │
│       title: "Test",                   title: "Test",           │
│       description: "...",              description: "...",      │
│       completed: false                 completed: false         │
│     }                                }                           │
│                                                                   │
│  3. Xác định collection name từ @Document annotation            │
│     → Collection: "task" (mặc định lấy từ tên class)            │
└──────────────────────────────────────────────────────────────────┘
       │
       │ Gửi lệnh đến MongoDB
       │
       ↓
┌──────────────────────────────────────────────────────────────────┐
│  BƯỚC 5: MongoClient gửi command đến MongoDB Atlas               │
│                                                                   │
│  MongoClient (đã được tạo từ spring.mongodb.uri)                │
│      ↓                                                            │
│  Connection Pool → Lấy 1 connection                              │
│      ↓                                                            │
│  Gửi lệnh: db.task.insertOne({                                  │
│      _id: ObjectId("..."),                                       │
│      title: "Test",                                              │
│      description: "...",                                         │
│      completed: false                                            │
│  })                                                               │
│      ↓                                                            │
│  MongoDB Atlas (cluster0.52b0vwu.mongodb.net)                   │
│      ↓                                                            │
│  Database: todolist_db                                           │
│      ↓                                                            │
│  Collection: task                                                │
│      ↓                                                            │
│  INSERT document thành công!                                     │
└──────────────────────────────────────────────────────────────────┘
       │
       │ Trả kết quả về
       │
       ↓
┌──────────────────────────────────────────────────────────────────┐
│  BƯỚC 6: MongoDB trả kết quả về Spring                           │
│                                                                   │
│  MongoDB Atlas → MongoClient → Spring Data MongoDB               │
│                                                                   │
│  → Document đã insert với _id mới                                │
│  → Convert BSON Document → Task object (với id đã set)          │
└──────────────────────────────────────────────────────────────────┘
       │
       │ save() method return
       │
       ↓
┌──────────────────────────────────────────────────────────────────┐
│  BƯỚC 7: MainController.addTask() hoàn thành                     │
│                                                                   │
│  public void addTask(@RequestBody Task task) {                   │
│      taskRepo.save(task); ← Đã xong                             │
│  }                                                                │
│                                                                   │
│  → Method return void                                            │
│  → Spring Web tự động trả HTTP 200 OK về client                 │
└──────────────────────────────────────────────────────────────────┘
       │
       │ HTTP Response
       │
       ↓
  CLIENT (Postman)
  → Nhận HTTP 200 OK
  → Task đã được lưu vào MongoDB!
```

---

## 🔗 CÁCH CÁC THÀNH PHẦN LIÊN KẾT VỚI NHAU

### 1️⃣ **Task.java (Model/Entity)**

```java
@Document  ← ĐÂY LÀ KEY! Đánh dấu class này map với MongoDB collection
@Data      ← Lombok tự tạo getter/setter
public class Task {
    @Id    ← Đánh dấu field này là _id trong MongoDB
    private String id;
    private String title;
    private String description;
    private boolean completed;
}
```

**Vai trò:**
- ✅ Đại diện cho **1 document** trong MongoDB collection
- ✅ `@Document` → Spring biết class này map với MongoDB
- ✅ `@Id` → Spring biết field nào là primary key (_id)
- ✅ Field names tự động map với document fields

**Ánh xạ:**
```
Task object                MongoDB Document
────────────────────      ──────────────────────
Task {                    {
  id: "abc123"     →        _id: "abc123"
  title: "Test"    →        title: "Test"
  description: "..." →      description: "..."
  completed: false →        completed: false
}                         }
```

---

### 2️⃣ **TaskRepo.java (Repository Interface)**

```java
public interface TaskRepo extends MongoRepository<Task, String> {
    // ↑                                    ↑      ↑
    // |                                    |      |
    // Interface                     Entity Type  ID Type
    // (không cần viết code!)        (Task)       (String)
}
```

**Vai trò:**
- ✅ **Interface** - chỉ khai báo, KHÔNG CẦN viết implementation
- ✅ Extends `MongoRepository<Task, String>`:
  - `Task` → Entity class (đối tượng làm việc)
  - `String` → Kiểu dữ liệu của ID
- ✅ Spring tự động TẠO implementation class ở runtime

**Spring làm gì với interface này?**

Khi app khởi động:
1. Spring quét thấy `TaskRepo extends MongoRepository`
2. Spring tự động tạo 1 class implement TaskRepo (bạn không thấy code)
3. Class này có sẵn các methods:
   - `save(Task)` - Insert hoặc update
   - `findById(String)` - Tìm theo ID
   - `findAll()` - Lấy tất cả
   - `deleteById(String)` - Xóa theo ID
   - `count()` - Đếm
4. Spring tạo bean `taskRepo` và đưa vào ApplicationContext

**Liên kết với Task:**
```java
MongoRepository<Task, String>
                ↑
                |
        Task.java được chỉ định
                |
        Spring biết làm việc với collection nào:
                ↓
        Collection "task" trong MongoDB
```

---

### 3️⃣ **MainController.java (REST Controller)**

```java
@RestController  ← Đánh dấu đây là REST API controller
public class MainController {

    private final TaskRepo taskRepo;  ← Declare dependency
    
    // Constructor injection
    public MainController(TaskRepo taskRepo) {
        this.taskRepo = taskRepo;  ← Spring TỰ ĐỘNG inject
    }
    
    @PostMapping("/api/addTask")  ← Map URL endpoint
    public void addTask(@RequestBody Task task) {
        taskRepo.save(task);  ← Sử dụng repository
    }
}
```

**Vai trò:**
- ✅ Nhận HTTP requests từ client
- ✅ Gọi Repository để thao tác với database
- ✅ Trả response về client

**Dependency Injection - Spring làm như thế nào?**

```
KHI APP KHỞI ĐỘNG:

1. Spring tạo taskRepo bean (implementation của TaskRepo)
   → Bean name: taskRepo
   
2. Spring quét MainController
   → Thấy constructor cần TaskRepo
   
3. Spring tìm bean có type TaskRepo trong ApplicationContext
   → Tìm thấy: taskRepo bean
   
4. Spring tự động inject:
   new MainController(taskRepo) ← Truyền bean vào constructor
   
5. mainController bean được tạo và quản lý bởi Spring
```

**Liên kết với TaskRepo:**
```java
MainController
      ↓ (dependency)
   TaskRepo
      ↓ (operates on)
   Task (Entity)
      ↓ (maps to)
   MongoDB Collection "task"
```

---

## 🎯 TẠI SAO KHÔNG CẦN VIẾT CODE KẾT NỐI?

### Cách truyền thống (MongoDB Java Driver):

```java
// Phải viết tất cả code này:
String uri = "mongodb+srv://...";
MongoClient mongoClient = MongoClients.create(uri);
MongoDatabase database = mongoClient.getDatabase("todolist_db");
MongoCollection<Document> collection = database.getCollection("task");

// Insert
Document doc = new Document("title", "Test")
                  .append("description", "...")
                  .append("completed", false);
collection.insertOne(doc);

// Find
Document found = collection.find(eq("_id", id)).first();
```

### Với Spring Data MongoDB:

```java
// Chỉ cần:
taskRepo.save(task);  // ← Spring lo tất cả!
Task found = taskRepo.findById(id).get();
```

**Spring tự động làm:**
1. ✅ Đọc `spring.mongodb.uri` từ properties
2. ✅ Tạo `MongoClient` bean
3. ✅ Quản lý connection pool
4. ✅ Tạo MongoTemplate
5. ✅ Tạo implementation cho Repository
6. ✅ Convert Object ↔ BSON Document
7. ✅ Xử lý exceptions
8. ✅ Đóng connections khi cần

---

## 🔄 LUỒNG DỮ LIỆU CHI TIẾT

### Request từ Postman → MongoDB

```
1. HTTP Request (JSON)
   ↓
   {"title": "Test", "description": "My task", "completed": false}

2. Spring Web (DispatcherServlet)
   ↓
   Tìm @PostMapping("/api/addTask")

3. MainController.addTask(@RequestBody Task task)
   ↓
   @RequestBody → JSON tự động convert thành Task object
   Task{id=null, title="Test", description="My task", completed=false}

4. taskRepo.save(task)
   ↓
   Gọi implementation (do Spring tạo)

5. Spring Data MongoDB Processing
   ↓
   - Kiểm tra id: null → insert mới (nếu có id → update)
   - Tạo ObjectId mới cho _id
   - Convert Task object → BSON Document

6. MongoTemplate.insert()
   ↓
   Sử dụng MongoClient để gửi command

7. MongoClient
   ↓
   Lấy connection từ pool
   ↓
   Gửi insertOne command qua network

8. MongoDB Atlas
   ↓
   Database: todolist_db
   ↓
   Collection: task
   ↓
   INSERT document
   ↓
   Trả về: {acknowledged: true, insertedId: "..."}

9. Response về Spring
   ↓
   Convert BSON → Task object (với id mới)
   ↓
   Return Task object

10. MainController return void
    ↓
    Spring Web tự động trả HTTP 200 OK

11. Postman nhận response
```

---

## 🧩 CÁC THÀNH PHẦN SPRING DATA MONGODB

```
┌─────────────────────────────────────────────────────────────┐
│                 APPLICATION LAYER                            │
│                                                              │
│  ┌──────────────┐         ┌──────────────┐                 │
│  │ Controller   │─────────│   Service    │ (optional)      │
│  │ (REST API)   │         │ (Business    │                 │
│  └──────────────┘         │   Logic)     │                 │
│         │                  └──────────────┘                 │
│         │                         │                          │
│         └─────────────────────────┘                          │
│                       │                                      │
└───────────────────────┼──────────────────────────────────────┘
                        ↓
┌─────────────────────────────────────────────────────────────┐
│              REPOSITORY LAYER (INTERFACE)                    │
│                                                              │
│  ┌──────────────────────────────────────────────────────┐  │
│  │  public interface TaskRepo                           │  │
│  │    extends MongoRepository<Task, String> {           │  │
│  │      // Spring tự động implement                     │  │
│  │  }                                                    │  │
│  └──────────────────────────────────────────────────────┘  │
└───────────────────────┬──────────────────────────────────────┘
                        ↓
┌─────────────────────────────────────────────────────────────┐
│         SPRING DATA MONGODB (AUTO-IMPLEMENTATION)            │
│                                                              │
│  Spring tự động tạo class implement TaskRepo:               │
│                                                              │
│  class TaskRepoImpl implements TaskRepo {                   │
│      private MongoTemplate mongoTemplate;                   │
│                                                              │
│      public Task save(Task task) {                          │
│          return mongoTemplate.save(task, "task");           │
│      }                                                       │
│      // ... các methods khác                                │
│  }                                                           │
└───────────────────────┬──────────────────────────────────────┘
                        ↓
┌─────────────────────────────────────────────────────────────┐
│                    MONGO TEMPLATE                            │
│                                                              │
│  - Convert Object ↔ Document                                │
│  - Xử lý mapping (@Document, @Id, @Field)                   │
│  - Query builder                                            │
│  - Exception translation                                    │
└───────────────────────┬──────────────────────────────────────┘
                        ↓
┌─────────────────────────────────────────────────────────────┐
│                     MONGO CLIENT                             │
│                                                              │
│  - Connection pool management                               │
│  - Network communication với MongoDB                        │
│  - BSON encoding/decoding                                   │
│  - Retry logic                                              │
└───────────────────────┬──────────────────────────────────────┘
                        ↓
┌─────────────────────────────────────────────────────────────┐
│                  MONGODB ATLAS                               │
│                                                              │
│  Database: todolist_db                                      │
│    └─ Collection: task                                      │
│         └─ Documents: { _id, title, description, ... }     │
└─────────────────────────────────────────────────────────────┘
```

---

## 🎓 TÓM TẮT CÁCH LIÊN KẾT

### 1. **application.properties → MongoClient**
```properties
spring.mongodb.uri=mongodb+srv://...
```
→ Spring đọc và tạo `MongoClient` bean tự động

### 2. **Task.java → MongoDB Collection**
```java
@Document  // Spring biết class này map với collection
public class Task { ... }
```
→ Mỗi Task object = 1 document trong collection "task"

### 3. **TaskRepo → Spring Implementation**
```java
interface TaskRepo extends MongoRepository<Task, String> { }
```
→ Spring TƯ ĐỘNG tạo implementation class
→ Implementation sử dụng MongoTemplate
→ MongoTemplate sử dụng MongoClient

### 4. **MainController → TaskRepo**
```java
public MainController(TaskRepo taskRepo) { }
```
→ Spring TƯ ĐỘNG inject taskRepo bean vào constructor
→ Controller dùng taskRepo để thao tác database

---

## 🔍 DEBUG - XEM SPRING LÀM GÌ

Thêm vào `application.properties`:
```properties
# Xem query MongoDB
logging.level.org.springframework.data.mongodb.core.MongoTemplate=DEBUG

# Xem connection
logging.level.org.mongodb.driver.connection=DEBUG
```

Khi chạy `taskRepo.save(task)`, bạn sẽ thấy log:
```
Inserting Document containing fields: [title, description, completed] 
in collection: task
```

→ Đây là Spring Data MongoDB thực hiện INSERT!

---

## 🎯 KẾT LUẬN

**Toàn bộ magic xảy ra nhờ:**
1. ✅ **Annotations** (@Document, @Id, @RestController)
2. ✅ **Spring Dependency Injection** (Auto-wiring)
3. ✅ **Spring Data JPA Pattern** (Repository interface)
4. ✅ **Spring Auto-Configuration** (Đọc properties → Tạo beans)

**Bạn CHỈ CẦN:**
- ✅ Khai báo `spring.mongodb.uri`
- ✅ Tạo Entity class với `@Document`
- ✅ Tạo Repository interface extends `MongoRepository`
- ✅ Inject Repository vào Controller

**Spring LO TẤT CẢ còn lại!** 🚀

