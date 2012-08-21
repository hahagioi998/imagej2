/*
 * #%L
 * ImageJ software for multidimensional image processing and analysis.
 * %%
 * Copyright (C) 2009 - 2012 Board of Regents of the University of
 * Wisconsin-Madison, Broad Institute of MIT and Harvard, and Max Planck
 * Institute of Molecular Cell Biology and Genetics.
 * %%
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDERS OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 * 
 * The views and conclusions contained in the software and documentation are
 * those of the authors and should not be interpreted as representing official
 * policies, either expressed or implied, of any organization.
 * #L%
 */

package imagej.ui.swing.tools.overlay;

import imagej.data.display.ImageDisplay;
import imagej.data.display.ImageDisplayService;
import imagej.data.display.OverlayView;
import imagej.data.overlay.Overlay;
import imagej.data.overlay.RectangleOverlay;
import imagej.event.StatusService;
import imagej.ext.display.event.input.MsButtonEvent;
import imagej.ext.display.event.input.MsDraggedEvent;
import imagej.ext.display.event.input.MsPressedEvent;
import imagej.ext.plugin.Plugin;
import imagej.ui.swing.overlay.AbstractJHotDrawAdapter;
import imagej.ui.swing.overlay.IJCreationTool;
import imagej.ui.swing.overlay.JHotDrawAdapter;
import imagej.ui.swing.overlay.JHotDrawTool;
import imagej.util.IntCoords;
import imagej.util.RealCoords;

import java.awt.Point;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

import org.jhotdraw.draw.Figure;
import org.jhotdraw.draw.RectangleFigure;

/**
 * TODO
 * 
 * @author Lee Kamentsky
 * @author Grant Harris
 * @author Barry DeZonia
 */
@Plugin(type = JHotDrawAdapter.class, name = "Rectangle",
	description = "Rectangular overlays",
	iconPath = "/icons/tools/rectangle.png",
	priority = SwingRectangleTool.PRIORITY, enabled = true)
public class SwingRectangleTool extends
	AbstractJHotDrawAdapter<RectangleOverlay>
{

	public static final double PRIORITY = 100;

	// initial mouse down point is recorded for status bar updates
	private final Point anchor = new Point();

	protected static RectangleOverlay downcastOverlay(final Overlay roi) {
		assert (roi instanceof RectangleOverlay);
		return (RectangleOverlay) roi;
	}

	@Override
	public boolean supports(final Overlay overlay, final Figure figure) {
		if ((figure != null) && (!(figure instanceof RectangleFigure))) return false;
		if (overlay instanceof RectangleOverlay) return true;
		return false;
	}

	@Override
	public Overlay createNewOverlay() {
		return new RectangleOverlay(getContext());
	}

	@Override
	public Figure createDefaultFigure() {
		final RectangleFigure figure = new RectangleFigure();
		initDefaultSettings(figure);
		return figure;
	}

	@Override
	public void updateFigure(final OverlayView overlay, final Figure f) {
		super.updateFigure(overlay, f);
		final RectangleOverlay rectOvr =
			downcastOverlay(overlay.getData());
		final double x0 = rectOvr.getOrigin(0);
		final double y0 = rectOvr.getOrigin(1);
		final double w = rectOvr.getExtent(0);
		final double h = rectOvr.getExtent(1);
		final Point2D.Double anch = new Point2D.Double(x0, y0);
		final Point2D.Double lead = new Point2D.Double(x0 + w, y0 + h);
		f.setBounds(anch, lead);
	}

	@Override
	public void updateOverlay(final Figure figure, final OverlayView overlay) {
		super.updateOverlay(figure, overlay);
		final RectangleOverlay rectOvr = downcastOverlay(overlay.getData());
		final Rectangle2D.Double bounds = figure.getBounds();
		rectOvr.setOrigin(bounds.getMinX(), 0);
		rectOvr.setOrigin(bounds.getMinY(), 1);
		rectOvr.setExtent(bounds.getWidth(), 0);
		rectOvr.setExtent(bounds.getHeight(), 1);
		rectOvr.update();
	}

	// NB - show x,y,w,h of rectangle in StatusBar on click-drag

	// click - record start point
	@Override
	public void onMouseDown(final MsPressedEvent evt) {
		if (evt.getButton() != MsButtonEvent.LEFT_BUTTON) return;
		anchor.x = evt.getX();
		anchor.y = evt.getY();
		// NB: Prevent PixelProbe from overwriting the status bar.
		evt.consume();
	}

	// drag - publish rectangle dimensions in status bar
	@Override
	public void onMouseDrag(final MsDraggedEvent evt) {
		if (evt.getButton() != MsButtonEvent.LEFT_BUTTON) return;
		final StatusService statusService =
			evt.getContext().getService(StatusService.class);
		final ImageDisplayService imgService =
			evt.getContext().getService(ImageDisplayService.class);
		final ImageDisplay imgDisp = imgService.getActiveImageDisplay();
		final IntCoords startPt = new IntCoords(anchor.x, anchor.y);
		final IntCoords endPt =
			new IntCoords(evt.getX() - anchor.x, evt.getY() - anchor.y);
		final RealCoords startPtModelSpace =
			imgDisp.getCanvas().panelToDataCoords(startPt);
		final RealCoords endPtModelSpace =
			imgDisp.getCanvas().panelToDataCoords(endPt);
		final int x = (int) startPtModelSpace.x;
		final int y = (int) startPtModelSpace.y;
		final int w = (int) endPtModelSpace.x;
		final int h = (int) endPtModelSpace.y;
		final String message = String.format("x=%d, y=%d, w=%d, h=%d", x, y, w, h);
		statusService.showStatus(message);
		// NB: Prevent PixelProbe from overwriting the status bar.
		evt.consume();
	}

	@Override
	public JHotDrawTool getCreationTool(final ImageDisplay display) {
		return new IJCreationTool(display, this);
	}

}