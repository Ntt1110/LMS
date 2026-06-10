import java.time.LocalDateTime;
import java.time.LocalTime;

public class EnrollmentResponse {
    // Môn học
    private Long courseId;
    private String courseCode;
    private String courseName;
    private Integer credits;

    // Lớp học phần
    private Long classId;
    private String classCode;

    // Lịch học
    private Integer dayOfWeek;   // Thứ mấy
    private String shiftName;
    private LocalTime startTimeShilf;
    private LocalTime endTimeShilf;
    private String roomName;     // Phòng nào

    // Trạng thái đăng ký (REGISTERED, OFFICIAL, DROPPED)
    private String status;

    private LocalDateTime enrolledAt;
}
