package ai.abstraction;

import rts.units.*;

import java.util.ArrayList;
import java.util.List;

public class Rule {
    RuleType functor;
    List<String> parameters;
    Unit actor;
    Unit target;
    Unit resource;
    boolean invert;

    private Rule(String f) {
        functor = RuleType.valueOf(f);
        parameters = new ArrayList<String>();
        invert = false;
    }

    private Rule(RuleType f) {
        functor = f;
        parameters = new ArrayList<String>();
        invert = false;
    }

    public RuleType getFunctor() {
        return functor;
    }

    public boolean hasParameter(String param) {
        return parameters.contains(param);
    }

    public boolean isAction() {
        switch (functor) {
            case doTrainWorker:
            case doTrainLight:
            case doTrainHeavy:
            case doTrainRanged:
            case doHarvest:
            case doBuildBarracks:
            case doBuildBase:
            case doAttack:
                return true;
            case type:
            case own:
            case ownBarrack:
            case ownBase:
            case ownWorker:
            case enemy:
            case idle:
            case idleLight:
            case idleHeavy:
            case idleRanged:
            case idleWorker:
            case enoughResourcesFor:
            default:
                return false;
        }
    }

    public static class Builder {
        Rule rule;
        List<Unit> units;

        public Builder(String f) {
            rule = new Rule(f);
            units = new ArrayList<Unit>();
        }

        public Builder(RuleType f) {
            rule = new Rule(f);
            units = new ArrayList<Unit>();
        }

        public Builder parameter(String param) {
            rule.parameters.add(param);
            return this;
        }

        public Builder invert(boolean bool) {
            rule.invert = bool;
            return this;
        }

        public Builder unit(Unit unit) {
            units.add(unit);
            return this;
        }

        public Rule build() {
            if(units.size() > 0) {
                switch (rule.functor) {
                    case doAttack:
                        rule.actor = units.get(0);
                        rule.target = units.get(0);
                        break;
                    case doHarvest:
                        rule.actor = units.get(0);
                        rule.resource = units.get(1);
                        rule.target = units.get(2);
                        break;
                    default:
                        rule.actor = units.get(0);
                }
            }
            return rule;
        }
    }

    @Override
    public String toString() {
        String line = "Functor: ";
        line += functor.name() + "; Invert " + invert + "; Params: ";
        for(String param : parameters)
            line += param + " ";
        if(actor != null)
            line += "; Actor: " + actor.toString();
        if(target != null)
            line += "; Target: " + target.toString();
        if(resource != null)
            line += "; Target: " + resource.toString();
        return line;
    }
}
