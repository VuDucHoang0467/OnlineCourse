package com.example.LMS.repository;

import com.example.LMS.model.Course;
import com.example.LMS.model.CourseCatalog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CourseRepository extends JpaRepository<Course, Long> {
    // Tìm kiếm khóa học theo tên không phân biệt chữ hoa/chữ thường
    List<Course> findByNameContainingIgnoreCase(String name);

    List<Course> findByCourseCatalog_Id(Long catalogId);
    @Query("SELECT COUNT(c) FROM Course c " +
            "JOIN Enrollment e ON c.id = e.course.id " +
            "JOIN Lesson l ON l.course.id = c.id " +
            "LEFT JOIN Progress p ON p.lesson.id = l.id AND p.user.id = e.user.id " +
            "WHERE e.user.id = :userId " +
            "GROUP BY c.id " +
            "HAVING COUNT(l.id) = SUM(CASE WHEN p.completed = true THEN 1 ELSE 0 END)")
    int countCompletedCoursesByUser(@Param("userId") Long userId);
    @Query("SELECT c FROM Course c " +
            "JOIN Enrollment e ON c.id = e.course.id " +
            "JOIN Lesson l ON l.course.id = c.id " +
            "LEFT JOIN Progress p ON p.lesson.id = l.id AND p.user.id = e.user.id " +
            "WHERE e.user.id = :userId " +
            "GROUP BY c.id " +
            "HAVING COUNT(l.id) = SUM(CASE WHEN p.completed = true THEN 1 ELSE 0 END)")
    List<Course> findCompletedCoursesByUser(@Param("userId") Long userId);
}
