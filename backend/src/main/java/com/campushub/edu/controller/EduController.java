package com.campushub.edu.controller;

import com.campushub.common.response.ApiResponse;
import com.campushub.common.util.CurrentUserHolder;
import com.campushub.edu.dto.TutorTaskCreateDTO;
import com.campushub.edu.service.TutorTaskService;
import com.campushub.edu.vo.TutorTaskVO;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/edu")
public class EduController {

    private final TutorTaskService tutorTaskService;

    public EduController(TutorTaskService tutorTaskService) {
        this.tutorTaskService = tutorTaskService;
    }

    @PostMapping("/tutor-tasks")
    public ApiResponse<TutorTaskVO> createTutorTask(@Valid @RequestBody TutorTaskCreateDTO dto) {
        return ApiResponse.success(tutorTaskService.createTutorTask(CurrentUserHolder.getUserId(), dto));
    }
}
