package com.example.finance.controller;

import com.example.finance.dto.CreateGoalRequest;
import com.example.finance.dto.GoalListResponse;
import com.example.finance.dto.GoalResponse;
import com.example.finance.dto.MessageResponse;
import com.example.finance.dto.UpdateGoalRequest;
import com.example.finance.service.GoalService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Routes under /api/goals. */
@RestController
@RequestMapping("/api/goals")
public class GoalController {

    private final GoalService goalService;

    public GoalController(GoalService goalService) {
        this.goalService = goalService;
    }

    /** POST /api/goals - creates a goal (201). */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public GoalResponse create(@Valid @RequestBody CreateGoalRequest request) {
        return goalService.create(request);
    }

    /** GET /api/goals - all goals of the user. */
    @GetMapping
    public GoalListResponse list() {
        return goalService.list();
    }

    /** GET /api/goals/{id} - one goal (403 if it belongs to someone else). */
    @GetMapping("/{id}")
    public GoalResponse get(@PathVariable Long id) {
        return goalService.get(id);
    }

    /** PUT /api/goals/{id} - updates target amount and/or target date. */
    @PutMapping("/{id}")
    public GoalResponse update(@PathVariable Long id, @Valid @RequestBody UpdateGoalRequest request) {
        return goalService.update(id, request);
    }

    /** DELETE /api/goals/{id} - deletes a goal (200 with a message). */
    @DeleteMapping("/{id}")
    public MessageResponse delete(@PathVariable Long id) {
        goalService.delete(id);
        return new MessageResponse("Goal deleted successfully");
    }
}
