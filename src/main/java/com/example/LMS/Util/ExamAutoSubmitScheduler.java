package com.example.LMS.Util;

import com.example.LMS.entity.Enum.AttemptStatus;
import com.example.LMS.entity.model.StudentExamAttempt;
import com.example.LMS.repository.StudentExamAttemptRepository;
import com.example.LMS.service.ExamService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class ExamAutoSubmitScheduler {

    private final StudentExamAttemptRepository attemptRepository;
    private final ExamService examService;

    // 🌟 Quét hệ thống mỗi phút một lần giống đợt đăng ký môn
    @Scheduled(cron = "0 * * * * ?")
    @Transactional
    public void scanAndForceSubmitOverdueExams() {
        LocalDateTime now = LocalDateTime.now();

        // 1. Tìm tất cả các lượt làm bài chưa nộp
        List<StudentExamAttempt> activeAttempts = attemptRepository.findAll()
                .stream()
                .filter(a -> a.getStatus() == AttemptStatus.IN_PROGRESS)
                .toList();

        if (activeAttempts.isEmpty()) return;

        log.info("🔍 [CRON EXAM] Đang rà soát {} phiên làm bài đang diễn ra...", activeAttempts.size());

        for (StudentExamAttempt attempt : activeAttempts) {
            // Tính toán thời gian: Giờ bắt đầu + Thời lượng bài thi (phút) + 1 phút bù trừ độ trễ mạng
            LocalDateTime deadline = attempt.getStartTime().plusMinutes(attempt.getExam().getTimeLimit()).plusMinutes(1);

            // 2. Nếu thời gian hiện tại đã vượt qua deadline -> Tiến hành "cưỡng chế" thu bài
            if (now.isAfter(deadline)) {
                log.warn("🚨 [CRON EXAM] Phát hiện sinh viên ID [{}] quá giờ làm bài tại Attempt [{}]. Tiến hành tự động thu bài!",
                        attempt.getStudentId(), attempt.getId());
                try {
                    // Gọi luồng chấm điểm cưỡng chế và đổi trạng thái thành FORCED
                    examService.forceSubmitExamAttemptFromScheduler(attempt.getId());
                } catch (Exception e) {
                    log.error("🚨 Lỗi khi tự động thu bài cho Attempt ID [{}]: {}", attempt.getId(), e.getMessage());
                }
            }
        }
    }
}