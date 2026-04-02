package com.looksee.pageBuilder.gcp;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.cloud.language.v1.AnalyzeSyntaxRequest;
import com.google.cloud.language.v1.AnalyzeSyntaxResponse;
import com.google.cloud.language.v1.Document;
import com.google.cloud.language.v1.Document.Type;
import com.google.cloud.language.v1.EncodingType;
import com.google.cloud.language.v1.Entity;
import com.google.cloud.language.v1.LanguageServiceClient;
import com.google.cloud.language.v1.Sentence;
import com.google.cloud.language.v1.Sentiment;
import com.google.cloud.language.v1.Token;

public class CloudNLPUtils {
	@SuppressWarnings("unused")
	private static Logger log = LoggerFactory.getLogger(CloudNLPUtils.class);

	public static List<Sentence> extractSentences(String text) throws IOException {
	    LanguageServiceClient language = LanguageServiceClient.create();
		Document doc = Document.newBuilder().setContent(text).setType(Type.PLAIN_TEXT).setLanguage("en").build();

		// Detects the sentiment of the text
		List<Sentence> sentences = language.analyzeSyntax(doc).getSentencesList();
		
		language.shutdown(); 
		return sentences;
	}
	
	public static List<Sentence> extractParagraphs(String text) throws IOException {
	    LanguageServiceClient language = LanguageServiceClient.create();
		Document doc = Document.newBuilder().setContent(text).setType(Type.PLAIN_TEXT).setLanguage("en").build();

		// Detects the sentiment of the text
		List<Sentence> sentences = language.analyzeSyntax(doc).getSentencesList();
		
		language.shutdown();
		return sentences;
	}
	
	public static Sentiment extractSentiment(String text) throws IOException {
	    LanguageServiceClient language = LanguageServiceClient.create();
	    Document doc = Document.newBuilder().setContent(text).setType(Type.PLAIN_TEXT).setLanguage("en").build();
	    
    	Sentiment sentiment = language.analyzeSentiment(doc).getDocumentSentiment();
    	language.shutdown();
    	System.out.printf("Text: %s%n", text);
    	System.out.printf("Sentiment: %s, %s%n", sentiment.getScore(), sentiment.getMagnitude());
    	return sentiment;
	}
	
	public static List<Entity> extractEntities(String text) throws IOException {
	    LanguageServiceClient language = LanguageServiceClient.create();
	    Document doc = Document.newBuilder().setContent(text).setType(Type.PLAIN_TEXT).setLanguage("en").build();
	    
    	List<Entity> entities = language.analyzeEntities(doc).getEntitiesList();
    	language.shutdown();
    	System.out.printf("Text: %s%n", text);
    	System.out.printf("Entities: %n, %s", entities.size(), entities.toString());
    	return entities;
	}
	
	public static SyntaxAnalysis analyzeSyntax(String text) throws IOException {
		assert text != null;
		
		Map<String, Boolean> moods = new HashMap<>();
		Map<String, Boolean> voices = new HashMap<>();

		LanguageServiceClient language = LanguageServiceClient.create();
		Document doc = Document.newBuilder()
								.setContent(text)
								.setType(Type.PLAIN_TEXT)
								.setLanguage("en")
								.build();
		
		AnalyzeSyntaxRequest request =
			      AnalyzeSyntaxRequest.newBuilder()
			          .setDocument(doc)
			          .setEncodingType(EncodingType.UTF16)
			          .build();
		// Detects the sentiment of the text
		//List<Sentence> sentences = language.analyzeSyntax(doc).getSentencesList();
		AnalyzeSyntaxResponse response = language.analyzeSyntax(request);
		
		for (Token token : response.getTokensList()) {
			System.out.printf("\tText: %s\n", token.getText().getContent());
		    System.out.printf("\tBeginOffset: %d\n", token.getText().getBeginOffset());
		    System.out.printf("Lemma: %s\n", token.getLemma());
		    System.out.printf("PartOfSpeechTag: %s\n", token.getPartOfSpeech().getTag());
		    System.out.printf("\tAspect: %s\n", token.getPartOfSpeech().getAspect());
		    System.out.printf("\tCase: %s\n", token.getPartOfSpeech().getCase());
		    System.out.printf("\tForm: %s\n", token.getPartOfSpeech().getForm());
		    System.out.printf("\tGender: %s\n", token.getPartOfSpeech().getGender());
		    System.out.printf("\tMood: %s\n", token.getPartOfSpeech().getMood());
		    System.out.printf("\tNumber: %s\n", token.getPartOfSpeech().getNumber());
		    System.out.printf("\tPerson: %s\n", token.getPartOfSpeech().getPerson());
		    System.out.printf("\tProper: %s\n", token.getPartOfSpeech().getProper());
		    System.out.printf("\tReciprocity: %s\n", token.getPartOfSpeech().getReciprocity());
		    System.out.printf("\tTense: %s\n", token.getPartOfSpeech().getTense());
		 	System.out.printf("\tVoice: %s\n", token.getPartOfSpeech().getVoice().name());
		    System.out.println("-----------------------------------------------------------------------");
		
		    if( !token.getPartOfSpeech().getMood().toString().contains("UNKNOWN") ) {
		    	moods.put(token.getPartOfSpeech().getMood().name(), Boolean.TRUE);
		    }
		    
		    if( !token.getPartOfSpeech().getVoice().name().contains("UNKNOWN") ) {
		    	voices.put(token.getPartOfSpeech().getVoice().name(), Boolean.TRUE);
		    }
		}
		language.shutdown();
		
		SyntaxAnalysis syntax_analysis = new SyntaxAnalysis();
		syntax_analysis.setMoods(moods);
		syntax_analysis.setVoices(voices);
		
		return syntax_analysis;
	}
}
