package com.looksee.gcp;

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
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.NoArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility class for Google Cloud NLP operations.
 */
@NoArgsConstructor
public class CloudNLPUtils {
	@SuppressWarnings("unused")
	private static Logger log = LoggerFactory.getLogger(CloudNLPUtils.class);

	/**
	 * Extracts the sentences from the text.
	 * @param text the text to extract the sentences from
	 * @return the sentences from the text
	 *
	 * precondition: text != null
	 * precondition: !text.isEmpty()
	 *
	 * @throws IOException if an error occurs while extracting the sentences
	 */
	public static List<Sentence> extractSentences(String text) throws IOException {
		assert text != null;
		assert !text.isEmpty();

		LanguageServiceClient language = LanguageServiceClient.create();
		Document doc = Document.newBuilder().setContent(text).setType(Type.PLAIN_TEXT).setLanguage("en").build();

		// Detects the sentiment of the text
		List<Sentence> sentences = language.analyzeSyntax(doc).getSentencesList();
		
		language.shutdown();
		return sentences;
	}
	
	/**
	 * Extracts the paragraphs from the text.
	 * @param text the text to extract the paragraphs from
	 * @return the paragraphs from the text
	 *
	 * precondition: text != null
	 * precondition: !text.isEmpty()
	 *
	 * @throws IOException if an error occurs while extracting the paragraphs
	 */
	public static List<Sentence> extractParagraphs(String text) throws IOException {
		assert text != null;
		assert !text.isEmpty();

	    LanguageServiceClient language = LanguageServiceClient.create();
		Document doc = Document.newBuilder().setContent(text).setType(Type.PLAIN_TEXT).setLanguage("en").build();

		// Detects the sentiment of the text
		List<Sentence> sentences = language.analyzeSyntax(doc).getSentencesList();
		
		language.shutdown();
		return sentences;
	}
	
	/**
	 * Extracts the sentiment of the text.
	 * @param text the text to extract the sentiment from
	 * @return the sentiment of the text
	 *
	 * precondition: text != null
	 * precondition: !text.isEmpty()
	 *
	 * @throws IOException if an error occurs while extracting the sentiment
	 */
	public static Sentiment extractSentiment(String text) throws IOException {
		assert text != null;
		assert !text.isEmpty();

	    LanguageServiceClient language = LanguageServiceClient.create();
	    Document doc = Document.newBuilder().setContent(text).setType(Type.PLAIN_TEXT).setLanguage("en").build();
	    
    	Sentiment sentiment = language.analyzeSentiment(doc).getDocumentSentiment();
    	language.shutdown();
    	log.info(String.format("Text: %s%n", text));
    	log.info(String.format("Sentiment: %s, %s%n", sentiment.getScore(), sentiment.getMagnitude()));
    	return sentiment;
	}
	
	/**
	 * Extracts the entities from the text.
	 * @param text the text to extract the entities from
	 * @return the entities from the text
	 *
	 * precondition: text != null
	 * precondition: !text.isEmpty()
	 *
	 * @throws IOException if an error occurs while extracting the entities
	 */
	public static List<Entity> extractEntities(String text) throws IOException {
		assert text != null;
		assert !text.isEmpty();
		
		LanguageServiceClient language = LanguageServiceClient.create();
		Document doc = Document.newBuilder().setContent(text).setType(Type.PLAIN_TEXT).setLanguage("en").build();
		
		List<Entity> entities = language.analyzeEntities(doc).getEntitiesList();
		language.shutdown();
		log.info(String.format("Text: %s%n", text));
		log.info(String.format("Entities: %n, %s", entities.size(), entities.toString()));
		return entities;
	}
	
	/**
	 * Analyzes the syntax of the text.
	 * @param text the text to analyze the syntax of
	 * @return the syntax analysis of the text
	 *
	 * precondition: text != null
	 * precondition: !text.isEmpty()
	 *
	 * @throws IOException if an error occurs while analyzing the syntax
	 */
	public static SyntaxAnalysis analyzeSyntax(String text) throws IOException {
		assert text != null;
		assert !text.isEmpty();
		
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
		AnalyzeSyntaxResponse response = language.analyzeSyntax(request);
		
		for (Token token : response.getTokensList()) {
			log.info(String.format("\tText: %s\n", token.getText().getContent()));
			log.info(String.format("\tBeginOffset: %d\n", token.getText().getBeginOffset()));
			log.info(String.format("Lemma: %s\n", token.getLemma()));
			log.info(String.format("PartOfSpeechTag: %s\n", token.getPartOfSpeech().getTag()));
			log.info(String.format("\tAspect: %s\n", token.getPartOfSpeech().getAspect()));
			log.info(String.format("\tCase: %s\n", token.getPartOfSpeech().getCase()));
			log.info(String.format("\tForm: %s\n", token.getPartOfSpeech().getForm()));
			log.info(String.format("\tGender: %s\n", token.getPartOfSpeech().getGender()));
			log.info(String.format("\tMood: %s\n", token.getPartOfSpeech().getMood()));
			log.info(String.format("\tNumber: %s\n", token.getPartOfSpeech().getNumber()));
			log.info(String.format("\tPerson: %s\n", token.getPartOfSpeech().getPerson()));
			log.info(String.format("\tProper: %s\n", token.getPartOfSpeech().getProper()));
			log.info(String.format("\tReciprocity: %s\n", token.getPartOfSpeech().getReciprocity()));
			log.info(String.format("\tTense: %s\n", token.getPartOfSpeech().getTense()));
			log.info(String.format("\tVoice: %s\n", token.getPartOfSpeech().getVoice().name()));
			log.info("{}", "-----------------------------------------------------------------------");
		
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
