package otogenerator;

import java.io.*;
import java.util.*;

public class OtoGenerator {
	private static LinkedHashMap<String,String[]> indexDict;
	private static HashMap<String,String> replaceDict;
	private static ArrayList<String[]> oto;
	private static String suffix, cons, cutoff, preutt, overlap;
	private static int offset, offInc;
	
	/* A GUI would have all the text fields, options, etc.
	 * When a "generate oto" button is pressed in the GUI,
	 * it calls these methods and passes these parameters:
	 * 
	 * CSV Files
	 * indexFile should be ./csv/index.csv
	 * replaceFile should be ./csv/replace.csv
	 * 
	 * OTO Parameters
	 * String: Suffix (send a blank string if no suffix, NOT NULL)
	 * int: Initial offset (for the first alias of a line in the reclist)
	 * int: Offset increment per each alias after
	 * String: Consonant
	 * String: Cutoff
	 * String: Preutterance
	 * String: Overlap
	 * 
	 * If startEnd is true, starting/ending aliases (like [- k]) are included
	 * If replace is true, aliases are replaced according to replace.csv
	 * maxDups is the maximum number of duplicate aliases
	 * 		(set to 0 to delete all duplicates)
	 */
	public static void generateOto(File indexFile, File replaceFile,
								   String suf, int of, int ofi, String co, String cu, String p, String ov,
								   boolean startEnd, boolean replace, int maxDups){
		indexDict = new LinkedHashMap<String,String[]>();
		try (BufferedReader br = new BufferedReader(new FileReader(indexFile))) {
			
			String line;
			while ((line = br.readLine()) != null) {
				String[] lineArr = line.split(",");
				String fn = lineArr[0].substring(0,lineArr[0].length()-4);
				String[] phonemes = lineArr[1].split("_");
                indexDict.put(fn,phonemes);
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
		
		replaceDict = new HashMap<String,String>();
		try (BufferedReader br = new BufferedReader(new FileReader(replaceFile))){
			String line;
			while ((line = br.readLine()) != null) {
				String[] lineArr = line.split(",");
				replaceDict.put(lineArr[0],lineArr[1]);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		oto = new ArrayList<String[]>();
		suffix = suf;
		offset = of;
		offInc = ofi;
		cons = co;
		cutoff = cu;
		preutt = p;
		overlap = ov;
		
		generateAliases(startEnd);
		
		if (replace)
			replaceAliases();
		
		handleDuplicates(maxDups);
		
		exportOto();
	}
	
	/* Goes through all keys in the index
	 * For each key, it goes through each pair of phonemes
	 * If startEnd is true, it'll make aliases for the first/last phoneme
	 * The phoneme pairs are made into a string array that represents a line of oto
	 * The line of oto is added to the "oto" arraylist
	 */
	private static void generateAliases(boolean startEnd){
		Object[] reclist = indexDict.keySet().toArray();
		for (Object line : reclist){
			int currentOffset = offset;
			String[] phonemes = indexDict.get(line);
			if (startEnd){
				String[] otoLine = {line.toString(),"alias","offset",cons,cutoff,preutt,overlap};
				String alias = "- " + phonemes[0];
				otoLine[1] = alias;
				otoLine[2] = Integer.toString(currentOffset);
				oto.add(otoLine);
				currentOffset += offInc;
			}
			for (int i = 0; i < indexDict.get(line).length-1; i++) {
				String[] otoLine = {line.toString(),"alias","offset",cons,cutoff,preutt,overlap};
				String alias = phonemes[i] + " " + phonemes[i+1];
				otoLine[1] = alias;
				otoLine[2] = Integer.toString(currentOffset);
				oto.add(otoLine);
				currentOffset += offInc;
			}
			if (startEnd){
				String[] otoLine = {line.toString(),"alias","offset",cons,cutoff,preutt,overlap};
				String alias = phonemes[phonemes.length-1] + " -";
				otoLine[1] = alias;
				otoLine[2] = Integer.toString(currentOffset);
				oto.add(otoLine);
			}
		}
	}
	
	/* Goes through the oto arraylist
	 * If the alias is a key in the replace dictionary, change it to the value
	 */
	private static void replaceAliases(){
		for (String[] line : oto){
			if (replaceDict.containsKey(line[1]))
				line[1] = replaceDict.get(line[1]);
		}
	}
	
	/* Goes through the oto arraylist
	 * Makes a temporary hashmap that counts occurences of every alias
	 * If the alias of a line isn't in the hashmap, add it with a count of 0
	 * If the alias IS in the hashmap, check if the current number of duplicates
	 * is less than the maximum
	 * If less, append duplicate number to the alias. If more, delete line
	 */
	private static void handleDuplicates(int maxDups){
		HashMap<String,Integer> aliasCount = new HashMap<String,Integer>();
		int counter = 0;
		while (counter < oto.size()){
			String[] line = oto.get(counter);
			if (aliasCount.containsKey(line[1]) && aliasCount.get(line[1]) < maxDups){
				aliasCount.replace(line[1],aliasCount.get(line[1])+1);
				line[1] += aliasCount.get(line[1]);
			} else if (aliasCount.containsKey(line[1]) && aliasCount.get(line[1]) >= maxDups) {
				oto.remove(counter);
			} else {
				aliasCount.put(line[1],0);
				counter++;
			}
		}
	}
	
	/* Goes through the oto arraylist
	 * Adds the suffix to all aliases
	 * Prints the whole oto to a file in the result folder
	 */
	private static void exportOto(){
		try {
            PrintWriter writer = new PrintWriter(new File("./oto/oto.ini"));
            
            for (String[] line : oto) {
            	writer.println(line[0] + ".wav=" + line[1] + suffix + "," + line[2] + "," + line[3] + "," + line[4] + "," + line[5] + "," + line[6]);
            }
            writer.close();
         } catch (Exception ex) {
        	 ex.printStackTrace();
         }
	}
}
