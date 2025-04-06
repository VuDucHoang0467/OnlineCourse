package com.example.LMS.controller;

import com.example.LMS.model.CustomUserDetail;
import com.example.LMS.model.Lesson;
import com.example.LMS.model.Progress;
import com.example.LMS.model.User;
import com.example.LMS.service.LessonService;
import com.example.LMS.service.ProgressService;
import com.example.LMS.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/lessons")
public class LessonApiController {

    @Autowired
    private LessonService lessonService;

    @Autowired
    private ProgressService progressService;

    @Autowired
    private UserService userService;

    @PostMapping("/{lessonId}/complete")
    public ResponseEntity<?> markLessonAsCompleted(@PathVariable Long lessonId, @AuthenticationPrincipal CustomUserDetail customUserDetail) {
        if (customUserDetail == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Bạn cần đăng nhập để thực hiện hành động này.");
        }

        // Lấy thông tin người dùng từ username
        User user = userService.findByUsername(customUserDetail.getUsername());
        if (user == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Người dùng không tồn tại.");
        }

        Long userId = user.getId();

        // Kiểm tra và cập nhật trạng thái hoàn thành bài học
        Progress progress = progressService.getUserProgressInLesson(userId, lessonId);
        if (progress == null) {
            progress = new Progress();
            progress.setUser(user);
            progress.setLesson(lessonService.getLessonById(lessonId));
            progress.setCompleted(true);
            progress.setCompletedAt(LocalDateTime.now());
        } else {
            progress.setCompleted(true);
            progress.setCompletedAt(LocalDateTime.now());
        }

        progressService.saveProgress(progress);
        return ResponseEntity.ok("Đánh dấu hoàn thành bài học thành công.");
    }

    @GetMapping("/{lessonId}/lessonstatus")
    public ResponseEntity<Map<String, Object>> checkLessonStatus(@PathVariable Long lessonId, Principal principal) {
        User user = userService.findByUsername(principal.getName());
        Lesson lesson = lessonService.getLessonById(lessonId);

        boolean completed = progressService.isLessonCompletedByUser(user, lesson);
        Map<String, Object> response = new HashMap<>();
        response.put("completed", completed);
        return ResponseEntity.ok(response);
    }


    @GetMapping("/{lessonId}/status")
    public ResponseEntity<Boolean> checkLessonCompletion(@PathVariable Long lessonId, @AuthenticationPrincipal CustomUserDetail customUserDetail) {
        if (customUserDetail == null) {
            // Nếu người dùng không đăng nhập, trả về trạng thái UNAUTHORIZED
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        Long userId = customUserDetail.getId();

        // Kiểm tra trạng thái hoàn thành bài học
        Progress progress = progressService.getUserProgressInLesson(userId, lessonId);

        if (progress == null) {
            // Nếu không có tiến độ cho bài học, coi như chưa hoàn thành
            return ResponseEntity.ok(false);
        }

        boolean isCompleted = progress.isCompleted();  // Kiểm tra xem bài học đã hoàn thành chưa
        return ResponseEntity.ok(isCompleted);
    }


    @GetMapping("/course/{courseId}/completion-status")
    public ResponseEntity<Boolean> checkAllLessonsCompleted(@PathVariable Long courseId, @AuthenticationPrincipal CustomUserDetail customUserDetail) {
        if (customUserDetail == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        Long userId = customUserDetail.getId();

        // Lấy danh sách bài học trong khóa học
        List<Lesson> lessons = lessonService.getLessonsByCourseId(courseId);

        // Lấy tiến trình học của người dùng
        List<Progress> progressList = progressService.getUserProgressInCourse(userId, courseId);

        // Kiểm tra nếu tất cả bài học đều hoàn thành
        boolean allCompleted = lessons.stream()
                .allMatch(lesson -> progressList.stream()
                        .anyMatch(progress -> progress.getLesson() != null && // Kiểm tra Lesson khác null
                                Long.valueOf(progress.getLesson().getId()).equals(lesson.getId()) && // So sánh ID an toàn
                                progress.isCompleted()));

        return ResponseEntity.ok(allCompleted);
    }



}

