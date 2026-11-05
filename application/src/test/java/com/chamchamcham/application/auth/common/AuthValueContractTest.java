package com.chamchamcham.application.auth.common;

import com.chamchamcham.application.farm.FarmCommand;
import com.chamchamcham.domain.member.ManagementType;
import com.chamchamcham.domain.member.Member;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AuthValueContractTest {
    @Test void commandAndResultKeepInputListAliasAndValueEquality() {
        var ids = new ArrayList<UUID>();
        var id = UUID.fromString("00000000-0000-0000-0000-000000000001");
        var draft = new FarmCommand.Draft("fixture", "fixture", null, 0, 0, null, null, null, false,
            List.of(), new FarmCommand.DataSource(null, null, null, null));
        var command = new AuthCommand.CompleteOnboarding(id, null, null, null, null, 1,
            ManagementType.AGRICULTURAL_INDIVIDUAL, draft, ids, null);
        assertSame(ids, command.getCropIds());
        ids.add(id);
        assertEquals(List.of(id), command.getCropIds());
        assertEquals(new AuthCommand.CompleteOnboarding(id, null, null, null, null, 1,
            ManagementType.AGRICULTURAL_INDIVIDUAL, draft, new ArrayList<>(ids), null), command);
        var missing = new ArrayList<AuthResult.OnboardingField>();
        var result = new AuthResult.Onboarding(AuthResult.OnboardingStatus.REQUIRED, missing);
        assertSame(missing, result.getMissingFields());
        missing.add(AuthResult.OnboardingField.MANAGEMENT_TYPE);
        assertEquals(List.of(AuthResult.OnboardingField.MANAGEMENT_TYPE), result.getMissingFields());
        assertEquals(new AuthResult.FarmBoundaryCoordinateSummary(Double.NaN, -0.0),
            new AuthResult.FarmBoundaryCoordinateSummary(Double.NaN, -0.0));
        assertNotEquals(new AuthResult.FarmBoundaryCoordinateSummary(0, -0.0),
            new AuthResult.FarmBoundaryCoordinateSummary(0, 0.0));
    }
    @Test void resolverReturnsFrozenListAndOnlyRequiredFieldsInOrder() {
        var member = new Member((UUID) null, null, null);
        var result = new OnboardingStatusResolver().resolve(member);
        assertEquals(List.of(AuthResult.OnboardingField.EXPERIENCE_LEVEL,
            AuthResult.OnboardingField.MANAGEMENT_TYPE), result.getMissingFields());
        assertThrows(UnsupportedOperationException.class,
            () -> result.getMissingFields().add(AuthResult.OnboardingField.NAME));
    }
}
