package ostrowski.util;

// Java Imports

public class CombatSemaphore {

// order 1:-------------------------------------------------------------------------------------------------------------------------------------
   public static final int CLASS_CHARACTER_EQUIPMENT                 = Semaphore.DependsOn(0);
   public static final int CLASS_COMBATSERVER_pausePlayControl       = Semaphore.DependsOn(0);
   public static final int CLASS_COMBATSERVER_pendingMessage         = Semaphore.DependsOn(0);

// order 2:-------------------------------------------------------------------------------------------------------------------------------------
   public static final int CLASS_ARENA_proxyList                     = Semaphore.DependsOn(CLASS_CHARACTER_EQUIPMENT);
   public static final int CLASS_ARENALOCATION_this                  = Semaphore.DependsOn(CLASS_CHARACTER_EQUIPMENT);
// order 3:-------------------------------------------------------------------------------------------------------------------------------------
   public static final int CLASS_BATTLE_waitingToAttack              = Semaphore.DependsOn(CLASS_ARENALOCATION_this);
   public static final int CLASS_ARENA_mapCombatantsToAI             = Semaphore.DependsOn(CLASS_ARENALOCATION_this);
// order 4:-------------------------------------------------------------------------------------------------------------------------------------
   public static final int CLASS_BATTLE_aimingCharacters             = Semaphore.DependsOn(CLASS_ARENA_proxyList,
                                                                                           CLASS_ARENALOCATION_this,
                                                                                           CLASS_ARENA_mapCombatantsToAI);
   public static final int CLASS_ARENA_combatants                    = Semaphore.DependsOn(CLASS_ARENA_proxyList,
                                                                                           CLASS_ARENA_mapCombatantsToAI);
// order 5:-------------------------------------------------------------------------------------------------------------------------------------

// order 8:-------------------------------------------------------------------------------------------------------------------------------------
   public static final int CLASS_MAPWIDGET3_locationToObjectMap      = Semaphore.DependsOn(AnimationControllerSemaphore.CLASS_BODYPART_CHILDREN_BODY,
                                                                                           AnimationControllerSemaphore.CLASS_OBJHEX_HUMANS);
// order 9:-------------------------------------------------------------------------------------------------------------------------------------
   public static final int CLASS_MAPWIDGET3_animatedObjects          = Semaphore.DependsOn(AnimationControllerSemaphore.CLASS_GLVIEW_MODELS);
   public static final int CLASS_ARENA_locationRequests              = Semaphore.DependsOn(AnimationControllerSemaphore.CLASS_GLVIEW_MODELS);
// order 10:-------------------------------------------------------------------------------------------------------------------------------------
   public static final int CLASS_MAPWIDGET3D_animationsPending       = Semaphore.DependsOn(CLASS_MAPWIDGET3_animatedObjects);
}
