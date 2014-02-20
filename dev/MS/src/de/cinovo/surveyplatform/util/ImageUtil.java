package de.cinovo.surveyplatform.util;

import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Transparency;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;

import org.eclipse.jetty.util.IO;

/**
 * 
 * Copyright 2010 Cinovo AG
 * 
 * @author yschubert
 * 
 */
public class ImageUtil {
	
	public static void createThumbnailWithCrop(int x, int y, int w, int h, final int targetWidth, final int targetHeight, final File imageFile, final File targetFile, final int viewPortWidth) throws IOException {
		final BufferedImage img = ImageIO.read(imageFile);
		double zoomfactor = (double) img.getWidth() / viewPortWidth;
		x *= zoomfactor;
		y *= zoomfactor;
		w *= zoomfactor;
		h *= zoomfactor;
		final BufferedImage scaledImg = ImageUtil.getScaledInstance(img.getSubimage(x, y, w, h), targetWidth, targetHeight, RenderingHints.VALUE_INTERPOLATION_BICUBIC, false);
		ImageIO.write(scaledImg, "jpg", targetFile);
		img.flush();
		scaledImg.flush();
	}
	
	public static void createThumbnail(final File imageFile, final File targetFile, final int viewPortWidth, final int viewPortHeight) throws IOException {
		final BufferedImage img = ImageIO.read(imageFile);
		if (img != null) {
			Dimension newDim = ImageUtil.resizeDimension(img.getWidth(), img.getHeight(), viewPortWidth, viewPortHeight);
			boolean highQuality = false;
			if ((newDim.height < img.getHeight()) || (newDim.width < img.getWidth())) {
				// only create thumbnail when downscaling
				highQuality = true;
				final BufferedImage scaledImg = ImageUtil.getScaledInstance(img, (int) newDim.getWidth(), (int) newDim.getHeight(), RenderingHints.VALUE_INTERPOLATION_BILINEAR, highQuality);
				ImageIO.write(scaledImg, "jpg", targetFile);
				scaledImg.flush();
			} else {
				IO.copy(imageFile, targetFile);
			}
			img.flush();
		}
	}
	
	public static int[] getSize(final File imageFile) {
		int[] size = new int[2];
		try {
			final BufferedImage img = ImageIO.read(imageFile);
			if (img != null) {
				size[0] = img.getWidth();
				size[1] = img.getHeight();
				img.flush();
			}
		} catch (Exception ex) {
			Logger.err("Cannot get size from image: " + imageFile, ex);
		}
		
		return size;
	}
	
	/**
	 * Convenience method that returns a scaled instance of the provided
	 * {@code BufferedImage}.
	 * 
	 * @param img
	 *            the original image to be scaled
	 * @param targetWidth
	 *            the desired width of the scaled instance, in pixels
	 * @param targetHeight
	 *            the desired height of the scaled instance, in pixels
	 * @param hint
	 *            one of the rendering hints that corresponds to
	 *            {@code RenderingHints.KEY_INTERPOLATION} (e.g.
	 *            {@code RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR},
	 *            {@code RenderingHints.VALUE_INTERPOLATION_BILINEAR},
	 *            {@code RenderingHints.VALUE_INTERPOLATION_BICUBIC})
	 * @param higherQuality
	 *            if true, this method will use a multi-step scaling technique
	 *            that provides higher quality than the usual one-step technique
	 *            (only useful in downscaling cases, where {@code targetWidth}
	 *            or {@code targetHeight} is smaller than the original
	 *            dimensions, and generally only when the {@code BILINEAR} hint
	 *            is specified)
	 * @return a scaled version of the original {@code BufferedImage}
	 */
	public static BufferedImage getScaledInstance(final BufferedImage img, final int targetWidth, final int targetHeight, final Object hint, final boolean higherQuality) {
		int type = (img.getTransparency() == Transparency.OPAQUE) ? BufferedImage.TYPE_INT_RGB : BufferedImage.TYPE_INT_ARGB;
		BufferedImage ret = img;
		int w, h;
		if (higherQuality) {
			// Use multi-step technique: start with original size, then
			// scale down in multiple passes with drawImage()
			// until the target size is reached
			w = img.getWidth();
			h = img.getHeight();
		} else {
			// Use one-step technique: scale directly from original
			// size to target size with a single drawImage() call
			w = targetWidth;
			h = targetHeight;
		}
		
		do {
			if (higherQuality && (w > targetWidth)) {
				w /= 2;
				if (w < targetWidth) {
					w = targetWidth;
				}
			}
			
			if (higherQuality && (h > targetHeight)) {
				h /= 2;
				if (h < targetHeight) {
					h = targetHeight;
				}
			}
			
			BufferedImage tmp = new BufferedImage(w, h, type);
			Graphics2D g2 = tmp.createGraphics();
			g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, hint);
			g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
			g2.drawImage(ret, 0, 0, w, h, null);
			g2.dispose();
			ret.flush();
			ret = tmp;
		} while ((w != targetWidth) || (h != targetHeight));
		
		return ret;
	}
	
	public static int[] resize(final int oldWidth, final int oldHeight, final int maxWidth, final int maxHeight, final boolean shrinkOnly) {
		int[] newSize = new int[2];
		Dimension resizeDimension = ImageUtil.resizeDimension(oldWidth, oldHeight, maxWidth, maxHeight);
		if (shrinkOnly && ((resizeDimension.width > oldWidth) || (resizeDimension.height > oldHeight))) {
			newSize[0] = oldWidth;
			newSize[1] = oldHeight;
		} else {
			newSize[0] = resizeDimension.width;
			newSize[1] = resizeDimension.height;
		}
		return newSize;
	}
	
	private static Dimension resizeDimension(final int oldWidth, final int oldHeight, final int maxWidth, final int maxHeight) {
		
		boolean takeNewWidth = false;
		boolean takeNewHeight = false;
		
		int newHeight;
		int newWidth;
		
		double factor = (double) maxHeight / maxWidth;
		
		if ((oldWidth < maxWidth) || (oldHeight < maxHeight)) {
			if (oldHeight > maxHeight) {
				takeNewHeight = true;
			}
			if (oldWidth > maxWidth) {
				takeNewHeight = false;
				takeNewWidth = true;
			}
		} else {
			// new height AND new width are smaller
			if (oldHeight <= (oldWidth * factor)) {
				takeNewWidth = true;
			} else {
				takeNewHeight = true;
			}
		}
		
		if (takeNewHeight) {
			newHeight = maxHeight;
			newWidth = (int) Math.round(((double) oldWidth / oldHeight) * newHeight);
		} else if (takeNewWidth) {
			newWidth = maxWidth;
			newHeight = (int) Math.round(((double) oldHeight / oldWidth) * newWidth);
		} else {
			newWidth = oldWidth;
			newHeight = oldHeight;
		}
		
		return new Dimension(newWidth, newHeight);
	}
}
