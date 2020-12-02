package ai.abstraction;

import ai.abstraction.pathfinding.AStarPathFinding;
import ai.abstraction.pathfinding.FloodFillPathFinding;
import ai.abstraction.pathfinding.GreedyPathFinding;
import ai.core.AI;
import ai.abstraction.pathfinding.PathFinding;
import ai.core.ParameterSpecification;

import java.util.*;

import rts.*;
import rts.units.*;
import java.util.List;

/**
 *
 * @author Davis Ranney
 */

public class RulesAI extends AbstractionLayerAI{

    protected UnitTypeTable utt;

    UnitType workerType;
    UnitType baseType;
    UnitType barracksType;
    UnitType lightType;
    UnitType heavyType;
    UnitType rangedType;

    KnowledgeBase knowledge;

    public RulesAI (UnitTypeTable a_utt) {
        this(a_utt, new AStarPathFinding());
    }

    public RulesAI (UnitTypeTable a_utt, PathFinding a_pf) {
        super(a_pf);
        reset(a_utt);
        knowledge = new KnowledgeBase();
        knowledge.getRulesFromFile("src/rules-complex.txt");
    }

    public void reset() {
        super.reset();
    }

    public void reset(UnitTypeTable a_utt) {
        utt = a_utt;
        workerType = utt.getUnitType("Worker");
        baseType = utt.getUnitType("Base");
        barracksType = utt.getUnitType("Barracks");
        lightType = utt.getUnitType("Light");
        heavyType = utt.getUnitType("Heavy");
        rangedType = utt.getUnitType("Ranged");
    }

    @Override
    public AI clone() {
        return new RulesAI(utt, pf);
    }

    @Override
    public PlayerAction getAction(int player, GameState gs) {
        Player p = gs.getPlayer(player);

        fillKnowledgeBase(gs, p);
//        for(Rule fact : knowledge.getFacts()) {
//            System.out.println("Fact: " + fact);
//        }
        List<Rule> executes = getActionRules(gs, p);
        executeActions(gs, p, executes);
        return translateActions(player, gs);
    }

    private void fillKnowledgeBase(GameState gs, Player p) {
        PhysicalGameState pgs = gs.getPhysicalGameState();
        knowledge.clearFacts();
        int resourcesAvailable = p.getResources();
        if(resourcesAvailable - barracksType.cost >= 0) {
            knowledge.addFact(new Rule.Builder("enoughResourcesFor").parameter("barracks").build());
        }
        if(resourcesAvailable - baseType.cost * 2 >= 0) {
            knowledge.addFact(new Rule.Builder("enoughResourcesFor").parameter("base").build());
        }
        if(resourcesAvailable - lightType.cost >= 0) {
            knowledge.addFact(new Rule.Builder("enoughResourcesFor").parameter("light").build());
        }
        if(resourcesAvailable - heavyType.cost >= 0) {
            knowledge.addFact(new Rule.Builder("enoughResourcesFor").parameter("heavy").build());
        }
        if(resourcesAvailable - rangedType.cost >= 0) {
            knowledge.addFact(new Rule.Builder("enoughResourcesFor").parameter("ranged").build());
        }
        if (resourcesAvailable - workerType.cost * 5 >= 0) {
            knowledge.addFact(new Rule.Builder("enoughResourcesFor").parameter("worker").build());
        }
        for(Unit unit : pgs.getUnits()) {
            //System.out.println("Parsing unit: " + unit);
            Rule newFact = null;
            String type = "";
            if(unit.getType() == baseType) {
                type = "base";
            } else if (unit.getType() == barracksType) {
                type = "barracks";
            } else if (unit.getType().isResource) {
                type = "resource";
            } else if (unit.getType() == lightType) {
                type = "light";
            } else if (unit.getType() == heavyType) {
                type = "heavy";
            } else if (unit.getType() == rangedType) {
                type = "ranged";
            } else if (unit.getType() == workerType) {
                type = "worker";
            }
            newFact = new Rule.Builder("type").unit(unit).parameter(type).build();
            //System.out.println(newFact);
            knowledge.addFact(newFact);
            if(unit.getPlayer() == p.getID()) {
                newFact = new Rule.Builder("own").unit(unit).parameter(type).build();
                //System.out.println(newFact);
                knowledge.addFact(newFact);
            } else if(unit.getPlayer() >= 0) {
                newFact = new Rule.Builder("enemy").unit(unit).parameter(type).build();
                //System.out.println(newFact);
                knowledge.addFact(newFact);
            }
//            if(unit.getType() == baseType){
//                System.out.println("Base Action:" + gs.getActionAssignment(unit));
//            }
            if(gs.getActionAssignment(unit) == null && !type.equals("resource") ) {
                newFact = new Rule.Builder("idle").unit(unit).parameter(type).build();
                //System.out.println(newFact);
                knowledge.addFact(newFact);
            }
        }
        //knowledge.shuffleFacts();
    }

    private List<Rule> getActionRules(GameState gs, Player p) {
        List<Rule> actions = new ArrayList<Rule>();
        for(String action : knowledge.getActions()) {
            //System.out.println("Checking: " + action);
            Rule output = knowledge.parseRuleString(action);
            if(output != null) {
                if(output.isAction()) {
                    actions.add(output);
                    knowledge.removeIdle(output.actor);
                    fillKnowledgeBase(gs, p);
                } else {
                    knowledge.addFact(output);
                }
            }
        }
        return actions;
    }

    private void executeActions(GameState gs, Player p, List<Rule> actions) {
        for(Rule action : actions) {
            executeAction(gs, p, action);
        }
    }

    private void executeAction(GameState gs, Player p, Rule action) {
    System.out.println("Action: " + action);
        switch (action.getFunctor()) {
            case doTrainWorker:
                trainUnit(action.actor, workerType, baseType, gs, p);
                break;
            case doTrainLight:
                trainUnit(action.actor, lightType, barracksType, gs, p);
                break;
            case doTrainHeavy:
                trainUnit(action.actor, heavyType, barracksType, gs, p);
                break;
            case doTrainRanged:
                trainUnit(action.actor, rangedType, barracksType, gs, p);
                break;
            case doHarvest:
                workerHarvest(gs, p, action);
                break;
            case doBuildBarracks:
                doBuild(action, barracksType, gs, p);
                break;
            case doBuildBase:
                doBuild(action, baseType, gs, p);
                break;
            case doAttack:
                attack(gs, p, action.actor);
                break;

        }
    }

    private void trainUnit(Unit base, UnitType unit, UnitType creator, GameState gs, Player p) {
        PhysicalGameState pgs = gs.getPhysicalGameState();
        if(base == null) {
            for (Unit u : pgs.getUnits()) {
                if (u.getType() == creator
                        && u.getPlayer() == p.getID()
                        && gs.getActionAssignment(u) == null) {
                    base = u;
                    break;
                }
            }
        }
        train(base, unit);
    }

    private void doBuild(Rule action, UnitType type, GameState gs, Player p) {
        PhysicalGameState pgs = gs.getPhysicalGameState();
        List<Integer> reservedPositions = new LinkedList<>();
        if(action.actor != null) {
            buildIfNotAlreadyBuilding(action.actor, type, action.actor.getX()+2, action.actor.getY()+2, reservedPositions, p, pgs);
        } else {
            List<Unit> unitList = pgs.getUnits();
            Collections.shuffle(unitList);
            for (Unit u : unitList) {
                if (u.getType().canHarvest && u.getPlayer() == p.getID() && gs.getActionAssignment(u) == null) {
                    buildIfNotAlreadyBuilding(u, type, u.getX()+2, u.getY()+2, reservedPositions, p, pgs);
                    break;
                }
            }
        }
    }

    private void workerHarvest(GameState gs, Player p, Rule work) {
        PhysicalGameState pgs = gs.getPhysicalGameState();
        Unit closestBase = null;
        Unit closestResource = null;
        int closestDistance = 0;
        Unit worker = work.actor;

        if(worker == null) {
            List<Unit> unitList = pgs.getUnits();
            Collections.shuffle(unitList);
            for (Unit u : unitList) {
                if (u.getType().canHarvest && u.getPlayer() == p.getID() && gs.getActionAssignment(u) == null) {
                    worker = u;
                    break;
                }
            }
        }
        if(worker == null)
            return;

        //System.out.println("WorkerHarvest: " + worker);
        for (Unit u2 : pgs.getUnits()) {
            if (u2.getType().isResource) {
                //System.out.println("Resource: " + u2);
                int d = Math.abs(u2.getX() - worker.getX()) + Math.abs(u2.getY() - worker.getY());
                if (closestResource == null || d < closestDistance) {
                    closestResource = u2;
                    closestDistance = d;
                }
            }
        }
        if(work.resource != null && (Math.abs(work.resource.getX() - worker.getX()) + Math.abs(work.resource.getY() - worker.getY()) < 8)) {
            closestResource = work.resource;
        }
        closestDistance = 0;
        for (Unit u2 : pgs.getUnits()) {
            if (u2.getType().isStockpile && u2.getPlayer()==p.getID()) {
                //System.out.println("Base: " + u2);
                int d = Math.abs(u2.getX() - worker.getX()) + Math.abs(u2.getY() - worker.getY());
                if (closestBase == null || d < closestDistance) {
                    closestBase = u2;
                    closestDistance = d;
                }
            }
        }
        boolean workerStillFree = true;
        if (worker.getResources() > 0) {
            //System.out.println("Worker has resources: " + worker);
            if (closestBase!=null) {
                harvest(worker, null, closestBase);
                workerStillFree = false;
            }
        } else {
            if (closestResource!=null && closestBase!=null) {
                harvest(worker, closestResource, closestBase);
                workerStillFree = false;
            }
        }
        if (workerStillFree) {
            System.out.println("Worker is attacking: " + worker);
            attack(gs, p, worker);
        }
    }

    private void attack(GameState gs, Player p, Unit attacker) {
        PhysicalGameState pgs = gs.getPhysicalGameState();
        Unit closestEnemy = null;
        int closestDistance = 0;

        if(attacker == null) {
            for (Unit u : pgs.getUnits()) {
                if (u.getType().canAttack && !u.getType().canHarvest
                        && u.getPlayer() == p.getID()
                        && gs.getActionAssignment(u) == null) {
                    attacker = u;
                    break;
                }
            }
        }
        if(attacker == null)
            return;
        for (Unit u2 : pgs.getUnits()) {
            if (u2.getPlayer() >= 0 && u2.getPlayer() != p.getID()) {
                int d = Math.abs(u2.getX() - attacker.getX()) + Math.abs(u2.getY() - attacker.getY());
                if (closestEnemy == null || d < closestDistance) {
                    closestEnemy = u2;
                    closestDistance = d;
                }
            }
        }
        if (closestEnemy != null) {
//            System.out.println("LightRushAI.meleeUnitBehavior: " + u + " attacks " + closestEnemy);
            attack(attacker, closestEnemy);
        }
    }

    @Override
    public List<ParameterSpecification> getParameters() {
        List<ParameterSpecification> parameters = new ArrayList<>();

        parameters.add(new ParameterSpecification("PathFinding", PathFinding.class, new AStarPathFinding()));

        return parameters;
    }


}
