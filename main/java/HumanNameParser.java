


import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.CharMatcher;
import javafx.util.Pair;
import org.json.JSONArray;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by sbalakrishnan on 8/26/15.
 */


public class HumanNameParser {

    final static Set<String> TITLES = new HashSet<String>();
    final static Set<String> SUFFIXES = new HashSet<String>();
    final static Set<String> CREDENTIALS = new HashSet<String>();
    final static Set<String> COMPOUND_NAMES = new HashSet<String>();
    final static Set<String> SUFFIXEPATTERN = new HashSet<String>();
    static HashMap<String, String> nameTypesTable = new HashMap<String, String>();
    static HashMap<String, String> ambiguityRuleTable = new HashMap<String, String>();

    static {
        //F = First M = Middle L=Last S=Suffix C=Credentials , AMB is ambiguty U=Unknown

        nameTypesTable.put("N,N"    ,"AMB");
        nameTypesTable.put("NNN"    ,"F,M,L");
        nameTypesTable.put("NN,S"   ,"F,L,S");
        nameTypesTable.put("NN,N"   ,"F,L,M");
        nameTypesTable.put("NNS"    ,"F,L,S");
        nameTypesTable.put("N,NN"   ,"L,F,M");
        nameTypesTable.put("NNN,N"  ,"F,M,L,U");
        nameTypesTable.put("NNNN"   ,"F,M,L,U");
        nameTypesTable.put("NNNS"   ,"F,M,L,S");
        nameTypesTable.put("NNN,S"   ,"F,M,L,S");
        nameTypesTable.put("N,N,N"  ,"F,F,F");
        nameTypesTable.put("N"      ,"F");
        nameTypesTable.put("NN"     ,"F,L");
        nameTypesTable.put("N,N-1"  ,"L,F");
        nameTypesTable.put("NN,C"   ,"F,L,C");
        nameTypesTable.put("NNC"    ,"F,L,C");
        nameTypesTable.put("NNNC"   ,"F,M,L,C");
        nameTypesTable.put("NNN,C"   ,"F,M,L,C");
    }

    static{
        ambiguityRuleTable.put("AMB,F,L"     ,"F,F");
        ambiguityRuleTable.put("AMB,F"       ,"L,F");
        ambiguityRuleTable.put("AMB,L,F"     ,"L,F");
        ambiguityRuleTable.put("AMB"         ,"L,F");
        ambiguityRuleTable.put("AMB,F,F,F"   ,"L,F");
        ambiguityRuleTable.put("AMB,F,M,L"   ,"F,F");
    }

    static {
        for (String title : new String[] { "dr.", "dr", "doctor", "mr.", "mr", "mister", "ms.", "ms", "miss", "mrs.",
                "mrs", "mistress", "hn.", "hn", "honorable", "the", "honorable", "his", "her", "honor", "fr", "fr.",
                "frau", "hr", "herr", "rv.", "rv", "rev.", "rev", "reverend", "reverend", "madam", "lord", "lady",
                "sir", "senior", "bishop", "rabbi", "holiness", "rebbe", "deacon", "eminence", "majesty", "consul",
                "vice", "president", "ambassador", "secretary", "undersecretary", "deputy", "inspector", "ins.",
                "detective", "det", "det.", "constable", "private", "pvt.", "pvt", "petty", "p.o.", "po", "first",
                "class", "p.f.c.", "pfc", "lcp.", "lcp", "corporal", "cpl.", "cpl", "colonel", "col", "col.",
                "capitain", "cpt.", "cpt", "ensign", "ens.", "ens", "lieutenant", "lt.", "lt", "ltc.", "ltc",
                "commander", "cmd.", "cmd", "cmdr", "rear", "radm", "r.adm.", "admiral", "adm.", "adm", "commodore",
                "cmd.", "cmd", "general", "gen", "gen.", "ltgen", "lt.gen.", "maj.gen.", "majgen.", "major", "maj.",
                "mjr", "maj", "seargent", "sgt.", "sgt", "chief", "cf.", "cf", "petty", "officer", "c.p.o.", "cpo",
                "master", "cmcpo", "fltmc", "formc", "mcpo", "mcpocg", "command", "fleet", "force" }) {
            HumanNameParser.TITLES.add(title);
        }

        for (String suffix : new String[] { "jr.", "jr", "junior", "ii", "iii", "iv", "senior", "sr.", "sr"}) {
            HumanNameParser.SUFFIXES.add(suffix);
        }

        for (String credentials : new String[] {
                "phd", "ph.d", "ph.d.", "m.d.", "md", "d.d.s.", "dds","ph d", // doctors
                "k.c.v.o", "kcvo", "o.o.c", "ooc", "o.o.a", "ooa", "g.b.e", "gbe", // knighthoods
                "k.b.e.", "kbe", "c.b.e.", "cbe", "o.b.e.", "obe", "m.b.e", "mbe", //   cont
                "esq.", "esq", "esquire", "j.d.", "jd", // lawyers
                "m.f.a.", "mfa", //misc
                "r.n.", "rn", "l.p.n.", "lpn", "l.n.p.", "lnp", //nurses
                "c.p.a.", "cpa", //money men
                "d.d.", "dd", "d.div.", "ddiv", //preachers
                "ret", "ret.","aud","dc","dds","dmd","do","od","dpm","dpt","dscpt","dsn","dvm","ent ","gp","gyn","md","ms","ob/gyn ","pharmd ","faaem","faafp","facs ","ffr","frcpsc ","mrcog","mrcs","dd","ded","edd","dpa","dph","dphil","" +
                "phd","ffphm","jd","phd ","psych","scd","sscd","thd","a.b.", "b.a.","b.a.b.a.","b.a.com.","b.a.e.","b.ag ","b.arch.","b.b.a.","b.c.e.","b.ch.e.",
                "b.d.","b.e.","b.e.e.","b.f.a.","b.in.dsn.","b.j.","b.l.a.","b.m.ed. ","b. pharm.","b.s.","s.b.","b.s.a.e.","b.s.b.a.","b.s.c.s.","b.s.chem.",
                "b.s.e.","b.s.ed.","b.s.m.e.","b.s.micr.","b.s.s.w.","ph.b.","th.b.","a.m.", "m.a.","m.acct.","m.aqua. ","m.b.a. ","m.c.d.","m.c.s.","m.div.",
                "m.e.","m.ed.","m.fstry.","m.l.arch.","m.l.i.s.","m.m. ","m.mus.","m.p.s.","m.s.","m.s.c.j.","m.s.c.s.","m.s.chem.","m.s.f.s.","m.s.m.sci.",
                "m.s.met.","m.sw.e","m.s.w.","m.th.","th.m.","au.d.","art.d.","d.a.","d.b.a.","d.c.","d.d.","d.ed.","d.l.s.","d.m.a.","d.p.a.","d.p.h.",
                "d.sc.","d.s.w.","d.v.m","ed.d.","j.d.","l.h.d.","ll.d.","mus.d.","d.m.","o.d.","ph.d.","s.d ","sc.d.","s.sc.d.","th.d."
                ,"b. n.","b.s.n.","m.n.","m.n.a.","d.d.s.","d.m.d.","d.o.","d.p.t.","d.s.n.","d.sc.pt","m.d.","o.d.","pharm.d.","ba","baba","bacom","bae",
                "bag ","barch","bba","bce","bche","bd","be","bee","bfa","bindsn","bj","bla","bmed","b pharm","bs ","sb","bsae","bsba","bscs","bschem",
                "bse","bsed","bsme","bsmicr","bssw","phb","thb","am", "ma","macct","maqua","mba","mcd","mcs","mdiv","me","med","mfstry","mlarch","mlis",
                "mm ","mmus","mps","ms","mscj","mscs","mschem","msfs","msmsci","msmet","mswe","msw","mth","thm","aud","artd","da","dba","dc","dd","ded","dls",
                "dma","dpa","dph","dsc","dsw","dvm","edd","jd","lhd","lld","musd","dm","od","phd","sd ","scd","sscd","thd","b n","bsn","mn","mna","dds","cphq","fahima","rhia","cpa","ccs","che","rhit",
                "dmd","do","dpt","dsn","dscpt","md","od","pharmd" ,"osf","chda","cph","fache","chp"}) {
            HumanNameParser.CREDENTIALS.add(credentials);
        }
        for (String suffixPattern : new String[] {"NN,N","NNN","NNN,N","NNNN"}) {
            HumanNameParser.SUFFIXEPATTERN.add(suffixPattern);
        }

        for (String comp : new String[] { "de", "la", "st", "st.", "ste", "ste.", "saint", "van", "der", "al", "bin",
                "le", "mac", "di", "del", "vel", "von", "e'", "san", "af", "el", "du" }) {
            HumanNameParser.COMPOUND_NAMES.add(comp);
        }
    }

    public  static void main(String [] args) throws Exception{
        String[] names = null;
        boolean isHumanNameParse =true;
        String name = null;

        /************************************
         STEP 1 :
         LOOP THRU THE STRING ARRAY OF NAMES
         *************************************/

        /*for(String name:origNames) {*/
        try {
            //supported name formats
            //Note For Mr and Mrs we will get only one name out of it.
            String[] nameTOParse= {
                    "Wong Kee Song , MD, phD",
                    "Mr. and Mrs. Calvin J. Klein",
                    "Mrs. Pamela A Jacob",
                    "Mr. and Mrs. Jerome J. Kapacinskas",
                    "Ion K. nson MD"


                    };

            JSONArray origNames = new JSONArray(Arrays.asList(nameTOParse));

            for (int startNum = 0; startNum <= origNames.length() - 1; startNum++) {
                name = origNames.get(startNum).toString();

                Person nonPerson = null;


                /************************************
                 STEP 2 :
                 REPLCE THE UNWANTED SPACES AND CLEAN UP THE COMMAS, HANDLE SPECIAL FORMAT NAMES
                 *************************************/
                if(name.lastIndexOf(":") != -1)
                    name = name.replace(name.substring(name.lastIndexOf(":"),name.length()),"");
                name = name.replaceAll("\\s+", " ").trim();
                name = name.replaceAll(" , ", ", ");
                name = name.replaceAll(" ,", ", ");
                name = name.replaceAll(",", ", ");
                String withNickNameOnIt = null;
                String withNameCredsOnIt = null;
                if (name.indexOf("(") != -1 && name.indexOf(")") != -1){
                    withNickNameOnIt = name;
                    name = name.replace(name.substring(name.indexOf("("), name.indexOf(")") + 1), "");
                }
                if (name.indexOf("\"") != -1 && name.lastIndexOf("\"",name.length()) != -1) {
                    withNickNameOnIt = name;
                    name = name.replace(name.substring(name.indexOf("\""), name.lastIndexOf("\"", name.length()) + 1), "");
                }

                name = name.replaceAll("\\s+", " ").trim();

                names = name.split(" AND ");
                if (names.length == 1) {
                    names = name.split(" and ");
                }
                if (names.length == 1) {
                    names = name.split("& ");
                }

                //identify if the line contains single name or multiple
                boolean isOneName = (names.length == 1) ? true : false;

                StringBuffer sb = new StringBuffer();
                LinkedHashMap<String, Person> nonParsePersonMap = new LinkedHashMap<String, Person>();
                LinkedList<Person> checkAMBList = new LinkedList<Person>();
                List<Pair<String, Person>> finalParsedListPair = new ArrayList<Pair<String, Person>>();
                ArrayList<Integer> positionArray = new ArrayList<Integer>();
                /************************************
                 STEP : 3 : This Flag is just a place holde may be in the future if we do somethign with organization or something else.
                 ************************************/
                if (isHumanNameParse) {
                    //Step 4 : Loop thru the splited names (after spliting from "&" or and )
                    for (String nameToSplitCheck : names) {
                        Person person = new Person();
                        String compName = null;
                        String[] singleNameSplitedBySpace = nameToSplitCheck.trim().split("\\s+");
                        boolean isSpaceReqForApostropheText = false;
                        String getApostropheText = null;
                        boolean isFirst = false;

                        //STEP 5 : HANDLE COMPOUND NAMES
                        for (int i = singleNameSplitedBySpace.length - 1; i >= 0; i--) {

                            if(singleNameSplitedBySpace[i].indexOf("'") != -1){
                                getApostropheText = singleNameSplitedBySpace[i].substring(singleNameSplitedBySpace[i].indexOf("'"),singleNameSplitedBySpace[i].length());

                                String regex = "\\d+";
                                if(getApostropheText.replace("'","").matches(regex)){
                                    isSpaceReqForApostropheText = true;
                                }

                            }


                            if (singleNameSplitedBySpace[i] != null && HumanNameParser.COMPOUND_NAMES.contains(singleNameSplitedBySpace[i].toLowerCase())) {

                                if (!isFirst) {
                                    if (compName == null && singleNameSplitedBySpace.length - 1 != i)
                                        compName = singleNameSplitedBySpace[i] + " " + singleNameSplitedBySpace[i + 1];
                                    else if (compName == null)
                                        compName = singleNameSplitedBySpace[i];
                                    else
                                        compName = singleNameSplitedBySpace[i] + " " + compName;
                                    isFirst = true;
                                } else {
                                    compName = singleNameSplitedBySpace[i] + " " + compName;
                                }


                            }
                        }
                        if (compName != null)
                            nameToSplitCheck = nameToSplitCheck.replace(compName, compName.replace(" ", ":"));
                        //System.out.println(nameToSplitCheck);
                        //Some times name may contains * in it , and that means the person is dead and we can set the flag
                        if (nameToSplitCheck.indexOf("*") != -1) {
                            person.setDeceased_flag("Y");
                            nameToSplitCheck = nameToSplitCheck.replace("*", "");
                        }
                        if(isSpaceReqForApostropheText){

                            nameToSplitCheck = nameToSplitCheck.replace(getApostropheText," "+getApostropheText);

                        }
                        //Step 5 : Split name by space
                        String[] singleNameSplitBySpace = nameToSplitCheck.trim().split("\\s+");
                        String nameTypeIdentified = null;
                        int start = 0;
                        StringBuffer creds = new StringBuffer();
                        //Step 6: Loop thru the splited names using space
                        //         And Assign the name type or format to match against the lookup table
                        for (String splitedName : singleNameSplitBySpace) {
                            //Note : add comma to the name format depends on the place it is located at

                            Matcher matcher1 = Pattern.compile("'\\d+").matcher(splitedName);
                            if(HumanNameParser.CREDENTIALS.contains(splitedName.toLowerCase().replaceAll("[^a-zA-z]",""))){
                                creds.append(" " + splitedName);

                            }else if (start == 0 & HumanNameParser.TITLES.contains(splitedName.toLowerCase())) {
                                person.setTitle(splitedName);

                                if (nameToSplitCheck != null)
                                    nameToSplitCheck = nameToSplitCheck.toString().replaceAll(splitedName, "").trim();
                            } else if (matcher1.find() ) {

                                if(person.getGraduatedYear() == null) {
                                    person.setGraduatedYear(splitedName.replace("'", ""));
                                }


                                sb.setLength(0);
                                sb.append(nameTypeIdentified);
                                nameToSplitCheck = nameToSplitCheck.toString().replaceAll(splitedName, "").trim();
                                //System.out.println(nameTypeIdentified);
                            } else {
                                if (splitedName.indexOf(",") == -1) {
                                    sb.append("N");
                                } else if (splitedName.indexOf(",") == 1) {
                                    sb.append(",N");
                                } else {
                                    sb.append("N,");
                                }
                                //Handling suffixes as a part of Step : 5 identifying name types or formats
                                if (sb != null && HumanNameParser.SUFFIXEPATTERN.contains(sb.toString())) {
                                    CharMatcher charMatcher = CharMatcher.DIGIT;

                                    int numberSuffix = charMatcher.countIn(splitedName);
                                    if (HumanNameParser.SUFFIXES.contains(splitedName.toLowerCase())) {
                                        nameTypeIdentified = sb.toString().replaceAll("N$", "S");
                                    } else {
                                        nameTypeIdentified = sb.toString();
                                    }
                                } else if (sb != null) {
                                    nameTypeIdentified = sb.toString();
                                }
                            }
                            start++;
                        }

                        if(creds.length() > 0) {

                            if(withNickNameOnIt == null)
                                withNickNameOnIt = name;

                            name = name.replace(creds.toString().trim(), "");
                            nameToSplitCheck = nameToSplitCheck.replace(creds.toString().trim(), "");

                            if(nameToSplitCheck.trim().indexOf(",") == nameToSplitCheck.trim().length()-1) {
                                name = name.replace(",", "");

                                nameTypeIdentified = nameTypeIdentified.substring(0,nameTypeIdentified.length()-1);

                            }

                            person.setCredentials(creds.toString().trim());
                        }


                        //Step 7: Set the person modal object with identified name types and the original name after the split from Step 3
                        if (nameTypeIdentified != null) {
                            if (nameTypesTable.get(nameTypeIdentified) == null) {
                                System.out.println("Sorry, Not Supported Name Format (require coding to be modified to support this. " + name);
                                person.setErrorIndicator("Y");
                                person.setErrorLevel("Sever");
                                person.setOrigName(name);
                                person.setProvidedName((withNickNameOnIt != null) ? withNickNameOnIt : name);
                                finalParsedListPair.add(new Pair((withNickNameOnIt != null) ? withNickNameOnIt : name, person));
                                continue;
                            } else {
                                person.setNameType(nameTypesTable.get(nameTypeIdentified).toString());
                                person.setOrigName(nameToSplitCheck);
                                person.setProvidedName((withNickNameOnIt != null) ? withNickNameOnIt : name);
                            }
                        }
                        //Step 8 : Assign the populated modal object to a linked hash map as name as Key and object as value
                        nonParsePersonMap.put(nameToSplitCheck, person);
                        sb.setLength(0);


                    }

                    //Step 9 : loop thru the linked hash map populated with name format to match against the Hash table (lookup table) "nameTypesTable"
                    for (Map.Entry<String, Person> entry : nonParsePersonMap.entrySet()) {
                        Person parsedPerson = entry.getValue();
                        if (entry.getValue().getNameType() != null) {
                            String[] nameArrayToParse = entry.getKey().split("\\s+");
                            String[] nameTypeArray = entry.getValue().getNameType().split(",");
                            //Step 10 : Call a method that matches against the lookup table and populate the value in person modal object
                            setPersonValues(nameArrayToParse, nameTypeArray, parsedPerson);
                            //Step 11 : Populate an linked list of parsed names
                            //Note : there are still names that aren't parsed and that falls under the category called "Ambiguity" and "First names handling"
                            //parsedPersonMap.put(entry.getKey(), parsedPerson);
                        }
                        checkAMBList.add(parsedPerson);
                    }

                    //Step 12 : identify the Ambiguity names and get its position from the mainly populated linked list
                    for (int j = 0; j < checkAMBList.size(); j++) {
                        if (checkAMBList.get(j).getNameType() != null && checkAMBList.get(j).getNameType().equalsIgnoreCase("AMB"))
                            positionArray.add(j);

                    }

                    //Step 13 : loop thru an arraylist contains the position of Ambiguity
                    //          Note: there are more than one case so looping reveresly
                    for (int j = positionArray.size() - 1; j >= 0; j--) {
                        int pos = positionArray.get(j);
                        String ambType = null;
                        if (isOneName)
                            ambType = checkAMBList.get(pos).getNameType();
                        else if (pos == checkAMBList.size() - 1) ambType = checkAMBList.get(pos).getNameType();
                        else
                            ambType = checkAMBList.get(pos).getNameType() + "," + checkAMBList.get(pos + 1).getNameType();


                        //Step 14: get the Ambiguity type and get the matching name type from hash table "ambiguityRuleTable"
                        if (ambType != null && ambiguityRuleTable.get(ambType).toString() != null) {
                            String[] nameArrayToParse = checkAMBList.get(pos).getOrigName().split("\\s+");
                            String[] nameTypeArray = ambiguityRuleTable.get(ambType).toString().split(",");
                            checkAMBList.get(pos).setNameType(ambiguityRuleTable.get(ambType).toString());

                            //Step 15 : Call a method to populate an linked list with name components parsed based on ambiguityRuleTable
                            setPersonValues(nameArrayToParse, nameTypeArray, checkAMBList.get(pos));
                            //parsedPersonMap.put(checkAMBList.get(pos).getOrigName(),checkAMBList.get(pos));


                        }
                    }

                    /*Step 16 : Handling First name only use case and populate the final parsed name list into a PAIR value

                        If (name type does not contains any of the other name component other than first name ) then
                                if ( current position + 1' name type is F,L )
                                    --> assign last name to the current position
                                else
                                    --> assign last name from the position - 1
                        end if
                    */

                    for (int j = 0; j < checkAMBList.size(); j++) {
                        if (
                                checkAMBList.get(j).getNameType() != null && (checkAMBList.get(j).getNameType().indexOf("L") == -1

                                )
                                ) {

                            String[] fNameOnlyArray = checkAMBList.get(j).getOrigName().trim().split("\\s+");


                            if ((j < checkAMBList.size() - 1) && checkAMBList.get(j + 1).getNameType() != null && (checkAMBList.get(j + 1).getNameType().indexOf("L") != -1/*equalsIgnoreCase("F,L")*/)) {
                                for (String fNames : fNameOnlyArray) {

                                    if (checkAMBList.get(j).getFirst().toLowerCase().indexOf(fNames.toLowerCase()) != -1) {
                                        checkAMBList.get(j).setLast(checkAMBList.get(j + 1).getLast());
                                        checkAMBList.get(j).setFirst(fNames);
                                        checkAMBList.get(j).setProvidedName((withNickNameOnIt != null) ? withNickNameOnIt : name);
                                        finalParsedListPair.add(new Pair((withNickNameOnIt != null) ? withNickNameOnIt : name, checkAMBList.get(j)));
                                    } else {
                                        Person newPerson = new Person();
                                        //newPerson.setGiftDetails(giftDetail);
                                        newPerson.setFirst(fNames);
                                        newPerson.setProvidedName((withNickNameOnIt != null) ? withNickNameOnIt : name);
                                        checkAMBList.get(j).setMiddle(checkAMBList.get(j).getLast());
                                        newPerson.setLast(checkAMBList.get(j + 1).getLast());
                                        newPerson.setNameType(checkAMBList.get(j).getNameType());
                                        finalParsedListPair.add(new Pair((withNickNameOnIt != null) ? withNickNameOnIt : name, newPerson));
                                    }
                                }
                            } else {

                                for (String fNames : fNameOnlyArray) {
                                    if ((j - 1) >= 0) {
                                        checkAMBList.get(j).setLast(checkAMBList.get(j - 1).getLast());
                                        checkAMBList.get(j).setFirst(fNames);
                                        checkAMBList.get(j).setProvidedName((withNickNameOnIt != null) ? withNickNameOnIt : name);
                                        finalParsedListPair.add(new Pair((withNickNameOnIt != null) ? withNickNameOnIt : name, checkAMBList.get(j)));
                                    } else {
                                        checkAMBList.get(j).setFirst(fNames);
                                        finalParsedListPair.add(new Pair((withNickNameOnIt != null) ? withNickNameOnIt : name, checkAMBList.get(j)));
                                        System.out.println("Sorry Not supported name formats");
                                        checkAMBList.get(j).setErrorIndicator("Y");
                                        checkAMBList.get(j).setErrorLevel("Sever");
                                        checkAMBList.get(j).setOrigName(name);
                                        checkAMBList.get(j).setProvidedName((withNickNameOnIt != null) ? withNickNameOnIt : name);

                                    }
                                }
                            }
                        } else if (checkAMBList.get(j).getNameType() != null) {
                            if (checkAMBList.get(j).getLast().replaceAll("[^\\p{Alpha}\\p{Digit}]+", "").length() == 1) {
                                if ((j < checkAMBList.size() - 1) && checkAMBList.get(j + 1) != null) {
                                    checkAMBList.get(j).setMiddle(checkAMBList.get(j).getLast());
                                    checkAMBList.get(j).setLast(checkAMBList.get(j + 1).getLast());
                                } else if ((j - 1) >= 0 && checkAMBList.get(j - 1) != null) {
                                    checkAMBList.get(j).setMiddle(checkAMBList.get(j).getLast());
                                    checkAMBList.get(j).setLast(checkAMBList.get(j - 1).getLast());
                                } else {
                                    System.out.println("Sorry, Not Supported Names");
                                    checkAMBList.get(j).setErrorIndicator("Y");
                                    checkAMBList.get(j).setErrorLevel("Sever");
                                    checkAMBList.get(j).setOrigName(name);
                                    checkAMBList.get(j).setProvidedName((withNickNameOnIt != null) ? withNickNameOnIt : name);
                                }
                            }
                            finalParsedListPair.add(new Pair((withNickNameOnIt != null) ? withNickNameOnIt : name, checkAMBList.get(j)));
                        } else if (checkAMBList.get(j).getNameType() == null){
                            checkAMBList.get(j).setErrorIndicator("Y");
                            checkAMBList.get(j).setErrorLevel("Sever");
                            continue;
                        }else {
                            if ((j < checkAMBList.size() - 1) && checkAMBList.get(j + 1) != null && checkAMBList.get(j).getNameType() == null && checkAMBList.get(j).getTitle() != null) {
                                checkAMBList.get(j + 1).setTitle(null);
                            } else {
                                if (j < checkAMBList.size() - 1) {

                                    checkAMBList.get(j).setFirst(checkAMBList.get(j + 1).getFirst());
                                    checkAMBList.get(j).setLast(checkAMBList.get(j + 1).getLast());
                                    checkAMBList.get(j).setMiddle(checkAMBList.get(j + 1).getMiddle());
                                }
                                finalParsedListPair.add(new Pair((withNickNameOnIt != null) ? withNickNameOnIt : name, checkAMBList.get(j)));

                            }
                        }
                    }
                } else {
                    finalParsedListPair.add(new Pair((withNickNameOnIt != null) ? withNickNameOnIt : name, nonPerson));
                    isHumanNameParse = true;
                }

                for (Pair<String, Person> pair : finalParsedListPair) {
                    System.out.println( pair.getKey() + " -> " + pair.getValue());

                }
                int index = 0;

                for (Pair<String, Person> pair : finalParsedListPair) {

                    if (
                            (pair.getValue().getFirst() != null &&  pair.getValue().getFirst().matches(".*[0-9].*")) ||
                                    (pair.getValue().getMiddle() != null &&  pair.getValue().getMiddle().matches(".*[0-9].*")) ||
                                    (pair.getValue().getLast() != null &&  pair.getValue().getLast().matches(".*[0-9].*"))
                            ) {
                        pair.getValue().setErrorIndicator("Y");
                        System.out.println("Number Format ");
                    }

                    if(pair.getValue().getNameType() != null &&
                            ( pair.getValue().getNameType().trim() == "F,L,M" ||  pair.getValue().getNameType().trim() ==  "F,M,L,U"))
                        pair.getValue().setErrorIndicator("Y");

                    //System.out.println(index + " : " + pair.getKey() + " -> " + pair.getValue());
                    index++;
                }


            }
        }catch(Exception e){

            e.printStackTrace();

        }


    }



    /*   method called as a part of Step 9 from the above code
           This populates the names based on the Name type "F" , "M" , "L"
           this method gets the name array and its respective name type format in terms of defined "F M L"
           assign the name components with respective to the incoming name format
    */
    public static void  setPersonValues(String[] namesToSet , String[] nameType,Person person){


        for(int i=0;i<=nameType.length-1;i++){

            if(nameType[i].equalsIgnoreCase("F")){
                person.setFirst(namesToSet[i]);
            }
            if(nameType[i].equalsIgnoreCase("M")){
                person.setMiddle(namesToSet[i]);
            }
            if(nameType[i].equalsIgnoreCase("L")){
                person.setLast(namesToSet[i]);
            }
            if(nameType[i].equalsIgnoreCase("S")  ){
                person.setSuffix(namesToSet[i]);
            }
            if(nameType[i].equalsIgnoreCase("U")  ){
                person.setUnknown(namesToSet[i]);
            }

        }

    }



    public static class Person{

        private String title;
        private String first;
        private String middle;
        private String last;
        private String suffix;
        private String status;
        private String originalName;
        private String nameType;
        private String providedName;
        private String unknown;
        private String graduatedYear;
        private String errorIndicator;
        private String errorLevel;
        private String credentials;
        private String displayName;

        public String getDisplayName() {
            return displayName;
        }

        public void setDisplayName(String displayName) {
            this.displayName = displayName;
        }

        public String getCredentials() {
            return credentials;
        }

        public void setCredentials(String credentials) {
            this.credentials = credentials;
        }


        public String getErrorLevel() {
            return errorLevel;
        }

        public void setErrorLevel(String errorLevel) {
            this.errorLevel = errorLevel;
        }

        public String getErrorIndicator() {
            return errorIndicator;
        }

        public void setErrorIndicator(String errorIndicator) {
            this.errorIndicator = errorIndicator;
        }


        /*public GiftDetails getGiftDetails(){
            return giftDetail;
        }

        public void setGiftDetails(GiftDetails giftDetail) {
            this.giftDetail = giftDetail;
        }*/

        public String getDeceased_flag() {
            return deceased_flag;
        }

        public void setDeceased_flag(String deceased_flag) {
            this.deceased_flag = deceased_flag;
        }

        private String deceased_flag;

        public String getUnknown() {
            return unknown;
        }

        public void setUnknown(String unknown) {
            this.unknown = unknown;
        }

        public String getGraduatedYear() {
            return graduatedYear;
        }

        public void setGraduatedYear(String graduatedYear) {
            this.graduatedYear = graduatedYear;
        }

        public String replceSpecialChar(String str){
            if(str != null)
                return str = str.replaceAll("\\p{Punct}+", " ");
            else
                return "";

        }

        public String getProvidedName(){ return providedName; }

        public void setProvidedName(String providedName){this.providedName = providedName; }

        public String getNameType(){ return nameType; }

        public void setNameType(String nameType){this.nameType = nameType; }

        public String getOrigName(){ return originalName; }

        public void setOrigName(String originalName){this.originalName = originalName; }

        public String getStatus() {return status ; }

        public void setStatus(String status){this.status = status ;}

        public String getTitle() { return title;  }

        public void setTitle(String title) { this.title = title; }

        public String getFirst() { return first;  }

        public void setFirst(String first) {
            first = replceSpecialChar(first);//first.replace(":"," ");
            this.first = first;}

        public String getMiddle() {return middle; }

        public void setMiddle(String middle) {
            middle = replceSpecialChar(middle);
            this.middle = middle;  }

        public String getLast() {return last;}

        public void setLast(String last) {
            last = replceSpecialChar(last);
            this.last = last; }

        public String getSuffix() {return suffix;}

        public void setSuffix(String suffix) {
            suffix = replceSpecialChar(suffix);
            this.suffix = suffix; }

        public String toString(){
            HashMap hm = new HashMap();

            hm.put("title",title);
            hm.put("first",first);
            hm.put("middle",middle);
            hm.put("last",last);
            hm.put("suffix",suffix);
            hm.put("graduatedyear",graduatedYear);
            hm.put("nameType",nameType);
            hm.put("unknown",unknown);
            hm.put("origname",originalName);
            hm.put("deceasedFlag",deceased_flag);
            hm.put("providedName",providedName);
            hm.put("cred",credentials);
            hm.put("error",errorIndicator);
            String toRetrun = null;
            try{
                toRetrun = new ObjectMapper().writeValueAsString(hm);
            }catch(Exception e){

            }
            return toRetrun;
        }
    }
}
