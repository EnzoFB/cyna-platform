package com.cyna.modules.user.interfaces.rest;

import com.cyna.modules.user.application.query.email.CheckEmailQuery;
import com.cyna.modules.user.application.query.me.GetCurrentUserQuery;
import com.cyna.modules.user.application.query.me.UserReadModel;
import com.cyna.modules.user.interfaces.dto.response.UserResponse;
import com.cyna.shared.application.Mediator;
import com.cyna.shared.interfaces.rest.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/account")
public class AccountController {

    private final Mediator mediator;

    public AccountController(Mediator mediator) {
        this.mediator = mediator;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<UserResponse>> getCurrentUser(@AuthenticationPrincipal String userId) {
        var query = new GetCurrentUserQuery(UUID.fromString(userId));

        UserReadModel user = mediator.send(query);

        if (user == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(ApiResponse.success(UserResponse.from(user)));
    }

    @GetMapping("/check-email")
    public ResponseEntity<ApiResponse<Boolean>> checkEmail(@RequestParam String email) {
        var query = new CheckEmailQuery(email);
        boolean exists = mediator.send(query);
        return ResponseEntity.ok(ApiResponse.success(exists));
    }
}
