/*
 * Copyright (C) 2014-2015 OpenKeeper
 *
 * OpenKeeper is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * OpenKeeper is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with OpenKeeper.  If not, see <http://www.gnu.org/licenses/>.
 */
package toniarts.openkeeper.game.trigger;

import com.jme3.app.state.AppStateManager;
import com.jme3.math.FastMath;
import java.awt.Point;
import java.util.EnumSet;
import java.util.logging.Logger;
import toniarts.openkeeper.game.action.ActionPoint;
import toniarts.openkeeper.game.action.ActionPointState;
import toniarts.openkeeper.game.control.Control;
import toniarts.openkeeper.game.party.Party;
import toniarts.openkeeper.game.party.PartytState;
import toniarts.openkeeper.game.player.PlayerCameraControl;
import toniarts.openkeeper.game.state.GameState;
import toniarts.openkeeper.game.state.PlayerState;
import toniarts.openkeeper.game.state.SoundState;
import toniarts.openkeeper.tools.convert.ConversionUtils;
import toniarts.openkeeper.tools.convert.map.Thing;
import toniarts.openkeeper.tools.convert.map.TriggerAction;
import toniarts.openkeeper.tools.convert.map.TriggerAction.FlagTargetValueActionType;
import toniarts.openkeeper.tools.convert.map.TriggerGeneric;
import toniarts.openkeeper.view.PlayerCameraState;
import toniarts.openkeeper.world.WorldState;

/**
 *
 * @author ArchDemon
 */

public class TriggerControl extends Control {

    protected TriggerGenericData trigger;
    protected TriggerGenericData root;

    protected AppStateManager stateManager;
    protected boolean checked = true;
    private static final Logger logger = Logger.getLogger(TriggerControl.class.getName());

    public TriggerControl() {
    }

    public TriggerControl(final AppStateManager stateManager, int triggerId) {

        this.stateManager = stateManager;
        root = new TriggerLoader(this.stateManager.getState(GameState.class).getLevelData()).load(triggerId);
        if (root == null) {
            throw new IllegalArgumentException("trigger can not be null");
        }
        trigger = root;
    }

    @Override
    protected void updateControl(float tpf) {
        TriggerGenericData next = null;
        trigger.subRepeatTimes();

        for (int i = trigger.getLastTriggerIndex() + 1; i < trigger.getQuantity(); i++) {
            TriggerData value = trigger.getChild(i);

            if (value == null) {
                logger.warning("Trigger is null");

            } else if (value instanceof TriggerGenericData) {

                if (next == null && isActive((TriggerGenericData) value)) {
                    trigger.setLastTrigger((TriggerGenericData) value);
                    next = (TriggerGenericData) value;
                }

            } else if (value instanceof TriggerActionData) {

                //System.out.println(String.format("%s: %d %s", this.getClass().getSimpleName(), trigger.getId(), trigger.getType()));
                doAction((TriggerActionData) value);
                if (!trigger.isRepeateable()) {
                    trigger.detachChild(value);
                    i--;
                }
            }
        }

        if (trigger.getQuantity() == 0 && trigger.getParent() != null) {
            next = trigger.getParent();
            trigger.detachFromParent();
        }

        if (next == null) {
            trigger.setLastTrigger(null);
            trigger = (trigger.getParent() != null) ? trigger.getParent() : root;
        } else {
            trigger = next;
        }
    }

    protected boolean isActive(TriggerGenericData trigger) {
        int value = 0;
        int target = 0;
        boolean result = false;
        checked = true;

        TriggerGeneric.TargetType targetType = trigger.getType();
        switch (targetType) {
            case FLAG:
                target = stateManager.getState(GameState.class).getFlag((Short) trigger.getUserData("targetId"));
                if ((Short) trigger.getUserData("flag") == 1) {
                    value = (Integer) trigger.getUserData("value");
                } else {
                    value = stateManager.getState(GameState.class).getFlag((Short) trigger.getUserData("flagId"));
                }
                break;

            case TIMER:
                target = (int) stateManager.getState(GameState.class).getTimer((Short) trigger.getUserData("targetId")).getTime();
                if ((Short) trigger.getUserData("flag") == 1) {
                    value = (Integer) trigger.getUserData("value");
                } else {
                    value = (int) stateManager.getState(GameState.class).getTimer((Short) trigger.getUserData("timerId")).getTime();
                }
                break;

            case LEVEL_TIME:
                target = (int) stateManager.getState(GameState.class).getGameTime();
                value = (Integer) trigger.getUserData("value");
                break;
            case LEVEL_CREATURES:
                return false;
            case LEVEL_PAY_DAY:
                return false;
            case LEVEL_PLAYED:
                return false;
            default:
                checked = false;
                // logger.warning("Target Type not supported");
                return false;
        }

        TriggerGeneric.ComparisonType comparisonType = trigger.getComparison();
        if (comparisonType != null && comparisonType != TriggerGeneric.ComparisonType.NONE) {
            result = compare(target, comparisonType, value);
        }

        return result;
    }

    protected void doAction(TriggerActionData trigger) {
        //System.out.println(String.format("\t Action: %d %s", trigger.getId(), trigger.getType())); // TODO remove this line

        TriggerAction.ActionType type = trigger.getType();
        switch (type) {
            case CREATE_CREATURE:
                break;
            case DISPLAY_OBJECTIVE:
                break;
            case MAKE:
                TriggerAction.MakeType flag = ConversionUtils.parseEnum(trigger.getUserData("type", short.class),
                        TriggerAction.MakeType.class);
                boolean available = trigger.getUserData("available", short.class) != 0;
                short playerId = trigger.getUserData("playerId", short.class);

                switch (flag) {
                    case CREATURE:
                        break;
                    case DOOR:
                        break;
                    case KEEPER_SPELL:
                        break;
                    case ROOM:
                        break;
                    case TRAP:
                        break;
                }
                break;

            case FLAG:
                short flagId = trigger.getUserData("flagId", short.class);
                EnumSet<FlagTargetValueActionType> flagType = ConversionUtils.parseFlagValue(trigger.getUserData("flag", short.class),
                        FlagTargetValueActionType.class);
                int value = trigger.getUserData("value", int.class);
                if (flagType.contains(FlagTargetValueActionType.TARGET)) {
                    value = stateManager.getState(GameState.class).getFlag(value);
                }

                if (flagId == 128) {
                    PlayerState ps = stateManager.getState(PlayerState.class);
                    ps.setScore(getTargetValue(ps.getScore(), value, flagType));
                } else {
                    int base = stateManager.getState(GameState.class).getFlag(flagId);
                    stateManager.getState(GameState.class).setFlag(flagId, getTargetValue(base, value, flagType));
                }
                break;

            case INITIALIZE_TIMER:
                short timerId = trigger.getUserData("timerId", short.class);
                if (timerId == 16) {
                    stateManager.getState(GameState.class).setTimeLimit(trigger.getUserData("value", int.class));
                } else {
                    stateManager.getState(GameState.class).getTimer(trigger.getUserData("timerId", short.class)).setActive(true);
                }
                break;

            case FLASH_BUTTON:
                PlayerState player = stateManager.getState(PlayerState.class);
                TriggerAction.MakeType buttonType = ConversionUtils.parseEnum(trigger.getUserData("type", short.class),
                        TriggerAction.MakeType.class);
                short targetId = trigger.getUserData("targetId", short.class);
                boolean enable = trigger.getUserData("available", short.class) != 0;
                int time = trigger.getUserData("value", int.class);
                player.flashButton(targetId, buttonType, enable, time);
                break;

            case WIN_GAME:
                stateManager.getState(GameState.class).setEnd(true);
                break;

            case LOSE_GAME:
                stateManager.getState(GameState.class).setEnd(false);
                break;

            case CREATE_HERO_PARTY:
                Party party = getParty(trigger.getUserData("partyId", short.class));
                party.setType(ConversionUtils.parseEnum(trigger.getUserData("type", short.class), Party.Type.class));
                ActionPoint ap = getActionPoint(trigger.getUserData("actionPointId", short.class));

                //KwdFile kwdFile = stateManager.getState(GameState.class).getLevelData();
                //BulletAppState bulletState = stateManager.getState(BulletAppState.class);
                //AssetManager assetManager = app.getAssetManager();
                for (Thing.GoodCreature creature : party.getMembers()) {
                    // TODO create
                    //GameCreature c = CreatureLoader.load(creature, bulletState, assetManager, kwdFile);
                }
                party.setCreated(true);
                break;

            case SET_OBJECTIVE:
                break;
            case FLASH_ACTION_POINT:
                WorldState world = stateManager.getState(WorldState.class);
                ap = getActionPoint((Short) trigger.getUserData("actionPointId"));
                time = trigger.getUserData("value", int.class);
                enable = trigger.getUserData("available", short.class) != 0;
                for (int x = (int) ap.getStart().x; x <= (int) ap.getEnd().x; x++) {
                    for (int y = (int) ap.getStart().y; y <= (int) ap.getEnd().y; y++) {
                        world.flashTile(x, y, time, enable);
                    }
                }
                break;

            case REVEAL_ACTION_POINT:
                // remove fog of war from tiles in action point
                // or
                // add fog of war to tiles in action point
                break;
            case SET_ALLIANCE:
                break;
            case ATTACH_PORTAL_GEM:
                break;
            case ALTER_TERRAIN_TYPE:
                Point pos = new Point(trigger.getUserData("posX", int.class), trigger.getUserData("posY", int.class));
                short terrainId = trigger.getUserData("terrainId", short.class);
                playerId = trigger.getUserData("playerId", short.class);
                stateManager.getState(WorldState.class).alterTerrain(pos, terrainId, playerId);
                break;

            case PLAY_SPEECH:
                int speechId = trigger.getUserData("speechId", int.class);
                stateManager.getState(SoundState.class).attachSpeech(speechId);

                int pathId = trigger.getUserData("pathId", int.class);
                // text show when Cinematic camera by pathId
                boolean introduction = trigger.getUserData("introduction", short.class) != 0;
                if (trigger.getUserData("text", short.class) == 0) {
                    stateManager.getState(PlayerState.class).setText(speechId, introduction, pathId);
                }
                break;

            case DISPLAY_TEXT_MESSAGE:
                int textId = trigger.getUserData("textId", int.class);
                // TODO display text message
                break;

            case ZOOM_TO_ACTION_POINT:
                PlayerCameraState pcs = stateManager.getState(PlayerCameraState.class);
                ap = getActionPoint(trigger.getUserData("targetId", short.class));
                ap.addControl(new PlayerCameraControl(pcs.getCamera()));
                break;

            case ROTATE_AROUND_ACTION_POINT:
                // TODO Not tested
                ap = getActionPoint(trigger.getUserData("targetId", short.class));
                boolean relative = trigger.getUserData("available", short.class) == 0;
                int angle = trigger.getUserData("angle", int.class);
                time = trigger.getUserData("time", int.class);
                // TODO convert to control
                pcs = stateManager.getState(PlayerCameraState.class);
                pcs.setCameraLookAt(ap);
                pcs.addRotation(angle * FastMath.DEG_TO_RAD, time);
                break;

            case GENERATE_CREATURE:
                break;
            case SHOW_HEALTH_FLOWER:
                break;
            case FOLLOW_CAMERA_PATH:
                // TODO disable control
                //GameState.setEnabled(false);
                pcs = stateManager.getState(PlayerCameraState.class);
                ap = getActionPoint(trigger.getUserData("actionPointId", short.class));
                pcs.doTransition(trigger.getUserData("pathId", short.class), ap);
                break;

            case COLLAPSE_HERO_GATE:
                break;
            case SET_PORTAL_STATUS:
                break;
            case SET_WIDESCREEN_MODE:
                PlayerState ps = stateManager.getState(PlayerState.class);
                enable = trigger.getUserData("available", short.class) != 0;
                ps.setWideScreen(enable);
                break;

            case MAKE_OBJECTIVE:
                break;
            case ZOOM_TO:
                break;
            case SET_CREATURE_MOODS:
                break;
            case SET_SYSTEM_MESSAGES:
                break;
            case DISPLAY_SLAB_OWNER:
                break;
            case DISPLAY_NEXT_ROOM_TYPE:
                break;
            case CHANGE_ROOM_OWNER:
                break;
            case SET_SLAPS_LIMIT:
                break;
            case SET_TIMER_SPEECH:
                break;
            default:
                logger.warning("Action not supported");
                break;
        }
    }

    protected boolean compare(float target, TriggerGeneric.ComparisonType compare, float value) {
        boolean result = false;
        switch (compare) {
            case EQUAL_TO:
                result = target == value;
                break;
            case GREATER_OR_EQUAL_TO:
                result = target >= value;
                break;
            case GREATER_THAN:
                result = target > value;
                break;
            case LESS_OR_EQUAL_TO:
                result = target <= value;
                break;
            case LESS_THAN:
                result = target < value;
                break;
            case NOT_EQUAL_TO:
                result = target != value;
                break;
            case NONE:
                logger.warning("Comparison Type not supported");
                break;
        }
        return result;
    }

    protected ActionPoint getActionPoint(int id) {
        return stateManager.getState(ActionPointState.class).getActionPoint(id);
    }

    protected Party getParty(int id) {
        return stateManager.getState(PartytState.class).getParty(id);
    }

    private int getTargetValue(int base, int value, EnumSet<FlagTargetValueActionType> flagType) {

        if (flagType.contains(FlagTargetValueActionType.EQUAL)) {
            return value;
        } else if (flagType.contains(FlagTargetValueActionType.PLUS)) {
            return base + value;
        } else if (flagType.contains(FlagTargetValueActionType.MINUS)) {
            return base - value;
        }

        logger.warning("Unsupported target flag type");
        return 0;
    }
}
