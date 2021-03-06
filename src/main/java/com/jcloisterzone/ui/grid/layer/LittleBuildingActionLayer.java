package com.jcloisterzone.ui.grid.layer;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import com.jcloisterzone.LittleBuilding;
import com.jcloisterzone.action.LittleBuildingAction;
import com.jcloisterzone.board.Position;
import com.jcloisterzone.board.Rotation;
import com.jcloisterzone.ui.GameController;
import com.jcloisterzone.ui.ImmutablePoint;
import com.jcloisterzone.ui.grid.ActionLayer;
import com.jcloisterzone.ui.grid.GridMouseAdapter;
import com.jcloisterzone.ui.grid.GridMouseListener;
import com.jcloisterzone.ui.grid.GridPanel;

public class LittleBuildingActionLayer extends AbstractTileLayer implements ActionLayer<LittleBuildingAction>, GridMouseListener {

    protected static final Composite SHADOW_COMPOSITE = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, .4f);
    private static final double PADDING_RATIO = 0.10;

    private Map<LittleBuilding, Image> images = new HashMap<>();
    private LittleBuildingAction action;
    private LittleBuilding selected = null;

    private HashMap<LittleBuilding, Rectangle> areas = new HashMap<>();

    int icoSize, padding;

    public LittleBuildingActionLayer(GridPanel gridPanel, GameController gc) {
        super(gridPanel, gc);
        for (LittleBuilding lb : LittleBuilding.values()) {
            Image img = rm.getImage("neutral/lb-"+lb.name().toLowerCase());
            images.put(lb, img);
        }
        recomputeDimenensions(getTileWidth());
    }

    @Override
    public void setAction(boolean active, LittleBuildingAction action) {
        this.action = action;
        setPosition(action == null ? null : getGame().getCurrentTile().getPosition());
        if (active) {
            prepareAreas();
        } else {
            selected = null;
            areas.clear();
        }
    }

    @Override
    public LittleBuildingAction getAction() {
        return action;
    }

    private void recomputeDimenensions(int squareSize) {
        icoSize = squareSize / 2;
        padding = (int) (icoSize * PADDING_RATIO);
    }


    @Override
    public void zoomChanged(int squareSize) {
        recomputeDimenensions(squareSize);
        areas.clear();
        prepareAreas();
        super.zoomChanged(squareSize);
    }

    @Override
    public void boardRotated(Rotation boardRotation) {
        areas.clear();
        prepareAreas();
        super.boardRotated(boardRotation);
    }

    private int getIconX(LittleBuilding lb) {
        int x = -icoSize / 2 - padding * 3;
        return x + lb.ordinal() * (icoSize + 3*padding);
    }

    @SuppressWarnings("unused")
    private int getIconY(LittleBuilding lb) {
        return -icoSize / 2;
    }


    private void prepareAreas() {
        if (action == null) return;

        AffineTransform at = getAffineTransformIgnoringRotation(getPosition());

        for (LittleBuilding lb : LittleBuilding.values()) {
            if (action.getOptions().contains(lb)) {
                int x = getIconX(lb), y = getIconY(lb);
                Rectangle rect = new Rectangle(x-padding, y-padding, icoSize+2*padding, icoSize+2*padding);
                areas.put(lb, at.createTransformedShape(rect).getBounds());
            }
        }
    }

    @Override
    public void paint(Graphics2D g2) {
        Composite origComposite = g2.getComposite();
        ImmutablePoint shadowOffset = new ImmutablePoint(3, 3);
        shadowOffset = shadowOffset.rotate(gridPanel.getBoardRotation().inverse());

        for (LittleBuilding lb : action.getOptions()) {
            Rectangle r = areas.get(lb);
            if (r == null) continue;
            g2.setComposite(SHADOW_COMPOSITE);
            g2.setColor(Color.BLACK);
            g2.fillRect(r.x+shadowOffset.getX(), r.y+shadowOffset.getY(), r.width, r.height);
            g2.setComposite(origComposite);
            g2.setColor(selected == lb ? Color.WHITE : Color.GRAY);
            g2.fill(r);

            Image img = images.get(lb);
            int x = getIconX(lb), y = getIconY(lb);
            AffineTransform at = getAffineTransform(getPosition(), gridPanel.getBoardRotation().inverse());
            at.concatenate(AffineTransform.getTranslateInstance(x, y));
            at.concatenate(AffineTransform.getScaleInstance(icoSize / (double) img.getWidth(null), icoSize / (double) img.getHeight(null)));
            g2.drawImage(images.get(lb), at, null);
        }

    }

    private class MoveTrackingGridMouseAdapter extends GridMouseAdapter {

        public MoveTrackingGridMouseAdapter(GridPanel gridPanel, GridMouseListener listener) {
            super(gridPanel, listener);
        }

        @Override
        public void mouseMoved(MouseEvent e) {
            super.mouseMoved(e);
            Point2D point = gridPanel.getRelativePoint(e.getPoint());
            int x = (int) point.getX();
            int y = (int) point.getY();
            LittleBuilding newValue = null;
            for (Entry<LittleBuilding, Rectangle> entry : areas.entrySet()) {
                if (entry.getValue().contains(x, y)) {
                    newValue = entry.getKey();
                    break;
                }
            }
            if (newValue != selected) {
                selected = newValue;
                gridPanel.repaint();
            }
        }

    }

    @Override
    protected GridMouseAdapter createGridMouserAdapter(GridMouseListener listener) {
        return new MoveTrackingGridMouseAdapter(gridPanel, listener);
    }

    @Override
    public void squareEntered(MouseEvent e, Position p) {
    }

    @Override
    public void squareExited(MouseEvent e, Position p) {

    }

    @Override
    public void mouseClicked(MouseEvent e, Position p) {
        if (e.getButton() == MouseEvent.BUTTON1) {
            if (selected != null) {
                action.perform(getRmiProxy(), selected);
                e.consume();
            }
        }
    }


}
