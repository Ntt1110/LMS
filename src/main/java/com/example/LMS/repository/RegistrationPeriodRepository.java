package com.example.LMS.repository;

import com.example.LMS.entity.Enum.RegistrationStatus;
import com.example.LMS.entity.model.RegistrationPeriod;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface RegistrationPeriodRepository extends JpaRepository<RegistrationPeriod, Long> {

    //  Tìm các đợt PENDING đã đến giờ mở cổng
    List<RegistrationPeriod> findByStatusAndStartTimeBefore(
            RegistrationStatus status, LocalDateTime now
    );

    //  Tìm các đợt ACTIVE đã đến giờ đóng cổng
    List<RegistrationPeriod> findByStatusAndEndTimeBefore(
            RegistrationStatus status, LocalDateTime now
    );

}