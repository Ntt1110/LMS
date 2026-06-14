package com.example.LMS.service;

import com.example.LMS.dto.request.RegistrationPeriodRequestDto;
import com.example.LMS.entity.Enum.ClassStatus;
import com.example.LMS.entity.Enum.EnrollmentStatus;
import com.example.LMS.entity.Enum.RegistrationStatus;
import com.example.LMS.entity.model.Notification;
import com.example.LMS.entity.model.RegistrationPeriod;
import com.example.LMS.entity.model.Semester;
import com.example.LMS.exception.CustomException;
import com.example.LMS.repository.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class RegistrationPeriodService {

    private final RegistrationPeriodRepository periodRepository;
    private final SemesterRepository semesterRepository;
    private final ClassEntityRepository classRepository;
    private final ObjectMapper objectMapper;

    private final NotificationRepository notificationRepository;

    private final EnrollmentRepository enrollmentRepository;

    @Transactional
    public void createRegistrationPeriod(RegistrationPeriodRequestDto dto) {
        log.info("⏳ Tầng Service đang xử lý đợt mở đăng ký: {}", dto.getName());

        if (dto.getStartTime().isAfter(dto.getEndTime())) {
            throw new CustomException(HttpStatus.BAD_REQUEST, "Thời gian bắt đầu đợt không thể nằm sau thời gian kết thúc!");
        }

        Semester semester = semesterRepository.findById(dto.getSemesterId())
                .orElseThrow(() -> new CustomException(HttpStatus.NOT_FOUND, "Học kỳ áp dụng không tồn tại!"));

        // Tính toán trạng thái tự động theo mốc thời gian thực tế
        LocalDateTime now = LocalDateTime.now();
        RegistrationStatus calculatedStatus = RegistrationStatus.PENDING;

        if (now.isAfter(dto.getStartTime()) && now.isBefore(dto.getEndTime())) {
            calculatedStatus = RegistrationStatus.ACTIVE;
        } else if (now.isAfter(dto.getEndTime())) {
            calculatedStatus = RegistrationStatus.CLOSED;
        }

        // ✅ KHAI BÁO BIẾN Ở ĐÂY: Phạm vi toàn cục của hàm để Builder nhìn thấy
        String cohortsJson = "[]";
        String departmentsJson = "[]";

        try {
            // Chỉ thực hiện gán giá trị bên trong khối try
            cohortsJson = objectMapper.writeValueAsString(dto.getTargetCohorts());
            departmentsJson = objectMapper.writeValueAsString(dto.getTargetDepartments());
        } catch (JsonProcessingException e) {
            log.error("🚨 Lỗi đóng gói cấu trúc dữ liệu mảng sang JSON string: {}", e.getMessage());
            throw new CustomException(HttpStatus.INTERNAL_SERVER_ERROR, "Lỗi hệ thống khi đóng gói cấu trúc dữ liệu Khóa/Khoa đối tượng!");
        }

        // Đóng gói đẩy xuống cơ sở dữ liệu
        RegistrationPeriod period = RegistrationPeriod.builder()
                .semester(semester)
                .name(dto.getName())
                .type(dto.getType())
                .startTime(dto.getStartTime())
                .endTime(dto.getEndTime())
                .targetCohorts(cohortsJson)
                .targetDepartments(departmentsJson)
                .status(calculatedStatus)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        RegistrationPeriod savedPeriod = periodRepository.save(period);

        // Kích hoạt ngầm danh sách lớp học phần nếu trạng thái đợt là ACTIVE
        if (calculatedStatus == RegistrationStatus.ACTIVE) {
            activateClassesForRegistration(semester.getId(), savedPeriod.getId());
        }



        // 🌟 TỰ ĐỘNG TẠO THÔNG BÁO GẮN PERIOD ID (Sinh viên đọc được để biết sắp đến ngày ĐKHP)
        String notificationTitle = "Thông báo: " + savedPeriod.getName();
        String notificationMessage = String.format(
                "Hệ thống LMS thông báo mở đợt đăng ký học phần: %s.\n" +
                        "Thời gian bắt đầu cổng mở: %s\n" +
                        "Thời gian kết thúc khóa cổng: %s\n" +
                        "Sinh viên chú ý lịch để thực hiện đăng ký học phần đúng hạn!",
                savedPeriod.getName(), savedPeriod.getStartTime(), savedPeriod.getEndTime()
        );

        // Trích xuất danh sách ID các Khoa được cấu hình trong đợt đăng ký này
        List<Long> targetDeptIds = dto.getTargetDepartments();

        if (targetDeptIds == null || targetDeptIds.isEmpty()) {
            // 👉 NHÁNH A: Đợt đăng ký áp dụng đại trà TOÀN TRƯỜNG -> departmentId để NULL
            Notification globalNotification = Notification.builder()
                    .title(notificationTitle)
                    .message(notificationMessage)
                    .departmentId(null) // Tất cả sinh viên thuộc mọi khoa đều quét thấy
                    .periodId(savedPeriod.getId())
                    .build();
            notificationRepository.save(globalNotification);
            log.info("📢 Đã tự động phát hành thông báo đợt đăng ký diện TOÀN TRƯỜNG.");
        } else {
            // 👉 NHÁNH B: Đợt đăng ký giới hạn cho một vài Khoa mục tiêu -> Đẻ thông báo đích danh
            log.info("📢 Đang phân phối thông báo đợt đăng ký đích danh cho {} Khoa hệ thống...", targetDeptIds.size());
            for (Long deptId : targetDeptIds) {
                Notification deptNotification = Notification.builder()
                        .title(notificationTitle)
                        .message(notificationMessage)
                        .departmentId(deptId) // Chỉ sinh viên thuộc khoa này mới thấy
                        .periodId(savedPeriod.getId())
                        .build();
                notificationRepository.save(deptNotification);
            }
            log.info("✅ Đã hoàn tất phân phối thông báo tới các Khoa cụ thể: {}", targetDeptIds);
        }

        log.info("✅ Lưu đợt đăng ký thành công. Đã kích hoạt liên kết với Controller.");
    }

    private void activateClassesForRegistration(Long semesterId, Long periodId) {
        log.info("⚡ Tự động đổi trạng thái các lớp học phần sang REGISTRATION...");
        var pendingClasses = classRepository.findBySemesterIdAndStatus(semesterId, ClassStatus.PENDING);
        if (pendingClasses != null && !pendingClasses.isEmpty()) {
            for (var clazz : pendingClasses) {
                clazz.setStatus(ClassStatus.REGISTRATION);
                clazz.setRegistrationPeriodId(periodId);
                clazz.setUpdatedAt(LocalDateTime.now());
            }
            classRepository.saveAll(pendingClasses);
            log.info("✅ Đã kích hoạt cổng đăng ký thành công cho {} lớp học phần!", pendingClasses.size());
        }
    }
    private void closeClassesAfterRegistration(Long periodId) {
        log.info("🔒 Tự động chuyển trạng thái các lớp thuộc đợt đăng ký ID [{}] sang ONGOING...", periodId);

        // Tìm toàn bộ các lớp đang ở trạng thái đăng ký thuộc đợt này
        var activeClasses = classRepository.findByRegistrationPeriodIdAndStatus(periodId, ClassStatus.REGISTRATION);

        if (activeClasses != null && !activeClasses.isEmpty()) {
            int totalFinalizedStudents = 0;
            for (var clazz : activeClasses) {
                clazz.setStatus(ClassStatus.ONGOING); // 🎯 Chuyển trạng thái sang ĐANG DIỄN RA
                clazz.setUpdatedAt(LocalDateTime.now());
                int finalizedCount = enrollmentRepository.updateStatusByClassId(clazz.getId(), EnrollmentStatus.REGISTERED, EnrollmentStatus.OFFICIAL);
                totalFinalizedStudents += finalizedCount;
            }
            classRepository.saveAll(activeClasses);
            log.info("✅ Đã đóng cổng thành công và chuyển hành trình học cho {} lớp học phần!", activeClasses.size(),totalFinalizedStudents);
        }
    }

    @Scheduled(cron = "0 * * * * ?") // Canh giờ quét mỗi phút
    @Transactional
    public void autoUpdateRegistrationPeriods() {
        LocalDateTime now = LocalDateTime.now();

        // 🟥 NHÁNH 1: TỰ ĐỘNG MỞ CỔNG (PENDING -> ACTIVE)
        List<RegistrationPeriod> periodsToOpen = periodRepository.findByStatusAndStartTimeBefore(
                RegistrationStatus.PENDING, now
        );
        for (RegistrationPeriod period : periodsToOpen) {
            log.info("🚀 [AUTO OPEN] Đã đến giờ mở cổng! Kích hoạt đợt đăng ký: {}", period.getName());

            period.setStatus(RegistrationStatus.ACTIVE);
            period.setUpdatedAt(now);
            periodRepository.save(period);

          // kích hoạt các lớp sang trạng thái công khai REGISTRATION
            activateClassesForRegistration(period.getSemester().getId(), period.getId());
        }

        //  NHÁNH 2: TỰ ĐỘNG ĐÓNG CỔNG ĐỢT ĐĂNG KÝ (ACTIVE -> CLOSED)
        List<RegistrationPeriod> periodsToClose = periodRepository.findByStatusAndEndTimeBefore(
                RegistrationStatus.ACTIVE, now
        );
        for (RegistrationPeriod period : periodsToClose) {
            log.info("🔒 [AUTO CLOSE] Hết giờ đăng ký môn! Tự động đóng đợt: {}", period.getName());

            //  CHỈ CẬP NHẬT TRẠNG THÁI ĐỢT: Sinh viên kiểm tra đợt thấy CLOSED sẽ không thể gửi đơn đăng ký được nữa
            period.setStatus(RegistrationStatus.CLOSED);
            period.setUpdatedAt(now);
            periodRepository.save(period);
            closeClassesAfterRegistration(period.getId());


        }
    }
}