package com.wars.app.actor.unit;


import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.scenes.scene2d.Action;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.actions.RemoveActorAction;
import com.badlogic.gdx.scenes.scene2d.actions.SequenceAction;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.wars.app.actor.listener.ApplyAttackListener;
import com.wars.app.actor.listener.OpenActionMenuListener;
import com.wars.app.actor.listener.UpdateInfoUnitListener;
import com.wars.app.actor.utils.*;
import com.wars.app.bean.game.component.event.OpenActionMenuComponent;
import com.wars.app.carte.CarteTexture;
import com.wars.app.playboard.PlayBoardActor;
import com.wars.app.utils.skin.unit.SkinUnit;
import com.wars.model.carte.Point;
import com.wars.model.playboard.unit.IUnit;
import com.wars.model.playboard.unit.Unit;
import dagger.Lazy;
import lombok.AccessLevel;
import lombok.Delegate;
import lombok.Getter;
import lombok.Setter;

import javax.inject.Provider;
import java.util.Observable;
import java.util.Spliterator;
import java.util.stream.StreamSupport;

@Getter
@Setter
public abstract class UnitActor extends WarsActor implements IUnit {
    private Point point;
    @Delegate(types = IUnit.class)
    private Unit unit;
    private Lazy<PlayBoardActor> lazyPlayboardActor;
    private Provider<OpenActionMenuComponent.Builder> openActionBuilder;

    @Getter(AccessLevel.NONE)
    @Setter(AccessLevel.NONE)
    private BarreLifeActor barreLife;

    @Getter(AccessLevel.NONE)
    @Setter(AccessLevel.NONE)
    private BarreExperienceActor barreExp;

    @Getter(AccessLevel.NONE)
    @Setter(AccessLevel.NONE)
    private boolean isDisable;
    private State currentState;

    private SkinUnit skinUnit;

    public enum State {
        WALKING, ATTACK, IDLE
    }

    public UnitActor(final Point point,
                     final Unit unit,
                     final BarreLifeActor barreLife,
                     final BarreExperienceActor barreExperienceActor,
                     final UpdateInfoUnitListener updateInfoUnitListener,
                     final SkinUnit skinUnit,
                     final Lazy<PlayBoardActor> lazyPlayboardActor,
                     final Provider<OpenActionMenuComponent.Builder> openActionBuilder) {
        super(skinUnit.getTextureWalking(unit.getTypeUnit()));
        this.skinUnit = skinUnit;
        this.unit = unit;
        this.lazyPlayboardActor = lazyPlayboardActor;
        this.openActionBuilder = openActionBuilder;
        this.setPoint(point);
        this.barreLife = barreLife;
        this.barreExp = barreExperienceActor;
        this.addActor(barreLife);
        this.addActor(barreExp);
        updateInfoUnitListener.setUnitActor(this);
        this.addListener(updateInfoUnitListener);
    }

    public void initUnit() {
        if (isUnitFromCurrentPlayer()) {
            this.setWalking();
            this.addListener(this.openActionBuilder.get().unitActor(this).build().getOpenActionMenu());
        } else {
            this.setIdle();
            this.setColor(new Color(0xFFAA00FF));
        }
    }

    private boolean isUnitFromCurrentPlayer() {
        return this.unit.getPlayer() != null && this.unit.getPlayer().equals(lazyPlayboardActor.get().getPlayBoard().getCurrentPlayer());
    }

    /**
     * Applique les actions necessaires apres une rule
     */
    public void resetUnit() {
        if (isUnitFromCurrentPlayer()) {
            this.addListener(this.openActionBuilder.get().unitActor(this).build().getOpenActionMenu());
        } else {
            //A voir
        }
    }

    public void setPoint(Point point) {
        this.point = point;
        this.setY(point.getY() * CarteTexture.TILE_SIZE);
        this.setX(point.getX() * CarteTexture.TILE_SIZE);
    }

    @Override
    public void update(Observable o, Object arg) {
        this.removeListenerType(OpenActionMenuListener.class);
        this.removeListenerType(ApplyAttackListener.class);
        initUnit();

    }

    /**
     * Effectue l"action d'attack sur la cible et desactive l'unit
     *
     * @param target
     * @return
     */
    public Boolean makeAttackAction(UnitActor target) {
        if (this.isInRange(target)) {
            PlayBoardActor current = lazyPlayboardActor.get();
            long pvStart = target.getPv();
            current.getPlayBoard().applyAttack(this.point, target.getPoint());
            this.addAction(Actions.sequence(getActionAttack(target), target.getAfterAttack(pvStart - target.getPv())));
            this.disable(true);
            this.setIdle();
            return true;
        } else {
            return false;
        }
    }

    public void setIdle() {
        this.currentState = State.IDLE;
        this.setTextureList(skinUnit.getTexture(this.unit.getTypeUnit()));
    }

    public void setWalking() {
        this.currentState = State.WALKING;
        this.setTextureList(skinUnit.getTextureWalking(this.unit.getTypeUnit()));
    }

    public void setAttacking() {
        this.setTextureList(skinUnit.getTexture(this.unit.getTypeUnit()));
        this.currentState = State.ATTACK;
    }


    /**
     * desactive l'unité
     */
    public void disable(boolean isDisable) {
        this.isDisable = isDisable;
        if (isDisable) {
            this.setColor(Color.GRAY);
            setIdle();
        } else {
            this.setColor(Color.CLEAR);
        }
    }

    /**
     * Retourne une action à effectuer sur en tant que cible aprés une attack
     *
     * @return
     */
    protected Action getAfterAttack(Long degat) {
        if (this.isDead()) {
            return getIsDeadAction();
        }
        SequenceAction sequenceAction = new SequenceAction(Actions.fadeOut(0.1f), Actions.fadeIn(0.1f));
        sequenceAction.setActor(this);

        Label pvEnleve = new DegatPopActor(degat.toString(), this);
        this.addActor(pvEnleve);

        return sequenceAction;
    }

    private RemoveActorAction getIsDeadAction() {
        return Actions.removeActor(this);
    }

    ;


    private boolean isInRange(UnitActor target) {
        return unit.getRange() >= this.point.getDistance(target.getPoint());
    }


    protected abstract Action getActionAttack(UnitActor target);


    @Override
    public void draw(Batch batch, float parentAlpha) {
        updateBarreDeVie();
        updateBarreExp();
        super.draw(batch, parentAlpha);
    }

    /**
     * Calcul la distance de vision en fonction du terrain sur lequel est le joueur
     *
     * @return
     */
    public Long calculateDistanceVision() {
        return this.getVision() + lazyPlayboardActor.get().getCarte().getCarte().getValue(this.point).getVisionAdvantage();
    }

    private void updateBarreDeVie() {
        this.barreLife.setRatioRestante((float) unit.getPv() / (float) unit.getPvMax());
    }

    private void updateBarreExp() {
        this.barreExp.setRatioRestante((float) unit.getExp() / (float) IUnit.EXP_MAX);
    }

    private boolean isDead() {
        return this.getPv() == 0;
    }

    @Override
    public void clearAttachedActor() {
        Spliterator<Actor> spliterator = this.getChildren().spliterator();
        StreamSupport.stream(spliterator, false).filter(actor -> actor instanceof ContainerWars).forEach(Actor::remove);
    }
}
