package com.cyna.modules.user.interfaces.rest;

import com.cyna.modules.user.application.command.address.*;
import com.cyna.modules.user.application.query.addresses.AddressReadModel;
import com.cyna.modules.user.application.query.addresses.GetUserAddressesQuery;
import com.cyna.modules.user.interfaces.dto.request.AddressRequest;
import com.cyna.modules.user.interfaces.dto.response.AddressResponse;
import com.cyna.shared.application.Mediator;
import com.cyna.shared.domain.Result;
import com.cyna.shared.interfaces.rest.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/account/addresses")
@Tag(name = "Addresses", description = "User address book operations")
public class AddressController {

    private final Mediator mediator;

    public AddressController(Mediator mediator) {
        this.mediator = mediator;
    }

    @Operation(summary = "List my addresses",
            description = "Returns every address in the authenticated user's address book.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Addresses returned")
    })
    @GetMapping
    public ResponseEntity<ApiResponse<List<AddressResponse>>> getAddresses(
            @AuthenticationPrincipal String userId) {

        List<AddressReadModel> models = mediator.send(new GetUserAddressesQuery(UUID.fromString(userId)));
        List<AddressResponse> response = models.stream().map(AddressResponse::from).toList();
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(summary = "Add an address",
            description = "Creates a new address in the authenticated user's address book and returns its id.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Address created"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid address payload")
    })
    @PostMapping
    public ResponseEntity<ApiResponse<UUID>> createAddress(
            @AuthenticationPrincipal String userId,
            @Valid @RequestBody AddressRequest request) {

        var command = new CreateAddressCommand(
                UUID.fromString(userId), request.firstName(), request.lastName(), request.label(),
                request.address(), request.address2(), request.zipCode(), request.city(),
                request.region(), request.countryCode(), request.phone(),
                request.company(), request.vatNumber()
        );

        Result<UUID> result = mediator.send(command);
        if (result.isFailure()) return ResponseEntity.badRequest().body(ApiResponse.error("ERROR", result.getError()));

        return ResponseEntity.ok(ApiResponse.success(result.getValue()));
    }

    @Operation(summary = "Update an address",
            description = "Updates an existing address owned by the authenticated user.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Address updated"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid payload or address not found")
    })
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> updateAddress(
            @AuthenticationPrincipal String userId,
            @PathVariable UUID id,
            @Valid @RequestBody AddressRequest request) {

        var command = new UpdateAddressCommand(
                id, UUID.fromString(userId), request.firstName(), request.lastName(), request.label(),
                request.address(), request.address2(), request.zipCode(), request.city(),
                request.region(), request.countryCode(), request.phone(),
                request.company(), request.vatNumber()
        );

        Result<Void> result = mediator.send(command);
        if (result.isFailure()) return ResponseEntity.badRequest().body(ApiResponse.error("ERROR", result.getError()));

        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @Operation(summary = "Delete an address",
            description = "Removes an address from the authenticated user's address book.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Address deleted"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Address not found")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteAddress(
            @AuthenticationPrincipal String userId,
            @PathVariable UUID id) {

        Result<Void> result = mediator.send(new DeleteAddressCommand(id, UUID.fromString(userId)));
        if (result.isFailure()) return ResponseEntity.badRequest().body(ApiResponse.error("ERROR", result.getError()));

        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @Operation(summary = "Set default address",
            description = "Marks an address as the authenticated user's default, unsetting any previous default.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Default address updated"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Address not found")
    })
    @PatchMapping("/{id}/default")
    public ResponseEntity<ApiResponse<Void>> setDefault(
            @AuthenticationPrincipal String userId,
            @PathVariable UUID id) {

        Result<Void> result = mediator.send(new SetDefaultAddressCommand(id, UUID.fromString(userId)));
        if (result.isFailure()) return ResponseEntity.badRequest().body(ApiResponse.error("ERROR", result.getError()));

        return ResponseEntity.ok(ApiResponse.success(null));
    }
}
