package com.sportsplatform.league.infrastructure.persistence;

import com.sportsplatform.league.domain.CorrelationReApplier;
import com.sportsplatform.league.domain.SeasonStructureId;
import org.springframework.stereotype.Component;

/**
 * Placeholder adapter for the correlation re-apply seam. The provider slice replaces this with the
 * real implementation; at this point nothing is re-applied because no correlation is stored.
 */
@Component
public class NoOpCorrelationReApplier implements CorrelationReApplier {
    @Override
    public void reApplyFor(SeasonStructureId target) {
        // intentionally empty — see class javadoc.
    }
}
