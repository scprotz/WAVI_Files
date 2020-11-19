package com.ibm.dmobley;

import java.io.File;
import java.io.FileNotFoundException;

import com.ibm.cloud.sdk.core.security.IamAuthenticator;
import com.ibm.watson.speech_to_text.v1.SpeechToText;
import com.ibm.watson.speech_to_text.v1.model.AddGrammarOptions;
import com.ibm.watson.speech_to_text.v1.model.CreateLanguageModelOptions;
import com.ibm.watson.speech_to_text.v1.model.DeleteLanguageModelOptions;
import com.ibm.watson.speech_to_text.v1.model.GetGrammarOptions;
import com.ibm.watson.speech_to_text.v1.model.Grammar;
import com.ibm.watson.speech_to_text.v1.model.LanguageModel;
import com.ibm.watson.speech_to_text.v1.model.LanguageModels;
import com.ibm.watson.speech_to_text.v1.model.TrainLanguageModelOptions;

public class InstallGrammar
{

	public static final String URL = XXX;
	public static final String API_KEY = YYY;
	public static final String MODEL_NAME = "Sample Language Model";

	public static void main(String[] args) throws InterruptedException
	{
		IamAuthenticator authenticator = new IamAuthenticator(API_KEY);
		SpeechToText speechToText = new SpeechToText(authenticator);
		speechToText.setServiceUrl(URL);

		// List all models and delete any that match the one we are building //
		LanguageModels languageModels = speechToText.listLanguageModels().execute().getResult();
		for(LanguageModel model: languageModels.getCustomizations())
		{
			if(model.getName().equals(MODEL_NAME))
			{
				System.out.println("Deleting Old Models");
				DeleteLanguageModelOptions deleteLanguageModelOptions =
						new DeleteLanguageModelOptions.Builder()
						.customizationId(model.getCustomizationId())
						.build();

				speechToText.deleteLanguageModel(deleteLanguageModelOptions).execute();	
			}
		}
		
		// Create the language model //
		System.out.println("Creating new model");
		CreateLanguageModelOptions createLanguageModelOptions =
				  new CreateLanguageModelOptions.Builder()
				    .name(MODEL_NAME)
				    .baseModelName("en-US_NarrowbandModel")
				    .description(MODEL_NAME)
				    .build();

		LanguageModel our_model =
				  speechToText.createLanguageModel(createLanguageModelOptions).execute().getResult();
		
		
		// Create the grammar //
		System.out.println("Adding the grammar");
		try {
		  AddGrammarOptions addGrammarOptions = new AddGrammarOptions.Builder()
		    .customizationId(our_model.getCustomizationId())
		    .grammarFile(new File("list.abnf"))
		    .grammarName("list-abnf")
		    .contentType("application/srgs")
		    .build();
		
		  speechToText.addGrammar(addGrammarOptions).execute();
		  // Poll for grammar status.
		} catch (FileNotFoundException e) {
		  e.printStackTrace();
		}
		
		// Check status of grammar //
		boolean done = false;
		while(!done)
		{
			GetGrammarOptions getGrammarOptions = new GetGrammarOptions.Builder()
				  .customizationId(our_model.getCustomizationId())
				  .grammarName("list-abnf")
				  .build();

			Grammar grammar =
				  speechToText.getGrammar(getGrammarOptions).execute().getResult();
			System.out.println("checking grammar status: " + grammar.getStatus());
			if(grammar.getStatus().equals("analyzed"))
				done = true;
			Thread.sleep(1000);
		}
		
		// Train the model //
		System.out.println("Training customization with grammar");
		TrainLanguageModelOptions trainLanguageModelOptions =
				  new TrainLanguageModelOptions.Builder()
				    .customizationId(our_model.getCustomizationId())
				    .build();

		speechToText.trainLanguageModel(trainLanguageModelOptions).execute().getResult();

		// Check the status of the model //	
		while(true)
		{
			languageModels = speechToText.listLanguageModels().execute().getResult();
			for (LanguageModel model: languageModels.getCustomizations())
			{
				if(model.getCustomizationId().equals(our_model.getCustomizationId()))
				{
					System.out.println("Current training status: " + model.getStatus());
					if(model.getStatus().equals("available"))
						System.exit(0);
				}
			}
			Thread.sleep(1000);			
		}		
	}

}
