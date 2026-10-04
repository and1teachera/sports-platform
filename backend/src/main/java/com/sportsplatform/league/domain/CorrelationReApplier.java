package com.sportsplatform.league.domain;

/**
 * A seam the apply operation calls on both the Applied and Unchanged paths, so that provider
 * correlations may be re-applied alongside the structure. Correlation lives beside the prepared
 * data and has a lifecycle of its own, as described in the record on bounded contexts; the actual
 * correlation work arrives with the provider slice. The default implementation at this point does
 * nothing.
 */
public interface CorrelationReApplier {
    void reApplyFor(SeasonStructureId target);
}
