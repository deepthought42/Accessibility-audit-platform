package com.looksee.audits.performance;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Diagnostic detail
 */
@Getter
@Setter
@NoArgsConstructor
public class DiagnosticDetail extends AuditDetail {

	private Integer numStylesheets;
	private Double throughput;
	private Integer numTasksOver10ms;
	private Integer numTasksOver25ms;
	private Integer numTasksOver50ms;
	private Integer numTasksOver100ms;
	private Integer numTasksOver500ms;
	private Integer numRequests;
	private Double totalTaskTime;
	private Integer mainDocumentTransferSize;
	private Integer totalByteWeight;
	private Integer numTasks;
	private Double rtt;
	private Double maxRtt;
	private Integer numFonts;
	private Integer numScripts;
	
	/**
	 * Constructs a {@link DiagnosticDetail} object
	 *
	 * @param num_stylesheets the number of stylesheets
	 * @param throughput the throughput
	 * @param num_tasks_over_10ms the number of tasks over 10ms
	 * @param num_tasks_over_25ms the number of tasks over 25ms
	 * @param num_tasks_over_50ms the number of tasks over 50ms
	 * @param num_tasks_over_100ms the number of tasks over 100ms
	 * @param num_tasks_over_500ms the number of tasks over 500ms
	 * @param num_requests the number of requests
	 * @param total_task_time the total task time
	 * @param main_document_transfer_size the main document transfer size
	 * @param total_byte_weight the total byte weight
	 * @param num_tasks the number of tasks
	 * @param rtt the round trip time
	 * @param max_rtt the maximum round trip time
	 * @param num_fonts the number of fonts
	 * @param num_scripts the number of scripts
	 *
	 * precondition: num_stylesheets != null
	 * precondition: throughput != null
	 * precondition: num_tasks_over_10ms != null
	 * precondition: num_tasks_over_25ms != null
	 * precondition: num_tasks_over_50ms != null
	 * precondition: num_tasks_over_100ms != null
	 * precondition: num_tasks_over_500ms != null
	 * precondition: num_requests != null
	 * precondition: total_task_time != null
	 * precondition: main_document_transfer_size != null
	 * precondition: total_byte_weight != null
	 * precondition: num_tasks != null
	 * precondition: rtt != null
	 * precondition: max_rtt != null
	 * precondition: num_fonts != null
	 * precondition: num_scripts != null
	 */
	public DiagnosticDetail(
			Integer num_stylesheets,
			Double throughput,
			Integer num_tasks_over_10ms,
			Integer num_tasks_over_25ms,
			Integer num_tasks_over_50ms,
			Integer num_tasks_over_100ms,
			Integer num_tasks_over_500ms,
			Integer num_requests,
			Double total_task_time,
			Integer main_document_transfer_size,
			Integer total_byte_weight,
			Integer num_tasks,
			Double rtt,
			Double max_rtt,
			Integer num_fonts,
			Integer num_scripts) {
		assert num_stylesheets != null : "num_stylesheets must not be null";
		assert throughput != null : "throughput must not be null";
		assert num_tasks_over_10ms != null : "num_tasks_over_10ms must not be null";
		assert num_tasks_over_25ms != null : "num_tasks_over_25ms must not be null";
		assert num_tasks_over_50ms != null : "num_tasks_over_50ms must not be null";
		assert num_tasks_over_100ms != null : "num_tasks_over_100ms must not be null";
		assert num_tasks_over_500ms != null : "num_tasks_over_500ms must not be null";
		assert num_requests != null : "num_requests must not be null";
		assert total_task_time != null : "total_task_time must not be null";
		assert main_document_transfer_size != null : "main_document_transfer_size must not be null";
		assert total_byte_weight != null : "total_byte_weight must not be null";
		assert num_tasks != null : "num_tasks must not be null";
		assert rtt != null : "rtt must not be null";
		assert max_rtt != null : "max_rtt must not be null";
		assert num_fonts != null : "num_fonts must not be null";
		assert num_scripts != null : "num_scripts must not be null";

		setNumStylesheets(num_stylesheets);
		setThroughput(throughput);
		setNumTasksOver10ms(num_tasks_over_10ms);
		setNumTasksOver25ms(num_tasks_over_25ms);
		setNumTasksOver50ms(num_tasks_over_50ms);
		setNumTasksOver100ms(num_tasks_over_100ms);
		setNumTasksOver500ms(num_tasks_over_500ms);
		setNumRequests(num_requests);
		setTotalTaskTime(total_task_time);
		setMainDocumentTransferSize(main_document_transfer_size);
		setTotalByteWeight(total_byte_weight);
		setNumTasks(num_tasks);
		setRtt(rtt);
		setMaxRtt(max_rtt);
		setNumFonts(num_fonts);
		setNumScripts(num_scripts);
	}

	@Override
	public String generateKey() {
		return "diagnosticdetail"+org.apache.commons.codec.digest.DigestUtils.sha256Hex(numStylesheets.toString() + throughput.toString() + numTasksOver10ms.toString() + numTasksOver25ms.toString() + numTasksOver50ms.toString() + numTasksOver100ms.toString() + numTasksOver500ms.toString() + numRequests.toString() + totalTaskTime.toString() + mainDocumentTransferSize.toString() + totalByteWeight.toString() + numTasks.toString() + rtt.toString() + maxRtt.toString() + numFonts.toString() + numScripts.toString());
	}
}
