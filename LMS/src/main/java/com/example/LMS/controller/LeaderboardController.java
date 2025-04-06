package com.example.LMS.controller;

import com.example.LMS.model.CustomUserDetail;
import com.example.LMS.model.User;
import com.example.LMS.service.EnrollmentService;
import com.example.LMS.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.*;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/leaderboard")
public class LeaderboardController {

    @Autowired
    private UserService userService;

    @Autowired
    private EnrollmentService enrollmentService;

    @GetMapping
    public String getLeaderboard(Model model, @AuthenticationPrincipal CustomUserDetail customUserDetail,
                                 @RequestParam(value = "search", required = false) String search,
                                 @RequestParam(value = "sortBy", required = false, defaultValue = "points") String sortBy) {
        if (customUserDetail == null) {
            // Nếu người dùng chưa đăng nhập, chuyển hướng về trang đăng nhập
            return "redirect:/login";
        }
        String currentUsername = customUserDetail.getUsername();
        Long currentUserId = customUserDetail.getId();
        User currentUser = userService.findById(currentUserId);
        // Tổng thời gian học của người dùng hiện tại
        long currentUserStudyTime = enrollmentService.calculateTotalStudyTimeForUser(currentUserId); // Dạng phút

        // Số khóa học đã hoàn thành của người dùng hiện tại
        int currentUserCompletedCourses = enrollmentService.countCompletedCoursesByUser(currentUserId);

        // Tính điểm cho người dùng hiện tại
        int currentUserPoints = (int) (currentUserStudyTime / 60) + (currentUserCompletedCourses * 10);

        // Gán huy hiệu cho người dùng hiện tại
        String currentUserBadge;
        if (currentUserPoints >= 200) {
            currentUserBadge = "Champion";
        } else if (currentUserPoints >= 100) {
            currentUserBadge = "Master";
        } else if (currentUserPoints >= 50) {
            currentUserBadge = "Learner";
        } else {
            currentUserBadge = "Beginner";
        }

        // Chuẩn bị dữ liệu cho người dùng hiện tại
        Map<String, Object> currentUserData = new HashMap<>();
        currentUserData.put("name", currentUser.getUsername());
        currentUserData.put("profilepic", currentUser.getImageData());
        currentUserData.put("totalStudyTime", currentUserStudyTime / 60); // Giờ
        currentUserData.put("completedCourses", currentUserCompletedCourses);
        currentUserData.put("totalPoints", currentUserPoints);
        currentUserData.put("badge", currentUserBadge);

        // Gửi thông tin người dùng hiện tại sang View
        model.addAttribute("currentUserData", currentUserData);

        // Lấy danh sách tất cả người dùng
        List<User> users = userService.findAll();

        // Nếu có giá trị tìm kiếm, lọc danh sách người dùng theo tên
        if (search != null && !search.isEmpty()) {
            users = users.stream()
                    .filter(user -> user.getUsername().toLowerCase().contains(search.toLowerCase()))
                    .collect(Collectors.toList());
        }

        // Dữ liệu xếp hạng
        List<Map<String, Object>> leaderboard = new ArrayList<>();

        for (User user : users) {
            Long userId = user.getId();

            // Tổng thời gian học của người dùng
            int totalStudyTime = (int) enrollmentService.calculateTotalStudyTimeForUser(userId); // Trả về tổng thời gian học dạng phút

            // Số khóa học đã hoàn thành
            int completedCourses = enrollmentService.countCompletedCoursesByUser(userId);

            // Tính điểm
            int totalPoints = (int) (totalStudyTime / 60) + (completedCourses * 10); // 1 giờ học = 1 điểm, 1 khóa học = 10 điểm

            // Gán huy hiệu
            String badge;
            if (totalPoints >= 200) {
                badge = "Champion";
            } else if (totalPoints >= 100) {
                badge = "Master";
            } else if (totalPoints >= 50) {
                badge = "Learner";
            } else {
                badge = "Beginner";
            }

            // Thêm dữ liệu vào danh sách
            Map<String, Object> userData = new HashMap<>();
            userData.put("name", user.getUsername()); // Hoặc full name nếu có
            userData.put("profilepic", user.getImageData());
            userData.put("totalStudyTime", totalStudyTime / 60); // Chuyển phút thành giờ
            userData.put("completedCourses", completedCourses);
            userData.put("totalPoints", totalPoints);
            userData.put("badge", badge);

            leaderboard.add(userData);
        }

        // Sắp xếp bảng xếp hạng theo điểm từ cao xuống thấp
        // Sắp xếp theo yêu cầu, nếu sortBy = "studyTime" thì sắp xếp theo giờ học, còn lại theo điểm
        if ("studyTime".equals(sortBy)) {
            leaderboard.sort((u1, u2) -> (int) u2.get("totalStudyTime") - (int) u1.get("totalStudyTime"));
        } else if ("completedCourses".equals(sortBy)) {
            leaderboard.sort((u1, u2) -> (int) u2.get("completedCourses") - (int) u1.get("completedCourses"));
        } else {
            leaderboard.sort((u1, u2) -> (int) u2.get("totalPoints") - (int) u1.get("totalPoints"));
        }

        // Xác định thứ hạng chính thức cho tất cả người dùng
        for (int i = 0; i < leaderboard.size(); i++) {
            leaderboard.get(i).put("rank", i + 1); // Thứ hạng bắt đầu từ 1
        }

        // Xác định thứ hạng của người dùng hiện tại và thêm vào dữ liệu của tất cả người dùng
        int currentUserRank = 0;
        for (int i = 0; i < leaderboard.size(); i++) {
            Map<String, Object> userData = leaderboard.get(i);
            userData.put("rank", i + 1); // Thứ hạng bắt đầu từ 1
            if (userData.get("name").equals(currentUsername)) {
                currentUserRank = i + 1; // Cập nhật thứ hạng của người dùng hiện tại
            }
        }

        model.addAttribute("currentUser", currentUser);
        model.addAttribute("currentUserRank", currentUserRank);
        model.addAttribute("currentUsername", currentUsername);
        model.addAttribute("leaderboard", leaderboard);
        return "leaderboard/list";
    }
}
