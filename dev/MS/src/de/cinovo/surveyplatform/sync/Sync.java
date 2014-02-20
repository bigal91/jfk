/**
 *
 */
package de.cinovo.surveyplatform.sync;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Copyright 2012 Cinovo AG<br>
 * <br>
 * Annotate fields with this annotation when taking part in a sync
 * process
 * 
 * @author yschubert
 * 
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface Sync {
	
	/**
	 * set a filter to define which fields shall be synced. If no filter is
	 * given, the synchronizer will sync for all
	 */
	SyncFilter[] filter() default {};
	
	/**
	 * indicate that the synchronizer shall recurse into the instance of this
	 * field
	 */
	boolean recurse() default false;
	
	/**
	 * override the getter name for the annotated field
	 */
	String getterName() default "";
	
}
