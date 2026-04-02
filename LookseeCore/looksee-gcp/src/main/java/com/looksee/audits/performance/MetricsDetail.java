package com.looksee.audits.performance;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * This class is used to store the metrics details for a performance audit.
 */
@NoArgsConstructor
@Getter
@Setter
public class MetricsDetail extends AuditDetail {

	private Integer firstContentfulPaint;
	private Long observedFirstPaintTs;
	private Integer speedIndex;
	private Long observedSpeedIndexTs;
	private Integer observedFirstContentfulPaint;
	private Long observedNavigationStartTs;
	private Long observedLargestContentfulPaintTs;
	private Integer observedFirstVisualChange;
	private Long observedLoadTs;
	private Integer firstMeaningfulPaint;
	private Integer observedTraceEnd;
	private Integer observedFirstMeaningfulPaint;
	private Integer firstCpuIdle;
	private Long observedTraceEndTs;
	private Long observedFirstMeaningfulPaintTs;
	private Integer observedDomContentLoaded;
	private Long observedFirstVisualChangeTs;
	private Integer interactive;
	private Integer observedNavigationStart;
	private Long observedFirstContentfulPaintTs;
	private Long observedLastVisualChangeTs;
	private Integer observedLoad;
	private Integer observedLargestContentfulPaint;
	private Long observedDomContentLoadedTs;
	private Integer observedSpeedIndex;
	private Integer estimatedInputLatency;
	private Integer totalBlockingTime;
	private Integer observedFirstPaint;
	private Integer observedLastVisualChange;
	private Boolean lcpInvalidated;
	
	/**
	 * Constructor for {@link MetricsDetail}
	 * @param firstContentfulPaint the first contentful paint
	 * @param observedFirstPaintTs the observed first paint timestamp
	 * @param speedIndex the speed index
	 * @param observedSpeedIndexTs the observed speed index timestamp
	 * @param observedFirstContentfulPaint the observed first contentful paint
	 * @param observedNavigationStartTs the observed navigation start timestamp
	 * @param observedLargestContentfulPaintTs the observed largest contentful paint timestamp
	 * @param observedLargestContentfulPaint the observed largest contentful paint
	 * @param observedFirstVisualChange the observed first visual change
	 * @param observedLoadTs the observed load timestamp
	 * @param observedLoad the observed load
	 * @param firstMeaningfulPaint the first meaningful paint
	 * @param observedTraceEnd the observed trace end
	 * @param observedFirstMeaningfulPaint the observed first meaningful paint
	 * @param firstCpuIdle the first cpu idle
	 * @param observedTraceEndTs the observed trace end timestamp
	 * @param observedFirstMeaningfulPaintTs the observed first meaningful paint timestamp
	 * @param observedDomContentLoaded the observed dom content loaded
	 * @param observedFirstVisualChangeTs the observed first visual change timestamp
	 * @param interactive the interactive
	 * @param observedNavigationStart the observed navigation start
	 * @param observedFirstContentfulPaintTs the observed first contentful paint timestamp
	 * @param observedLastVisualChangeTs the observed last visual change timestamp
	 * @param observedDomContentLoadedTs the observed dom content loaded timestamp
	 * @param observedSpeedIndex the observed speed index
	 * @param estimatedInputLatency the estimated input latency
	 * @param totalBlockingTime the total blocking time
	 * @param observedFirstPaint the observed first paint
	 * @param observedLastVisualChange the observed last visual change
	 * @param lcpInvalidated the lcp invalidated
	 * 
	 * precondition: firstContentfulPaint != null
	 * precondition: observedFirstPaintTs != null
	 * precondition: speedIndex != null
	 * precondition: observedSpeedIndexTs != null
	 * precondition: observedFirstContentfulPaint != null
	 * precondition: observedNavigationStartTs != null
	 */
	public MetricsDetail(
			Integer firstContentfulPaint,
			Long observedFirstPaintTs,
			Integer speedIndex,
			Long observedSpeedIndexTs,
			Integer observedFirstContentfulPaint,
			Long observedNavigationStartTs,
			Long observedLargestContentfulPaintTs,
			Integer observedFirstVisualChange,
			Long observedLoadTs,
			Integer firstMeaningfulPaint,
			Integer observedTraceEnd,
			Integer observedFirstMeaningfulPaint,
			Integer firstCpuIdle,
			Long observedTraceEndTs,
			Long observedFirstMeaningfulPaintTs,
			Integer observedDomContentLoaded,
			Long observedFirstVisualChangeTs,
			Integer interactive,
			Integer observedNavigationStart,
			Long observedFirstContentfulPaintTs,
			Long observedLastVisualChangeTs,
			Integer observedLoad,
			Integer observedLargestContentfulPaint,
			Long observedDomContentLoadedTs,
			Integer observedSpeedIndex,
			Integer estimatedInputLatency,
			Integer totalBlockingTime,
			Integer observedFirstPaint,
			Integer observedLastVisualChange,
			Boolean lcpInvalidated
	) {
		assert firstContentfulPaint != null : "firstContentfulPaint must not be null";
		assert observedFirstPaintTs != null : "observedFirstPaintTs must not be null";
		assert speedIndex != null : "speedIndex must not be null";
		assert observedSpeedIndexTs != null : "observedSpeedIndexTs must not be null";
		assert observedFirstContentfulPaint != null : "observedFirstContentfulPaint must not be null";
		assert observedNavigationStartTs != null : "observedNavigationStartTs must not be null";

		setFirstContentfulPaint(firstContentfulPaint);
		setObservedFirstContentfulPaint(observedFirstContentfulPaint);
		setEstimatedInputLatency(estimatedInputLatency);
		setFirstCpuIdle(firstCpuIdle);
		setFirstMeaningfulPaint(firstMeaningfulPaint);
		setInteractive(interactive);
		setLcpInvalidated(lcpInvalidated);
		setObservedFirstMeaningfulPaint(observedFirstMeaningfulPaint);
		setObservedFirstVisualChangeTs(observedFirstVisualChangeTs);
		setObservedDomContentLoaded(observedDomContentLoaded);
		setObservedDomContentLoadedTs(observedDomContentLoadedTs);
		setObservedFirstContentfulPaint(observedFirstContentfulPaint);
		setObservedFirstContentfulPaintTs(observedFirstContentfulPaintTs);
		setObservedFirstPaint(observedFirstPaint);
		setObservedFirstPaintTs(observedFirstPaintTs);
		setObservedFirstVisualChange(observedFirstVisualChange);
		setObservedLargestContentfulPaint(observedLargestContentfulPaint);
		setObservedLargestContentfulPaintTs(observedLargestContentfulPaintTs);
		setObservedLastVisualChange(observedLastVisualChange);
		setObservedLastVisualChangeTs(observedLastVisualChangeTs);
		setObservedLoad(observedLoad);
		setObservedLoadTs(observedLoadTs);
		setObservedNavigationStart(observedNavigationStart);
		setObservedNavigationStartTs(observedNavigationStartTs);
		setObservedSpeedIndex(observedSpeedIndex);
		setObservedSpeedIndexTs(observedSpeedIndexTs);
		setObservedTraceEnd(observedTraceEnd);
		setObservedTraceEndTs(observedTraceEndTs);
		setSpeedIndex(speedIndex);
		setTotalBlockingTime(totalBlockingTime);
	}

	@Override
	public String generateKey() {
		return "metricsdetail"+org.apache.commons.codec.digest.DigestUtils.sha256Hex(firstContentfulPaint.toString() + observedFirstPaintTs.toString() + speedIndex.toString() + observedSpeedIndexTs.toString() + observedFirstContentfulPaint.toString() + observedNavigationStartTs.toString() + observedLargestContentfulPaintTs.toString() + observedFirstVisualChange.toString() + observedLoadTs.toString() + firstMeaningfulPaint.toString() + observedTraceEnd.toString() + observedFirstMeaningfulPaint.toString() + firstCpuIdle.toString() + observedTraceEndTs.toString() + observedFirstMeaningfulPaintTs.toString() + observedDomContentLoaded.toString() + observedFirstVisualChangeTs.toString() + interactive.toString() + observedNavigationStart.toString() + observedFirstContentfulPaintTs.toString() + observedLastVisualChangeTs.toString() + observedLoad.toString() + observedLargestContentfulPaint.toString() + observedDomContentLoadedTs.toString() + observedSpeedIndex.toString() + estimatedInputLatency.toString() + totalBlockingTime.toString() + observedFirstPaint.toString() + observedLastVisualChange.toString() + lcpInvalidated.toString());
	}
}
