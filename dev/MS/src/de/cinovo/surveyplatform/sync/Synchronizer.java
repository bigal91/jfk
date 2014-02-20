/**
 *
 */
package de.cinovo.surveyplatform.sync;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import de.cinovo.surveyplatform.util.Logger;

/**
 * Copyright 2012 Cinovo AG<br>
 * <br>
 * This class synchronizes two object with each other. This is done by copying
 * the content of each field from object A to object B. When a field is marked
 * with @Sync(recurse=true) the algorithm decends to the object in order to make
 * a deep copy of the object that is the content of the field. <br>
 * This is especially useful to handle hibernate objects with proxy stuff
 * 
 * @author yschubert
 * 
 */
public class Synchronizer {
	
	/**
	 *
	 */
	private static final String PREFIX_SETTER = "set";
	/**
	 *
	 */
	private static final String PREFIX_GETTER_BOOLEAN = "is";
	/**
	 *
	 */
	private static final String PREFIX_GETTER = "get";
	/**
	 *
	 */
	private static final String CLONE_METHOD_NAME = "clone";
	private SyncCloneCallback cloneCallback;
	private SyncEqualsCallback equalsCallback;
	
	private SyncPreSyncCallback preSyncCallback;
	private SyncPostSyncCallback postSyncCallback;
	
	
	/**
	 * The standard equals() and clone() are used
	 */
	public Synchronizer() {
		// --
	}
	
	/**
	 * Use this constructor to give custom clone() and equal() implementations
	 */
	public Synchronizer(final SyncCloneCallback cloneCallback, final SyncEqualsCallback equalsCallback) {
		this.cloneCallback = cloneCallback;
		this.equalsCallback = equalsCallback;
	}
	
	public void setCloneCallback(final SyncCloneCallback cloneCallback) {
		this.cloneCallback = cloneCallback;
	}
	
	public void setEqualsCallback(final SyncEqualsCallback equalsCallback) {
		this.equalsCallback = equalsCallback;
	}
	
	public void setPreSyncCallback(final SyncPreSyncCallback preSyncCallback) {
		this.preSyncCallback = preSyncCallback;
	}
	
	public void setPostSyncCallback(final SyncPostSyncCallback postSyncCallback) {
		this.postSyncCallback = postSyncCallback;
	}
	
	/**
	 * Syncronizes objects from left to right (data from left is copied to the
	 * right)
	 * 
	 * @param left the left object
	 * @param right the right object
	 * @param syncFilter specify the filter to use for the sync process
	 * @return true if annotations could be found, false otherwise
	 */
	@SuppressWarnings("unchecked")
	public boolean leftToRight(Object left, final Object right, final SyncFilter syncFilter) {
		
		if (right == null) {
			return false;
		}
		
		final Class<?> clazz = this.getRealClass(right);
		final Field[] fields = clazz.getDeclaredFields();
		
		try {
			if (left == null) {
				left = clazz.newInstance();
			}
		} catch (Exception ex) {
			Logger.err("Cannot create instance of " + String.valueOf(clazz), ex);
			return false;
		}
		
		boolean annotationsFound = false;
		for (final Field field : fields) {
			final Sync syncAnnotation = field.getAnnotation(Sync.class);
			if (syncAnnotation != null) {
				annotationsFound = true;
				String getterName = syncAnnotation.getterName();
				// does our field apply to the given synctype?
				if ((syncAnnotation.filter().length == 0) || this.contains(syncAnnotation.filter(), syncFilter)) {
					try {
						field.setAccessible(true);
						Object leftValue = this.get(field, left, clazz, getterName);
						Object rightValue = null;
						
						rightValue = this.get(field, right, clazz, getterName);
						if (this.preSyncCallback != null) {
							this.preSyncCallback.preSync(left, leftValue, right, rightValue, field.getName());
						}
						
						if (syncAnnotation.recurse()) {
							if ((leftValue == null) && (rightValue == null)) {
								continue;
							}
							
							if (rightValue == null) {
								rightValue = this.getClone(leftValue);
								this.set(field, right, clazz, rightValue);
							}
							if (leftValue == null) {
								leftValue = field.getType().newInstance();
								this.set(field, left, clazz, leftValue);
							}
							this.leftToRight(leftValue, rightValue, syncFilter);
						} else {
							if (leftValue instanceof Collection) {
								
								if (rightValue == null) {
									rightValue = leftValue.getClass().newInstance();
									this.set(field, right, clazz, rightValue);
								}
								
								List<Object> missingInRight = new ArrayList<Object>();
								// int leftIndex = 0;
								Iterator<?> leftIterator = ((Collection<?>) leftValue).iterator();
								
								// the following can not detect if the right
								// list has more items than the left one.
								// therefore no deletion of list entries is
								// taking place
								
								while (leftIterator.hasNext()) {
									Object leftItem = leftIterator.next();
									if (leftItem != null) {
										// int rightIndex = 0;
										Iterator<?> rightIterator = ((Collection<?>) rightValue).iterator();
										boolean foundInRightList = false;
										while (rightIterator.hasNext()) {
											Object rightItem = rightIterator.next();
											// assumes that the equals method is
											// implemented
											if (this.equals(leftItem, rightItem)) {
												foundInRightList = true;
												this.leftToRight(leftItem, rightItem, syncFilter);
												break;
											}
											// rightIndex++;
										}
										if (!foundInRightList) {
											missingInRight.add(leftItem);
										}
									}
									// leftIndex++;
								}
								
								// we are adding the items that where found in
								// the left but not in the right to the right
								// list
								for (Object o : missingInRight) {
									
									((Collection<Object>) rightValue).add(this.getClone(o));
								}
							} else if (leftValue instanceof Map<?, ?>) {
								System.err.println(this.getClass().getName() + ": Cannot handle Maps!");
							} else {
								this.set(field, right, clazz, leftValue);
							}
						}
						
						if (this.postSyncCallback != null) {
							this.postSyncCallback.postSync(left, leftValue, right, rightValue, field.getName());
						}
						
					} catch (Exception ex) {
						ex.printStackTrace();
					}
				}
				// } else {
				// System.out.println(field.getName() +
				// " does not apply for synctype: " + syncEntity);
			}
		}
		return annotationsFound;
	}
	
	public Class<?> getRealClass(final Object obj) {
		Class<?> clazz;
		if (obj.getClass().getName().indexOf("$$_javassist") >= 0) {
			clazz = obj.getClass().getSuperclass();
		} else {
			clazz = obj.getClass();
		}
		return clazz;
	}
	
	/** call the getter **/
	private Object get(final Field field, final Object obj, final Class<?> objType, String getterName) {
		if (getterName.isEmpty()) {
			getterName = this.getterName(field);
		}
		Method method = null;
		try {
			method = objType.getMethod(getterName);
			return method.invoke(obj);
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		return null;
	}
	
	private boolean equals(final Object leftItem, final Object rightItem) {
		if ((leftItem instanceof SyncIdentifiable) && (rightItem instanceof SyncIdentifiable)) {
			// preferred: the object brings its own syncID
			return ((SyncIdentifiable<?>) leftItem).getSyncId().equals(((SyncIdentifiable<?>) rightItem).getSyncId());
		} else {
			if (this.equalsCallback == null) {
				// not so good but sometimes good enough
				return leftItem.equals(rightItem);
			} else {
				// not that bad. we can make a good logic here
				return this.equalsCallback.equals(leftItem, rightItem);
			}
		}
	}
	
	/** call the setter **/
	private void set(final Field field, final Object object, final Class<?> objType, final Object value) throws IllegalArgumentException, IllegalAccessException, InvocationTargetException, SecurityException, NoSuchMethodException {
		Method method = null;
		try {
			method = objType.getMethod(this.setterName(field), field.getType());
		} catch (NoSuchMethodException nsm) {
			// try again with wrapped type
			method = objType.getMethod(this.setterName(field), this.wrappedClass(field.getType()));
		}
		method.invoke(object, value);
	}
	
	private Class<?> wrappedClass(final Class<?> clazz) {
		if (Boolean.class.equals(clazz)) {
			return boolean.class;
		} else if (Character.class.equals(clazz)) {
			return char.class;
		} else if (Byte.class.equals(clazz)) {
			return byte.class;
		} else if (Short.class.equals(clazz)) {
			return short.class;
		} else if (Integer.class.equals(clazz)) {
			return int.class;
		} else if (Long.class.equals(clazz)) {
			return long.class;
		} else if (Float.class.equals(clazz)) {
			return float.class;
		} else if (Double.class.equals(clazz)) {
			return double.class;
		} else if (Void.class.equals(clazz)) {
			return void.class;
		}
		return clazz;
	}
	
	private boolean contains(final SyncFilter[] entities, final SyncFilter syncEntity) {
		if (entities == null) {
			return false;
		}
		for (SyncFilter entity : entities) {
			if (entity.equals(syncEntity)) {
				return true;
			}
		}
		return false;
	}
	
	private String setterName(final Field field) {
		String name = field.getName();
		return Synchronizer.PREFIX_SETTER + name.substring(0, 1).toUpperCase() + name.substring(1);
	}
	
	private String getterName(final Field field) {
		String name = field.getName();
		String prefix = Synchronizer.PREFIX_GETTER;
		if (field.getType().equals(Boolean.class) || field.getType().equals(boolean.class)) {
			prefix = Synchronizer.PREFIX_GETTER_BOOLEAN;
		}
		return prefix + name.substring(0, 1).toUpperCase() + name.substring(1);
	}
	
	private Object getClone(final Object o) {
		Object clone = null;
		if (this.cloneCallback != null) {
			clone = this.cloneCallback.clone(o);
		}
		
		if (clone == null) {
			if (o instanceof Cloneable) {
				try {
					Method cloneMethod = o.getClass().getMethod(Synchronizer.CLONE_METHOD_NAME);
					clone = cloneMethod.invoke(o);
				} catch (Exception e) {
					Logger.err("Error while calling the clone method", e);
					clone = null;
				}
			} else {
				clone = o;
			}
		}
		return clone;
	}
	
	
	private static class TestClassB implements Cloneable {
		
		private String id = UUID.randomUUID().toString();
		
		@Sync
		private String a;
		
		
		/**
		 * @return the id
		 */
		public String getId() {
			return this.id;
		}
		
		/**
		 * @param id the id to set
		 */
		public void setId(final String id) {
			this.id = id;
		}
		
		/**
		 * @param a the a to set
		 */
		public void setA(final String a) {
			this.a = a;
		}
		
		/**
		 * @return the a
		 */
		public String getA() {
			return this.a;
		}
		
		@Override
		public TestClassB clone() {
			try {
				TestClassB clone = (TestClassB) super.clone();
				return clone;
			} catch (CloneNotSupportedException e) {
				e.printStackTrace();
			}
			return null;
		}
		
		/*
		 * (non-Javadoc)
		 * 
		 * @see java.lang.Object#equals(java.lang.Object)
		 */
		@Override
		public boolean equals(final Object obj) {
			return this.getId().equals(((TestClassB) obj).getId());
		}
		
		/*
		 * (non-Javadoc)
		 * 
		 * @see java.lang.Object#hashCode()
		 */
		@Override
		public int hashCode() {
			return this.getId().hashCode();
		}
		
	}
	
	private static class TestClass {
		
		@Sync(filter = SyncFilter.PARTICIPANT)
		private String a;
		
		@Sync(filter = {SyncFilter.PARTICIPANT, SyncFilter.SYSTEMUSER})
		private int b;
		
		@Sync(filter = SyncFilter.SYSTEMUSER)
		private Boolean c;
		
		@Sync
		private List<TestClassB> d;
		
		@Sync
		private List<String> strings;
		
		@Sync(recurse = true)
		private TestClassB e;
		
		
		/**
		 * @return the a
		 */
		public String getA() {
			return this.a;
		}
		
		/**
		 * @param a the a to set
		 */
		public void setA(final String a) {
			this.a = a;
		}
		
		/**
		 * @return the b
		 */
		public int getB() {
			return this.b;
		}
		
		/**
		 * @param b the b to set
		 */
		public void setB(final int b) {
			this.b = b;
		}
		
		/**
		 * @return the c
		 */
		public Boolean isC() {
			return this.c;
		}
		
		/**
		 * @param c the c to set
		 */
		public void setC(final Boolean c) {
			this.c = c;
		}
		
		/**
		 * @return the d
		 */
		public List<TestClassB> getD() {
			return this.d;
		}
		
		/**
		 * @param d the d to set
		 */
		public void setD(final List<TestClassB> d) {
			this.d = d;
		}
		
		/**
		 * @return the e
		 */
		public TestClassB getE() {
			return this.e;
		}
		
		/**
		 * @param e the e to set
		 */
		public void setE(final TestClassB e) {
			this.e = e;
		}
		
		/**
		 * @return the strings
		 */
		public List<String> getStrings() {
			return this.strings;
		}
		
		/**
		 * @param strings the strings to set
		 */
		public void setStrings(final List<String> strings) {
			this.strings = strings;
		}
		
	}
	
	
	public static void main(final String[] args) {
		
		TestClass a = new TestClass();
		a.setA("Hallo");
		a.setB(123);
		a.setC(true);
		TestClassB ab1 = new TestClassB();
		ab1.setA("ahhoiohi");
		ab1.setId("id1");
		TestClassB ab2 = new TestClassB();
		ab2.setA("sfjsfdkj");
		ab2.setId("id2");
		
		List<TestClassB> tbs = new ArrayList<Synchronizer.TestClassB>();
		tbs.add(ab1);
		tbs.add(ab2);
		a.setD(tbs);
		TestClassB testClassB = new TestClassB();
		testClassB.setA("sglojsdfglkj");
		a.setE(testClassB);
		List<String> stringsA = new ArrayList<String>();
		stringsA.add("a1");
		stringsA.add("a2");
		stringsA.add("a3");
		stringsA.add("a4");
		stringsA.add("a5");
		stringsA.add("a6");
		a.setStrings(stringsA);
		
		TestClass b = new TestClass();
		b.setA("Hallo was geht");
		b.setB(321);
		b.setC(false);
		
		TestClassB bb1 = new TestClassB();
		bb1.setA("dummdiodumm");
		bb1.setId("id2");
		
		List<TestClassB> bb1List = new ArrayList<Synchronizer.TestClassB>();
		bb1List.add(bb1);
		b.setD(bb1List);
		
		List<String> stringsB = new ArrayList<String>();
		stringsB.add("a7");
		stringsB.add("a4");
		stringsB.add("a5");
		stringsB.add("a6");
		stringsB.add("a8");
		stringsB.add("a9");
		b.setStrings(stringsB);
		
		System.out.println("--vorher");
		Synchronizer.print(a);
		Synchronizer.print(b);
		
		Synchronizer syncronizer = new Synchronizer();
		syncronizer.leftToRight(a, b, SyncFilter.SYSTEMUSER);
		
		System.out.println("\n--nachher");
		Synchronizer.print(a);
		Synchronizer.print(b);
		
	}
	
	private static void print(final TestClass a) {
		System.out.println(a.getA() + " " + a.getB() + " " + a.isC() + " " + String.valueOf(a.getD() + " " + String.valueOf(a.getE().getA()) + " " + String.valueOf(a.getE()) + " " + String.valueOf(a.getStrings())));
	}
	
}
