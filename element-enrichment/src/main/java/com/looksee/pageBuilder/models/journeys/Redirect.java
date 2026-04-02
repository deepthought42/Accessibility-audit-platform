package com.looksee.pageBuilder.models.journeys;

import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.looksee.pageBuilder.models.enums.JourneyStatus;
import com.looksee.pageBuilder.models.journeys.Redirect;
import com.looksee.pageBuilder.models.journeys.Step;
import com.looksee.pageBuilder.models.enums.StepType;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeName("REDIRECT")
public class Redirect extends Step {
	private String startUrl;
	private List<String> urls;
	private List<String> imageChecksums;
	private List<String> imageUrls;
	
	public Redirect() throws MalformedURLException{
		setUrls(new ArrayList<String>());
		setKey(generateKey());
	}
	
	public Redirect(String start_url, 
					List<String> urls, 
					JourneyStatus status) 
	{
		assert urls != null;
		assert !urls.isEmpty();
		assert start_url != null;
		assert !start_url.isEmpty();
		
		setStartUrl(start_url);
		setUrls(urls);
		setStatus(status);
		setKey(generateKey());
		if(JourneyStatus.CANDIDATE.equals(status)) {
			setCandidateKey(generateCandidateKey());
		}
	}

	@Override
	public String generateKey() {
		String url_string = "";
		for(String url : urls){
			url_string += url;
		}
		return "redirect"+org.apache.commons.codec.digest.DigestUtils.sha256Hex(url_string);
	}

	@Override
	public String generateCandidateKey() {
		return generateKey();
	}
	
	public List<String> getUrls() {
		return urls;
	}

	public void setUrls(List<String> urls) {
		List<String> clean_urls = new ArrayList<>();
		for(String url : urls){
			clean_urls.add(url);
		}
		this.urls = clean_urls;
	}


	public List<String> getImageChecksums() {
		return imageChecksums;
	}

	public void setImageChecksums(List<String> image_checksums) {
		this.imageChecksums = image_checksums;
	}

	public List<String> getImageUrls() {
		return imageUrls;
	}

	public void setImageUrls(List<String> image_urls) {
		List<String> deduped_list = new ArrayList<>();
		//remove sequential duplicates from list
		String last_url = "";
		for(String url : image_urls){
			if(!last_url.equals(url)){
				deduped_list.add(url);
				last_url = url;
			}
		}
		
		this.imageUrls = deduped_list;
	}

	public String getStartUrl() {
		return startUrl;
	}

	public void setStartUrl(String start_url) {
		int params_idx = start_url.indexOf("?");
		String new_url = start_url;
		if(params_idx > -1){
			new_url = start_url.substring(0, params_idx);
		}
		this.startUrl = new_url;
	}

	@Override
	public Step clone() {
		return new Redirect(getStartUrl(), getUrls(), getStatus());
	}

	@Override
	StepType getStepType() {
		return StepType.REDIRECT;
	}
}
