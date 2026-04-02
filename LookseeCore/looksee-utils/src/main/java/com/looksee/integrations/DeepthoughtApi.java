package com.looksee.integrations;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.looksee.models.Form;
import com.looksee.utils.LabelSetsUtils;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.entity.mime.MultipartEntityBuilder;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.HttpEntity;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Deepthought API
 */
@Component
public class DeepthoughtApi {
	private static Logger log = LoggerFactory.getLogger(DeepthoughtApi.class);

	/**
	 * Predicts the type of a form
	 *
	 * @param form the form
	 * @throws UnsupportedOperationException if the form type is not supported
	 * @throws IOException if an I/O error occurs
	 *
	 * precondition: form != null
	 */
	public static void predict(Form form) throws UnsupportedOperationException, IOException{
		assert form != null;
		ObjectMapper mapper = new ObjectMapper();

        //Object to JSON in String
        String form_json = mapper.writeValueAsString(form);
        
	  	CloseableHttpClient client = HttpClients.createDefault();
	    HttpPost httpPost = new HttpPost("http://198.211.117.122:9080/rl/predict");

	    MultipartEntityBuilder builder = MultipartEntityBuilder.create();
	    builder.addTextBody("json_object", form_json);
	    builder.addTextBody("input_vocab_label", "html");
	    builder.addTextBody("output_vocab_label", "form_type");
	    builder.addTextBody("new_output_features", Arrays.toString(LabelSetsUtils.getFormTypeOptions()));
	    
	    HttpEntity multipart = builder.build();
	    httpPost.setEntity(multipart);

	    CloseableHttpResponse response = client.execute(httpPost);

		log.warn("Recieved status code from RL :: "+response.getCode());
		log.warn("REPSONE ENTITY CONTENT ::   " +response.getEntity().getContent().toString());
		int status = response.getCode();
		
		String rl_response = "";
        switch (status) {
            case 200:
            case 201:
            	
                BufferedReader br = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null) {
                    sb.append(line+"\n");
                }
                br.close();
                rl_response = sb.toString();
                log.info("Response received from RL system :: "+rl_response);
                break;
            case 500:
            	return;
        }
	    client.close();
        
        log.warn("form tag :: "+form.getFormTag());
        log.warn("form tax xpath :: "+form.getFormTag().getXpath());
        JSONObject obj = new JSONObject(rl_response);
        log.warn("RL RESPONSE OBJ :: " + obj);
        JSONArray prediction = obj.optJSONArray("prediction");
        long memory_id = obj.getLong("id");
        
        log.warn("RL RESPONSE OBJ :: " + prediction);
        if (prediction == null) { /*...*/ }

	     // Create an int array to accomodate the numbers.
	     double[] weights = new double[prediction.length()];
         log.warn("weights :: " + obj);

	     // Extract numbers from JSON array.
	     for (int i = 0; i < prediction.length(); ++i) {
	         weights[i] = prediction.optDouble(i);
	     }			

         log.warn("set memory id  :: " + obj);

	     form.setMemoryId(memory_id);
	     
         log.warn("set predictions :: " + obj);

	  	// form.setPredictions(weights);
	}
	
	/**
	 * Learns from a form
	 *
	 * @param form the form
	 * @param memory_id the memory ID
	 * @throws UnsupportedOperationException if the form type is not supported
	 * @throws IOException if an I/O error occurs
	 *
	 * precondition: form != null
	 * precondition: memory_id != null
	 */
	public static void learn(Form form, Long memory_id) throws UnsupportedOperationException, IOException{
		assert form != null;
		assert memory_id != null;
		log.info("FORM ::    "+form);
		log.info("FORM MEMORY ID   :::   "+form.getMemoryId());
		log.info("feature value :: "+form.getType());
	  	log.info("Requesting prediction for form from RL system");
	  	
	  	CloseableHttpClient client = HttpClients.createDefault();
	    HttpPost httpPost = new HttpPost("http://198.211.117.122:9080/rl/learn");
	 
	    MultipartEntityBuilder builder = MultipartEntityBuilder.create();
	    builder.addTextBody("memory_id", memory_id.toString());
	    builder.addTextBody("feature_value", form.getType().toString());
	    
	    //builder.addTextBody("isRewarded", Boolean.toString(isRewarded));
	    //builder.addTextBody("new_output_features", Arrays.toString(Arrays.stream(form.getTypeOptions()).map(Enum::name).toArray(String[]::new)));
	    
	    HttpEntity multipart = builder.build();
	    httpPost.setEntity(multipart);

	    CloseableHttpResponse response = client.execute(httpPost);

		log.info("Recieved status code from RL :: "+response.getCode());
		log.info("REPSONE ENTITY CONTENT ::   " +response.getEntity().getContent().toString());
		int status = response.getCode();
	  	
  		String rl_response = "";
        switch (status) {
            case 200:
            case 201:
                BufferedReader br = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null) {
                    sb.append(line+"\n");
                }
                br.close();
                rl_response = sb.toString();
                log.info("Response received from RL system :: "+rl_response);
                break;
            case 400:
            	log.info("***********************************************************");
            	log.info("RL returned a 400");
            	log.info("***********************************************************");
            	break;
            case 500:
            	return;
            default:
            	return;
        }
	    client.close();
	}
}
