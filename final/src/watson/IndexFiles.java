/***
 * Author: Tatiana Kapos
 * Class: CSC 483 Fall 2020
 * DUE: 12/9/2020 12pm
 * Purpose: Builds Watson, a participate to jeopardy, that uses Lucene to create an index of Wikipedia Articles and find the answer to jeopardy questions
 * based on the title of the Wikipedia aritcles.
 */

package watson;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.io.BufferedReader;

import org.apache.lucene.analysis.core.WhitespaceAnalyzer;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

public class IndexFiles {
	// position of Index Directory
	private static final String INDEX_DIR = "index/";
	// MRR score
	static float score = 0;
	static int temp = 0;
	// Number of answers correct
	static int correct = 0;

	
	/*
	 * Create a Document to be put into the Lucene index
	 * PARAMS:
	 * 	title: title of wiki article
	 * 	catagory: catagories of wiki article seperated by whitespace
	 * 	body: body of wiki article
	 * 	headlines: headline of wiki article
	 * */
	public static Document createDoc(String title, String catagory, String body, String headlines) {
		Document doc = new Document();
		doc.add(new TextField("catagory", catagory, Field.Store.YES));
		doc.add(new TextField("title", title, Field.Store.YES));
		doc.add(new TextField("body", body, Field.Store.YES));
		doc.add(new TextField("headlines", headlines, Field.Store.YES));
		
		return doc;
	}
	
	/**
	 * Parses the data into a Lucene index in the src/index directory
	 */
	public static void parseWikiPage() {
		// standard analyzer with English stop word sets
		Path temp = Paths.get(INDEX_DIR);
		StandardAnalyzer standardAnalyzer = new StandardAnalyzer(EnglishAnalyzer.ENGLISH_STOP_WORDS_SET);
		//WhitespaceAnalyzer standardAnalyzer = new WhitespaceAnalyzer(); UNCOMMENT FOR TOKENIZATION -> worse performance
		Directory index = null;
		IndexWriter writer = null;
		
		// try to open directory
		try {
			index = FSDirectory.open(Paths.get(INDEX_DIR));
			IndexWriterConfig config = new IndexWriterConfig(standardAnalyzer);
		    writer = new IndexWriter(index, config);
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

		File directory = new File("data");
		File[] directoryListing = directory.listFiles();

		if (directoryListing != null) {
			// loop through each File in the directory
			for(File child : directoryListing) {
				try {
					// read in each file
					BufferedReader reader = new BufferedReader(new FileReader(child));
					String line = reader.readLine();
					
					boolean first = true;
					String catagory = "";
					String body = "";
					String headlines = "";
					while(line.length() < 2) {
						line = reader.readLine();
					}
					String title = line.substring(2,line.length()-2);

					
					while(line != null) {
						// check if line is a title
						if(line.startsWith("[[") && line.endsWith("]]")) {
							if(!first) {
							Document a = createDoc(title, catagory, body, headlines);
							writer.addDocument(a);
							//System.out.println("TITLE: " + title + "\n CATAGORIED: " + catagory + "\n HEADLINES: " + headlines + "\n BODY: " + body);
							catagory = "";
							body = "";
							headlines = "";
							try {
							title = line.substring(2,line.length()-2);
							}catch(IndexOutOfBoundsException e) {
							}
							}else {
								first = false;
							}
						// check if line is a category
						}else if(line.startsWith("CATEGORIES:")) {
							catagory = catagory + " " + line.substring(11);
						// check if line is a headline
						}else if(line.startsWith("==") && line.endsWith("==")) {
							//System.out.println(line);
							String temp2 = "";
							try {
							if(line.startsWith("====")) {
								temp2 = line.strip().substring(4,line.length()-4);
							}else if(line.startsWith("===")){
								temp2 = line.strip().substring(3,line.length()-3);
							}else {
								temp2 = line.strip().substring(2,line.length()-2);
							}
							}catch(IndexOutOfBoundsException e) {
							}
							//System.out.println(temp2);
							headlines = headlines + " " + temp2;
						// check if line is empty
						}else if(!line.equals("\n")){
							body = body + " " + line;
						}
						// move onto next line
						line = reader.readLine();
					}
					Document a = createDoc(title, catagory, body, headlines);
					writer.addDocument(a);
					reader.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				
				System.out.println("finished " + child.toString());
			}
		} else {
			System.out.println("ERROR: No files found in directory");
		}
		try {
			writer.commit();
			writer.close();
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		
	}
	
	/*
	 * Seaches the index for a Document
	 * PARAMS:
	 * 	catagory: catagory of clue
	 * 	clue: the clue
	 *  answer: answer to query
	 * */
	public static Document searchIndex(String catagory, String clue, String answer) {
		Directory dir;
		try {
			// Create a Index Searcher to find TopDocs
			dir = FSDirectory.open(Paths.get(INDEX_DIR));
			IndexReader reader = DirectoryReader.open(dir);
		    IndexSearcher searcher = new IndexSearcher(reader);
		    QueryParser qp = new QueryParser("body", new StandardAnalyzer(EnglishAnalyzer.ENGLISH_STOP_WORDS_SET));
		    Query test = qp.parse(QueryParser.escape(clue.strip() +" "+catagory.strip()));
		    TopDocs hits = searcher.search(test, 5000);
		    MRRCount(hits, answer, searcher);
		    if(hits.scoreDocs.length > 0) {
		    	// return the first Document found
		    	return searcher.doc(hits.scoreDocs[0].doc);
		    }
		} catch (IOException | ParseException e) {
			e.printStackTrace();
		}
		
		return null;
	}
	
	/**
	 * finds the MRRCount of a specific query
	 * @param hits the topDocs for the query
	 * @param answer the answer to the query
	 * @param searcher the indexsearcher
	 */
	public static void MRRCount(TopDocs hits, String answer, IndexSearcher searcher) {
			String[] answers = answer.split(Pattern.quote("|"));
			int position = 1;
			// loop through all top docs
			for(ScoreDoc topdoc : hits.scoreDocs) {
				try {
					Document temp = searcher.doc(topdoc.doc);
					for(String tempans : answers) {
						if(temp.get("title").toLowerCase().equals(tempans.toLowerCase())) {
							// add position to the score
							score += 1.0/position;
							if(position == 1) {
								correct++;
							}
							return;
						}
					}
					position++;
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			
			// score was not found within 20000 Documents
			score += 1.0/20000.0;
	}
	
	/**
	 * Loop through and searches for every question
	 */
	public static void goThroughQuestions() {
		// searching
		BufferedReader reader;
		int count = 0;
		try {
			reader = new BufferedReader(new FileReader(new File("questions.txt")));
			String line = reader.readLine();
			while(line != null) {
				count++;
				String catagory = line;
				line = reader.readLine();
				String clue = line;
				line = reader.readLine();
				String answer = line;
				
				System.out.println("catagory: " + catagory);
				System.out.println("clue: " + clue);
				
				Document found = searchIndex(catagory, clue, answer);
				if(found != null) {
					System.out.println("Correct answer: " + answer + "\nMy answer: " + found.get("title"));
					System.out.println();
				}
				
				line = reader.readLine();
				line = reader.readLine();
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		score = score * ((float)1/100);
		System.out.println("MMR score: " + score);
		System.out.println("Number correct: " + correct);
		System.out.println("Number incorrect: " + (100-correct));
		
		
	}

	public static void main(String[] args) {
		// generate index
		parseWikiPage();
		
		// loop through all questions
		goThroughQuestions();
		
	}
	

}