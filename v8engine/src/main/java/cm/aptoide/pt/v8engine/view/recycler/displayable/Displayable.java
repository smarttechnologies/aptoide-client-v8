/*
 * Copyright (c) 2016.
 * Modified by SithEngineer on 12/08/2016.
 */

package cm.aptoide.pt.v8engine.view.recycler.displayable;

import android.os.Bundle;
import android.support.annotation.LayoutRes;
import android.support.annotation.Nullable;

import cm.aptoide.pt.annotation.Ignore;
import cm.aptoide.pt.model.v7.Type;
import cm.aptoide.pt.utils.AptoideUtils;
import cm.aptoide.pt.v8engine.interfaces.LifecycleSchim;
import cm.aptoide.pt.v8engine.view.recycler.widget.WidgetFactory;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import rx.subscriptions.CompositeSubscription;

/**
 * Created by neuro on 14-04-2016.
 */
@Ignore
@Accessors(chain = true)
public abstract class Displayable implements LifecycleSchim {

	@Getter CompositeSubscription subscriptions;
	private Boolean fixedPerLineCount;
	@Setter private Integer defaultPerLineCount;
	@Setter @Getter private boolean isVisible = false;

	/**
	 * Needed for reflective {@link Class#newInstance()}.
	 */
	public Displayable() {
	}

	public Displayable(boolean fixedPerLineCount) {
		this.fixedPerLineCount = fixedPerLineCount;
	}

	public abstract Type getType();

	@LayoutRes
	public abstract int getViewLayout();

	/**
	 * Same code as in {@link Type#getPerLineCount()} todo: terminar este doc
	 *
	 * @return
	 */
	public int getPerLineCount() {
		return isFixedPerLineCount() ? getDefaultPerLineCount() : (int) (AptoideUtils.ScreenU.getScreenWidthInDip() /
				AptoideUtils.ScreenU.REFERENCE_WIDTH_DPI * getDefaultPerLineCount());
	}

	public boolean isFixedPerLineCount() {
		return fixedPerLineCount == null ? getType() != null && getType().isFixedPerLineCount() :
				fixedPerLineCount;
	}

	public int getDefaultPerLineCount() {
		if (defaultPerLineCount == null) {
			if (getType() != null) {
				return getType().getDefaultPerLineCount();
			} else {
				return 1;
			}
		} else {
			return defaultPerLineCount;
		}
	}

	public int getSpanSize() {
		return WidgetFactory.getColumnSize() / getPerLineCount();
	}

	//
	// LifecycleSchim interface
	// optional methods

	public void onResume() {
		isVisible = true;
	}

	public void onPause() {
		isVisible = false;
	}

	public void onSaveInstanceState(Bundle outState) {
	}

	public void onViewStateRestored(@Nullable Bundle savedInstanceState) {
	}
}
