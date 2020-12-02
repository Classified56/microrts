package ai.abstraction;

import rts.units.*;

import java.io.File;
import java.util.*;
import java.util.regex.*;

public class KnowledgeBase {
    List<Rule> facts;
    List<String> actions;
    int instr;

    public KnowledgeBase() {
        facts = new ArrayList<Rule>();
        actions = new ArrayList<String>();
        instr = 0;
    }

    public void addFact(Rule t) {
        facts.add(t);
    }

    public void clearFacts() {
        facts.clear();
    }

    public void addAction(String s) {
        actions.add(s);
    }

    public List<String> getActions() {
        return actions;
    }

    public void clearAction() {
        actions.clear();
    }

    public List<Rule> getFacts() {
        return facts;
    }

    public void shuffleFacts() {
        Collections.shuffle(facts);
    }

    //stores all the lines from the Rules file in actions
    public void getRulesFromFile(String file) {
        try {
            Scanner reader = new Scanner(new File(file));
            while(reader.hasNextLine()) {
                String line = reader.nextLine();
                if(line.length() > 0 && line.charAt(0) != '#') {
                    //System.out.println("Read line: " + line);
                    actions.add(line);
                }

            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    //Assembles a rule from the line from the Rules text file and returns the rule
    //Will return a rule if the action can occur/the predicates are permissible, or else it returns null
    public Rule parseRuleString(String ruleString) {
        String ruleParts[] = ruleString.split(":-");
        Rule actionRule = getActionRule(ruleParts[0]);
        List<Rule> predicates = getPredicates(ruleParts[1]);
        Rule output = unifyVariables(actionRule, predicates, new Hashtable<String, Unit>());
        return output;
    }

    //Parses out the action part of the Rule
    private Rule getActionRule(String rulePart) {
        String actionFunctor = rulePart.substring(0, rulePart.indexOf("("));
        Matcher matcher = Pattern.compile("\\((.*?)\\)").matcher(rulePart);
        Rule.Builder actionBuilder = new Rule.Builder(actionFunctor);
        if (matcher.find()) {
            String args[] = matcher.group(1).split(",");
            for (String arg : args) {
                if(arg.charAt(0) == '"'){
                    arg = arg.substring(1, arg.length() - 1).toLowerCase();
                } else {
                    arg = arg.toUpperCase();
                }
                actionBuilder = actionBuilder.parameter(arg);
            }
        }
        return actionBuilder.build();
    }

    //Parse out the predicates of a Rule
    private List<Rule> getPredicates(String rulePart) {
        /*
        Pattern pattern = Pattern.compile("(.+\\(.+\\)[,\\.])|(,[^\\(\\),]+,)");
        Matcher matcher = pattern.matcher(ruleParts[1]);
        */

        String predicateStrings[] = rulePart.split(";");
        List<Rule> predicates = new ArrayList<Rule>();
        for (String pred : predicateStrings) {
            pred = pred.trim();
            //System.out.println("Predicate: " + pred);
            int funcStart = 0;
            boolean invert = false;
            if(pred.charAt(0) == '~') {
                funcStart = 1;
                invert = true;
            }
            String functor = pred.substring(funcStart, pred.indexOf("("));
            //System.out.println("Functor: " + functor);
            Matcher matcher = Pattern.compile("\\((.*?)\\)").matcher(pred);
            Rule.Builder builder = new Rule.Builder(functor).invert(invert);
            if (matcher.find()) {
                String args[] = matcher.group(1).split(",");
                for (String arg : args) {
                    if(arg.charAt(0) == '"'){
                        arg = arg.substring(1, arg.length() - 1).toLowerCase();
                    } else {
                        arg = arg.toUpperCase();
                    }
                    builder = builder.parameter(arg);
                }
            }
            Rule newRule = builder.build();
            predicates.add(newRule);
        }
        return predicates;
    }

    //Using the parsed action and predicates, this checks to see if all the predicates are valid
    //If all predicates are permissible, then it returns an action that can be execute, otherwise it returns null
    private Rule unifyVariables(Rule rule, List<Rule> predicates, Hashtable<String, Unit> variables) {
//        if(rule.functor == RuleType.doBuildBase) {
//            System.out.println("Unify Rule: " + rule);
//            for (Rule pred : predicates)
//                System.out.println("Pred: " + pred);
//        }

        String variable = "#";
        if(predicates.size() == 0) {
            return createRuleVariables(rule, variables);
        }
        Rule predicate = predicates.remove(0);
        Rule matched = null;
        boolean invertMatch = false;
        List<Rule> localFacts = new ArrayList<Rule>(facts);
        Collections.shuffle(localFacts);
        for (Rule fact : localFacts) {
            if(fact.functor == predicate.functor) {
                //System.out.println("Compared Functors and Match: " + fact.functor);
                boolean match = true;
                for(String param : predicate.parameters){
                    //System.out.println("Param: " + param);
                    //System.out.println("Variable: " + variable);
                    if(param.charAt(0) > 96) {
                        if(!fact.hasParameter(param)) {
                            //System.out.println("Not a match");
                            match = false;
                        }
                    } else {
                        variable = param;
                        //System.out.println("Variable changed: " + variable);
                        Unit check = variables.get(variable);
                        //System.out.println("Check: " + check);
                        if(check != null && check != fact.actor) {
                            match = false;
                        }
                    }
                }
                if(match) {
//                    if(predicate.invert) {
//                        invertMatch = true;
//                    }
                    if(fact.actor != null) {
                        variables.put(variable, fact.actor);
                    }
                    if(predicate.invert)
                        return null;
                    Rule output = unifyVariables(rule, new ArrayList<Rule>(predicates), variables);
                    if(output != null) {
                        return output;
                    } else {
                        variables.remove(variable);
                    }
                }
            }
        }
        if(predicate.invert) {
            return unifyVariables(rule, new ArrayList<Rule>(predicates), variables);
        }
        return null;
    }

    //Creates the action to be executed in the GameState
    private Rule createRuleVariables(Rule rule, Hashtable<String, Unit> variables) {
        Rule.Builder builder = new Rule.Builder(rule.functor);
        List<String> ruleParams = new ArrayList<String>(rule.parameters);
        for(String param : rule.parameters){
            Unit unit = variables.get(param);
            if(unit != null) {
                ruleParams.remove(param);
                builder = builder.unit(unit);
            } else {
                builder = builder.parameter(param);
            }
        }
//        Unit unit = variables.get("#");
//        if(unit != null) {
//            builder = builder.unit(unit);
//        }
        Rule output = builder.build();
        //System.out.println("Created Rule: " + output);
        return output;
    }

    public boolean removeIdle(Unit unit) {
        for(int i = 0; i < facts.size(); i++) {
            Rule fact = facts.get(i);
            if(fact.functor == RuleType.idle && fact.actor == unit) {
                facts.remove(i);
                return true;
            }
        }
        return false;
    }

    public static void main(String args[]) {
        KnowledgeBase know = new KnowledgeBase();
        UnitType worker = new UnitType();
        Unit unit1 = new Unit(0, worker, 0, 0);
        Unit unit2 = new Unit(0, worker, 0, 1);
        Unit unit3 = new Unit(0, worker, 0, 2);
        Unit unit4 = new Unit(0, worker, 1, 3);
        Unit unit5 = new Unit(0, worker, 1, 4);
        Unit unit6 = new Unit(0, worker, 1, 5);
        know.facts.add(new Rule.Builder("enoughResourcesFor").parameter("light").unit(unit1).build());
        know.facts.add(new Rule.Builder("enoughResourcesFor").parameter("worker").unit(unit1).build());
        know.facts.add(new Rule.Builder("ownBase").parameter("base").unit(unit2).build());
        know.facts.add(new Rule.Builder("ownBarrack").parameter("base").unit(unit4).build());
        know.facts.add(new Rule.Builder("type").parameter("base").unit(unit3).build());
        know.facts.add(new Rule.Builder("own").parameter("base").unit(unit3).build());
        know.facts.add(new Rule.Builder("idle").parameter("worker").unit(unit2).build());
        know.facts.add(new Rule.Builder("idle").parameter("base").unit(unit4).build());

        //know.getRulesFromFile("/home/classified56/Documents/Drexel/Fall_2020/CS611/Projects/Project 4/rules-complex.txt");
        know.getRulesFromFile("/home/classified56/Documents/Drexel/Fall_2020/CS611/Projects/Project 4/rules-simple.txt");
        for(String rule : know.actions) {
            System.out.println("\n\nInput: " + rule);
            Rule output = know.parseRuleString(rule);
            System.out.print("Rule Output: " + output);
        }
    }
}
