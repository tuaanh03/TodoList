package com.example.todolist.Repository;

import com.example.todolist.Model.Task;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface TaskRepo extends MongoRepository<Task, String> {

}
