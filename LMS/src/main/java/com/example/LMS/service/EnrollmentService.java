package com.example.LMS.service;


import com.example.LMS.model.*;
import com.example.LMS.repository.CourseRepository;
import com.example.LMS.repository.EnrollmentRepository;
import com.example.LMS.repository.UserRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


@Service
public class EnrollmentService {
    @Autowired
    private EnrollmentRepository enrollmentRepository;

    @Autowired
    private ProgressService progressService;

    @Autowired
    private  CourseService courseService;

    @Autowired
    private QuizService quizService;

    @Autowired
    private QuizResultService quizResultService;
    @Autowired
    private LessonService lessonService;

    public void saveEnrollment(Enrollment enrollment) {
        enrollmentRepository.save(enrollment);
    }

    // Xóa đăng ký khóa học, tiến trình học tập và bài tập trắc nghiệm
    @Transactional
    public void removeEnrollmentAndProgress(Long userId, Long courseId) {
        // Xóa toàn bộ tiến trình học tập
        progressService.deleteProgressByUserAndCourse(userId, courseId);

        // Xóa toàn bộ kết quả bài tập trắc nghiệm
        quizResultService.deleteQuizResultsByUserAndCourse(userId, courseId);

        // Xóa đăng ký khóa học
        enrollmentRepository.deleteByUserIdAndCourseId(userId, courseId);
    }

    public List<Enrollment> getEnrollmentsByUser(User user) {
        return enrollmentRepository.findByUser(user);
    }

    public boolean isUserEnrolledInCourse(Long userId, Long courseId) {
        return enrollmentRepository.existsByUserIdAndCourseId(userId, courseId);
    }

    public List<Enrollment> getEnrollmentsByUserId(Long userId) {
        return enrollmentRepository.findByUserId(userId);
    }

    @Transactional
    public void removeEnrollmentByUserAndCourse(Long userId, Long courseId) {
        enrollmentRepository.deleteByUserIdAndCourseId(userId, courseId);
    }

    public long calculateTotalStudyTime(Enrollment enrollment) {
        if (enrollment == null || enrollment.getEnrollmentDate() == null) {
            return 0;
        }

        // Tính tổng thời gian (theo giờ) từ ngày tham gia đến hiện tại
        LocalDateTime now = LocalDateTime.now();
        Duration duration = Duration.between(enrollment.getEnrollmentDate(), now);
        return duration.toHours();
    }

    // Lấy thông tin Enrollment theo userId và courseId
    public Enrollment getEnrollmentByUserAndCourse(Long userId, Long courseId) {
        return enrollmentRepository.findByUserIdAndCourseId(userId, courseId)
                .orElse(null); // Trả về null nếu không tìm thấy
    }

    public Map<Course, List<Lesson>> getCompletedLessonsByUser(Long userId) {
        List<Enrollment> enrollments = enrollmentRepository.findAllByUserId(userId);
        Map<Course, List<Lesson>> completedLessonsMap = new HashMap<>();

        for (Enrollment enrollment : enrollments) {
            Course course = enrollment.getCourse();
            List<Lesson> lessons = course.getLessons();
            List<Lesson> completedLessons = new ArrayList<>();

            for (Lesson lesson : lessons) {
                Progress progress = progressService.getUserProgressInLesson(userId, lesson.getId());
                if (progress != null && progress.isCompleted()) {
                    completedLessons.add(lesson);
                }
            }

            // Gắn completedLessons cho từng khóa học
            completedLessonsMap.put(course, completedLessons);
        }

        return completedLessonsMap;
    }

    public boolean isCourseCompleted(Long userId, Long courseId) {
        List<Lesson> lessons = enrollmentRepository.getLessonsByCourseId(courseId);
        for (Lesson lesson : lessons) {
            Progress progress = progressService.getUserProgressInLesson(userId, lesson.getId());
            if (progress == null || !progress.isCompleted()) {
                return false; // Nếu có bất kỳ bài học nào chưa hoàn thành
            }
        }
        return true; // Tất cả bài học trong khóa đều hoàn thành
    }

    public LocalDateTime getCourseCompletionDate(Long userId, Long courseId) {
        List<Lesson> lessons = enrollmentRepository.getLessonsByCourseId(courseId);
        LocalDateTime latestCompletionDate = null;

        for (Lesson lesson : lessons) {
            Progress progress = progressService.getUserProgressInLesson(userId, lesson.getId());
            if (progress == null || !progress.isCompleted()) {
                return null; // Nếu có bài học chưa hoàn thành, trả về null
            }
            if (latestCompletionDate == null || (progress.getCompletedAt() != null && progress.getCompletedAt().isAfter(latestCompletionDate))) {
                latestCompletionDate = progress.getCompletedAt();
            }
        }
        return latestCompletionDate;
    }

    public LocalDateTime calculateCourseCompletionDate(Long userId, Long courseId) {
        // Lấy danh sách tiến trình của người dùng trong khóa học
        List<Progress> progressList = progressService.getUserProgressInCourse(userId, courseId);

        // Lọc ra các bài học đã hoàn thành
        List<LocalDateTime> completionDates = progressList.stream()
                .filter(Progress::isCompleted)
                .map(Progress::getCompletedAt)
                .collect(Collectors.toList());

        // Kiểm tra xem tất cả bài học trong khóa học đã hoàn thành chưa
        Course course = courseService.getCourseById(courseId);
        if (completionDates.size() == course.getLessons().size()) {
            // Ngày hoàn thành là ngày hoàn thành bài học cuối cùng
            return completionDates.stream().max(LocalDateTime::compareTo).orElse(null);
        }

        // Nếu chưa hoàn thành tất cả bài học, trả về null
        return null;
    }

    public Map<Course, Long> calculateTotalStudyTime(Long userId) {
        List<Enrollment> enrollments = enrollmentRepository.findByUserId(userId);

        Map<Course, Long> totalStudyTimeMap = new HashMap<>();
        for (Enrollment enrollment : enrollments) {
            LocalDateTime enrollmentDate = enrollment.getEnrollmentDate();
            LocalDateTime completionDate = calculateCourseCompletionDate(userId, enrollment.getCourse().getId());

            if (completionDate != null) {
                // Tính thời gian học (từ ngày tham gia đến ngày hoàn thành) bằng giờ
                long hours = ChronoUnit.HOURS.between(enrollmentDate, completionDate);
                totalStudyTimeMap.put(enrollment.getCourse(), hours);
            } else {
                totalStudyTimeMap.put(enrollment.getCourse(), 0L); // Nếu chưa hoàn thành
            }
        }
        return totalStudyTimeMap;
    }

    // Tính tổng thời gian học cho một khóa học cụ thể
    public long calculateTotalStudyTimeForCourse(Long userId, Long courseId) {
        // Lấy thông tin đăng ký của người dùng trong khóa học
        Enrollment enrollment = enrollmentRepository.findByUserIdAndCourseId(userId, courseId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy thông tin đăng ký"));

        if (enrollment.getEnrollmentDate() == null) {
            throw new IllegalStateException("enrollmentDate không được null");
        }

        // Lấy danh sách tiến trình học
        List<Progress> progressList = progressService.getUserProgressInCourse(userId, courseId);

        long totalStudyTimeInSeconds = 0;
        for (Progress progress : progressList) {
            if (progress.isCompleted() && progress.getCompletedAt() != null) {
                totalStudyTimeInSeconds += progress.getCompletedAt().toEpochSecond(ZoneOffset.UTC)
                        - enrollment.getEnrollmentDate().toEpochSecond(ZoneOffset.UTC);
            }
        }
        return totalStudyTimeInSeconds;
    }

    public long calculateTotalStudyTimeForUser(Long userId) {
        List<Course> courses = getCompletedCoursesByUser(userId);
        long totalSeconds = 0;

        for (Course course : courses) {
            totalSeconds += calculateTotalStudyTimeForCourse(userId, course.getId());
        }
        // Chuyển đổi từ giây sang phút
        return totalSeconds / 60; // Trả về tổng thời gian học theo phút
    }

    public boolean isCourseCompletedd(Long userId, Long courseId) {
        List<Lesson> lessons = enrollmentRepository.getLessonsByCourseId(courseId);
        return lessons.stream()
                .allMatch(lesson -> {
                    Progress progress = progressService.getUserProgressInLesson(userId, lesson.getId());
                    return progress != null && progress.isCompleted();
                });
    }

    public boolean isCourseCompleteddd(Long userId, Long courseId) {
        // Lấy danh sách tất cả bài học trong khóa học
        List<Lesson> lessons = lessonService.getLessonsByCourseId(courseId);
        if (lessons.isEmpty()) {
            return false; // Không có bài học nào => chưa hoàn thành
        }

        // Kiểm tra nếu tất cả bài học đã hoàn thành
        boolean allLessonsCompleted = lessons.stream().allMatch(lesson -> {
            Progress progress = progressService.getUserProgressInLesson(userId, lesson.getId());
            return progress != null && progress.isCompleted();
        });

        if (!allLessonsCompleted) {
            return false; // Chưa hoàn thành tất cả bài học
        }

        // Lấy danh sách tất cả bài tập trắc nghiệm trong khóa học
        List<Quiz> quizzes = quizService.getQuizzesByCourseId(courseId);
        if (quizzes.isEmpty()) {
            return allLessonsCompleted; // Nếu không có bài tập trắc nghiệm, chỉ cần hoàn thành bài học
        }

        // Kiểm tra nếu tất cả bài tập trắc nghiệm đã hoàn thành
        boolean allQuizzesCompleted = quizzes.stream().allMatch(quiz ->
                quizResultService.isQuizCompletedByUser(quiz.getId(), userId)
        );

        return allQuizzesCompleted; // Chỉ trả về true nếu cả bài học và bài tập trắc nghiệm đã hoàn thành
    }


    public int ccountCompletedCoursesByUser(Long userId) {
        // Lấy danh sách các khóa học mà người dùng đã hoàn thành
        return (int) enrollmentRepository.findAllByUserId(userId).stream()
                .filter(enrollment -> isCourseCompleted(userId, enrollment.getCourse().getId()))
                .count();
    }

    public int countCompletedCoursesByUser(Long userId) {
        // Lấy danh sách các khóa học mà người dùng đã đăng ký
        List<Enrollment> enrollments = enrollmentRepository.findAllByUserId(userId);

        // Đếm số khóa học đã hoàn thành
        return (int) enrollments.stream()
                .filter(enrollment -> isCourseCompleteddd(userId, enrollment.getCourse().getId()))
                .count();
    }

    public List<Course> getCompletedCoursesByUser(Long userId) {
        // Lọc danh sách các khóa học mà người dùng đã hoàn thành
        return enrollmentRepository.findAllByUserId(userId).stream()
                .filter(enrollment -> isCourseCompleted(userId, enrollment.getCourse().getId()))
                .map(Enrollment::getCourse)
                .collect(Collectors.toList());
    }
}
