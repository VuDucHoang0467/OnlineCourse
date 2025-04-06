package com.example.LMS.controller;

import com.example.LMS.model.*;
import com.example.LMS.service.EnrollmentService;
import com.example.LMS.service.QuizResultService;
import com.example.LMS.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/history")
public class HistoryController {

    @Autowired
    private QuizResultService quizResultService;

    @Autowired
    private EnrollmentService enrollmentService;

    @Autowired
    private UserService userService;

    /*@GetMapping
    public String getUserHistory(@AuthenticationPrincipal CustomUserDetail customUserDetail, Model model) {
        Long userId = customUserDetail.getId();

        // Lấy danh sách bài học hoàn thành
        Map<Course, List<Lesson>> completedLessons = enrollmentService.getCompletedLessonsByUser(userId);

        // Lấy trạng thái hoàn thành của khóa học
        Map<Course, Boolean> courseCompletionMap = new HashMap<>();
        Map<Course, LocalDateTime> courseCompletionDates = new HashMap<>();
        Map<Long, Boolean> quizCompletedMap = new HashMap<>();

        for (Course course : completedLessons.keySet()) {
            boolean isCompleted = enrollmentService.isCourseCompleted(userId, course.getId());
            courseCompletionMap.put(course, isCompleted);

            if (isCompleted) {
                LocalDateTime completionDate = enrollmentService.getCourseCompletionDate(userId, course.getId());
                courseCompletionDates.put(course, completionDate);
            }

            // Tính toán trạng thái hoàn thành cho các bài tập trắc nghiệm
            for (Quiz quiz : course.getQuizzes()) {
                boolean isQuizCompleted = quizResultService.isQuizCompletedByUser(quiz.getId(), userId);
                quizCompletedMap.put(quiz.getId(), isQuizCompleted);
            }
        }

        // Tính tổng thời gian học
        Map<Course, Long> totalStudyTime = enrollmentService.calculateTotalStudyTime(userId);


        model.addAttribute("completedLessons", completedLessons);
        model.addAttribute("courseCompletionMap", courseCompletionMap);
        model.addAttribute("courseCompletionDates", courseCompletionDates);
        model.addAttribute("totalStudyTime", totalStudyTime);
        model.addAttribute("quizCompletedMap", quizCompletedMap);

        return "history/list";
    }*/

    @GetMapping
    public String getUserHistory(@AuthenticationPrincipal CustomUserDetail customUserDetail, Model model) {
        Long userId = customUserDetail.getId();
        var enrollments = enrollmentService.getEnrollmentsByUserId(userId);

        // Lấy danh sách bài học hoàn thành
        Map<Course, List<Lesson>> completedLessons = enrollmentService.getCompletedLessonsByUser(userId);

        // Các map để lưu thông tin
        Map<Course, Boolean> courseCompletionMap = new HashMap<>();
        Map<Course, LocalDateTime> courseCompletionDates = new HashMap<>();
        Map<Long, Boolean> quizCompletedMap = new HashMap<>();
        Map<Course, Long> totalStudyTime = new HashMap<>();
        Map<Long, Long> totalStudyTimeMapp = new HashMap<>();
        Map<Course, Long> learningDurationMap = new HashMap<>(); // Thời gian đã học (ngày)
        Map<Course, String> formattedLearningDurationMap = new HashMap<>();
        for (Course course : completedLessons.keySet()) {
            boolean isCourseCompleted = enrollmentService.isCourseCompleted(userId, course.getId());
            courseCompletionMap.put(course, isCourseCompleted);

            if (isCourseCompleted) {
                boolean allQuizzesCompleted = course.getQuizzes().stream()
                        .allMatch(quiz -> quizResultService.isQuizCompletedByUser(quiz.getId(), userId));

                if (allQuizzesCompleted) {
                    // Ngày hoàn thành
                    LocalDateTime completionDate = enrollmentService.getCourseCompletionDate(userId, course.getId());
                    courseCompletionDates.put(course, completionDate);

                    // Tổng thời gian học (giây/phút/giờ)
                    long totalTime = enrollmentService.calculateTotalStudyTimeForCourse(userId, course.getId());
                    totalStudyTime.put(course, totalTime);

                    // Tính thời gian đã học (giờ)
                    Enrollment enrollment = enrollmentService.getEnrollmentByUserAndCourse(userId, course.getId());
                    if (enrollment != null && enrollment.getEnrollmentDate() != null && completionDate != null) {
                        long hoursBetween = ChronoUnit.HOURS.between(enrollment.getEnrollmentDate(), completionDate);
                        learningDurationMap.put(course, hoursBetween);
                    }
                    for (Map.Entry<Course, Long> entry : learningDurationMap.entrySet()) {
                        formattedLearningDurationMap.put(entry.getKey(), entry.getValue() + " giờ");
                    }
                }
            }

            // Tính tổng thời gian học cho từng khóa học
            for (Enrollment enrollment : enrollments) {
                long totalStudyTimee = enrollmentService.calculateTotalStudyTime(enrollment);
                totalStudyTimeMapp.put(enrollment.getCourse().getId(), totalStudyTimee);
            }

            // Tính toán trạng thái hoàn thành cho các bài tập trắc nghiệm
            for (Quiz quiz : course.getQuizzes()) {
                boolean isQuizCompleted = quizResultService.isQuizCompletedByUser(quiz.getId(), userId);
                quizCompletedMap.put(quiz.getId(), isQuizCompleted);
            }
        }

        // Gửi dữ liệu sang view
        model.addAttribute("enrollments", enrollments);
        model.addAttribute("totalStudyTimeMap", totalStudyTimeMapp);
        model.addAttribute("completedLessons", completedLessons);
        model.addAttribute("courseCompletionMap", courseCompletionMap);
        model.addAttribute("courseCompletionDates", courseCompletionDates);
        model.addAttribute("totalStudyTime", totalStudyTime);
        model.addAttribute("quizCompletedMap", quizCompletedMap);
        model.addAttribute("learningDurationMap", learningDurationMap); // Thời gian đã học (ngày)
        model.addAttribute("learningDurationMapp", formattedLearningDurationMap);
        return "history/list";
    }


}

