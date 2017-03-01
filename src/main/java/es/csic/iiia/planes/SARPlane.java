package es.csic.iiia.planes;

import es.csic.iiia.planes.Plane.Type;
import es.csic.iiia.planes.liam.LIAMBehavior;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.logging.Level;

/**
 * Created on 12/11/2015.
 * Implementation of a Search and Rescue (SAR) Plane, that implements very basic lower-level
 * behavior.
 *
 * @author Guillermo Bautista <gbau at mit.edu>
 */
public class SARPlane extends AbstractPlane {

    /**
     * Current plane state
     */
//     private State state = State.NORMAL;

    /**
     * Current plane type
     * By default, all planes start as SEARCHER.
     */
    private Type type = Type.SEARCHER;

    /**
     * Percentage power level at which plane switches to EAGLE type.
     */
//     private long eagleEnergy;

    /**
     * Percentage power level at which plane switches to STANDBY type.
     */
  //   private long standbyEnergy;

    /**
     * Number of blocks that a Plane tries to stay away from other
     * active UAV's while in Eagle or Scout mode.
     */
    private int eagleCrowdDistance;

    /**
     * Preferred range that an Eagle-type plane will try to stay within
     * for the next available block to search.
     */
    private double eagleJumpDistance;

    /**
     * Preferred range that a Scout-type plane will try to stay within
     * for the next available block to search.
     */
    private double scoutJumpDistance;

    /**
     * Penalty incurred on battery power for searching a block.
     */
  //  private long searchPowerPenalty;

    /**
     * Penalty incurred on waiting time for searching a block.
     */
    private long searchTimePenalty;

    /**
     * Penalty incurred on battery power for rescuing a survivor.
     */
    //private double rescuePowerPenalty;

    /**
     * Penalty incurred on waiting time for rescuing a survivor.
     */
    private double rescueTimePenalty;

    /**
     * Agent maximum speed in meters per tenth of second.
     * This is the speed at which Standby planes travel.
     */
    private double maxSpeed = -1;

    /**
     * Eagle-type speed as a percentage of maximum speed.
     */
    private double eagleSpeedPercentage;

    /**
     * Scout-type speed as a percentage of maximum speed.
     */
    private double scoutSpeedPercentage;

    /**
     * Tells the system whether the agent's initial destination has been initialized.
     */
    private boolean initialized = false;

    /**
     * Next block to be completed by the plane
     */
    private Block nextBlock = null;

    /**
     * Next region to be completed by the plane
     * Only used for Scouts
     * Noura: used for Searcher and rescuer
     */
    private Region nextRegion = null;

    /**
     * Time in tenthes of a second before plane switches
     * to Rescue Type.
     */
    private long rescueTime;

    /**
     * Default constructor
     *
     * @param location initial location of the plane
     */
    public SARPlane(Location location) {
        super(location);
        addBehavior(new LIAMBehavior(this));
    }

    @Override
    public void initialize() {
        super.initialize();
       // searchPowerPenalty = getWorld().getConfig().getSearchPowerPenalty();
        searchTimePenalty = getWorld().getConfig().getSearchTimePenalty();
        //rescuePowerPenalty = getWorld().getConfig().getRescuePowerPenalty();
        rescueTimePenalty = getWorld().getConfig().getRescueTimePenalty();
        rescueTime = getWorld().getConfig().getRescueTime();

        if(initialized) {
            if (type == Type.SEARCHER) {
                setNextRegion();
                setNextBlock(nextRegion);
            }
            else {
                setNextBlockRescue();
            }
            setDestination(nextBlock.getCenter());
        }
        else{
            initialized = true;
        }
    }


    /**
     * Get the type of this plane.
     *
     * @see Type
     * @return type of this plane.
     */
    public Type getType() { return type; }

    /**
     * Set the type of this plane.
     *
     * @see Type
     */
    public void setType(Type t) {
        this.type = t;
         setSpeed(maxSpeed);
        /* noura
        if (t == Type.SEARCHER) {
            setSpeed(maxSpeed*scoutSpeedPercentage);
        } else if (t == Type.RESCUER) {
            setSpeed(maxSpeed*eagleSpeedPercentage);
        } else {
            setSpeed(maxSpeed);
        } noura */
    }

    private int getEagleCrowdDistance() { return eagleCrowdDistance; }

    public void setEagleCrowdDistance(int eagleCrowdDistance) { this.eagleCrowdDistance = eagleCrowdDistance; }

    public void setEagleJumpDistance(double eagleJumpDistance) { this.eagleJumpDistance = eagleJumpDistance; }

    public void setScoutJumpDistance(double scoutJumpDistance) {
        this.scoutJumpDistance = scoutJumpDistance;
    }


    public void setEagleSpeedPercentage(double eagleSpeedPercentage) {
        this.eagleSpeedPercentage = eagleSpeedPercentage;
    }

    public void setScoutSpeedPercentage(double scoutSpeedPercentage) {
        this.scoutSpeedPercentage = scoutSpeedPercentage;
    }


    @Override
    public List<Location> getPlannedLocations() {
        List<Location> plannedLocations = new ArrayList<Location>();

        plannedLocations.add(getCurrentDestination().destination);
        return plannedLocations;
    }


    @Override
    public void step() {
        //TODO: Make this time a set percentage of time in the configuration
        //Switch to rescuer if number of beacones <= threshold 
        if (getNumOfbeacones()<= getthresholdB() && type != Type.RESCUER ) {
            setType(Type.RESCUER);
            setIdelTime(0);
        }else 
            if( getIdelTime()>=10&& type != Type.SEARCHER){
                setType(Type.SEARCHER);
                setIdelTime(0);
            
            }
                //TODO:// Code to continue finishing up tasks the plane was doing before it had to charge
                if(nextBlock != null) {
                    setDestination(nextBlock.getCenter());
                }
                else { // if there is no assign block 
                    if (type == Type.SEARCHER) 
                        if(setNextRegion()) {
                            setNextBlock(nextRegion);
                        }
                        else 
                            if (type==Type.RESCUER)
                            if (!setNextBlockRescue()) {
                               // idleAction();
                                nextRegion = null;
                                nextBlock = null;
           
        }
                }

       
        // Handle this iteration's messages
       //Noura  super.step();

        // Iterate through all of the plane's list of survivors it's trying
        // to find, and see if they have died at this point.
        // TODO: Uncomment when survivors can expire
        if (!tasksToRemove.isEmpty()) {
            for (Task t:tasksToRemove) {
                t.expire();
                getWorld().removeExpired(t);
            }
        }

        tasksToRemove.clear();


        // Move the plane if it has some task to fulfill and is not charging
        // or going to charge
        if (nextBlock != null) {
            if (getWaitingTime() > 0) {
                idleAction();
                tick();
                return;
            }
            else if (move()) {
                final Block completed = nextBlock;

                if(type == Type.SEARCHER) {
                    stepSearcher(completed);
                }
                else if(type == Type.RESCUER) {
                
                    // checkRegionExplored();
                    if(completed.getBeaconOrNot() && completed.getSurvivor().isAlive()) {
                        triggerTaskCompleted(completed);
                        nextBlock.setState(Block.blockState.OK);
                        completed.setBeaconOrNot(false);
                        setNumOfbeacones(getNumOfbeacones()+1);
                        
                    }
                    else{ 
                        if(!(completed.getSurvivor().isAlive()))
                        nextBlock.setState(Block.blockState.Empty);
                        completed.setBeaconOrNot(false);
                        setNumOfbeacones(getNumOfbeacones()+1);
                    }
                    checkRegionExplored();
                    //setNextBlockRescue();
                     if (!setNextBlockRescue()) {
                               // idleAction();
                                nextRegion = null;
                                nextBlock = null;
           
        }
                }
        
            }
            return;
        }

        // If we reach this point, it means that the plane is idle, so let it
        // do some "idle action"
        idleAction();
    }


    /**
     * Actions performed by the plane whenever it is in Scout mode.
     */
    private void stepSearcher(Block completed) {
       
        waitFor(searchTimePenalty);
       //check  checkRegionExplored();
        if(completed.hasSurvivor() && completed.getSurvivor().isAlive()) {
            nextRegion.taskFound();
            nextRegion.setNumOfbeacones(nextRegion.getNumOfbeacones()+1);
            //noura triggerTaskCompleted(completed);
           completed.setState(Block.blockState.Pending);
           completed.setBeaconOrNot(true);
           setNumOfbeacones(getNumOfbeacones()-1);
        }else
        { 
                        if(!completed.hasSurvivor()||!(completed.getSurvivor().isAlive()))
                        nextBlock.setState(Block.blockState.Empty);
                   
                    }
         checkRegionExplored();
        if(nextRegion.getState() == Region.regionState.Explored) {
            //System.out.print("Scout:\nCurrent region explored:\n");
                  
              if(setNextRegion()) {
                    //System.out.print("Scout:\nFound unassigned region, assigning new block:\n");
                    setNextBlock(nextRegion);
                }
                else {
                        nextRegion = null;
                       nextBlock = null;
                      
                }
            }
        
        else {
            //System.out.print("Scout:\nCurrent region still not fully explored:\n");
            if(!setNextBlock(nextRegion)) {
                //System.out.print("Scout:\nCurrent region's blocks are fully assigned, searching for new region\n");
                if(setNextRegion()) {
                    //System.out.print("Scout:\nFound unassigned region, assigning new block:\n");
                    setNextBlock(nextRegion);
                }
                else {
                    
                        nextRegion = null;
                        nextBlock = null;
                       
            }
           
        }
   
    }
    }
    
    private void checkRegionExplored() {
        boolean remainingAssignment = false;
        boolean explored = true;
        for (Block b:getWorld().getBlocks()[nextRegion.getID()]) {
            if (b.getState() == Block.blockState.Pending) {
                remainingAssignment = true;
                explored = false;
            }
            if (b.getState() == Block.blockState.Unexplored) {
                explored = false;
            }
        }
       //noura  if( type == Type.RESCUER && !explored && !remainingAssignment){
            if( !explored && !remainingAssignment){
            nextRegion.setState(Region.regionState.Pending);
            return;
        }
        if(explored) {
            nextRegion.setState(Region.regionState.Explored);
        }
    }

    /**
     * Action done by the plane whenever it is ready to handle tasks but no
     * task has been assigned to it.
     */
    protected void idleAction() {
        //Noura we need for rescuer 
        if (!getIdleStrategy().idleAction(this)) {
            double newAngle = getAngle() + 0.01;
            setAngle(newAngle);
            setIdelTime(getIdelTime()+1);
           
        }
    }

    @Override
    public void setSpeed(double speed) {
        super.setSpeed(speed);
        // Section below only runs upon initialization to set the maximum speed
        if (this.maxSpeed < 0) {
            this.maxSpeed = speed;
            super.setSpeed(maxSpeed*scoutSpeedPercentage);
        }
    }

    
    /**TODO: Make sure this is coded well.
     * Signals that a task has been completed.
     *
     * @param b block in which task has been completed.
     */
  /*  private void taskCompleted(Block b) {
        if( type == Type.SEARCHER) {
            for (Block[] blocks : getWorld().getBlocks()) {
                for (Block block : blocks) {
                    if (block.getId() == b.getId()) {
                        block.setState(Block.blockState.OK);
                    }
                }
            }
        }
    }*/

    protected void taskCompleted(Task t) {}

 /* Noura   private void setNextBlockBasic() {
        Random rnd = new Random();
        if (getWorld().getUnassignedBlocks().size() < 1){
            idleAction();
            return;
        }
        nextBlock = getWorld().getUnassignedBlocks().remove(rnd.nextInt(getWorld().getUnassignedBlocks().size()));
        setDestination(nextBlock.getCenter());
    }*/
    /**
     * Sets the next region that this plane is going to fulfill.
     *
     * This plane will fly in a straight line towards that location, until one
     * of the following happens:
     *   - It reaches (and thus completes) the given task.
     *   - It runs out of battery (and therefore goes to recharse itself).
     *   - A new "next region" is set by calling this method again.
     *
     */
    private boolean setNextRegion() {
        List<Region> regionsNear = new ArrayList<Region>();
        List<Region> regionsFar = new ArrayList<Region>();

      //  Random rnd = new Random();


        for (Region r:getWorld().getRegions()) {
            if (r.getState()== Region.regionState.Unexplored) {
                if (this.getLocation().getDistance(r.getCenter()) < scoutJumpDistance) {
                    regionsNear.add(r);
                }
                else {
                    regionsFar.add(r);
                }
            }
        }

        if (!regionsNear.isEmpty()) {
            //select nearest not randomly
            Region t=regionsNear.remove(0);
            regionsNear.add(t);
            for(Region r:regionsNear){
            if(this.getLocation().getDistance(r.getCenter())<= this.getLocation().getDistance(t.getCenter()))
                t=r;
            }
            nextRegion = regionsNear.remove(regionsNear.indexOf(t));
            nextRegion.setState(Region.regionState.Pending);
            return true;
        }
        else if (!regionsFar.isEmpty()) {
            //select nearest not randomly
             Region t=regionsFar.remove(0);
            regionsFar.add(t);
            for(Region r:regionsFar){
            if(this.getLocation().getDistance(r.getCenter())<= this.getLocation().getDistance(t.getCenter()))
                t=r;
            }
            nextRegion = regionsFar.remove(regionsFar.indexOf(t));
            
            nextRegion.setState(Region.regionState.Pending);
            return true;
        }
        else {
            return false;
        }
    }

    /**
     * Sets the next region that this plane is going to fulfill.
     *
     * This plane will fly in a straight line towards that location, until one
     * of the following happens:
     *   - It reaches (and thus completes) the given task.
     *   - It runs out of battery (and therefore goes to recharse itself).
     *   - A new "next region" is set by calling this method again.
     *
     * @param r region to select block from.
     */
    private boolean setNextBlock(Region r) {

        //First check if region we are attempting to get a block from is fully assigned:
        List<Block> availableBlocks = new ArrayList<Block>();
        int id = r.getID();
        for (Block block:getWorld().getBlocks()[id]) {
            if (block.getState()== Block.blockState.Unexplored) {
                availableBlocks.add(block);
            }
        }
        if (availableBlocks.isEmpty()) {
            return false;
        }
        else {
     //select nearest not randomly
          //  Random rand = new Random();
           /*  Region t=regionsNear.remove(0);
            regionsNear.add(t);
            for(Region r:regionsNear){
            if(this.getLocation().getDistance(r.getCenter())<= this.getLocation().getDistance(t.getCenter()))
                t=r;
            }
            nextRegion = regionsNear.remove(regionsNear.indexOf(t));
            nextRegion.setState(Region.regionState.Pending);
            return true;
            */
            Block t=availableBlocks.remove(0);
            availableBlocks.add(t);
            for(Block s:availableBlocks){
            if(this.getLocation().getDistance(s.getCenter())<= this.getLocation().getDistance(t.getCenter()))
                t=s;
            }
            nextBlock = availableBlocks.get(availableBlocks.indexOf(t));
            getWorld().getUnassignedBlocks().remove(nextBlock);

            //System.out.print("Assigned block:\n");
            //System.out.print("Region ID: "+id+", Block ID: "+nextBlock.getId()+"\n");
            //System.out.print("Location: "+nextBlock.getCenter().getX()+", "+nextBlock.getCenter().getY()+"\n");
            
            // may we use it !(ASSIGNED block)
           //noura nextBlock.setState(Block.blockState.ASSIGNED);
            setDestination(nextBlock.getCenter());
            return true;
        }
    }

    //TODO: Double check if this is how Heba wants crowd distance control.
    private boolean crowdCheck(Block b) {

        int leftBound = b.getxLoc() - getEagleCrowdDistance();
        int rightBound = b.getxLoc() + getEagleCrowdDistance() + 1;
        int lowerBound = b.getyLoc() - getEagleCrowdDistance();
        int upperBound = b.getyLoc() + getEagleCrowdDistance() + 1;

        if(leftBound < 0) {
            leftBound = 0;
        }
        if(rightBound >= getWorld().getBlockGrid().length) {
            rightBound = getWorld().getBlockGrid().length - 1;
        }
        if(lowerBound < 0) {
            lowerBound = 0;
        }
        if(upperBound >= getWorld().getBlockGrid().length) {
            upperBound = getWorld().getBlockGrid().length - 1;
        }

        for (int i = leftBound; i < rightBound; i++) {
            for (int j = lowerBound; j < upperBound; j++) {
                Block a = getWorld().getBlockGrid()[i][j];
                if (Math.abs(a.getxLoc()-b.getxLoc())+Math.abs(a.getyLoc()-b.getyLoc()) < getEagleCrowdDistance() &&
                        a.getState() == Block.blockState.Pending) {
                    return false;
                }
            }
        }
//        for (Block[] blocks: getWorld().getBlocks()) {
//            for (Block a: blocks) {
//                if (Math.abs(a.getxLoc()-b.getxLoc())+Math.abs(a.getyLoc()-b.getyLoc()) < getEagleCrowdDistance() &&
//                        a.getState() == Block.blockState.ASSIGNED) {
//                    return false;
//                }
//            }
//        }
        return true;
    }

    /*Noura protected void setNextBlockStandby(Block b) {
        nextBlock = b;
        nextRegion = getWorld().getRegions().get(b.getRegion());
        setDestination(b.getCenter());
    }*/

    /**
     * Sets the next region that this plane is going to fulfill.
     *
     * This plane will fly in a straight line towards that location, until one
     * of the following happens:
     *   - It reaches (and thus completes) the given task.
     *   - It runs out of battery (and therefore goes to recharges itself).
     *   - A new "next region" is set by calling this method again.
     *
     */
    private boolean setNextBlockRescue() {

        //boolean allExplored = true;
        List<Region> regionsUnexplored = new ArrayList<Region>();
        int maxCrowded = -1;

        for (Region r: getWorld().getRegions()) {
            if (r.getState() != Region.regionState.Explored) {
                regionsUnexplored.add(r);
                if(r.getTasksFound() > maxCrowded) {
                    nextRegion = r;
                    maxCrowded = r.getTasksFound();
                }
            }
        }

        if (regionsUnexplored.isEmpty()) {
            nextRegion = null;
            nextBlock = null;
            return false;
        }

        List<Block> searchList = new ArrayList<Block>();
        for (Block b: getWorld().getBlocks()[nextRegion.getID()]) {
            if (b.getState() != Block.blockState.Pending) {
                searchList.add(b);
            }
        }
        
            Block t=searchList.remove(0);
            searchList.add(t);
            for(Block s:searchList){
            if(this.getLocation().getDistance(s.getCenter())<= this.getLocation().getDistance(t.getCenter()))
                t=s;
            }
      //  Random rnd = new Random();
        nextBlock = searchList.get(searchList.indexOf(t));
        getWorld().getUnassignedBlocks().remove(nextBlock);
          return true;
//        for (Block[] blocks: getWorld().getBlocks()) {
//            for (Block b: blocks) {
//                if (b.getState() != Block.blockState.EXPLORED) {
//                    allExplored = false;
//                }
//            }
//        }
//        if (allExplored) {
//            return false;
//        }
//        else {
//
//            for (Block[] blocks: getWorld().getBlocks()) {
//
//                for (Block b: blocks) {
//                    if (b.getState() == Block.blockState.EXPLORED && b.hasSurvivor()) {
//                        nextBlock = b;
//                        setDestination(nextBlock.getCenter());
//                        return true;
//                    }
//                }
//
//            }
//            return false;
//        }
    }

    /**
     * Record a task completion trigger any post-completion effects
     *
     * @param b block in which task has been completed
     */
    private void triggerTaskCompleted(Block b) {
        Task t = b.getSurvivor();
        getLog().log(Level.FINE, "{0} completes {1}", new Object[]{this, t});
        getCompletedLocations().add(t.getLocation());
       // noura if (getType() != Type.STANDBY && getType() != Type.EAGLE) {
         //   getWorld().foundTask(t);
        //}
        getWorld().removeTask(t);
        removeTask(t);
        taskCompleted(t);
        final long timeLeft = getWorld().getDuration() - getWorld().getTime()%getWorld().getDuration();
        waitFor((long)(timeLeft*rescueTimePenalty));
     //   getBattery().consume((long)(getBattery().getEnergy()*rescuePowerPenalty));
//        if (nextBlock == null) {
//            Operator o = getWorld().getNearestOperator(getLocation());
//            setDestination(o.getLocation());
//        }
    }

    /**TODO: Change to instead of setNextTask, setNextBlock or setNextRegion
     * Signals that a new task has been added.
     *
     * @param t task that has been added.
     */
    protected void taskAdded(Task t) {}

    /**TODO
     * Signals that a task has been removed.
     *
     * @param t task that has been removed.
     */
    protected void taskRemoved(Task t) {}

    @Override
    public Task removeTask(Task task) {
        for (Plane p : getWorld().getPlanes()) {
            p.getSearchForTasks().remove(task);
            p.getTasks().remove(task);
        }
        // TODO: Remove next line?
        taskRemoved(task);
        return task;
    }

}