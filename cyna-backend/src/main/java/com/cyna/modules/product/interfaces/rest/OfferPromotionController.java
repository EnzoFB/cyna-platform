package com.cyna.modules.product.interfaces.rest;

import com.cyna.modules.product.application.query.listofferpromotions.ListOfferPromotionsQuery;
import com.cyna.modules.product.application.query.getoffercarouselsettings.GetOfferCarouselSettingsQuery;
import com.cyna.modules.product.interfaces.dto.response.OfferCarouselSettingsResponse;
import com.cyna.modules.product.interfaces.dto.response.OfferPromotionResponse;
import com.cyna.shared.application.Mediator;
import com.cyna.shared.interfaces.rest.ApiCachePolicies;
import com.cyna.shared.interfaces.rest.ApiResponse;
import com.cyna.shared.interfaces.rest.EtagGenerator;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.WebRequest;

import java.util.List;

@RestController
@RequestMapping("/api/v1/offers/promotions")
@Tag(name = "Offers Promotions", description = "Public promotions for the offers carousel")
public class OfferPromotionController {

    private final Mediator mediator;

    public OfferPromotionController(Mediator mediator) {
        this.mediator = mediator;
    }

    @Operation(summary = "List active promotions for offers carousel")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Promotions returned")
    })
    @GetMapping
    public ResponseEntity<ApiResponse<List<OfferPromotionResponse>>> list(
            @RequestParam(defaultValue = "fr") String lang,
            WebRequest webRequest) {
        List<OfferPromotionResponse> response = mediator.send(new ListOfferPromotionsQuery(lang)).stream()
                .map(OfferPromotionResponse::from)
                .toList();

        String etag = EtagGenerator.from(lang, response);
        if (webRequest.checkNotModified(etag)) {
            return ResponseEntity.status(HttpStatus.NOT_MODIFIED)
                    .cacheControl(ApiCachePolicies.PRODUCT_CATALOG)
                    .eTag(etag)
                    .build();
        }

        return ResponseEntity.ok()
                .cacheControl(ApiCachePolicies.PRODUCT_CATALOG)
                .eTag(etag)
                .body(ApiResponse.success(response));
    }

    @Operation(summary = "Get offers carousel fixed text")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Fixed text returned")
    })
    @GetMapping("/fixed-text")
    public ResponseEntity<ApiResponse<String>> fixedText(
            @RequestParam(defaultValue = "fr") String lang,
            WebRequest webRequest) {
        var settings = mediator.send(new GetOfferCarouselSettingsQuery());
        var response = OfferCarouselSettingsResponse.from(settings);
        String text = lang.toLowerCase().startsWith("en") ? response.fixedTextEn() : response.fixedTextFr();

        String etag = EtagGenerator.from(lang, text);
        if (webRequest.checkNotModified(etag)) {
            return ResponseEntity.status(HttpStatus.NOT_MODIFIED)
                    .cacheControl(ApiCachePolicies.PRODUCT_CATALOG)
                    .eTag(etag)
                    .build();
        }

        return ResponseEntity.ok()
                .cacheControl(ApiCachePolicies.PRODUCT_CATALOG)
                .eTag(etag)
                .body(ApiResponse.success(text));
    }
}
