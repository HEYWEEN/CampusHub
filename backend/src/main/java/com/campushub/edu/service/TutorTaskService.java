package com.campushub.edu.service;

import com.campushub.common.exception.BizException;
import com.campushub.edu.dto.TutorTaskCreateDTO;
import com.campushub.edu.vo.TutorTaskVO;

public interface TutorTaskService {

    TutorTaskVO createTutorTask(long publisherId, TutorTaskCreateDTO dto);
}
