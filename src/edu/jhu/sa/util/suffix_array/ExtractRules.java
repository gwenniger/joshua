/* This file is part of the Joshua Machine Translation System.
 * 
 * Joshua is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation; either version 2.1 of the License, or 
 * (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful, but 
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public
 * License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this library; if not, write to the Free Software Foundation,
 * Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */
package edu.jhu.sa.util.suffix_array;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import joshua.decoder.ff.tm.Rule;
import joshua.util.CommandLineParser;
import joshua.util.CommandLineParser.Option;
import joshua.util.lexprob.LexProbs;
import joshua.util.sentence.Vocabulary;


/**
 * 
 * @author Lane Schwartz
 * @version $LastChangedDate$
 */
public class ExtractRules {

	/** Logger for this class. */
	private static final Logger logger = Logger.getLogger(ExtractRules.class.getName());
	
	
	/**
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {
		
		CommandLineParser commandLine = new CommandLineParser();
		
		Option<String> source = commandLine.addStringOption('f',"source","SOURCE_FILE","Source language training file");
		Option<String> target = commandLine.addStringOption('e',"target","TARGET_FILE","Target language training file");		
		Option<String> alignment = commandLine.addStringOption('a',"alignments","ALIGNMENTS_FILE","Source-target alignments training file");	
		Option<String> test = commandLine.addStringOption('t',"test","TEST_FILE","Source language test file");
		
		Option<String> output = commandLine.addStringOption('o',"output","OUTPUT_FILE","-","Output file");
		
		
		Option<String> encoding = commandLine.addStringOption("encoding","ENCODING","UTF-8","File encoding format");
		
		Option<Integer> maxPhraseSpan = commandLine.addIntegerOption("maxPhraseSpan","MAX_PHRASE_SPAN",10, "Max phrase span");
		Option<Integer> maxPhraseLength = commandLine.addIntegerOption("maxPhraseLength","MAX_PHRASE_LENGTH",10, "Max phrase length");
		Option<Integer> maxNonterminals = commandLine.addIntegerOption("maxNonterminals","MAX_NONTERMINALS",2, "Max nonterminals");
		
		Option<String> target_given_source_counts = commandLine.addStringOption("target-given-source-counts","FILENAME","file containing co-occurence counts of source and target word pairs, sorted by source words");
		Option<String> source_given_target_counts = commandLine.addStringOption("source-given-target-counts","FILENAME","file containing co-occurence counts of target and source word pairs, sorted by target words");
		
		Option<Boolean> output_gz = commandLine.addBooleanOption("output-gzipped",false,"snould the outpu file be gzipped");
		Option<Boolean> target_given_source_gz = commandLine.addBooleanOption("target-given-source-gzipped",false,"is the target given source word pair counts file gzipped");
		Option<Boolean> source_given_target_gz = commandLine.addBooleanOption("source-given-target-gzipped",false,"is the source given target word pair counts file gzipped");
		
		Option<Integer> cachePrecomputationFrequencyThreshold = commandLine.addIntegerOption("c", "CACHE_PRECOMPUTATION_FREQUENCY_THRESHOLD", 100, "the minimum number of times a phrase must appear before it will be pre-stored in the inverted index cache");
		
		commandLine.parse(args);

		
		// Set System.out and System.err to use the provided character encoding
		try {
			System.setOut(new PrintStream(System.out, true, commandLine.getValue(encoding)));
			System.setErr(new PrintStream(System.err, true, commandLine.getValue(encoding)));
		} catch (UnsupportedEncodingException e1) {
			System.err.println(commandLine.getValue(encoding) + " is not a valid encoding; using system default encoding for System.out and System.err.");
		} catch (SecurityException e2) {
			System.err.println("Security manager is configured to disallow changes to System.out or System.err; using system default encoding.");
		}
		
		
		if (logger.isLoggable(Level.FINE)) logger.fine("Constructing source language vocabulary.");
		String sourceFileName = commandLine.getValue(source);
		Vocabulary sourceVocab = new Vocabulary();
		int[] sourceWordsSentences = SuffixArrayFactory.createVocabulary(sourceFileName, sourceVocab);
		if (logger.isLoggable(Level.FINE)) logger.fine("Constructing source language corpus array.");
		CorpusArray sourceCorpusArray = SuffixArrayFactory.createCorpusArray(sourceFileName, sourceVocab, sourceWordsSentences[0], sourceWordsSentences[1]);
		if (logger.isLoggable(Level.FINE)) logger.fine("Constructing source language suffix arra.");
		SuffixArray sourceSuffixArray = SuffixArrayFactory.createSuffixArray(sourceCorpusArray, commandLine.getValue(cachePrecomputationFrequencyThreshold));
		
		if (logger.isLoggable(Level.FINE)) logger.fine("Constructing target language vocabulary.");		
		String targetFileName = commandLine.getValue(target);
		Vocabulary targetVocab = new Vocabulary();
		int[] targetWordsSentences = SuffixArrayFactory.createVocabulary(commandLine.getValue(target), targetVocab);
		if (logger.isLoggable(Level.FINE)) logger.fine("Constructing target language corpus array.");
		CorpusArray targetCorpusArray = SuffixArrayFactory.createCorpusArray(targetFileName, targetVocab, targetWordsSentences[0], targetWordsSentences[1]);
		if (logger.isLoggable(Level.FINE)) logger.fine("Constructing target language suffix array.");
		SuffixArray targetSuffixArray = SuffixArrayFactory.createSuffixArray(targetCorpusArray, commandLine.getValue(cachePrecomputationFrequencyThreshold));
		
		if (logger.isLoggable(Level.FINE)) logger.fine("Reading alignment data.");
		String alignmentFileName = commandLine.getValue(alignment);
		AlignmentArray alignmentArray = SuffixArrayFactory.createAlignmentArray(alignmentFileName, sourceSuffixArray, targetSuffixArray);
		
		// Set up the source text for reading
		Scanner target_given_source;
		if (commandLine.getValue(target_given_source_counts).endsWith(".gz") || commandLine.getValue(target_given_source_gz))
			target_given_source = new Scanner(new BufferedReader(new InputStreamReader(new GZIPInputStream(new FileInputStream(commandLine.getValue(target_given_source_counts))),commandLine.getValue(encoding))));
		else
			target_given_source = new Scanner( new File(commandLine.getValue(target_given_source_counts)), commandLine.getValue(encoding));

		
		// Set up the target text for reading
		Scanner source_given_target;
		if (commandLine.getValue(source_given_target_counts).endsWith(".gz") || commandLine.getValue(source_given_target_gz))
			source_given_target = new Scanner(new BufferedReader(new InputStreamReader(new GZIPInputStream(new FileInputStream(commandLine.getValue(source_given_target_counts))),commandLine.getValue(encoding))));
		else
			source_given_target = new Scanner( new File(commandLine.getValue(source_given_target_counts)), commandLine.getValue(encoding));
		
		PrintStream out;
		if ("-".equals(commandLine.getValue(output))) {
			out = System.out;
		} else if (commandLine.getValue(output).endsWith(".gz") || commandLine.getValue(output_gz)) {
			//XXX This currently doesn't work
			out = new PrintStream(new GZIPOutputStream(new FileOutputStream(commandLine.getValue(output))));
			System.err.println("GZIP output not currently working properly");
			System.exit(-1);
		} else {
			out = new PrintStream(commandLine.getValue(output));
		}
		
		if (logger.isLoggable(Level.FINE)) logger.fine("Constructing lexical probabilities table");
		
		LexProbs lexProbs = new LexProbs(source_given_target, target_given_source, sourceVocab, targetVocab);
	
		if (logger.isLoggable(Level.FINE)) logger.fine("Done constructing lexical probabilities table");
		
		Map<Integer,String> ntVocab = new HashMap<Integer,String>();
		ntVocab.put(PrefixTree.X, "X");
		
		Scanner testFileScanner = new Scanner(new File(commandLine.getValue(test)), commandLine.getValue(encoding));
		
		while (testFileScanner.hasNextLine()) {
			String line = testFileScanner.nextLine();
			int[] words = sourceVocab.getIDs(line);
			
			if (logger.isLoggable(Level.FINE)) logger.fine("Constructing prefix tree for source line: " + line);
			
			PrefixTree prefixTree = new PrefixTree(sourceSuffixArray, targetCorpusArray, alignmentArray, lexProbs, words, commandLine.getValue(maxPhraseSpan), commandLine.getValue(maxPhraseLength), commandLine.getValue(maxNonterminals));
			
			if (logger.isLoggable(Level.FINER)) logger.finer("Outputting rules for source line: " + line);

			for (Rule rule : prefixTree.getAllRules()) {
				String ruleString = rule.toString(ntVocab, sourceVocab, targetVocab);
				if (logger.isLoggable(Level.FINEST)) logger.finest("Rule: " + ruleString);
				out.println(ruleString);
			}
		}
		
	}

}
