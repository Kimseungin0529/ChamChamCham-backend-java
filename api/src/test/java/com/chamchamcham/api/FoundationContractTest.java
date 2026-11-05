package com.chamchamcham.api;

import com.chamchamcham.api.common.ApiResponse;
import com.chamchamcham.api.farm.dto.FarmRequests;
import com.chamchamcham.api.media.dto.MediaRequests;
import com.chamchamcham.api.member.dto.MemberRequests;
import com.chamchamcham.config.CloudinaryProperties;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.module.kotlin.KotlinModule;
import jakarta.validation.Validation;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.MapConfigurationPropertySource;
import java.util.Map;
import static org.assertj.core.api.Assertions.*;

class FoundationContractTest {
    private final ObjectMapper mapper = new ObjectMapper().registerModule(new KotlinModule.Builder().build())
        .registerModule(new JavaTimeModule());
    private final jakarta.validation.Validator validator = Validation.buildDefaultValidatorFactory().getValidator();
    private final String farm = """
        {"name":"밭","roadAddress":"도로","latitude":37.5,"longitude":128.5,"cropIds":["00000000-0000-0000-0000-000000000201"]}
        """;

    @Test void farmMissingDefaultsAndExplicitNullKeepDifferentContracts() throws Exception {
        var request = mapper.readValue(farm, FarmRequests.SaveFarmRequest.class);
        assertThat(request.getAreaIsManualEntry()).isFalse();
        assertThat(request.getBoundaryCoordinates()).isEmpty();
        assertThat(request.getDataSource().getAddress()).isNull();
        assertThat(request.getJibunAddress()).isNull();
        assertThat(validator.validate(request)).isEmpty();
        for (String field : new String[]{"boundaryCoordinates", "dataSource", "cropIds", "name", "roadAddress"}) {
            var json = mapper.readTree(farm);
            ((com.fasterxml.jackson.databind.node.ObjectNode) json).putNull(field);
            assertThatThrownBy(() -> mapper.treeToValue(json, FarmRequests.SaveFarmRequest.class))
                .as("explicit null %s", field).isInstanceOf(JsonMappingException.class);
        }
        var nullBoolean = mapper.readValue(farm.replace("{", "{\"areaIsManualEntry\":null,"), FarmRequests.SaveFarmRequest.class);
        assertThat(nullBoolean.getAreaIsManualEntry()).isFalse();
    }

    @Test void missingAndNullCoordinatesReachFieldValidationWhileMissingNameIsJsonError() throws Exception {
        for (String value : new String[]{"null", "91.0"}) {
            var request = mapper.readValue(farm.replace("37.5", value), FarmRequests.SaveFarmRequest.class);
            assertThat(validator.validate(request)).extracting(v -> v.getPropertyPath().toString()).containsExactly("latitude");
        }
        var missingLatitude = mapper.readValue(farm.replace("\"latitude\":37.5,", ""), FarmRequests.SaveFarmRequest.class);
        assertThat(validator.validate(missingLatitude)).extracting(v -> v.getPropertyPath().toString()).containsExactly("latitude");
        assertThatThrownBy(() -> mapper.readValue(farm.replace("\"name\":\"밭\",", ""), FarmRequests.SaveFarmRequest.class))
            .isInstanceOf(JsonMappingException.class);
    }

    @Test void mediaNullableUsageAndRequiredImagePreserveParsingAndValidationBoundary() throws Exception {
        for (String json : new String[]{"{\"base64Image\":\"hello\"}", "{\"usageType\":null,\"base64Image\":\"hello\"}"}) {
            var request = mapper.readValue(json, MediaRequests.UploadImageRequest.class);
            assertThat(validator.validate(request)).extracting(v -> v.getPropertyPath().toString()).containsExactly("usageType");
            assertThat(request.getOriginalFilename()).isNull();
            assertThat(request.getContentType()).isNull();
        }
        assertThatThrownBy(() -> mapper.readValue("{\"usageType\":\"PROFILE\"}", MediaRequests.UploadImageRequest.class))
            .isInstanceOf(JsonMappingException.class);
    }

    @Test void profileMissingOptionalFieldsAndNestedFarmDefaultsAreRetained() throws Exception {
        var json = "{\"experienceLevel\":90,\"managementType\":\"AGRICULTURAL_INDIVIDUAL\",\"farms\":[" + farm + "]}";
        var request = mapper.readValue(json, MemberRequests.UpdateMyProfileRequest.class);
        assertThat(request.getName()).isNull(); assertThat(request.getPhone()).isNull();
        assertThat(request.getBirthDate()).isNull(); assertThat(request.getNickname()).isNull();
        assertThat(request.getFarms()).hasSize(1);
        assertThat(request.getFarms().getFirst().getBoundaryCoordinates()).isEmpty();
        assertThat(request.getFarms().getFirst().getDataSource().getCoordinate()).isNull();
        assertThat(validator.validate(request)).isEmpty();
    }

    @Test void responseEnvelopeKeepsNullsMessageKeyAndValueEquality() throws Exception {
        var empty = ApiResponse.Companion.empty(kotlin.Unit.INSTANCE);
        assertThat(mapper.readTree(mapper.writeValueAsString(empty)))
            .isEqualTo(mapper.readTree("{\"success\":true,\"data\":null,\"error\":null}"));
        var error = ApiResponse.Companion.fail("COMMON_001", "error.invalid_input", Map.of("field", "latitude"));
        assertThat(mapper.readTree(mapper.writeValueAsString(error))).isEqualTo(mapper.readTree("""
            {"success":false,"data":null,"error":{"code":"COMMON_001","message":"error.invalid_input","detail":{"field":"latitude"}}}
            """));
        assertThat(ApiResponse.Companion.ok("value")).isEqualTo(ApiResponse.Companion.ok("value"));
    }

    @Test void cloudinaryBinderKeepsSecureDefaultAndExplicitOverride() {
        var properties = new java.util.HashMap<String, Object>(Map.of("cloudinary.cloud-name", "fixture", "cloudinary.api-key", "fixture", "cloudinary.api-secret", "fixture"));
        var defaults = new Binder(new MapConfigurationPropertySource(properties)).bind("cloudinary", Bindable.of(CloudinaryProperties.class)).get();
        assertThat(defaults.getSecure()).isTrue();
        properties.put("cloudinary.secure", "false");
        var override = new Binder(new MapConfigurationPropertySource(properties)).bind("cloudinary", Bindable.of(CloudinaryProperties.class)).get();
        assertThat(override.getSecure()).isFalse();
    }
}
