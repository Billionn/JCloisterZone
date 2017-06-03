package com.jcloisterzone.feature;

import java.lang.reflect.Method;

import com.jcloisterzone.Immutable;
import com.jcloisterzone.board.Position;
import com.jcloisterzone.board.Rotation;
import com.jcloisterzone.board.pointer.FeaturePointer;
import com.jcloisterzone.game.Game;

import io.vavr.collection.List;

@Immutable
public abstract class TileFeature implements Feature {

    protected final Game game; //Game is not immutable! is it ok?
    protected final List<FeaturePointer> places;

    public TileFeature(Game game, List<FeaturePointer> places) {
        this.game = game;
        this.places = places;
    }

    public Game getGame() {
        return game;
    }

    @Override
    public List<FeaturePointer> getPlaces() {
       return places;
    }

    @Override
    public String toString() {
        return String.format("%s%s", getClass().getSimpleName(), places);
    }

    public static String getLocalizedNamefor(Class<? extends Feature> feature) {
        try {
            Method m = feature.getMethod("name");
            return (String) m.invoke(null);
        } catch (Exception e) {
            return feature.getSimpleName();
        }
    }

    // immutable helpers

    protected List<FeaturePointer> mergePlaces(TileFeature obj) {
        return this.places.appendAll(obj.places);
    }

    protected List<FeaturePointer> placeOnBoardPlaces(Position pos, Rotation rot) {
        return this.places.map(fp -> fp.rotateCW(rot).translate(pos));
    }

}
