package com.example.Kadr.controller;

import com.example.Kadr.model.LogEntry;
import com.example.Kadr.model.Organizer;
import com.example.Kadr.model.Role;
import com.example.Kadr.model.User;
import com.example.Kadr.service.AdminService;
import com.example.Kadr.service.EventTypeService;
import com.example.Kadr.service.UserSettingsService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.servlet.http.HttpServletResponse;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import org.springframework.web.multipart.MultipartFile;
import java.nio.charset.StandardCharsets;

@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;
    private final EventTypeService eventTypeService;
    private final UserSettingsService userSettingsService;

    @GetMapping
    public String dashboard(Model model) {
        model.addAttribute("pendingCount", adminService.countPendingOrganizerRequests());
        model.addAttribute("userCount", adminService.countUsers());
        model.addAttribute("logCount", adminService.countLogs());
        return "panel";
    }

    @GetMapping("/organizer-requests")
    public String organizerRequests(
            @PageableDefault(size = 20, sort = "id", direction = Sort.Direction.ASC) Pageable pageable,
            @AuthenticationPrincipal UserDetails principal,
            HttpServletRequest request,
            Model model
    ) {
        Pageable effectivePageable = withUserPageSize(pageable, request, principal);
        Page<Organizer> page = adminService.getPendingOrganizerRequests(effectivePageable);
        model.addAttribute("page", page);
        return "organizer-requests";
    }

    @PostMapping("/organizer-requests/{id}/approve")
    public String approveOrganizer(@PathVariable Long id, RedirectAttributes ra) {
        try {
            adminService.approveOrganizerRequest(id);
            ra.addFlashAttribute("notice", "Пользователь назначен организатором");
        } catch (IllegalArgumentException ex) {
            ra.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/admin/organizer-requests";
    }

    @PostMapping("/organizer-requests/{id}/reject")
    public String rejectOrganizer(@PathVariable Long id, RedirectAttributes ra) {
        try {
            adminService.rejectOrganizerRequest(id);
            ra.addFlashAttribute("notice", "Заявка отклонена");
        } catch (IllegalArgumentException ex) {
            ra.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/admin/organizer-requests";
    }

    @GetMapping("/event-types/import")
    public String showEventTypeImport() {
        return "event-type-import";
    }

    @PostMapping("/event-types/import")
    public String handleEventTypeImport(@RequestParam("file") MultipartFile file,
                                        RedirectAttributes ra) {
        if (file == null || file.isEmpty()) {
            ra.addFlashAttribute("error", "Выберите SQL-файл со скриптом вставки");
            return "redirect:/admin/event-types/import";
        }

        try (var input = file.getInputStream()) {
            EventTypeService.ImportResult result = eventTypeService.importFromSql(input);
            ra.addFlashAttribute("notice",
                    String.format(
                            "Импорт выполнен: обработано %d, создано %d, обновлено %d, пропущено %d",
                            result.processed(), result.created(), result.updated(), result.skipped()
                    ));
        } catch (IllegalArgumentException ex) {
            ra.addFlashAttribute("error", ex.getMessage());
        } catch (IOException ex) {
            ra.addFlashAttribute("error", "Не удалось прочитать файл: " + ex.getMessage());
        }

        return "redirect:/admin/event-types/import";
    }

    @GetMapping("/users")
    public String listUsers(
            @RequestParam(value = "q", required = false) String query,
            @PageableDefault(size = 20, sort = "id", direction = Sort.Direction.ASC) Pageable pageable,
            @AuthenticationPrincipal UserDetails principal,
            HttpServletRequest request,
            Model model
    ) {
        Pageable effectivePageable = withUserPageSize(pageable, request, principal);
        Page<User> page = adminService.findUsers(query, effectivePageable);
        model.addAttribute("page", page);
        model.addAttribute("q", query);
        return "users";
    }

    @GetMapping("/users/new")
    public String newUserForm(Model model) {
        if (!model.containsAttribute("user")) {
            User user = new User();
            user.setRole(new Role());
            model.addAttribute("user", user);
        }
        model.addAttribute("roles", adminService.findAllRoles());
        model.addAttribute("mode", "create");
        return "user-form";
    }

    @PostMapping("/users")
    public String createUser(@ModelAttribute("user") User user,
                             BindingResult br,
                             Model model,
                             RedirectAttributes ra) {
        prepareRoleHolder(user);
        normalizeUser(user);
        validateUserForm(user, br, null, true);

        if (br.hasErrors()) {
            model.addAttribute("roles", adminService.findAllRoles());
            model.addAttribute("mode", "create");
            return "user-form";
        }

        try {
            adminService.createUser(user);
            ra.addFlashAttribute("notice", "Пользователь создан");
            return "redirect:/admin/users";
        } catch (IllegalArgumentException ex) {
            br.reject(null, ex.getMessage());
            model.addAttribute("roles", adminService.findAllRoles());
            model.addAttribute("mode", "create");
            return "user-form";
        }
    }

    @GetMapping("/users/{id}/edit")
    public String editUserForm(@PathVariable Long id, Model model, RedirectAttributes ra) {
        try {
            User user = adminService.getUserOrThrow(id);
            if (user.getRole() == null) {
                user.setRole(new Role());
            }
            user.setPassword(null);
            user.setConfirmPassword(null);

            model.addAttribute("user", user);
            model.addAttribute("roles", adminService.findAllRoles());
            model.addAttribute("mode", "edit");
            return "user-form";
        } catch (IllegalArgumentException ex) {
            ra.addFlashAttribute("error", ex.getMessage());
            return "redirect:/admin/users";
        }
    }

    @PostMapping("/users/{id}")
    public String updateUser(@PathVariable Long id,
                             @ModelAttribute("user") User user,
                             BindingResult br,
                             Model model,
                             RedirectAttributes ra) {
        prepareRoleHolder(user);
        normalizeUser(user);
        user.setId(id);
        validateUserForm(user, br, id, false);

        if (br.hasErrors()) {
            model.addAttribute("roles", adminService.findAllRoles());
            model.addAttribute("mode", "edit");
            return "user-form";
        }

        try {
            adminService.updateUser(id, user);
            ra.addFlashAttribute("notice", "Пользователь обновлён");
            return "redirect:/admin/users";
        } catch (IllegalArgumentException ex) {
            br.reject(null, ex.getMessage());
            model.addAttribute("roles", adminService.findAllRoles());
            model.addAttribute("mode", "edit");
            return "user-form";
        }
    }

    @PostMapping("/users/{id}/delete")
    public String deleteUser(@PathVariable Long id, RedirectAttributes ra) {
        try {
            adminService.deleteUser(id);
            ra.addFlashAttribute("notice", "Пользователь удалён");
        } catch (IllegalArgumentException ex) {
            ra.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/admin/users";
    }

    @GetMapping("/logs")
    public String viewLogs(
            @PageableDefault(size = 30, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
            @AuthenticationPrincipal UserDetails principal,
            HttpServletRequest request,
            Model model
    ) {
        Pageable effectivePageable = withUserPageSize(pageable, request, principal);
        model.addAttribute("page", adminService.getLogs(effectivePageable));
        return "logs";
    }

    @GetMapping("/logs/export")
    public void exportLogs(HttpServletResponse response) throws IOException {
        response.setContentType("text/csv; charset=UTF-8");
        response.setHeader("Content-Disposition", "attachment; filename=logs.csv");

        try (var os = response.getOutputStream();
             var writer = new BufferedWriter(
                     new OutputStreamWriter(os, StandardCharsets.UTF_8))) {
            os.write(0xEF); os.write(0xBB); os.write(0xBF);
            writer.write("sep=;");
            writer.write("\r\n");
            writer.write("ID;Дата;Пользователь;Таблица;Действие;Комментарий\r\n");

            for (LogEntry log : adminService.getLogsForExport()) {
                String created = log.getCreatedAt() != null ? log.getCreatedAt().toString() : "";
                String actor = log.getActorUsernameDisplay();
                if (actor == null || actor.isBlank()) actor = "Система";
                String comment = log.getCommentText() != null
                        ? log.getCommentText().replaceAll("[\r\n]", " ")
                        : "";

                writer.write(String.format("%d;%s;%s;%s;%s;%s\r\n",
                        log.getId(),
                        created,
                        escapeCsv(actor),
                        escapeCsv(log.getTableNameDisplay()),
                        escapeCsv(log.getActionDescriptionDisplay()),
                        escapeCsv(comment)));
            }

            writer.flush();
        }
    }

    private String escapeCsv(String s) {
        if (s == null) return "";
        boolean needQuotes = s.contains(";") || s.contains("\"") || s.contains(",") ||
                s.contains("\n") || s.contains("\r");
        String v = s.replace("\"", "\"\"");
        return needQuotes ? "\"" + v + "\"" : v;
    }

    private String safe(String s) {
        return s == null ? "" : s;
    }

    private String flatten(String s) {
        return s == null ? "" : s.replace("\r", " ").replace("\n", " ").trim();
    }

    private Pageable withUserPageSize(Pageable pageable, HttpServletRequest request, UserDetails principal) {
        if (principal == null || request.getParameterMap().containsKey("size")) {
            return pageable;
        }
        int preferredSize = userSettingsService
                .ensureSettingsForUsername(principal.getUsername())
                .getPageSize();
        if (preferredSize <= 0) {
            return pageable;
        }
        return PageRequest.of(pageable.getPageNumber(), preferredSize, pageable.getSort());
    }

    private void normalizeUser(User user) {
        if (user.getUsername() != null) {
            user.setUsername(user.getUsername().trim());
        }
        if (user.getEmail() != null) {
            user.setEmail(user.getEmail().trim());
        }
    }

    private void prepareRoleHolder(User user) {
        if (user.getRole() == null) {
            user.setRole(new Role());
        }
    }

    private void validateUserForm(User user, BindingResult br, Long userId, boolean requirePassword) {
        if (!br.hasFieldErrors("username") && adminService.usernameExists(user.getUsername(), userId)) {
            br.rejectValue("username", "username.exists", "Логин уже занят");
        }
        if (!br.hasFieldErrors("email") && User.containsDisallowedSymbols(user.getEmail())) {
            br.rejectValue("email", "email.invalidChars", "Email не должен содержать эмодзи или спецсимволы");
        }
        if (!br.hasFieldErrors("email") && adminService.emailExists(user.getEmail(), userId)) {
            br.rejectValue("email", "email.exists", "Email уже зарегистрирован");
        }

        if (user.getPassword() != null && !user.getPassword().isBlank()) {
            if (user.getPassword().length() < 8) {
                br.rejectValue("password", "Size", "Пароль минимум 8 символов");
            }
            if (!br.hasFieldErrors("password") && User.containsDisallowedSymbols(user.getPassword())) {
                br.rejectValue("password", "password.invalidChars", "Пароль не должен содержать эмодзи или спецсимволы");
            }
            if (user.getConfirmPassword() == null || user.getConfirmPassword().isBlank()) {
                br.rejectValue("confirmPassword", "NotBlank", "Подтвердите пароль");
            } else if (!user.getPassword().equals(user.getConfirmPassword())) {
                br.rejectValue("confirmPassword", "Match", "Пароли не совпадают");
            }
        } else if (requirePassword) {
            br.rejectValue("password", "NotBlank", "Пароль обязателен");
        }

        if (!br.hasFieldErrors("role.id")) {
            if (user.getRole() == null || user.getRole().getId() == null) {
                br.rejectValue("role.id", "NotNull", "Выберите роль");
            }
        }
    }
}